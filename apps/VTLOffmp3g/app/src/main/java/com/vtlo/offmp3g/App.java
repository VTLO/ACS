package com.vtlo.offmp3g;

import android.app.Application;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashReport.install(this);
        // FFmpegKit is initialised lazily on the job thread (see Job.initFFmpeg): loading its
        // native libraries here would block startup and crash the app if they fail to load.
        Job.cleanUp(this);
    }
}
