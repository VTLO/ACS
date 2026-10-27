package com.vtlo.offmp3g;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Explains, on the next launch, why the app died last time: Java crashes are written by an
 * uncaught-exception handler; native crashes, ANRs, low-memory kills and exit() calls come from
 * ApplicationExitInfo. The running job keeps a journal (command + ffmpeg log) so a report says
 * what was being processed.
 */
final class CrashReport {

    private static final String JAVA_CRASH = "last_crash.txt";
    private static final String JOURNAL = "job_journal.txt";
    private static final String FINISHED = "\n[job finished]\n";
    private static final String PREFS = "crash_report";
    private static final String LAST_EXIT_SEEN = "last_exit_seen";
    private static final int TAIL_CHARS = 6000;

    private static File dir;

    private CrashReport() {}

    static void install(Context context) {
        dir = context.getFilesDir();
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            try (PrintWriter w = new PrintWriter(new FileWriter(new File(dir, JAVA_CRASH)))) {
                w.println("Java crash on thread \"" + thread.getName() + "\" at " + now());
                e.printStackTrace(w);
            } catch (IOException ignored) {
                // Nothing more we can do while crashing.
            }
            if (previous != null) previous.uncaughtException(thread, e);
        });
    }

    // --- job journal -----------------------------------------------------------------------------

    /** Starts a new journal for a job; returns a writer the job appends its log to. */
    static Writer openJournal(Recipe recipe, String commandText) {
        if (dir == null) return null;
        try {
            Writer w = new OutputStreamWriter(new FileOutputStream(new File(dir, JOURNAL)), StandardCharsets.UTF_8);
            w.write("Recipe: " + recipe.title + " (" + recipe.id + ")\nStarted: " + now()
                + "\nCommand:\n" + commandText + "\n\nLog:\n");
            w.flush();
            return w;
        } catch (IOException e) {
            return null;
        }
    }

    static void closeJournal(Writer journal, String status) {
        if (journal == null) return;
        try {
            journal.write("\n" + status + FINISHED);
            journal.close();
        } catch (IOException ignored) {
            // The journal is best effort.
        }
    }

    // --- report ----------------------------------------------------------------------------------

    /** What went wrong last time, or null if the app ended normally. Each problem is reported once. */
    static String collect(Context context) {
        StringBuilder report = new StringBuilder();
        File javaCrash = new File(context.getFilesDir(), JAVA_CRASH);
        if (javaCrash.isFile()) {
            report.append(read(javaCrash, 20_000)).append('\n');
            //noinspection ResultOfMethodCallIgnored
            javaCrash.delete();
        }
        String exit = lastAbnormalExit(context);
        if (exit != null) report.append(exit).append('\n');

        File journal = new File(context.getFilesDir(), JOURNAL);
        String journalText = journal.isFile() ? read(journal, 200_000) : "";
        boolean diedDuringJob = !journalText.isEmpty() && !journalText.endsWith(FINISHED);
        if (diedDuringJob) {
            report.append("The app stopped while this job was running:\n\n").append(tail(journalText)).append('\n');
            //noinspection ResultOfMethodCallIgnored
            journal.delete();
        }
        if (report.length() == 0) return null;
        return "VTLOffmp3g " + BuildConfig.VERSION_NAME + " · Android " + Build.VERSION.RELEASE
            + " (API " + Build.VERSION.SDK_INT + ", " + Build.DISPLAY + ") · " + Build.MANUFACTURER + " "
            + Build.MODEL + " · " + String.join(",", Build.SUPPORTED_ABIS) + "\n\n" + report;
    }

    private static String lastAbnormalExit(Context context) {
        if (Build.VERSION.SDK_INT < 30) return null;
        ActivityManager am = context.getSystemService(ActivityManager.class);
        List<ApplicationExitInfo> exits = am.getHistoricalProcessExitReasons(context.getPackageName(), 0, 5);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long seen = prefs.getLong(LAST_EXIT_SEEN, 0);
        if (exits.isEmpty()) return null;
        prefs.edit().putLong(LAST_EXIT_SEEN, exits.get(0).getTimestamp()).apply();
        for (ApplicationExitInfo info : exits) {
            if (info.getTimestamp() <= seen) break;
            if (!isAbnormal(info)) continue;
            StringBuilder sb = new StringBuilder("Process exit at ")
                .append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(info.getTimestamp())))
                .append(": ").append(reasonName(info.getReason()))
                .append(", status ").append(info.getStatus())
                .append(", importance ").append(info.getImportance());
            if (info.getDescription() != null) sb.append("\n").append(info.getDescription());
            // ANR traces are text; native crash traces are binary tombstones.
            if (info.getReason() == ApplicationExitInfo.REASON_ANR) {
                try (InputStream in = info.getTraceInputStream()) {
                    if (in != null) {
                        byte[] buf = new byte[TAIL_CHARS];
                        int n = in.read(buf);
                        if (n > 0) sb.append("\n").append(new String(buf, 0, n, StandardCharsets.UTF_8));
                    }
                } catch (IOException ignored) {
                    // Trace unavailable.
                }
            }
            return sb.toString();
        }
        return null;
    }

    private static boolean isAbnormal(ApplicationExitInfo info) {
        switch (info.getReason()) {
            case ApplicationExitInfo.REASON_CRASH:
            case ApplicationExitInfo.REASON_CRASH_NATIVE:
            case ApplicationExitInfo.REASON_ANR:
            case ApplicationExitInfo.REASON_LOW_MEMORY:
            case ApplicationExitInfo.REASON_SIGNALED:
            case ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE:
            case ApplicationExitInfo.REASON_INITIALIZATION_FAILURE:
            case ApplicationExitInfo.REASON_DEPENDENCY_DIED:
                return true;
            case ApplicationExitInfo.REASON_EXIT_SELF:
                // e.g. ffmpeg calling exit(); 0 is a normal System.exit(0).
                return info.getStatus() != 0;
            default:
                return false;
        }
    }

    private static String reasonName(int reason) {
        switch (reason) {
            case ApplicationExitInfo.REASON_CRASH: return "Java crash";
            case ApplicationExitInfo.REASON_CRASH_NATIVE: return "native crash";
            case ApplicationExitInfo.REASON_ANR: return "not responding (ANR)";
            case ApplicationExitInfo.REASON_LOW_MEMORY: return "killed for low memory";
            case ApplicationExitInfo.REASON_SIGNALED: return "killed by a signal";
            case ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE: return "killed for excessive resource use";
            case ApplicationExitInfo.REASON_INITIALIZATION_FAILURE: return "initialization failure";
            case ApplicationExitInfo.REASON_DEPENDENCY_DIED: return "a dependency died";
            case ApplicationExitInfo.REASON_EXIT_SELF: return "process called exit()";
            default: return "reason " + reason;
        }
    }

    private static String tail(String s) {
        return s.length() <= TAIL_CHARS ? s : "…" + s.substring(s.length() - TAIL_CHARS);
    }

    private static String read(File f, int maxBytes) {
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            long start = Math.max(0, raf.length() - maxBytes);
            raf.seek(start);
            byte[] buf = new byte[(int) (raf.length() - start)];
            raf.readFully(buf);
            return new String(buf, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }
}
