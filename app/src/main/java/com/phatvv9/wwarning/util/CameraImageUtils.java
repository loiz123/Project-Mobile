package com.phatvv9.wwarning.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Lưu ảnh xem trước từ camera vào cache để tái sử dụng luồng upload ảnh hiện có. */
public final class CameraImageUtils {
    private CameraImageUtils() {}
    public static Uri savePreview(Context context, Bitmap bitmap) throws IOException {
        File file = File.createTempFile("wwarning_camera_", ".jpg", context.getCacheDir());
        try (FileOutputStream out = new FileOutputStream(file)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)) {
                throw new IOException("Không thể lưu ảnh camera");
            }
            out.flush();
        }
        return Uri.fromFile(file);
    }
}
