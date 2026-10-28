package com.vtlo.offmp3g;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Home: the current job as a ring, engine tiles, search, and the recipes of tools/ffmpeg.sh by tab. */
public class MainActivity extends AppCompatActivity implements Job.Listener {

    /** Bottom tabs and the cheatsheet sections each one shows. */
    private static final int[] TABS = {R.id.tab_convert, R.id.tab_audio, R.id.tab_edit, R.id.tab_frames};
    private static final List<List<String>> TAB_SECTIONS = Arrays.asList(
        Arrays.asList(Recipe.TRANSCODE, Recipe.COMPRESSION),
        Arrays.asList(Recipe.AUDIO),
        Arrays.asList(Recipe.CUT, Recipe.SCALE, Recipe.GIF),
        Arrays.asList(Recipe.FRAMES, Recipe.CUSTOM));

    private final List<View> headers = new ArrayList<>();
    private final List<View> rows = new ArrayList<>();
    private LinearLayout list;
    private EditText search;
    private TextView empty;
    private int tab = 0;
    private String crashReport;
    private Job watched;

    private GaugeView heroRing;
    private TextView heroValue, heroDetail, heroLabel, statsTitle;
    private View statSpeed, statTime, statSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState != null) tab = savedInstanceState.getInt("tab", 0);

        ((TextView) findViewById(R.id.subtitle)).setText(String.format(Locale.US,
            "FFmpeg 6.1 · %d recipes · offline", Recipe.ALL.size()));
        findViewById(R.id.about).setOnClickListener(v -> showAbout());

        heroRing = findViewById(R.id.heroRing);
        heroRing.setStrokeDp(12);
        heroValue = findViewById(R.id.heroValue);
        heroDetail = findViewById(R.id.heroDetail);
        heroLabel = findViewById(R.id.heroLabel);
        statsTitle = findViewById(R.id.statsTitle);
        statSpeed = setupStat(R.id.statSpeed, R.drawable.ic_bolt, "Speed");
        statTime = setupStat(R.id.statTime, R.drawable.ic_timer, "Processed");
        statSize = setupStat(R.id.statSize, R.drawable.ic_storage, "Output");
        findViewById(R.id.hero).setOnClickListener(v -> {
            Job j = Job.current();
            if (j != null) openRecipe(j.recipe);
        });

        buildTiles();
        buildList();

        search = findViewById(R.id.search);
        empty = findViewById(R.id.empty);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { filter(); }
        });
        search.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) hideKeyboard();
            return false;
        });
        findViewById(R.id.searchGo).setOnClickListener(v -> {
            hideKeyboard();
            openFirstMatch();
        });

        BottomNavigationView nav = findViewById(R.id.nav);
        nav.setSelectedItemId(TABS[tab]);
        nav.setOnItemSelectedListener(item -> {
            for (int i = 0; i < TABS.length; i++) if (TABS[i] == item.getItemId()) tab = i;
            filter();
            return true;
        });
        filter();

        crashReport = CrashReport.collect(this);
        View banner = findViewById(R.id.crashBanner);
        banner.setVisibility(crashReport != null ? View.VISIBLE : View.GONE);
        banner.setOnClickListener(v -> showCrashReport(crashReport));

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("tab", tab);
    }

    @Override
    protected void onResume() {
        super.onResume();
        watch(Job.current());
        renderHero();
    }

    @Override
    protected void onPause() {
        watch(null);
        super.onPause();
    }

    private void watch(Job j) {
        if (watched != null) watched.removeListener(this);
        watched = j;
        if (watched != null) watched.addListener(this);
    }

    @Override
    public void onJobUpdate(Job j) {
        renderHero();
    }

    // --- hero ----------------------------------------------------------------------------------

    private void renderHero() {
        Job j = Job.current();
        int green = ContextCompat.getColor(this, R.color.green);
        int track = ContextCompat.getColor(this, R.color.surface_high);
        if (j == null) {
            heroRing.setColors(track, green);
            heroRing.setProgress(1);
            heroValue.setText("Ready");
            heroDetail.setText("x264 · VP9 · Opus");
            heroLabel.setText("Pick a recipe");
            statsTitle.setText("Last job");
            setStat(statSpeed, "—");
            setStat(statTime, "—");
            setStat(statSize, "—");
            return;
        }
        boolean running = j.state() == Job.State.RUNNING;
        int color;
        switch (j.state()) {
            case DONE: color = green; break;
            case RUNNING: color = green; break;
            case CANCELLED: color = ContextCompat.getColor(this, R.color.amber); break;
            default: color = ContextCompat.getColor(this, R.color.orange);
        }
        heroRing.setColors(track, color);
        double p = j.progress();
        if (running) {
            heroRing.setProgress((float) p);
            heroValue.setText(p < 0 ? "…" : Math.round(p * 100) + "%");
            heroLabel.setText(j.status());
        } else {
            heroRing.setProgress(j.state() == Job.State.DONE ? 1 : (float) Math.max(0, p));
            heroValue.setText(j.state() == Job.State.DONE ? "Done" : j.state() == Job.State.CANCELLED ? "Stopped" : "Failed");
            heroLabel.setText("Tap to open");
        }
        heroDetail.setText(j.recipe.title);
        statsTitle.setText(running ? "Current job" : "Last job");
        setStat(statSpeed, j.speed() > 0 ? String.format(Locale.US, "%.2fx", j.speed()) : "—");
        setStat(statTime, Job.formatMs(j.timeMs()));
        setStat(statSize, j.outputBytes() > 0 ? formatBytes(j.outputBytes()) : "—");
    }

    private View setupStat(int id, int icon, String label) {
        View v = findViewById(id);
        ((ImageView) v.findViewById(R.id.statIcon)).setImageResource(icon);
        ((TextView) v.findViewById(R.id.statLabel)).setText(label);
        return v;
    }

    private static void setStat(View stat, String value) {
        ((TextView) stat.findViewById(R.id.statValue)).setText(value);
    }

    static String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024 * 1024) return String.format(Locale.US, "%.2f GB", bytes / 1073741824.0);
        if (bytes >= 1024L * 1024) return String.format(Locale.US, "%.1f MB", bytes / 1048576.0);
        return String.format(Locale.US, "%d kB", Math.max(1, bytes / 1024));
    }

    // --- tiles ---------------------------------------------------------------------------------

    private void buildTiles() {
        LinearLayout tiles = findViewById(R.id.tiles);
        Object[][] data = {
            {R.drawable.ic_movie, "H.264", "libx264"},
            {R.drawable.ic_movie, "VP8 · VP9", "libvpx + presets"},
            {R.drawable.ic_audio, "Opus · MP3", "libopus · LAME"},
            {R.drawable.ic_subtitles, "Subtitles", "libass"},
            {R.drawable.ic_chip, "16 KB", "page ready"},
        };
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Object[] d : data) {
            View t = inflater.inflate(R.layout.view_tile, tiles, false);
            ((ImageView) t.findViewById(R.id.tileIcon)).setImageResource((Integer) d[0]);
            ((TextView) t.findViewById(R.id.tileValue)).setText((String) d[1]);
            ((TextView) t.findViewById(R.id.tileLabel)).setText((String) d[2]);
            tiles.addView(t);
        }
    }

    // --- recipe list ---------------------------------------------------------------------------

    private void buildList() {
        list = findViewById(R.id.recipes);
        LayoutInflater inflater = LayoutInflater.from(this);
        String section = null;
        View header = null;
        for (Recipe r : Recipe.ALL) {
            if (!r.section.equals(section)) {
                section = r.section;
                header = inflater.inflate(R.layout.item_header, list, false);
                ((TextView) header).setText(section);
                header.setTag(section);
                list.addView(header);
                headers.add(header);
            }
            View row = inflater.inflate(R.layout.item_recipe, list, false);
            ((TextView) row.findViewById(R.id.title)).setText(r.title);
            TextView desc = row.findViewById(R.id.desc);
            desc.setText(r.original.isEmpty() ? r.description : r.original.split("\n")[0]);
            ((TextView) row.findViewById(R.id.badge)).setText(badge(r));
            row.setTag(r);
            row.setOnClickListener(v -> openRecipe(r));
            list.addView(row);
            rows.add(row);
        }
    }

    /** The trailing value of a row: the output format, like the scanner's score column. */
    static String badge(Recipe r) {
        switch (r.outputKind) {
            case NONE: return "FLV";
            case DIRECTORY: return "JPG";
            default: return Recipe.applyParams(r.outputExt, r.defaultParams()).toUpperCase(Locale.US);
        }
    }

    private void filter() {
        String q = search == null ? "" : search.getText().toString().trim().toLowerCase(Locale.US);
        boolean searching = !q.isEmpty();
        int visible = 0;
        for (View row : rows) {
            Recipe r = (Recipe) row.getTag();
            boolean show = searching ? matches(r, q) : TAB_SECTIONS.get(tab).contains(r.section);
            row.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) visible++;
        }
        for (View header : headers) {
            boolean any = false;
            for (View row : rows) {
                if (row.getVisibility() == View.VISIBLE && ((Recipe) row.getTag()).section.equals(header.getTag())) {
                    any = true;
                }
            }
            header.setVisibility(any ? View.VISIBLE : View.GONE);
        }
        if (empty != null) empty.setVisibility(visible == 0 ? View.VISIBLE : View.GONE);
    }

    private static boolean matches(Recipe r, String q) {
        String hay = (r.title + " " + r.description + " " + r.original + " " + r.section + " "
            + badge(r)).toLowerCase(Locale.US);
        for (String word : q.split("\\s+")) if (!hay.contains(word)) return false;
        return true;
    }

    private void openFirstMatch() {
        if (search.getText().toString().trim().isEmpty()) return;
        for (View row : rows) {
            if (row.getVisibility() == View.VISIBLE) {
                openRecipe((Recipe) row.getTag());
                return;
            }
        }
    }

    private void openRecipe(Recipe r) {
        startActivity(new Intent(this, RecipeActivity.class).putExtra(RecipeActivity.EXTRA_RECIPE, r.id));
    }

    private void hideKeyboard() {
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (imm != null) imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
        search.clearFocus();
    }

    // --- dialogs -------------------------------------------------------------------------------

    private void showAbout() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("VTLOffmp3g " + BuildConfig.VERSION_NAME)
            .setMessage("Every command of tools/ffmpeg.sh, offline, on your phone.\n\n"
                + "Engine: FFmpeg 6.1 (FFmpegKit full-gpl): x264, libvpx with libvpx presets, "
                + "Opus, LAME, libass.\n\nPick a recipe, choose your files, adjust the parameters "
                + "or edit the command itself, then Run.")
            .setPositiveButton("OK", null)
            .show();
    }

    private void showCrashReport(String report) {
        if (report == null) return;
        TextView text = new TextView(this);
        text.setText(report);
        text.setTextIsSelectable(true);
        text.setTypeface(android.graphics.Typeface.MONOSPACE);
        text.setTextSize(11);
        text.setTextColor(ContextCompat.getColor(this, R.color.scope_text));
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        text.setPadding(pad, pad, pad, pad);
        text.setBackground(Panels.scope(this));
        ScrollView scroll = new ScrollView(this);
        scroll.setPadding(pad, pad / 2, pad, 0);
        scroll.addView(text);
        new MaterialAlertDialogBuilder(this)
            .setTitle("VTLOffmp3g stopped unexpectedly last time")
            .setView(scroll)
            .setPositiveButton("Share", (d, w) -> startActivity(Intent.createChooser(
                new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, "VTLOffmp3g crash report")
                    .putExtra(Intent.EXTRA_TEXT, report), "Share crash report")))
            .setNeutralButton("Copy", (d, w) -> {
                ClipboardManager cm = getSystemService(ClipboardManager.class);
                cm.setPrimaryClip(ClipData.newPlainText("VTLOffmp3g crash report", report));
                Toast.makeText(this, "Crash report copied", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Dismiss", (d, w) -> findViewById(R.id.crashBanner).setVisibility(View.GONE))
            .show();
    }
}
