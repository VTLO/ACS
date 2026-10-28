package com.vtlo.offmp3g;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/** Background textures: the analyser's black grid and the scanner's diagonal stripes. */
final class Panels {

    private Panels() {}

    /** Black instrument panel with a fine grid (command line, log). */
    static Drawable scope(Context context) {
        float d = context.getResources().getDisplayMetrics().density;
        int bg = ContextCompat.getColor(context, R.color.scope_bg);
        int line = ContextCompat.getColor(context, R.color.scope_grid);
        return new Textured(16 * d, bg, (canvas, b, paint) -> {
            paint.setColor(line);
            paint.setStrokeWidth(1);
            float step = 18 * d;
            for (float x = b.left + step; x < b.right; x += step) canvas.drawLine(x, b.top, x, b.bottom, paint);
            for (float y = b.top + step; y < b.bottom; y += step) canvas.drawLine(b.left, y, b.right, y, paint);
        });
    }

    /** Card with faint diagonal stripes (status gauge). */
    static Drawable stripes(Context context) {
        float d = context.getResources().getDisplayMetrics().density;
        int bg = ContextCompat.getColor(context, R.color.surface);
        return new Textured(28 * d, bg, (canvas, b, paint) -> {
            paint.setColor(0x0DFFFFFF);
            paint.setStrokeWidth(26 * d);
            float step = 64 * d;
            for (float x = b.left - b.height(); x < b.right; x += step) {
                canvas.drawLine(x, b.bottom, x + b.height(), b.top, paint);
            }
        });
    }

    private interface Pattern {
        void draw(Canvas canvas, RectF bounds, Paint paint);
    }

    private static final class Textured extends Drawable {
        private final float radius;
        private final int background;
        private final Pattern pattern;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path clip = new Path();
        private final RectF rect = new RectF();

        Textured(float radius, int background, Pattern pattern) {
            this.radius = radius;
            this.background = background;
            this.pattern = pattern;
        }

        @Override
        protected void onBoundsChange(@NonNull Rect bounds) {
            rect.set(bounds);
            clip.reset();
            clip.addRoundRect(rect, radius, radius, Path.Direction.CW);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            canvas.save();
            canvas.clipPath(clip);
            canvas.drawColor(background);
            pattern.draw(canvas, rect, paint);
            canvas.restore();
        }

        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(ColorFilter colorFilter) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
