package com.vtlo.offmp3g;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.TextView;

import androidx.core.content.FileProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/** Drives a recipe screen like a user: pick a file, tap Run, wait for the result. */
@RunWith(AndroidJUnit4.class)
public class RecipeScreenTest {

    @Test
    public void convertToMp4FromTheScreen() throws Exception {
        runFromScreen("to_mp4");
    }

    @Test
    public void webmWithBundledLibvpxPreset() throws Exception {
        runFromScreen("webm_vpre");
    }

    private void runFromScreen(String recipeId) throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Uri input = testVideo(context);
        Intent intent = new Intent(context, RecipeActivity.class).putExtra(RecipeActivity.EXTRA_RECIPE, recipeId);
        try (ActivityScenario<RecipeActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(a -> {
                a.pickForTest("input", Arrays.asList(input));
                assertTrue(a.findViewById(R.id.run).isEnabled());
                a.findViewById(R.id.run).performClick();
            });
            Job job = Job.current();
            assertNotNull("tapping Run did not start a job", job);
            long deadline = System.currentTimeMillis() + 10 * 60_000;
            while (job.state() == Job.State.RUNNING && System.currentTimeMillis() < deadline) {
                Thread.sleep(500);
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            String[] status = new String[1];
            scenario.onActivity(a -> status[0] = ((TextView) a.findViewById(R.id.status)).getText().toString());
            assertEquals(job.status() + "\n" + job.log(), Job.State.DONE, job.state());
            assertTrue("screen shows: " + status[0], status[0].startsWith("Done"));
            assertTrue(job.output.length() > 0);
        }
    }

    private static Uri testVideo(Context context) throws Exception {
        File dir = new File(context.getCacheDir(), "jobs/test-media");
        dir.mkdirs();
        File f = new File(dir, "screen-input.mp4");
        try (InputStream in = InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("input.mp4");
             OutputStream out = new FileOutputStream(f)) {
            Job.copy(in, out);
        }
        return FileProvider.getUriForFile(context, context.getPackageName() + ".files", f);
    }
}
