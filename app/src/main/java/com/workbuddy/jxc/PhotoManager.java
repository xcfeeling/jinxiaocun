package com.workbuddy.jxc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public final class PhotoManager {
    public static final String AUTHORITY = "com.workbuddy.jxc.photos";

    private PhotoManager() {
    }

    public static File directory(Context context) {
        File base = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (base == null) {
            base = new File(context.getFilesDir(), "pictures");
        }
        if (!base.exists()) {
            base.mkdirs();
        }
        return base;
    }

    public static Uri createCameraPhoto(Context context) {
        File file = new File(directory(context), UUID.randomUUID().toString() + ".jpg");
        return Uri.parse("content://" + AUTHORITY + "/" + file.getName());
    }

    public static String pathFromUri(Context context, Uri uri) {
        return new File(directory(context), uri.getLastPathSegment()).getAbsolutePath();
    }

    public static String copyFromUri(Context context, Uri source) throws IOException {
        File target = new File(directory(context), UUID.randomUUID().toString() + ".jpg");
        try (InputStream input = context.getContentResolver().openInputStream(source);
             FileOutputStream output = new FileOutputStream(target)) {
            if (input == null) {
                throw new IOException("无法读取图片");
            }
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
        }
        return target.getAbsolutePath();
    }

    public static Bitmap thumbnail(String path, int targetSize) {
        if (path == null || path.isEmpty() || !new File(path).exists()) {
            return null;
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);
        int sample = 1;
        while (bounds.outWidth / sample > targetSize * 2 || bounds.outHeight / sample > targetSize * 2) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        return BitmapFactory.decodeFile(path, options);
    }
}
