package com.vtlo.offmp3g;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Spectrum-analyser style telemetry: encoding fps (yellow, left axis) and output bitrate
 * (magenta, right axis) against processed media time, with a teal cursor on the latest sample.
 */
public class ScopeView extends View {

    private final Paint grid = new Paint();
    private final Paint axis = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fpsLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rateLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cursor = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cursorText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float density;
    private List<float[]> samples = new ArrayList<>();

    public ScopeView(Context context) {
        this(context, null);
    }

    public ScopeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        grid.setColor(ContextCompat.getColor(context, R.color.scope_grid));
        grid.setStrokeWidth(1);
        axis.setColor(ContextCompat.getColor(context, R.color.scope_axis));
        axis.setTypeface(Typeface.MONOSPACE);
        axis.setTextSize(10 * density);
        for (Paint p : new Paint[]{fpsLine, rateLine}) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.6f * density);
            p.setStrokeJoin(Paint.Join.ROUND);
        }
        fpsLine.setColor(ContextCompat.getColor(context, R.color.scope_yellow));
        rateLine.setColor(ContextCompat.getColor(context, R.color.scope_magenta));
        cursor.setColor(ContextCompat.getColor(context, R.color.scope_teal));
        cursor.setStrokeWidth(density);
        cursor.setPathEffect(new DashPathEffect(new float[]{4 * density, 3 * density}, 0));
        cursorText.setColor(ContextCompat.getColor(context, R.color.scope_teal));
        cursorText.setTypeface(Typeface.MONOSPACE);
        cursorText.setTextSize(11 * density);
    }

    public void setSamples(List<float[]> data) {
        samples = data;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(ContextCompat.getColor(getContext(), R.color.scope_bg));
        float padL = 34 * density, padR = 40 * density, padT = 10 * density, padB = 18 * density;
        float w = getWidth() - padL - padR, h = getHeight() - padT - padB;
        if (w <= 0 || h <= 0) return;

        // Grid: fine lines, every 4th brighter, like the analyser.
        int cols = 16, rows = 8;
        for (int i = 0; i <= cols; i++) {
            grid.setAlpha(i % 4 == 0 ? 255 : 110);
            float x = padL + w * i / cols;
            canvas.drawLine(x, padT, x, padT + h, grid);
        }
        for (int i = 0; i <= rows; i++) {
            grid.setAlpha(i % 2 == 0 ? 255 : 110);
            float y = padT + h * i / rows;
            canvas.drawLine(padL, y, padL + w, y, grid);
        }

        if (samples.size() < 2) {
            axis.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("NO SIGNAL", padL + w / 2, padT + h / 2, axis);
            return;
        }

        float maxT = 0, maxFps = 1, maxRate = 1;
        for (float[] s : samples) {
            maxT = Math.max(maxT, s[0]);
            maxFps = Math.max(maxFps, s[1]);
            maxRate = Math.max(maxRate, s[2]);
        }
        maxFps = niceCeil(maxFps);
        maxRate = niceCeil(maxRate);
        float minT = samples.get(0)[0];
        float spanT = Math.max(0.001f, maxT - minT);

        // Axis labels: fps on the left (yellow-ish grey), kb/s on the right, time below.
        axis.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= 4; i++) {
            float y = padT + h - h * i / 4 + 4 * density;
            canvas.drawText(compact(maxFps * i / 4), padL - 5 * density, y, axis);
        }
        axis.setTextAlign(Paint.Align.LEFT);
        for (int i = 0; i <= 4; i++) {
            float y = padT + h - h * i / 4 + 4 * density;
            canvas.drawText(compact(maxRate * i / 4), padL + w + 5 * density, y, axis);
        }
        axis.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i <= 4; i++) {
            float x = padL + w * i / 4;
            canvas.drawText(clock(minT + spanT * i / 4), x, getHeight() - 4 * density, axis);
        }

        drawSeries(canvas, 2, maxRate, minT, spanT, padL, padT, w, h, rateLine);
        drawSeries(canvas, 1, maxFps, minT, spanT, padL, padT, w, h, fpsLine);

        // Cursor on the newest sample with a readout, like the analyser's marker.
        float[] last = samples.get(samples.size() - 1);
        float cx = padL + w * (last[0] - minT) / spanT;
        float cy = padT + h - h * Math.min(1, last[1] / maxFps);
        canvas.drawLine(cx, padT, cx, padT + h, cursor);
        canvas.drawLine(padL, cy, padL + w, cy, cursor);
        String readout = String.format(Locale.US, "%.1f fps  %s kb/s", last[1], compact(last[2]));
        cursorText.setTextAlign(cx > padL + w / 2 ? Paint.Align.RIGHT : Paint.Align.LEFT);
        float tx = cx > padL + w / 2 ? cx - 6 * density : cx + 6 * density;
        float ty = Math.max(padT + 12 * density, cy - 6 * density);
        canvas.drawText(readout, tx, ty, cursorText);
    }

    private void drawSeries(Canvas canvas, int index, float max, float minT, float spanT,
                            float padL, float padT, float w, float h, Paint paint) {
        path.reset();
        boolean first = true;
        for (float[] s : samples) {
            float x = padL + w * (s[0] - minT) / spanT;
            float y = padT + h - h * Math.min(1, s[index] / max);
            if (first) path.moveTo(x, y); else path.lineTo(x, y);
            first = false;
        }
        canvas.drawPath(path, paint);
    }

    private static float niceCeil(float v) {
        double exp = Math.pow(10, Math.floor(Math.log10(v)));
        for (double step : new double[]{1, 2, 2.5, 5, 10}) {
            if (step * exp >= v) return (float) (step * exp);
        }
        return (float) (10 * exp);
    }

    private static String compact(float v) {
        if (v >= 10_000) return String.format(Locale.US, "%.0fk", v / 1000);
        if (v >= 1000) return String.format(Locale.US, "%.1fk", v / 1000);
        if (v >= 10 || v == 0) return String.format(Locale.US, "%.0f", v);
        return String.format(Locale.US, "%.1f", v);
    }

    private static String clock(float seconds) {
        int s = Math.round(seconds);
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }
}
