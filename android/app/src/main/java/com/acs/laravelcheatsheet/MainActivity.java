package com.acs.laravelcheatsheet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

/** Lists the cheatsheet sections and searches across every entry. */
public class MainActivity extends Activity {

    private Cheatsheet cheatsheet;
    private Rows adapter;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        cheatsheet = CheatsheetRepository.get(this);

        ListView list = findViewById(R.id.list);
        empty = findViewById(R.id.empty);
        adapter = new Rows(this);
        list.setAdapter(adapter);

        list.setOnItemClickListener((parent, view, position, id) -> {
            Rows.Row row = adapter.getItem(position);
            if (row.type == Rows.SECTION) {
                startActivity(new Intent(this, SectionActivity.class)
                        .putExtra(SectionActivity.EXTRA_SECTION, cheatsheet.sections.indexOf(row.item)));
            } else if (row.type == Rows.ENTRY) {
                EntryActions.copy(this, ((Cheatsheet.Entry) row.item).code);
            }
        });
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            Rows.Row row = adapter.getItem(position);
            if (row.type != Rows.ENTRY) return false;
            EntryActions.showMenu(this, (Cheatsheet.Entry) row.item);
            return true;
        });

        EditText search = findViewById(R.id.search);
        search.setHint(getString(R.string.search_hint, cheatsheet.entries.size()));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { show(s.toString()); }
        });
        show(search.getText().toString());
    }

    private void show(String query) {
        if (query.trim().isEmpty()) {
            adapter.setRows(Rows.sections(cheatsheet.sections), false);
            empty.setVisibility(View.GONE);
            return;
        }
        List<Cheatsheet.Entry> matches = Cheatsheet.search(cheatsheet.entries, query);
        adapter.setRows(Rows.entries(matches, false), true);
        empty.setText(getString(R.string.no_results, query.trim()));
        empty.setVisibility(matches.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.about) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setIcon(R.mipmap.ic_launcher)
                    .setMessage(getString(R.string.about_text, cheatsheet.entries.size(), cheatsheet.sections.size()))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
