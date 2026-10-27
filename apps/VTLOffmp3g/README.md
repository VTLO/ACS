# VTLOffmp3g

Android app that runs every command of [`tools/ffmpeg.sh`](../../tools/ffmpeg.sh) on the phone,
powered by an FFmpegKit *full-gpl* build (libx264, libvpx, libopus, libmp3lame, libass…).

| Cheatsheet section    | In the app                                                                 |
|-----------------------|----------------------------------------------------------------------------|
| Transcode             | Anything → MP4, MP4 → WebM, Remux                                          |
| Compression & quality | CRF, slow preset, two-pass (both passes run in sequence)                   |
| Audio                 | Extract MP3 / M4A / stream copy, replace audio track                       |
| GIF                   | Palette generation + palette use (two steps, one tap)                      |
| Cut & join            | Fast cut, frame-accurate cut, concat (list file generated for you)         |
| Scale, crop, overlay  | Exact scale, scale by width, crop, watermark, burned-in subtitles          |
| Frames & streaming    | Poster frame, frame dump (saved to a folder), push to RTMP                 |
| Custom                | Any ffmpeg command you type                                                |

Each screen lets you pick the input files, edit the parameters (CRF, bitrate, times, sizes…),
and edit the final command itself. `<input>`, `<output>`, `<audio>`, `<logo>`… are replaced by
your files when the job runs. Results can be saved anywhere (Storage Access Framework), shared,
or opened. Long encodes keep running in the background with a progress notification.

## Build

Requirements: JDK 17+, Android SDK (platform 35).

```sh
cd apps/VTLOffmp3g
echo "sdk.dir=$ANDROID_HOME" > local.properties   # if ANDROID_HOME isn't picked up
./gradlew assembleRelease
```

APKs land in `app/build/outputs/apk/release/` — one per ABI. Most phones need
`app-arm64-v8a-release.apk`; older 32-bit phones need `app-armeabi-v7a-release.apk`.

The release build is signed with the local debug key so it installs directly. Use your own
`signingConfig` before publishing to a store. The **VTLOffmp3g APK** GitHub Actions workflow
builds the same APKs and uploads them as an artifact.

FFmpeg is GPL-licensed in this build (x264), so the app as a whole falls under the GPL.
