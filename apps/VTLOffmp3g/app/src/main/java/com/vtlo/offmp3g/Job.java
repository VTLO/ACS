package com.vtlo.offmp3g;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegKitConfig;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.FFprobeKit;
import com.arthenica.ffmpegkit.MediaInformation;
import com.arthenica.ffmpegkit.MediaInformationSession;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Signal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A single running (or finished) ffmpeg recipe. Only one job runs at a time. */
public final class Job {

    public enum State { RUNNING, DONE, FAILED, CANCELLED }

    public interface Listener {
        void onJobUpdate(Job job);
    }

    private static final Pattern PLACEHOLDER = Pattern.compile("<(\\w+)>");
    private static final int MAX_LOG_CHARS = 200_000;

    private static Job current;

    public static synchronized Job current() {
        return current;
    }

    public static synchronized boolean isRunning() {
        return current != null && current.state == State.RUNNING;
    }

    public final Recipe recipe;
    public final File dir;
    /** The result: a file, a directory of frames, or null for streaming. */
    public final File output;

    private final Context app;
    private final Map<String, List<Uri>> picks;
    private final String commandText;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final StringBuilder log = new StringBuilder();
    private final List<Listener> listeners = new ArrayList<>();
    /** Mirror of the log on disk, so a crash report can show what ffmpeg was doing. */
    private final Writer journal;

    private volatile State state = State.RUNNING;
    private volatile String status = "Starting…";
    private volatile double durationMs = -1;
    private volatile double timeMs = 0;
    private volatile double speed = 0;
    private volatile int stepIndex = 0;
    private volatile long sessionId = -1;
    private volatile boolean cancelRequested;
    private boolean updatePosted;

    private Job(Context context, Recipe recipe, Map<String, List<Uri>> picks,
                String commandText, String outputName) {
        this.app = context.getApplicationContext();
        this.recipe = recipe;
        this.picks = picks;
        this.commandText = commandText;
        this.journal = CrashReport.openJournal(recipe, commandText);
        this.dir = new File(new File(app.getCacheDir(), "jobs"), String.valueOf(System.currentTimeMillis()));
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        switch (recipe.outputKind) {
            case FILE: output = new File(dir, outputName); break;
            case DIRECTORY: output = new File(dir, outputName); output.mkdirs(); break;
            default: output = null;
        }
    }

    public static synchronized Job start(Context context, Recipe recipe, Map<String, List<Uri>> picks,
                                         String commandText, String outputName) {
        if (isRunning()) throw new IllegalStateException("A job is already running");
        if (current != null) deleteRecursive(current.dir);
        Job job = new Job(context, recipe, picks, commandText, outputName);
        current = job;
        new Thread(job::run, "vtlo-ffmpeg-job").start();
        JobService.start(context);
        return job;
    }

    /** Removes leftovers of earlier runs (results are only kept until the next job). */
    public static synchronized void cleanUp(Context context) {
        File jobs = new File(context.getCacheDir(), "jobs");
        File[] children = jobs.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (current == null || !f.equals(current.dir)) deleteRecursive(f);
        }
    }

    public State state() { return state; }
    public String commandText() { return commandText; }
    public String status() { return status; }
    public int stepIndex() { return stepIndex; }
    public int stepCount() { return commandLines(commandText).size(); }
    public double speed() { return speed; }

    /** 0..1, or -1 when the duration is unknown. */
    public double progress() {
        if (durationMs <= 0) return -1;
        return Math.max(0, Math.min(1, timeMs / durationMs));
    }

    public String processedTime() {
        return formatMs(timeMs) + (durationMs > 0 ? " / " + formatMs(durationMs) : "");
    }

    public String log() {
        synchronized (log) {
            return log.toString();
        }
    }

    public void cancel() {
        cancelRequested = true;
        long id = sessionId;
        if (id != -1) FFmpegKit.cancel(id);
    }

    /** Adds an app message (not ffmpeg output) to the job log. */
    void note(String message) {
        appendLog("\n[VTLOffmp3g] " + message + "\n");
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    // ------------------------------------------------------------------------------------------

    private static boolean ffmpegReady;

    /** Loads FFmpegKit's native libraries on first use. */
    private static synchronized void initFFmpeg(Context context) {
        if (ffmpegReady) return;
        // By default ffmpeg installs handlers for these signals and calls exit() after a few of
        // them, which kills the whole app. Leave them to Android; FFmpegKit cancels via a flag.
        for (Signal signal : Signal.values()) FFmpegKitConfig.ignoreSignal(signal);
        // libass (burned-in subtitles) needs a font directory on Android.
        FFmpegKitConfig.setFontDirectory(context, "/system/fonts", null);
        // -vpre looks up <name>.ffpreset in $FFMPEG_DATADIR.
        FFmpegKitConfig.setEnvironmentVariable("FFMPEG_DATADIR", installPresets(context).getAbsolutePath());
        ffmpegReady = true;
    }

    /** Copies the bundled .ffpreset files (assets/ffpresets) to a real directory. */
    private static File installPresets(Context context) {
        File dir = new File(context.getFilesDir(), "ffpresets");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        try {
            String[] names = context.getAssets().list("ffpresets");
            if (names != null) {
                for (String name : names) {
                    try (InputStream in = context.getAssets().open("ffpresets/" + name);
                         OutputStream out = new FileOutputStream(new File(dir, name))) {
                        copy(in, out);
                    }
                }
            }
        } catch (IOException e) {
            android.util.Log.w("VTLOffmp3g", "Could not install ffmpeg presets", e);
        }
        return dir;
    }

    private void run() {
        try {
            status = "Loading FFmpeg";
            notifyUpdate();
            initFFmpeg(app);
            Map<String, String> copied = prepareInputs();
            if (cancelRequested) { finish(State.CANCELLED, "Cancelled"); return; }
            probeDuration();

            List<String> lines = commandLines(commandText);
            for (int i = 0; i < lines.size(); i++) {
                stepIndex = i;
                timeMs = 0;
                String[] args = resolve(lines.get(i), copied);
                status = lines.size() > 1 ? "Running step " + (i + 1) + "/" + lines.size() : "Running";
                appendLog("\n$ ffmpeg " + String.join(" ", args) + "\n\n");
                notifyUpdate();

                FFmpegSession session = FFmpegSession.create(args, null,
                    l -> appendLog(l.getMessage()),
                    s -> {
                        timeMs = s.getTime();
                        speed = s.getSpeed();
                        notifyUpdate();
                    });
                sessionId = session.getSessionId();
                if (cancelRequested) { finish(State.CANCELLED, "Cancelled"); return; }
                FFmpegKitConfig.ffmpegExecute(session);
                sessionId = -1;

                ReturnCode rc = session.getReturnCode();
                if (cancelRequested || ReturnCode.isCancel(rc)) {
                    finish(State.CANCELLED, "Cancelled");
                    return;
                }
                if (!ReturnCode.isSuccess(rc)) {
                    String trace = session.getFailStackTrace();
                    if (trace != null) appendLog("\n" + trace);
                    finish(State.FAILED, "Failed (exit code " + (rc == null ? "?" : rc.getValue()) + ")"
                        + (lines.size() > 1 ? " at step " + (i + 1) : "") + " — see log");
                    return;
                }
            }
            if (output != null && !hasResult()) {
                finish(State.FAILED, "ffmpeg finished but produced no output");
                return;
            }
            timeMs = durationMs;
            finish(State.DONE, recipe.outputKind == Recipe.OutputKind.NONE ? "Stream finished" : "Done");
        } catch (Throwable e) {
            // Also catches native-library load errors so they are reported instead of crashing.
            appendLog("\nERROR: " + e + (e.getCause() != null ? "\nCaused by: " + e.getCause() : "") + "\n");
            finish(State.FAILED, e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private boolean hasResult() {
        if (output.isDirectory()) {
            String[] files = output.list();
            return files != null && files.length > 0;
        }
        return output.isFile() && output.length() > 0;
    }

    /** Copies the inputs that need a real path and writes the concat list. Returns key → path. */
    private Map<String, String> prepareInputs() throws IOException {
        Map<String, String> paths = new HashMap<>();
        for (Recipe.Input in : recipe.inputs) {
            List<Uri> uris = picks.get(in.key);
            if (!in.copy || uris == null || uris.isEmpty()) continue;
            List<String> files = new ArrayList<>();
            for (int i = 0; i < uris.size(); i++) {
                status = "Copying " + in.label.toLowerCase() + " (" + (i + 1) + "/" + uris.size() + ")";
                notifyUpdate();
                String ext = extension(displayName(app, uris.get(i)));
                File f = new File(dir, "in_" + in.key + "_" + (i + 1) + (ext.isEmpty() ? "" : "." + ext));
                copy(app, uris.get(i), f);
                files.add(f.getAbsolutePath());
                if (cancelRequested) return paths;
            }
            if (in.multiple) {
                File list = new File(dir, "list.txt");
                try (Writer w = new OutputStreamWriter(new FileOutputStream(list), StandardCharsets.UTF_8)) {
                    for (String p : files) w.write("file '" + p.replace("'", "'\\''") + "'\n");
                }
                paths.put("list", list.getAbsolutePath());
            } else {
                paths.put(in.key, files.get(0));
            }
        }
        return paths;
    }

    private void probeDuration() {
        List<Uri> main = picks.get("input");
        if (main == null || main.isEmpty()) {
            main = picks.get("videos");
        }
        if (main == null || main.isEmpty()) return;
        status = "Reading media info";
        notifyUpdate();
        try {
            double total = 0;
            for (Uri uri : main) {
                MediaInformationSession s = FFprobeKit.getMediaInformation(
                    FFmpegKitConfig.getSafParameterForRead(app, uri));
                MediaInformation info = s.getMediaInformation();
                if (info == null || info.getDuration() == null) return;
                total += Double.parseDouble(info.getDuration()) * 1000;
            }
            durationMs = total;
        } catch (Exception ignored) {
            // Progress is shown as indeterminate when the duration is unknown.
        }
    }

    private String[] resolve(String line, Map<String, String> copied) {
        String[] parsed = FFmpegKitConfig.parseArguments(line);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < parsed.length; i++) {
            String token = parsed[i];
            if (i == 0 && token.equals("ffmpeg")) continue;
            Matcher m = PLACEHOLDER.matcher(token);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                m.appendReplacement(sb, Matcher.quoteReplacement(placeholder(m.group(1), copied)));
            }
            m.appendTail(sb);
            out.add(sb.toString());
        }
        if (out.isEmpty()) throw new IllegalArgumentException("Empty command");
        return out.toArray(new String[0]);
    }

    private String placeholder(String name, Map<String, String> copied) {
        if (copied.containsKey(name)) return copied.get(name);
        switch (name) {
            case "output":
                if (output == null || output.isDirectory()) break;
                return output.getAbsolutePath();
            case "outdir":
                if (output == null || !output.isDirectory()) break;
                return output.getAbsolutePath();
            case "palette": return new File(dir, "palette.png").getAbsolutePath();
            case "passlog": return new File(dir, "passlog").getAbsolutePath();
        }
        Recipe.Input in = recipe.findInput(name);
        if (in != null && !in.multiple) {
            List<Uri> uris = picks.get(name);
            if (uris == null || uris.isEmpty()) throw new IllegalArgumentException("Pick: " + in.label);
            // A SAF parameter can be opened only once, so every occurrence gets a fresh one.
            return FFmpegKitConfig.getSafParameterForRead(app, uris.get(0));
        }
        throw new IllegalArgumentException("Unknown placeholder <" + name + "> for this recipe");
    }

    private void appendLog(String s) {
        synchronized (log) {
            log.append(s);
            if (log.length() > MAX_LOG_CHARS) log.delete(0, log.length() - MAX_LOG_CHARS);
            if (journal != null) {
                try {
                    journal.write(s);
                    journal.flush();
                } catch (IOException ignored) {
                    // The journal is best effort.
                }
            }
        }
        notifyUpdate();
    }

    private void finish(State s, String message) {
        synchronized (log) {
            CrashReport.closeJournal(journal, message);
        }
        status = message;
        state = s;
        notifyUpdate();
    }

    private void notifyUpdate() {
        synchronized (this) {
            if (updatePosted && state == State.RUNNING) return;
            updatePosted = true;
        }
        main.postDelayed(() -> {
            synchronized (this) {
                updatePosted = false;
            }
            for (Listener l : new ArrayList<>(listeners)) l.onJobUpdate(this);
        }, state == State.RUNNING ? 200 : 0);
    }

    // ------------------------------------------------------------------------------------------

    public static List<String> commandLines(String text) {
        List<String> lines = new ArrayList<>();
        for (String l : text.split("\n")) {
            if (!l.trim().isEmpty()) lines.add(l.trim());
        }
        return lines;
    }

    /** Names of the &lt;placeholders&gt; used anywhere in the command text. */
    public static List<String> placeholders(String text) {
        List<String> names = new ArrayList<>();
        Matcher m = PLACEHOLDER.matcher(text);
        while (m.find()) if (!names.contains(m.group(1))) names.add(m.group(1));
        return names;
    }

    public static String displayName(Context context, Uri uri) {
        try (Cursor c = context.getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) return c.getString(0);
        } catch (Exception ignored) {
            // fall through
        }
        String last = uri.getLastPathSegment();
        return last == null ? "file" : last;
    }

    public static String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        return name.substring(dot + 1).replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    public static String stem(String name) {
        int dot = name.lastIndexOf('.');
        String s = dot > 0 ? name.substring(0, dot) : name;
        s = s.replaceAll("[^A-Za-z0-9._-]", "_");
        return s.isEmpty() ? "output" : s;
    }

    static void copy(Context context, Uri from, File to) throws IOException {
        try (InputStream in = context.getContentResolver().openInputStream(from);
             OutputStream out = new FileOutputStream(to)) {
            if (in == null) throw new IOException("Cannot open " + from);
            copy(in, out);
        }
    }

    static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[1 << 16];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
    }

    static String formatMs(double ms) {
        long total = (long) (ms / 1000);
        return String.format(java.util.Locale.US, "%02d:%02d:%02d", total / 3600, (total / 60) % 60, total % 60);
    }

    private static void deleteRecursive(File f) {
        File[] children = f.listFiles();
        if (children != null) for (File c : children) deleteRecursive(c);
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
