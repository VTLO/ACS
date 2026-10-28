package com.vtlo.offmp3g;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pick files, tweak parameters, run one cheatsheet recipe and export the result. */
public class RecipeActivity extends AppCompatActivity implements Job.Listener {

    public static final String EXTRA_RECIPE = "recipe";

    private Recipe recipe;
    private final Map<String, String> values = new LinkedHashMap<>();
    private final Map<String, List<Uri>> picks = new HashMap<>();
    private final Map<String, TextView> pickLabels = new HashMap<>();
    private String pendingPick;

    private EditText command;
    private MaterialButton run, cancel, reset;
    private GaugeView gauge;
    private ScopeView scope;
    private View statTime, statSpeed, statSize;
    private TextView status, log;
    private View result;
    private Job job;

    private ActivityResultLauncher<String[]> pickOne;
    private ActivityResultLauncher<String[]> pickMany;
    private ActivityResultLauncher<Intent> saveFile;
    private ActivityResultLauncher<Intent> saveTree;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recipe = Recipe.byId(getIntent().getStringExtra(EXTRA_RECIPE));
        if (recipe == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_recipe);
        ((TextView) findViewById(R.id.title)).setText(recipe.title);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        findViewById(R.id.help).setOnClickListener(v -> showCheatsheetLine());
        findViewById(R.id.gaugeCard).setBackground(Panels.stripes(this));
        findViewById(R.id.scopeBox).setBackground(Panels.scope(this));
        findViewById(R.id.commandBox).setBackground(Panels.scope(this));
        findViewById(R.id.logBox).setBackground(Panels.scope(this));
        gauge = findViewById(R.id.gauge);
        gauge.setSweep(270);
        gauge.setStrokeDp(9);
        scope = findViewById(R.id.scope);
        statTime = setupStat(R.id.statTime, R.drawable.ic_timer, "Processed");
        statSpeed = setupStat(R.id.statSpeed, R.drawable.ic_bolt, "Speed");
        statSize = setupStat(R.id.statSize, R.drawable.ic_storage, "Output");

        pickOne = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
            uri -> { if (uri != null) onPicked(Arrays.asList(uri)); });
        pickMany = registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(),
            uris -> { if (uris != null && !uris.isEmpty()) onPicked(uris); });
        saveFile = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            this::onSaveFile);
        saveTree = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            this::onSaveTree);

        TextView desc = findViewById(R.id.desc);
        desc.setText(recipe.description);
        desc.setVisibility(recipe.description.isEmpty() ? View.GONE : View.VISIBLE);

        command = findViewById(R.id.command);
        run = findViewById(R.id.run);
        cancel = findViewById(R.id.cancel);
        reset = findViewById(R.id.reset);
        status = findViewById(R.id.status);
        log = findViewById(R.id.log);
        result = findViewById(R.id.result);

        values.putAll(recipe.defaultParams());
        buildInputs();
        buildParams();
        command.setText(recipe.buildCommand(values));

        run.setOnClickListener(v -> startJob());
        cancel.setOnClickListener(v -> { if (job != null) job.cancel(); });
        reset.setOnClickListener(v -> resetParams());
        findViewById(R.id.save).setOnClickListener(v -> save());
        findViewById(R.id.share).setOnClickListener(v -> share());
        findViewById(R.id.open).setOnClickListener(v -> open());
        findViewById(R.id.copyLog).setOnClickListener(v -> {
            ClipboardManager cm = getSystemService(ClipboardManager.class);
            cm.setPrimaryClip(ClipData.newPlainText("ffmpeg log", job == null ? "" : job.log()));
            Toast.makeText(this, "Log copied", Toast.LENGTH_SHORT).show();
        });

        // Re-attach to a job started from this screen earlier (e.g. opened from the notification).
        Job current = Job.current();
        if (current != null && current.recipe == recipe) {
            attach(current);
            command.setText(current.commandText());
        } else {
            render();
        }
    }

    @Override
    protected void onDestroy() {
        if (job != null) job.removeListener(this);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- inputs & params -------------------------------------------------------------------------

    private void buildInputs() {
        LinearLayout box = findViewById(R.id.inputs);
        if (recipe.inputs.isEmpty()) {
            findViewById(R.id.inputsHeader).setVisibility(View.GONE);
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Recipe.Input in : recipe.inputs) {
            View row = inflater.inflate(R.layout.item_input, box, false);
            ((TextView) row.findViewById(R.id.inputLabel)).setText(in.label);
            MaterialButton pick = row.findViewById(R.id.inputPick);
            pick.setText(in.multiple ? "Add" : "Pick");
            pick.setOnClickListener(v -> {
                pendingPick = in.key;
                String[] mimes = {in.mime};
                if (in.multiple) pickMany.launch(mimes); else pickOne.launch(mimes);
            });
            View clear = row.findViewById(R.id.inputClear);
            clear.setVisibility(in.multiple ? View.VISIBLE : View.GONE);
            clear.setOnClickListener(v -> {
                picks.remove(in.key);
                updatePickLabel(in);
            });
            pickLabels.put(in.key, row.findViewById(R.id.inputName));
            box.addView(row);
            updatePickLabel(in);
        }
    }

    /** Test hook: behaves as if the user picked {@code uris} for input {@code key}. */
    @androidx.annotation.VisibleForTesting
    void pickForTest(String key, List<Uri> uris) {
        pendingPick = key;
        onPicked(uris);
    }

    private void onPicked(List<Uri> uris) {
        Recipe.Input in = pendingPick == null ? null : recipe.findInput(pendingPick);
        if (in == null) return;
        List<Uri> list = picks.get(in.key);
        if (list == null || !in.multiple) {
            list = new ArrayList<>();
            picks.put(in.key, list);
        }
        list.addAll(uris);
        updatePickLabel(in);
    }

    private void updatePickLabel(Recipe.Input in) {
        TextView label = pickLabels.get(in.key);
        List<Uri> list = picks.get(in.key);
        if (list == null || list.isEmpty()) {
            label.setText("nothing picked");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (sb.length() > 0) sb.append('\n');
            if (in.multiple) sb.append(i + 1).append(". ");
            sb.append(Job.displayName(this, list.get(i)));
        }
        label.setText(sb);
    }

    private final List<TextInputEditText> paramFields = new ArrayList<>();

    private void buildParams() {
        LinearLayout box = findViewById(R.id.params);
        if (recipe.params.isEmpty()) {
            findViewById(R.id.paramsHeader).setVisibility(View.GONE);
            return;
        }
        for (Recipe.Param p : recipe.params) {
            TextInputLayout til = new TextInputLayout(this);
            til.setHint(p.label);
            TextInputEditText field = new TextInputEditText(til.getContext());
            field.setSingleLine(true);
            field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            field.setText(p.defaultValue);
            field.setTag(p.key);
            field.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {
                    values.put(p.key, s.toString().trim());
                    command.setText(recipe.buildCommand(values));
                }
            });
            til.addView(field);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(4);
            box.addView(til, lp);
            paramFields.add(field);
        }
    }

    private void resetParams() {
        for (TextInputEditText f : paramFields) {
            String key = (String) f.getTag();
            for (Recipe.Param p : recipe.params) if (p.key.equals(key)) f.setText(p.defaultValue);
        }
        values.clear();
        values.putAll(recipe.defaultParams());
        command.setText(recipe.buildCommand(values));
    }

    // --- running ---------------------------------------------------------------------------------

    private void startJob() {
        if (Job.isRunning()) {
            Toast.makeText(this, "Another job is running", Toast.LENGTH_SHORT).show();
            return;
        }
        String text = command.getText().toString();
        if (Job.commandLines(text).isEmpty()) {
            Toast.makeText(this, "The command is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        for (String name : Job.placeholders(text)) {
            Recipe.Input in = name.equals("list") ? firstMultiInput() : recipe.findInput(name);
            if (in != null && (picks.get(in.key) == null || picks.get(in.key).isEmpty())) {
                Toast.makeText(this, (in.multiple ? "Add: " : "Pick: ") + in.label, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        try {
            attach(Job.start(this, recipe, copyPicks(), text, outputName()));
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Recipe.Input firstMultiInput() {
        for (Recipe.Input in : recipe.inputs) if (in.multiple) return in;
        return null;
    }

    private Map<String, List<Uri>> copyPicks() {
        Map<String, List<Uri>> m = new HashMap<>();
        for (Map.Entry<String, List<Uri>> e : picks.entrySet()) m.put(e.getKey(), new ArrayList<>(e.getValue()));
        return m;
    }

    private String outputExt() {
        String ext = Recipe.applyParams(recipe.outputExt, values).replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        return ext.isEmpty() ? "out" : ext;
    }

    private String outputName() {
        String base = recipe.id;
        if (!recipe.inputs.isEmpty()) {
            List<Uri> first = picks.get(recipe.inputs.get(0).key);
            if (first != null && !first.isEmpty()) {
                base = Job.stem(Job.displayName(this, first.get(0))) + "_" + recipe.id;
            }
        }
        return recipe.outputKind == Recipe.OutputKind.DIRECTORY ? base + "_frames" : base + "." + outputExt();
    }

    private void attach(Job j) {
        if (job != null) job.removeListener(this);
        job = j;
        job.addListener(this);
        render();
    }

    @Override
    public void onJobUpdate(Job j) {
        render();
    }

    private void render() {
        boolean running = job != null && job.state() == Job.State.RUNNING;
        boolean otherRunning = !running && Job.isRunning();
        run.setEnabled(!running && !otherRunning);
        run.setVisibility(running ? View.GONE : View.VISIBLE);
        cancel.setVisibility(running ? View.VISIBLE : View.GONE);
        cancel.setEnabled(running);
        reset.setEnabled(!running);
        command.setEnabled(!running);
        if (running) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        TextView score = findViewById(R.id.score);
        TextView pillLabel = findViewById(R.id.pillLabel);
        TextView pill = findViewById(R.id.pill);
        TextView detail = findViewById(R.id.statusDetail);
        // The scanner's gauge: coloured progress over a white remainder.
        int green = color(R.color.green), track = color(R.color.gauge_track);

        if (job == null) {
            gauge.setColors(track, green);
            gauge.setProgress(0);
            score.setText("0");
            score.setTextColor(color(R.color.text));
            status.setText(otherRunning ? "Busy" : "Ready");
            status.setTextColor(otherRunning ? color(R.color.amber) : green);
            pillLabel.setText("Output");
            pill.setText(MainActivity.badge(recipe));
            detail.setText(otherRunning ? "Another job is running: " + Job.current().recipe.title
                : "Pick your files, then Run.");
            findViewById(R.id.scopeBox).setVisibility(View.GONE);
            findViewById(R.id.stats).setVisibility(View.GONE);
            result.setVisibility(View.GONE);
            return;
        }

        int stateColor;
        double p = job.progress();
        switch (job.state()) {
            case RUNNING:
                stateColor = green;
                status.setText(p < 0 ? "Working" : "Encoding");
                gauge.setProgress((float) p);
                score.setText(p < 0 ? "…" : String.valueOf(Math.round(p * 100)));
                pillLabel.setText("Speed");
                pill.setText(job.speed() > 0 ? String.format(java.util.Locale.US, "%.2fx", job.speed()) : "—");
                detail.setText(job.status() + "\n" + job.processedTime());
                break;
            case DONE:
                stateColor = green;
                status.setText("Done");
                gauge.setProgress(1);
                score.setText("100");
                pillLabel.setText("Size");
                pill.setText(job.output == null ? "stream" : describeSize(job.output));
                detail.setText(job.output == null ? job.status() : job.output.getName());
                break;
            case CANCELLED:
                stateColor = color(R.color.amber);
                status.setText("Stopped");
                gauge.setProgress((float) Math.max(0, p));
                score.setText(p < 0 ? "–" : String.valueOf(Math.round(p * 100)));
                pillLabel.setText("Output");
                pill.setText("none");
                detail.setText(job.status());
                break;
            default:
                stateColor = color(R.color.orange);
                status.setText("Failed");
                gauge.setProgress((float) Math.max(0.02, p));
                score.setText(p < 0 ? "!" : String.valueOf(Math.round(p * 100)));
                pillLabel.setText("Risk");
                pill.setText("see log");
                detail.setText(job.status());
        }
        gauge.setColors(track, stateColor);
        status.setTextColor(stateColor);
        score.setTextColor(job.state() == Job.State.FAILED ? stateColor : color(R.color.text));

        findViewById(R.id.scopeBox).setVisibility(View.VISIBLE);
        scope.setSamples(job.samples());
        findViewById(R.id.stats).setVisibility(View.VISIBLE);
        setStat(statTime, Job.formatMs(job.timeMs()));
        setStat(statSpeed, job.speed() > 0 ? String.format(java.util.Locale.US, "%.2fx", job.speed()) : "—");
        setStat(statSize, job.outputBytes() > 0 ? MainActivity.formatBytes(job.outputBytes())
            : job.output != null && job.state() == Job.State.DONE ? describeSize(job.output) : "—");

        result.setVisibility(job.state() == Job.State.DONE && job.output != null ? View.VISIBLE : View.GONE);
        findViewById(R.id.open).setVisibility(job.output != null && job.output.isFile() ? View.VISIBLE : View.GONE);

        String text = job.log();
        if (text.length() > 12_000) text = "…" + text.substring(text.length() - 12_000);
        log.setText(text.isEmpty() ? "waiting for ffmpeg…" : text);
    }

    private static String describeSize(File f) {
        if (f.isDirectory()) {
            String[] names = f.list();
            return (names == null ? 0 : names.length) + " files";
        }
        return MainActivity.formatBytes(f.length());
    }

    private View setupStat(int id, int icon, String label) {
        View v = findViewById(id);
        ((android.widget.ImageView) v.findViewById(R.id.statIcon)).setImageResource(icon);
        ((TextView) v.findViewById(R.id.statLabel)).setText(label);
        return v;
    }

    private static void setStat(View stat, String value) {
        ((TextView) stat.findViewById(R.id.statValue)).setText(value);
    }

    private int color(int res) {
        return androidx.core.content.ContextCompat.getColor(this, res);
    }

    private void showCheatsheetLine() {
        TextView text = new TextView(this);
        text.setText(recipe.original.isEmpty() ? recipe.buildCommand(recipe.defaultParams()) : recipe.original);
        text.setTextIsSelectable(true);
        text.setTypeface(android.graphics.Typeface.MONOSPACE);
        text.setTextSize(12);
        text.setTextColor(color(R.color.scope_yellow));
        int pad = dp(16);
        text.setPadding(pad, pad, pad, pad);
        text.setBackground(Panels.scope(this));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(8), dp(24), 0);
        box.addView(text);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("From tools/ffmpeg.sh")
            .setMessage(recipe.description.isEmpty() ? null : recipe.description)
            .setView(box)
            .setPositiveButton("OK", null)
            .show();
    }

    // --- export ----------------------------------------------------------------------------------

    private String mimeOf(File f) {
        String m = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Job.extension(f.getName()));
        return m == null ? "application/octet-stream" : m;
    }

    private void save() {
        if (job == null || job.output == null) return;
        if (job.output.isDirectory()) {
            saveTree.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE));
        } else {
            saveFile.launch(new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(mimeOf(job.output))
                .putExtra(Intent.EXTRA_TITLE, job.output.getName()));
        }
    }

    private void onSaveFile(ActivityResult r) {
        if (r.getData() == null || r.getData().getData() == null || job == null) return;
        Uri target = r.getData().getData();
        File source = job.output;
        exportInBackground(() -> copyTo(source, target), "Saved " + source.getName());
    }

    private void onSaveTree(ActivityResult r) {
        if (r.getData() == null || r.getData().getData() == null || job == null) return;
        Uri tree = r.getData().getData();
        File dir = job.output;
        exportInBackground(() -> {
            ContentResolver cr = getContentResolver();
            Uri parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
            Uri folder = DocumentsContract.createDocument(cr, parent, DocumentsContract.Document.MIME_TYPE_DIR, dir.getName());
            if (folder == null) throw new IllegalStateException("Cannot create folder");
            File[] files = dir.listFiles();
            if (files == null) return;
            Arrays.sort(files);
            for (File f : files) {
                Uri doc = DocumentsContract.createDocument(cr, folder, mimeOf(f), f.getName());
                if (doc == null) throw new IllegalStateException("Cannot create " + f.getName());
                copyTo(f, doc);
            }
        }, "Saved folder " + dir.getName());
    }

    private interface IoTask {
        void run() throws Exception;
    }

    private void exportInBackground(IoTask task, String done) {
        Toast.makeText(this, "Saving…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            String msg = done;
            try {
                task.run();
            } catch (Exception e) {
                msg = "Save failed: " + e.getMessage();
            }
            String m = msg;
            runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show());
        }).start();
    }

    private void copyTo(File source, Uri target) throws Exception {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = getContentResolver().openOutputStream(target, "wt")) {
            if (out == null) throw new IllegalStateException("Cannot write " + target);
            Job.copy(in, out);
        }
    }

    private Uri shareUri(File f) {
        return FileProvider.getUriForFile(this, getPackageName() + ".files", f);
    }

    private void share() {
        if (job == null || job.output == null) return;
        Intent send;
        if (job.output.isDirectory()) {
            File[] files = job.output.listFiles();
            if (files == null || files.length == 0) return;
            Arrays.sort(files);
            ArrayList<Uri> uris = new ArrayList<>();
            for (File f : files) uris.add(shareUri(f));
            send = new Intent(Intent.ACTION_SEND_MULTIPLE)
                .setType("image/jpeg")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        } else {
            send = new Intent(Intent.ACTION_SEND)
                .setType(mimeOf(job.output))
                .putExtra(Intent.EXTRA_STREAM, shareUri(job.output));
        }
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, "Share"));
    }

    private void open() {
        if (job == null || job.output == null || !job.output.isFile()) return;
        Intent view = new Intent(Intent.ACTION_VIEW)
            .setDataAndType(shareUri(job.output), mimeOf(job.output))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(view);
        } catch (Exception e) {
            Toast.makeText(this, "No app can open this file", Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
