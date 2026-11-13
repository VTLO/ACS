package com.vtlo.offmp3g;

import static org.junit.Assert.assertNotNull;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LaunchTest {

    @Test
    public void mainActivityStarts() {
        assertNotNull(Robolectric.buildActivity(MainActivity.class).setup().get());
    }

    @Test
    public void frenchGuideOpensOnFirstLaunchOnly() {
        MainActivity first = Robolectric.buildActivity(MainActivity.class).setup().get();
        org.junit.Assert.assertTrue(startedGuide(first));

        // Finishing the guide marks it as seen.
        OnboardingActivity guide = Robolectric.buildActivity(OnboardingActivity.class).setup().get();
        guide.findViewById(R.id.skip).performClick();
        org.junit.Assert.assertTrue(guide.isFinishing());

        MainActivity second = Robolectric.buildActivity(MainActivity.class).setup().get();
        org.junit.Assert.assertFalse(startedGuide(second));
    }

    /** Whether the activity launched the guide (it may also launch the notification permission prompt). */
    private static boolean startedGuide(android.app.Activity activity) {
        org.robolectric.shadows.ShadowActivity shadow = org.robolectric.Shadows.shadowOf(activity);
        for (Intent i = shadow.getNextStartedActivity(); i != null; i = shadow.getNextStartedActivity()) {
            if (i.getComponent() != null && i.getComponent().getClassName().equals(OnboardingActivity.class.getName())) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void everyRecipeScreenStarts() {
        for (Recipe r : Recipe.ALL) {
            Intent i = new Intent(ApplicationProvider.getApplicationContext(), RecipeActivity.class)
                .putExtra(RecipeActivity.EXTRA_RECIPE, r.id);
            assertNotNull(r.id, Robolectric.buildActivity(RecipeActivity.class, i).setup().get());
        }
    }
}
