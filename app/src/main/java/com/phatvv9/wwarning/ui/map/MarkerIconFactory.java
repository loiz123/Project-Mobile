package com.phatvv9.wwarning.ui.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Vẽ icon marker tùy chỉnh:
 * - 1 báo cáo: một ô tròn góc có emoji của hiện tượng.
 * - Cụm nhiều báo cáo: ô dài chứa emoji của 2 hiện tượng đứng đầu bảng xếp hạng
 *   (to hơn cho hạng 1) và một huy hiệu tròn ghi tổng số báo cáo trong cụm.
 */
public final class MarkerIconFactory {

    private static final Map<String, Bitmap> CACHE = new HashMap<>();
    private static final int CACHE_LIMIT = 60;

    private MarkerIconFactory() {}

    public static int getCategoryColor(@Nullable String categoryId) {
        if (categoryId == null) return 0xFFD32F2F;
        switch (categoryId) {
            case "cat_nang":
            case "Sunny":
                return 0xFFFB8C00;
            case "cat_mua":
            case "Rain":
                return 0xFF1E88E5;
            case "cat_suong":
            case "Fog":
                return 0xFF00ACC1;
            case "cat_cay":
            case "FallenTree":
                return 0xFF7E57C2;
            default:
                return 0xFFD32F2F;
        }
    }

    /**
     * @param emoji1 emoji hiện tượng hạng 1
     * @param emoji2 emoji hạng 2, null nếu chỉ có một hiện tượng
     * @param count  tổng số báo cáo trong cụm; chỉ vẽ huy hiệu khi > 1
     * @param color  màu viền và huy hiệu
     */
    @NonNull
    public static BitmapDescriptor create(float density, @NonNull String emoji1,
                                          @Nullable String emoji2, int count, int color) {
        String key = emoji1 + "|" + emoji2 + "|" + count + "|" + color + "|" + density;
        Bitmap bmp = CACHE.get(key);
        if (bmp == null) {
            bmp = draw(density, emoji1, emoji2, count, color);
            if (CACHE.size() >= CACHE_LIMIT) CACHE.clear();
            CACHE.put(key, bmp);
        }
        return BitmapDescriptorFactory.fromBitmap(bmp);
    }

    private static Bitmap draw(float d, String emoji1, String emoji2, int count, int color) {
        boolean two = emoji2 != null;
        boolean badge = count > 1;

        float pad = 8 * d;                       // chừa chỗ cho huy hiệu
        float bodyW = (two ? 78 : 46) * d;
        float bodyH = 42 * d;
        float pointerH = 9 * d;
        int w = (int) Math.ceil(bodyW + 2 * pad);
        int h = (int) Math.ceil(pad + bodyH + pointerH);

        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        RectF body = new RectF(pad, pad, pad + bodyW, pad + bodyH);
        float radius = 14 * d;

        // Thân marker: nền trắng, viền màu
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(0xFFFFFFFF);
        canvas.drawRoundRect(body, radius, radius, fill);

        // Mũi nhọn chỉ xuống vị trí thật
        Path tip = new Path();
        float cx = body.centerX();
        tip.moveTo(cx - 7 * d, body.bottom - 1 * d);
        tip.lineTo(cx, body.bottom + pointerH);
        tip.lineTo(cx + 7 * d, body.bottom - 1 * d);
        tip.close();
        Paint tipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tipPaint.setColor(color);
        canvas.drawPath(tip, tipPaint);

        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.5f * d);
        stroke.setColor(color);
        canvas.drawRoundRect(body, radius, radius, stroke);

        // Emoji
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setTextAlign(Paint.Align.CENTER);
        if (two) {
            drawEmoji(canvas, text, emoji1, body.left + bodyW * 0.29f, body.centerY(), 25 * d);
            drawEmoji(canvas, text, emoji2, body.left + bodyW * 0.73f, body.centerY(), 18 * d);
        } else {
            drawEmoji(canvas, text, emoji1, body.centerX(), body.centerY(), 25 * d);
        }

        // Huy hiệu số báo cáo
        if (badge) {
            float br = 9.5f * d;
            float bx = body.right - 2 * d;
            float by = body.top + 2 * d;
            Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
            bg.setColor(color);
            canvas.drawCircle(bx, by, br, bg);
            Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(1.5f * d);
            ring.setColor(0xFFFFFFFF);
            canvas.drawCircle(bx, by, br, ring);

            Paint num = new Paint(Paint.ANTI_ALIAS_FLAG);
            num.setColor(0xFFFFFFFF);
            num.setTypeface(Typeface.DEFAULT_BOLD);
            num.setTextAlign(Paint.Align.CENTER);
            num.setTextSize(11 * d);
            Paint.FontMetrics fm = num.getFontMetrics();
            String label = count > 99 ? "99+" : String.valueOf(count);
            canvas.drawText(label, bx, by - (fm.ascent + fm.descent) / 2f, num);
        }
        return bitmap;
    }

    private static void drawEmoji(Canvas canvas, Paint paint, String emoji,
                                  float cx, float cy, float size) {
        paint.setTextSize(size);
        Paint.FontMetrics fm = paint.getFontMetrics();
        canvas.drawText(emoji, cx, cy - (fm.ascent + fm.descent) / 2f, paint);
    }
}
