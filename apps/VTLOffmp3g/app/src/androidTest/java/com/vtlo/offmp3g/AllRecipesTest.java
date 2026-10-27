package com.vtlo.offmp3g;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Runs every cheatsheet recipe on the device through the app's real job pipeline. */
@RunWith(AndroidJUnit4.class)
public class AllRecipesTest {

    private static Context context;
    private static File media;

    @BeforeClass
    public static void copyMedia() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        media = new File(context.getCacheDir(), "jobs/test-media");
        media.mkdirs();
        // 2 s 320x240 H.264/AAC clip, MP3, PNG logo and SRT subtitles (see androidTest/assets).
        android.content.res.AssetManager assets = InstrumentationRegistry.getInstrumentation().getContext().getAssets();
        for (String name : new String[]{"input.mp4", "audio.mp3", "logo.png", "subs.srt"}) {
            try (java.io.InputStream in = assets.open(name);
                 java.io.OutputStream out = new java.io.FileOutputStream(new File(media, name))) {
                Job.copy(in, out);
            }
        }
    }

    private static String path(String name) {
        return new File(media, name).getAbsolutePath();
    }

    private static Uri uri(String name) {
        return FileProvider.getUriForFile(context, context.getPackageName() + ".files", new File(media, name));
    }

    @Test
    public void everyRecipeProducesOutput() throws Exception {
        // Foreground the app, as when a user taps Run (needed to start the foreground service).
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            List<String> failures = new ArrayList<>();
            for (Recipe r : Recipe.ALL) {
                String error = run(r);
                android.util.Log.i("AllRecipesTest", r.id + ": " + (error == null ? "OK" : error));
                if (error != null) failures.add(r.id + ": " + error);
            }
            assertEquals(String.join("\n\n", failures), 0, failures.size());
        }
    }

    private String run(Recipe r) throws Exception {
        Map<String, String> values = r.defaultParams();
        // Fit the defaults to the 2 s, 320x240 test clip.
        override(values, "start", "00:00:00.5");
        override(values, "duration", "00:00:01");
        override(values, "time", "00:00:01");
        override(values, "w", "160");
        override(values, "h", "120");
        override(values, "x", "10");
        override(values, "y", "10");
        override(values, "width", "160");
        override(values, "height", "120");
        override(values, "preset", "ultrafast");
        override(values, "url", path("stream.flv"));
        if (r.id.equals("to_webm")) values.put("vbitrate", "200k");

        Map<String, List<Uri>> picks = new HashMap<>();
        for (Recipe.Input in : r.inputs) {
            switch (in.key) {
                case "input": picks.put("input", Arrays.asList(uri("input.mp4"))); break;
                case "videos": picks.put("videos", Arrays.asList(uri("input.mp4"), uri("input.mp4"))); break;
                case "audio": picks.put("audio", Arrays.asList(uri("audio.mp3"))); break;
                case "logo": picks.put("logo", Arrays.asList(uri("logo.png"))); break;
                case "subs": picks.put("subs", Arrays.asList(uri("subs.srt"))); break;
                default: return "no test file for input " + in.key;
            }
        }
        String ext = Recipe.applyParams(r.outputExt, values);
        String name = r.outputKind == Recipe.OutputKind.DIRECTORY ? "out_frames" : "out." + ext;

        Job[] job = new Job[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
            job[0] = Job.start(context, r, picks, r.buildCommand(values), name));
        long deadline = System.currentTimeMillis() + 10 * 60_000;
        while (job[0].state() == Job.State.RUNNING) {
            if (System.currentTimeMillis() > deadline) {
                job[0].cancel();
                return "timed out";
            }
            Thread.sleep(250);
        }
        if (job[0].state() != Job.State.DONE) {
            String log = job[0].log();
            return job[0].status() + "\n" + log.substring(Math.max(0, log.length() - 1500));
        }
        if (r.outputKind == Recipe.OutputKind.NONE) {
            File stream = new File(media, "stream.flv");
            return stream.length() > 0 ? null : "no stream output";
        }
        File out = job[0].output;
        if (out.isDirectory()) {
            String[] files = out.list();
            return files != null && files.length > 0 ? null : "no frames";
        }
        return out.length() > 0 ? null : "empty output";
    }

    private static void override(Map<String, String> values, String key, String value) {
        if (values.containsKey(key)) values.put(key, value);
    }
}
