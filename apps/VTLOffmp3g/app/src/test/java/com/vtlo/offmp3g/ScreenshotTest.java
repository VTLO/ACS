package com.vtlo.offmp3g;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders the screens with Android's real graphics stack (Robolectric native graphics) to
 * build/screenshots, for reviewing the UI without a device. Job states are set up through
 * reflection so no test hooks live in the app.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w411dp-h891dp-xxhdpi")
public class ScreenshotTest {

    private static final File OUT = new File("build/screenshots");

    @After
    public void clearJob() throws Exception {
        setStatic("current", null);
    }

    @Test
    public void home() throws Exception {
        capture(Robolectric.buildActivity(MainActivity.class).setup().get(), "1-home");
    }

    @Test
    public void homeWhileEncoding() throws Exception {
        fakeJob("to_webm", Job.State.RUNNING, 0.42, "Running");
        capture(Robolectric.buildActivity(MainActivity.class).setup().get(), "2-home-encoding");
    }

    @Test
    public void recipeIdle() throws Exception {
        capture(recipe("crf"), "3-recipe-idle");
    }

    @Test
    public void recipeEncoding() throws Exception {
        fakeJob("crf", Job.State.RUNNING, 0.63, "Running");
        capture(recipe("crf"), "4-recipe-encoding");
    }

    @Test
    public void recipeDone() throws Exception {
        fakeJob("gif", Job.State.DONE, 1, "Done");
        capture(recipe("gif"), "5-recipe-done");
    }

    @Test
    public void recipeFailed() throws Exception {
        fakeJob("subtitles", Job.State.FAILED, 0.18, "Failed (exit code 1) — see log");
        capture(recipe("subtitles"), "6-recipe-failed");
    }

    // ------------------------------------------------------------------------------------------

    private Activity recipe(String id) {
        Intent i = new Intent(ApplicationProvider.getApplicationContext(), RecipeActivity.class)
            .putExtra(RecipeActivity.EXTRA_RECIPE, id);
        return Robolectric.buildActivity(RecipeActivity.class, i).setup().get();
    }

    private static void fakeJob(String recipeId, Job.State state, double progress, String status) throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Constructor<Job> ctor = Job.class.getDeclaredConstructor(Context.class, Recipe.class, Map.class,
            String.class, String.class);
        ctor.setAccessible(true);
        Recipe r = Recipe.byId(recipeId);
        Job job = ctor.newInstance(context, r, new HashMap<String, List<android.net.Uri>>(),
            r.buildCommand(r.defaultParams()), "clip_" + recipeId + "." + MainActivity.badge(r).toLowerCase());
        set(job, "durationMs", 96_000d);
        set(job, "timeMs", 96_000d * progress);
        set(job, "speed", 1.84d);
        set(job, "outputBytes", 23_400_000L);
        set(job, "status", status);
        Method sample = Job.class.getDeclaredMethod("addSample", float.class, float.class, float.class);
        sample.setAccessible(true);
        for (int i = 0; i <= 120 * progress; i++) {
            float t = i * 0.8f;
            float fps = 46 + 9 * (float) Math.sin(i / 5.0) + (i % 7) * 1.3f - (i < 6 ? (6 - i) * 7 : 0);
            float kbps = 2100 + 650 * (float) Math.cos(i / 9.0) + (i % 5) * 90;
            sample.invoke(job, t, Math.max(0, fps), kbps);
        }
        Method log = Job.class.getDeclaredMethod("appendLog", String.class);
        log.setAccessible(true);
        log.invoke(job, "$ ffmpeg -i saf:41.mp4 -c:v libx264 -crf 23 -preset medium out.mp4\n\n"
            + "Input #0, mov,mp4,m4a,3gp,3g2,mj2, from 'saf:41.mp4':\n"
            + "  Duration: 00:01:36.00, start: 0.000000, bitrate: 6120 kb/s\n"
            + "  Stream #0:0(und): Video: h264 (High), yuv420p, 1920x1080, 30 fps\n"
            + "Stream mapping:\n  Stream #0:0 -> #0:0 (h264 (native) -> h264 (libx264))\n"
            + "frame= 1740 fps= 48 q=28.0 size=   22850kB time=00:00:58.00 bitrate=3227.5kbits/s speed=1.84x\n"
            + (state == Job.State.FAILED ? "[Parsed_subtitles_0] Unable to open subs.srt\nError initializing filters\n" : ""));
        if (state == Job.State.DONE) {
            File out = (File) get(job, "output");
            try (FileOutputStream f = new FileOutputStream(out)) {
                f.write(new byte[18_874]);
            }
        }
        set(job, "state", state);
        setStatic("current", job);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field f = Job.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Object get(Object target, String name) throws Exception {
        Field f = Job.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void setStatic(String name, Object value) throws Exception {
        set(null, name, value);
    }

    /** Draws the whole scrollable content (not just the visible window) to a PNG. */
    private static void capture(Activity activity, String name) throws Exception {
        ShadowLooper.idleMainLooper(2, java.util.concurrent.TimeUnit.SECONDS);
        View root = activity.getWindow().getDecorView();
        int width = root.getWidth() > 0 ? root.getWidth() : 1233;
        View content = activity.findViewById(R.id.scroll);
        View inner = ((ViewGroup) content).getChildAt(0);
        inner.measure(View.MeasureSpec.makeMeasureSpec(content.getWidth() > 0 ? content.getWidth() : width,
                View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        inner.layout(0, 0, inner.getMeasuredWidth(), inner.getMeasuredHeight());
        int extra = 0;
        View nav = activity.findViewById(R.id.nav);
        if (nav != null) extra = nav.getHeight();
        Bitmap bmp = Bitmap.createBitmap(inner.getMeasuredWidth(), inner.getMeasuredHeight() + extra, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(activity.getColor(R.color.bg));
        inner.draw(c);
        if (nav != null) {
            c.translate(0, inner.getMeasuredHeight());
            nav.draw(c);
        }
        OUT.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(OUT, name + ".png"))) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }
}
