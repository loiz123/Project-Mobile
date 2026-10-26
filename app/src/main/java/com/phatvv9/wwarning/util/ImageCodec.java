package com.phatvv9.wwarning.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Nén ảnh để vừa trong 1 field Firestore (document tối đa 1 MiB, và mọi người dùng trong
 * bán kính 10 km đều tải field này cùng báo cáo, nên giữ thật nhỏ).
 */
public final class ImageCodec {
    public static final int MAX_SIDE_PX = 800;
    public static final int MAX_BYTES = 80 * 1024;   // ~107.000 ký tự Base64

    private ImageCodec() {}

    /** Đọc ảnh từ Uri, xoay đúng chiều, thu nhỏ và nén JPEG xuống tối đa MAX_BYTES. */
    @NonNull
    public static byte[] compressFromUri(@NonNull Context context, @NonNull Uri uri) throws IOException {
        ContentResolver cr = context.getContentResolver();

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = cr.openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("Không đọc được ảnh");
        }

        int sample = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE_PX) {
            sample *= 2;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bitmap;
        try (InputStream in = cr.openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bitmap == null) throw new IOException("Không giải mã được ảnh");

        bitmap = rotateByExif(cr, uri, bitmap);
        bitmap = scaleToMaxSide(bitmap, MAX_SIDE_PX);

        // Giảm chất lượng dần; nếu vẫn lớn thì thu nhỏ thêm rồi nén lại.
        for (int attempt = 0; attempt < 4; attempt++) {
            for (int quality = 75; quality >= 35; quality -= 10) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos);
                if (bos.size() <= MAX_BYTES) return bos.toByteArray();
            }
            bitmap = scaleToMaxSide(bitmap, (int) (Math.max(bitmap.getWidth(), bitmap.getHeight()) * 0.8));
        }
        throw new IOException("Ảnh quá lớn sau khi nén");
    }

    @Nullable
    public static Bitmap decode(@NonNull byte[] jpeg) {
        return BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
    }

    private static Bitmap scaleToMaxSide(Bitmap src, int maxSide) {
        int w = src.getWidth(), h = src.getHeight();
        int longest = Math.max(w, h);
        if (longest <= maxSide) return src;
        float ratio = (float) maxSide / longest;
        return Bitmap.createScaledBitmap(src, Math.max(1, Math.round(w * ratio)),
                Math.max(1, Math.round(h * ratio)), true);
    }

    private static Bitmap rotateByExif(ContentResolver cr, Uri uri, Bitmap bitmap) {
        try (InputStream in = cr.openInputStream(uri)) {
            if (in == null) return bitmap;
            int orientation = new ExifInterface(in).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int degrees;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90: degrees = 90; break;
                case ExifInterface.ORIENTATION_ROTATE_180: degrees = 180; break;
                case ExifInterface.ORIENTATION_ROTATE_270: degrees = 270; break;
                default: return bitmap;
            }
            Matrix m = new Matrix();
            m.postRotate(degrees);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
        } catch (IOException e) {
            return bitmap;
        }
    }
}
