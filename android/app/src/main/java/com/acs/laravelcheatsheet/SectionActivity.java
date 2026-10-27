package com.acs.laravelcheatsheet;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Shows one section's entries, grouped by heading, with its documentation links. */
public class SectionActivity extends Activity {
    static final String EXTRA_SECTION = "section";

    private Cheatsheet.Section section;
    private Rows adapter;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        section = CheatsheetRepository.get(this).sections.get(getIntent().getIntExtra(EXTRA_SECTION, 0));
        setTitle(section.title);
        if (getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);

        ListView list = findViewById(R.id.list);
        empty = findViewById(R.id.empty);
        adapter = new Rows(this);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            Rows.Row row = adapter.getItem(position);
            if (row.type == Rows.ENTRY) {
                EntryActions.copy(this, ((Cheatsheet.Entry) row.item).code);
            } else if (row.type == Rows.NOTE && row.item instanceof String) {
                EntryActions.openUrl(this, (String) row.item);
            }
        });
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            Rows.Row row = adapter.getItem(position);
            if (row.type != Rows.ENTRY) return false;
            EntryActions.showMenu(this, (Cheatsheet.Entry) row.item);
            return true;
        });

        EditText search = findViewById(R.id.search);
        if (section.entries.isEmpty()) {
            search.setVisibility(View.GONE);
        } else {
            search.setHint(getString(R.string.search_section_hint, section.title.toLowerCase(java.util.Locale.ROOT)));
            search.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) { show(s.toString()); }
            });
        }
        show(search.getText().toString());
    }

    private void show(String query) {
        boolean filtering = !query.trim().isEmpty();
        List<Cheatsheet.Entry> entries = filtering ? Cheatsheet.search(section.entries, query) : section.entries;
        List<Rows.Row> rows = new ArrayList<>();
        if (section.entries.isEmpty()) rows.add(new Rows.Row(Rows.NOTE, getString(R.string.empty_section), null));
        rows.addAll(Rows.entries(entries, true));
        if (!filtering) {
            for (String link : section.links) rows.add(new Rows.Row(Rows.NOTE, "↗ " + link, link));
        }
        adapter.setRows(rows, false);
        empty.setText(getString(R.string.no_results, query.trim()));
        empty.setVisibility(filtering && entries.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
