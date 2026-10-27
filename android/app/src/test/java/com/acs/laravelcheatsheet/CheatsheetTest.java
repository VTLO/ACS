package com.acs.laravelcheatsheet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class CheatsheetTest {

    private static List<String> sourceLines() throws Exception {
        InputStream in = CheatsheetTest.class.getClassLoader().getResourceAsStream(CheatsheetRepository.ASSET);
        assertNotNull("laravel.php must be bundled", in);
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) lines.add(line);
        }
        return lines;
    }

    private static Cheatsheet bundled() throws Exception {
        return Cheatsheet.parse(new StringReader(String.join("\n", sourceLines())));
    }

    @Test
    public void everyCodeLineOfLaravelPhpBecomesAnEntry() throws Exception {
        Cheatsheet cheatsheet = bundled();
        List<String> codes = new ArrayList<>();
        for (Cheatsheet.Entry entry : cheatsheet.entries) codes.add(entry.code);

        int expected = 0;
        boolean inBanner = false;
        for (String line : sourceLines()) {
            String text = line.trim();
            if (inBanner) {
                inBanner = !text.endsWith("*/");
                continue;
            }
            if (text.isEmpty() || text.startsWith("<?php") || text.startsWith("//")) continue;
            if (text.startsWith("/*")) {
                inBanner = true;
                continue;
            }
            expected++;
            assertTrue("missing: " + text, text.startsWith(codes.get(expected - 1)));
        }
        assertEquals(expected, cheatsheet.entries.size());
    }

    @Test
    public void bannersBecomeSectionsWithLinks() throws Exception {
        Cheatsheet cheatsheet = bundled();
        assertEquals("CLI AVAILABLE COMMANDS FOR ARTISAN", cheatsheet.sections.get(0).title);
        assertEquals("https://laravel.com/docs/5.8/artisan", cheatsheet.sections.get(0).links.get(0));
        List<String> titles = new ArrayList<>();
        for (Cheatsheet.Section section : cheatsheet.sections) titles.add(section.title);
        assertTrue(titles.contains("VALIDATION"));
        assertTrue(titles.contains("COLLECTION ELOQUENT METHODS"));
        assertTrue(titles.contains("LARAVEL VALET COMMANDS"));
    }

    @Test
    public void splitsCodeDescriptionAndGroup() throws Exception {
        Cheatsheet sheet = Cheatsheet.parse(new StringReader(
                "<?php\n/****\n * TITLE\n * https://laravel.com/docs/5.8/validation\n ****/\n"
                        + "// RULES.\n\nafter_or_equal:date   // Must be after.\nplain\n"));
        Cheatsheet.Entry first = sheet.entries.get(0);
        assertEquals("RULES", first.group);
        assertEquals("after_or_equal:date", first.code);
        assertEquals("Must be after.", first.description);
        assertEquals("https://laravel.com/docs/5.8/validation#rule-after-or-equal", first.docsUrl());
        assertNull(sheet.entries.get(1).description);
    }

    @Test
    public void searchMatchesAllTermsAcrossFields() throws Exception {
        Cheatsheet cheatsheet = bundled();
        List<Cheatsheet.Entry> hits = Cheatsheet.search(cheatsheet.entries, "MAKE controller");
        assertEquals(1, hits.size());
        assertEquals("php artisan make:controller", hits.get(0).code);
        assertEquals("https://laravel.com/docs/5.7/collections#method-containsstrict",
                Cheatsheet.search(cheatsheet.entries, "containsStrict").get(0).docsUrl());
    }
}
