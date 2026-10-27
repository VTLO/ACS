package com.acs.laravelcheatsheet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** A ListView adapter over mixed rows: section links, group headers, notes and entries. */
final class Rows extends BaseAdapter {

    static final int SECTION = 0;
    static final int HEADER = 1;
    static final int NOTE = 2;
    static final int ENTRY = 3;

    static final class Row {
        final int type;
        final String text;
        final Object item;

        Row(int type, String text, Object item) {
            this.type = type;
            this.text = text;
            this.item = item;
        }
    }

    private final LayoutInflater inflater;
    private final Context context;
    private List<Row> rows = new ArrayList<>();
    /** Show the section and group above each entry, for search results spanning sections. */
    private boolean showEntryOrigin;

    Rows(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    void setRows(List<Row> rows, boolean showEntryOrigin) {
        this.rows = rows;
        this.showEntryOrigin = showEntryOrigin;
        notifyDataSetChanged();
    }

    static List<Row> sections(List<Cheatsheet.Section> sections) {
        List<Row> rows = new ArrayList<>();
        for (Cheatsheet.Section section : sections) rows.add(new Row(SECTION, section.title, section));
        return rows;
    }

    /** Entries grouped under their "// HEADING", in source order. */
    static List<Row> entries(List<Cheatsheet.Entry> entries, boolean withGroupHeaders) {
        List<Row> rows = new ArrayList<>();
        String lastGroup = null;
        for (Cheatsheet.Entry entry : entries) {
            if (withGroupHeaders && entry.group != null && !entry.group.equals(lastGroup)) {
                rows.add(new Row(HEADER, entry.group, null));
            }
            lastGroup = entry.group;
            rows.add(new Row(ENTRY, entry.code, entry));
        }
        return rows;
    }

    @Override public int getCount() { return rows.size(); }
    @Override public Row getItem(int position) { return rows.get(position); }
    @Override public long getItemId(int position) { return position; }
    @Override public int getViewTypeCount() { return 4; }
    @Override public int getItemViewType(int position) { return rows.get(position).type; }

    @Override
    public boolean isEnabled(int position) {
        return rows.get(position).type != HEADER;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        Row row = rows.get(position);
        switch (row.type) {
            case SECTION: {
                if (view == null) view = inflater.inflate(R.layout.item_section, parent, false);
                Cheatsheet.Section section = (Cheatsheet.Section) row.item;
                ((TextView) view.findViewById(R.id.title)).setText(section.title);
                ((TextView) view.findViewById(R.id.subtitle)).setText(sectionSummary(section));
                return view;
            }
            case HEADER:
            case NOTE: {
                if (view == null) {
                    view = inflater.inflate(row.type == HEADER ? R.layout.item_header : R.layout.item_note, parent, false);
                }
                ((TextView) view.findViewById(R.id.title)).setText(row.text);
                return view;
            }
            default: {
                if (view == null) view = inflater.inflate(R.layout.item_entry, parent, false);
                Cheatsheet.Entry entry = (Cheatsheet.Entry) row.item;
                TextView meta = view.findViewById(R.id.meta);
                if (showEntryOrigin) {
                    meta.setText(entry.group == null ? entry.section.title : entry.section.title + " › " + entry.group);
                    meta.setVisibility(View.VISIBLE);
                } else {
                    meta.setVisibility(View.GONE);
                }
                ((TextView) view.findViewById(R.id.code)).setText(entry.code);
                TextView description = view.findViewById(R.id.description);
                description.setText(entry.description);
                description.setVisibility(entry.description == null ? View.GONE : View.VISIBLE);
                return view;
            }
        }
    }

    private String sectionSummary(Cheatsheet.Section section) {
        if (section.entries.isEmpty()) return section.links.isEmpty() ? "" : section.links.get(0);
        StringBuilder groups = new StringBuilder();
        String last = null;
        for (Cheatsheet.Entry entry : section.entries) {
            if (entry.group != null && !entry.group.equals(last)) {
                groups.append(groups.length() == 0 ? " · " : ", ").append(entry.group);
                last = entry.group;
            }
        }
        return context.getString(R.string.entry_count, section.entries.size()) + groups;
    }
}
