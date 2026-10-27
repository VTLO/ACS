package com.vtlo.offmp3g;

import android.app.Application;

import com.arthenica.ffmpegkit.FFmpegKitConfig;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // libass (burned-in subtitles) needs a font directory on Android.
        FFmpegKitConfig.setFontDirectory(this, "/system/fonts", null);
        Job.cleanUp(this);
    }
}
