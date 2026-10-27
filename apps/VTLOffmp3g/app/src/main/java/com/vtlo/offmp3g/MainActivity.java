package com.vtlo.offmp3g;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Lists every command of tools/ffmpeg.sh, grouped like the cheatsheet. */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<Object> rows = new ArrayList<>();
        String section = null;
        for (Recipe r : Recipe.ALL) {
            if (!r.section.equals(section)) {
                section = r.section;
                rows.add(section);
            }
            rows.add(r);
        }

        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new Adapter(rows));

        String crash = CrashReport.collect(this);
        if (crash != null) showCrashReport(crash);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    private void showCrashReport(String report) {
        TextView text = new TextView(this);
        text.setText(report);
        text.setTextIsSelectable(true);
        text.setTypeface(android.graphics.Typeface.MONOSPACE);
        text.setTextSize(11);
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        text.setPadding(pad, pad / 2, pad, 0);
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(text);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("VTLOffmp3g stopped unexpectedly last time")
            .setView(scroll)
            .setPositiveButton("Share", (d, w) -> startActivity(Intent.createChooser(
                new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, "VTLOffmp3g crash report")
                    .putExtra(Intent.EXTRA_TEXT, report), "Share crash report")))
            .setNeutralButton("Copy", (d, w) -> {
                android.content.ClipboardManager cm = getSystemService(android.content.ClipboardManager.class);
                cm.setPrimaryClip(android.content.ClipData.newPlainText("VTLOffmp3g crash report", report));
                android.widget.Toast.makeText(this, "Crash report copied", android.widget.Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Close", null)
            .show();
    }

    private final class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<Object> rows;

        Adapter(List<Object> rows) {
            this.rows = rows;
        }

        @Override
        public int getItemViewType(int position) {
            return rows.get(position) instanceof String ? 0 : 1;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = viewType == 0 ? R.layout.item_header : R.layout.item_recipe;
            View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
            return new RecyclerView.ViewHolder(v) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object row = rows.get(position);
            if (row instanceof String) {
                ((TextView) holder.itemView).setText((String) row);
                return;
            }
            Recipe r = (Recipe) row;
            View v = holder.itemView;
            ((TextView) v.findViewById(R.id.title)).setText(r.title);
            TextView desc = v.findViewById(R.id.desc);
            desc.setText(r.description);
            desc.setVisibility(r.description.isEmpty() ? View.GONE : View.VISIBLE);
            TextView cmd = v.findViewById(R.id.cmd);
            cmd.setText(r.original);
            cmd.setVisibility(r.original.isEmpty() ? View.GONE : View.VISIBLE);
            v.setOnClickListener(x -> startActivity(new Intent(MainActivity.this, RecipeActivity.class)
                .putExtra(RecipeActivity.EXTRA_RECIPE, r.id)));
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }
    }
}
