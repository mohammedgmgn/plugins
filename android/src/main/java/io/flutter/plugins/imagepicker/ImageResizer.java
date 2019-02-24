// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.imagepicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

class ImageResizer {
    private final File externalFilesDirectory;
    private final ExifDataCopier exifDataCopier;

    ImageResizer(File externalFilesDirectory, ExifDataCopier exifDataCopier) {
        this.externalFilesDirectory = externalFilesDirectory;
        this.exifDataCopier = exifDataCopier;
    }

    /**
     * If necessary, resizes the image located in imagePath and then returns the path for the scaled
     * image.
     *
     * <p>If no resizing is needed, returns the path for the original image.
     */
    String resizeImageIfNeeded(String imagePath, Double maxWidth, Double maxHeight,
                               boolean shouldCompress,Context context,Uri uri) {
        boolean shouldScale = maxWidth != null || maxHeight != null;

        if(shouldCompress){
            try {
                File scaledImage = compressImage(imagePath,context,uri);
                exifDataCopier.copyExif(imagePath, scaledImage.getPath());
                return scaledImage.getPath();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        if (shouldScale) {
            try {
                File scaledImage = resizedImage(imagePath, maxWidth, maxHeight);
                exifDataCopier.copyExif(imagePath, scaledImage.getPath());
                return scaledImage.getPath();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return imagePath;

    }
    private File compressImage(String path, Context context, Uri uri) throws IOException {
        Bitmap bitmap= MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
       int sizeBefore= bitmap.getByteCount()/(1024*1024);
        Log.i("sizeBefore", sizeBefore+"");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        boolean isCompressed=   bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        Log.i("outputStream",outputStream.size()/(1024*1024)+"");
        Log.i("isCompressed", isCompressed+"");
        String[] pathParts = path.split("/");
        String imageName = pathParts[pathParts.length - 1];
        File imageFile = new File(externalFilesDirectory, "/scaled_" + imageName);
        FileOutputStream fileOutput = new FileOutputStream(imageFile);
        fileOutput.write(outputStream.toByteArray());
        fileOutput.close();

/*
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmapAfter = BitmapFactory.decodeFile(imageFile.getPath(), options);
        int sizeAfter= bitmapAfter.getByteCount()/(1024*1024);
        Log.i("sizeAfter", sizeAfter+"");
*/


        return imageFile;


    }


    private File resizedImage(String path, Double maxWidth, Double maxHeight) throws IOException {
        Bitmap bmp = BitmapFactory.decodeFile(path);
        double originalWidth = bmp.getWidth() * 1.0;
        double originalHeight = bmp.getHeight() * 1.0;

        boolean hasMaxWidth = maxWidth != null;
        boolean hasMaxHeight = maxHeight != null;

        Double width = hasMaxWidth ? Math.min(originalWidth, maxWidth) : originalWidth;
        Double height = hasMaxHeight ? Math.min(originalHeight, maxHeight) : originalHeight;

        boolean shouldDownscaleWidth = hasMaxWidth && maxWidth < originalWidth;
        boolean shouldDownscaleHeight = hasMaxHeight && maxHeight < originalHeight;
        boolean shouldDownscale = shouldDownscaleWidth || shouldDownscaleHeight;

        if (shouldDownscale) {
            double downscaledWidth = (height / originalHeight) * originalWidth;
            double downscaledHeight = (width / originalWidth) * originalHeight;

            if (width < height) {
                if (!hasMaxWidth) {
                    width = downscaledWidth;
                } else {
                    height = downscaledHeight;
                }
            } else if (height < width) {
                if (!hasMaxHeight) {
                    height = downscaledHeight;
                } else {
                    width = downscaledWidth;
                }
            } else {
                if (originalWidth < originalHeight) {
                    width = downscaledWidth;
                } else if (originalHeight < originalWidth) {
                    height = downscaledHeight;
                }
            }
        }

        Bitmap scaledBmp = Bitmap.createScaledBitmap(bmp, width.intValue(), height.intValue(), false);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        scaledBmp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

        String[] pathParts = path.split("/");
        String imageName = pathParts[pathParts.length - 1];

        File imageFile = new File(externalFilesDirectory, "/scaled_" + imageName);
        FileOutputStream fileOutput = new FileOutputStream(imageFile);
        fileOutput.write(outputStream.toByteArray());
        fileOutput.close();

        return imageFile;
    }
}
