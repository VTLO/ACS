package com.acs.laravelcheatsheet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The parsed contents of backend/laravel.php. Pure Java so it can be unit tested on the JVM. */
public final class Cheatsheet {

    public static final class Section {
        public final String title;
        public final List<String> links;
        public final List<Entry> entries = new ArrayList<>();

        Section(String title, List<String> links) {
            this.title = title;
            this.links = Collections.unmodifiableList(links);
        }
    }

    public static final class Entry {
        public final Section section;
        /** The "// HEADING" the entry sits under, or null. */
        public final String group;
        public final String code;
        /** The trailing "// comment", or null when the line has none. */
        public final String description;

        Entry(Section section, String group, String code, String description) {
            this.section = section;
            this.group = group;
            this.code = code;
            this.description = description;
        }

        boolean matches(String[] terms) {
            String haystack = (section.title + ' ' + (group == null ? "" : group) + ' ' + code + ' '
                    + (description == null ? "" : description)).toLowerCase(Locale.ROOT);
            for (String term : terms) {
                if (!haystack.contains(term)) return false;
            }
            return true;
        }

        /** Most specific documentation page for this entry. */
        public String docsUrl() {
            if (section.links.isEmpty()) return null;
            String base = section.links.get(section.links.size() - 1);
            String name = code.split("[:\\s(]", 2)[0];
            if (base.endsWith("/collections") && name.matches("\\w+")) {
                return base + "#method-" + name.toLowerCase(Locale.ROOT);
            }
            if (base.endsWith("/validation") && name.matches("\\w+")) {
                return base + "#rule-" + name.toLowerCase(Locale.ROOT).replace('_', '-');
            }
            return base;
        }

        public String fullText() {
            return description == null ? code : code + "  // " + description;
        }
    }

    private static final Pattern COMMENTED_LINE = Pattern.compile("^(.*?\\S)\\s+//\\s*(.*)$");

    public final List<Section> sections;
    public final List<Entry> entries;

    private Cheatsheet(List<Section> sections) {
        List<Entry> all = new ArrayList<>();
        for (Section section : sections) all.addAll(section.entries);
        this.sections = Collections.unmodifiableList(sections);
        this.entries = Collections.unmodifiableList(all);
    }

    /** Entries containing every whitespace separated term of the query, case-insensitively. */
    public static List<Entry> search(List<Entry> entries, String query) {
        String[] terms = query.trim().toLowerCase(Locale.ROOT).split("\\s+");
        List<Entry> result = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.matches(terms)) result.add(entry);
        }
        return result;
    }

    public static Cheatsheet parse(Reader source) throws IOException {
        BufferedReader reader = new BufferedReader(source);
        List<Section> sections = new ArrayList<>();
        Section current = null;
        String group = null;
        boolean inBanner = false;
        String bannerTitle = null;
        List<String> bannerLinks = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            String text = line.trim();
            if (inBanner) {
                if (text.endsWith("*/")) {
                    current = new Section(bannerTitle == null ? "General" : bannerTitle, bannerLinks);
                    sections.add(current);
                    group = null;
                    inBanner = false;
                } else if (text.startsWith("*")) {
                    String content = text.substring(1).trim();
                    if (content.startsWith("http")) {
                        bannerLinks.add(content);
                    } else if (!content.isEmpty() && bannerTitle == null) {
                        bannerTitle = content;
                    }
                }
                continue;
            }
            if (text.isEmpty() || text.startsWith("<?php")) continue;
            if (text.startsWith("/*")) {
                inBanner = !text.endsWith("*/");
                bannerTitle = null;
                bannerLinks = new ArrayList<>();
                continue;
            }
            if (text.startsWith("//")) {
                group = text.substring(2).trim().replaceAll("\\.$", "");
                continue;
            }
            if (current == null) {
                current = new Section("General", new ArrayList<>());
                sections.add(current);
            }
            Matcher m = COMMENTED_LINE.matcher(text);
            if (m.matches()) {
                String description = m.group(2).trim();
                current.entries.add(new Entry(current, group, m.group(1), description.isEmpty() ? null : description));
            } else {
                current.entries.add(new Entry(current, group, text, null));
            }
        }
        return new Cheatsheet(sections);
    }
}
