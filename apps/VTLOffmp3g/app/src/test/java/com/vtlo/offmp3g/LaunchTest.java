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
    public void everyRecipeScreenStarts() {
        for (Recipe r : Recipe.ALL) {
            Intent i = new Intent(ApplicationProvider.getApplicationContext(), RecipeActivity.class)
                .putExtra(RecipeActivity.EXTRA_RECIPE, r.id);
            assertNotNull(r.id, Robolectric.buildActivity(RecipeActivity.class, i).setup().get());
        }
    }
}
