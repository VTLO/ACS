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

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
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
