package com.vtlo.offmp3g;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

/** Foreground service that keeps the process alive while a long encode runs. */
public class JobService extends Service implements Job.Listener {

    private static final String CHANNEL = "jobs";
    private static final int NOTIFICATION_ID = 1;

    private Job job;
    private NotificationManager nm;

    static void start(Context context) {
        ContextCompat.startForegroundService(context, new Intent(context, JobService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        nm = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "FFmpeg jobs",
                NotificationManager.IMPORTANCE_LOW));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int type = 0;
        if (Build.VERSION.SDK_INT >= 35) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING;
        } else if (Build.VERSION.SDK_INT >= 29) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
        }
        Job current = Job.current();
        ServiceCompat.startForeground(this, NOTIFICATION_ID, build(current), type);
        if (current == null || current.state() != Job.State.RUNNING) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (job != current) {
            if (job != null) job.removeListener(this);
            job = current;
            job.addListener(this);
            // The job may have ended between the check above and registering the listener.
            if (job.state() != Job.State.RUNNING) onJobUpdate(job);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onJobUpdate(Job j) {
        if (j.state() == Job.State.RUNNING) {
            nm.notify(NOTIFICATION_ID, build(j));
        } else {
            j.removeListener(this);
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
            stopSelf();
        }
    }

    @Override
    public void onTimeout(int startId, int fgsType) {
        // Android 15 limits background media processing; stop cleanly instead of crashing.
        if (job != null) job.cancel();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (job != null) job.removeListener(this);
        super.onDestroy();
    }

    private Notification build(Job j) {
        Intent open = new Intent(this, MainActivity.class);
        if (j != null) {
            open = new Intent(this, RecipeActivity.class).putExtra(RecipeActivity.EXTRA_RECIPE, j.recipe.id);
        }
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(j == null ? getString(R.string.app_name) : j.recipe.title)
            .setContentText(j == null ? "" : j.status() + " · " + j.processedTime())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi);
        if (j != null) {
            double p = j.progress();
            if (p < 0) b.setProgress(0, 0, true);
            else b.setProgress(1000, (int) (p * 1000), false);
        }
        return b.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
