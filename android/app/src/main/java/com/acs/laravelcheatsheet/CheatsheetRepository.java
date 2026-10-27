package com.acs.laravelcheatsheet;

import android.content.Context;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/** Parses the bundled cheatsheet once per process. */
final class CheatsheetRepository {
    static final String ASSET = "laravel.php";

    private static Cheatsheet instance;

    private CheatsheetRepository() {}

    static synchronized Cheatsheet get(Context context) {
        if (instance == null) {
            try (Reader reader = new InputStreamReader(
                    context.getApplicationContext().getAssets().open(ASSET), StandardCharsets.UTF_8)) {
                instance = Cheatsheet.parse(reader);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read bundled " + ASSET, e);
            }
        }
        return instance;
    }
}
