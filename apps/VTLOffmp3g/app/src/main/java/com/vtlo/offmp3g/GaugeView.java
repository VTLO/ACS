package com.vtlo.offmp3g;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

/**
 * Arc gauge with round caps: a full ring (sweep 360, the battery-app look) or an open
 * three-quarter gauge (sweep 270, the scanner look). Progress -1 shows a moving segment.
 */
public class GaugeView extends View {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private float sweep = 360f;
    private float progress = 0f;
    private float shown = 0f;
    private float spin = 0f;
    private ValueAnimator spinner;
    private ValueAnimator easing;

    public GaugeView(Context context) {
        this(context, null);
    }

    public GaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float stroke = 10 * getResources().getDisplayMetrics().density;
        for (Paint p : new Paint[]{track, arc}) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeWidth(stroke);
        }
        track.setColor(ContextCompat.getColor(context, R.color.surface_high));
        arc.setColor(ContextCompat.getColor(context, R.color.green));
    }

    public void setSweep(float degrees) {
        sweep = degrees;
        invalidate();
    }

    public void setStrokeDp(float dp) {
        float px = dp * getResources().getDisplayMetrics().density;
        track.setStrokeWidth(px);
        arc.setStrokeWidth(px);
        invalidate();
    }

    public void setColors(@ColorInt int trackColor, @ColorInt int arcColor) {
        track.setColor(trackColor);
        arc.setColor(arcColor);
        invalidate();
    }

    /** 0..1, or -1 for "working, amount unknown". */
    public void setProgress(float value) {
        boolean indeterminate = value < 0;
        if (indeterminate) {
            progress = -1;
            startSpinner();
            invalidate();
            return;
        }
        stopSpinner();
        float target = Math.max(0, Math.min(1, value));
        if (target == progress) return;
        progress = target;
        if (easing != null) easing.cancel();
        easing = ValueAnimator.ofFloat(shown, target);
        easing.setDuration(350);
        easing.addUpdateListener(a -> {
            shown = (float) a.getAnimatedValue();
            invalidate();
        });
        easing.start();
    }

    private void startSpinner() {
        if (spinner != null) return;
        spinner = ValueAnimator.ofFloat(0, 1);
        spinner.setDuration(1400);
        spinner.setRepeatCount(ValueAnimator.INFINITE);
        spinner.setInterpolator(new LinearInterpolator());
        spinner.addUpdateListener(a -> {
            spin = (float) a.getAnimatedValue();
            invalidate();
        });
        spinner.start();
    }

    private void stopSpinner() {
        if (spinner != null) spinner.cancel();
        spinner = null;
    }

    @Override
    protected void onDetachedFromWindow() {
        stopSpinner();
        if (easing != null) easing.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float inset = track.getStrokeWidth() / 2 + 1;
        float size = Math.min(getWidth(), getHeight());
        float left = (getWidth() - size) / 2f, top = (getHeight() - size) / 2f;
        bounds.set(left + inset, top + inset, left + size - inset, top + size - inset);
        // An open gauge leaves its gap at the bottom.
        float start = sweep >= 360 ? -90 : 90 + (360 - sweep) / 2f;
        canvas.drawArc(bounds, start, sweep, false, track);
        if (progress < 0) {
            float segment = sweep * 0.22f;
            float offset = (sweep >= 360 ? sweep : sweep - segment) * spin;
            canvas.drawArc(bounds, start + offset, segment, false, arc);
        } else if (shown > 0) {
            canvas.drawArc(bounds, start, Math.max(1f, sweep * shown), false, arc);
        }
    }
}
