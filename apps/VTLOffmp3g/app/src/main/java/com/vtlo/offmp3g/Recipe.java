package com.vtlo.offmp3g;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One runnable entry of tools/ffmpeg.sh.
 *
 * Step templates use two kinds of placeholders:
 *   {name}  - a user-editable parameter, substituted when the command preview is built
 *   <name>  - a file, resolved only at run time (picked inputs, the output, temp files)
 */
public final class Recipe {

    public enum OutputKind { FILE, DIRECTORY, NONE }

    public static final class Input {
        public final String key;
        public final String label;
        public final String mime;
        /** Several files at once (used by the concat recipe). */
        public final boolean multiple;
        /** Copy to a real path instead of streaming via SAF (subtitle filter, concat list). */
        public final boolean copy;

        Input(String key, String label, String mime, boolean multiple, boolean copy) {
            this.key = key;
            this.label = label;
            this.mime = mime;
            this.multiple = multiple;
            this.copy = copy;
        }
    }

    public static final class Param {
        public final String key;
        public final String label;
        public final String defaultValue;

        Param(String key, String label, String defaultValue) {
            this.key = key;
            this.label = label;
            this.defaultValue = defaultValue;
        }
    }

    public final String id;
    public final String section;
    public final String title;
    public final String description;
    /** The line exactly as it appears in the cheatsheet. */
    public final String original;
    public final List<Input> inputs;
    public final List<Param> params;
    public final List<String> steps;
    public final OutputKind outputKind;
    /** Output extension, may reference params, e.g. "{format}". */
    public final String outputExt;
    public final String outputMime;

    private Recipe(Builder b) {
        id = b.id;
        section = b.section;
        title = b.title;
        description = b.description;
        original = b.original;
        inputs = Collections.unmodifiableList(b.inputs);
        params = Collections.unmodifiableList(b.params);
        steps = Collections.unmodifiableList(b.steps);
        outputKind = b.outputKind;
        outputExt = b.outputExt;
        outputMime = b.outputMime;
    }

    public Map<String, String> defaultParams() {
        Map<String, String> m = new LinkedHashMap<>();
        for (Param p : params) m.put(p.key, p.defaultValue);
        return m;
    }

    public static String applyParams(String template, Map<String, String> values) {
        String s = template;
        for (Map.Entry<String, String> e : values.entrySet()) {
            s = s.replace("{" + e.getKey() + "}", e.getValue());
        }
        return s;
    }

    /** The full multi-line command text shown to (and editable by) the user. */
    public String buildCommand(Map<String, String> values) {
        StringBuilder sb = new StringBuilder();
        for (String step : steps) {
            if (sb.length() > 0) sb.append('\n');
            sb.append("ffmpeg ").append(applyParams(step, values));
        }
        return sb.toString();
    }

    public Input findInput(String key) {
        for (Input i : inputs) if (i.key.equals(key)) return i;
        return null;
    }

    // ------------------------------------------------------------------------------------------

    private static final class Builder {
        final String id, section, title;
        String description = "", original = "";
        final List<Input> inputs = new ArrayList<>();
        final List<Param> params = new ArrayList<>();
        final List<String> steps = new ArrayList<>();
        OutputKind outputKind = OutputKind.FILE;
        String outputExt = "mp4", outputMime = "video/mp4";

        Builder(String id, String section, String title) {
            this.id = id;
            this.section = section;
            this.title = title;
        }

        Builder desc(String d) { description = d; return this; }
        Builder orig(String o) { original = o; return this; }
        Builder video() { return in("input", "Input video", "video/*"); }
        Builder in(String key, String label, String mime) {
            inputs.add(new Input(key, label, mime, false, false)); return this;
        }
        Builder inCopy(String key, String label, String mime) {
            inputs.add(new Input(key, label, mime, false, true)); return this;
        }
        Builder inMulti(String key, String label, String mime) {
            inputs.add(new Input(key, label, mime, true, true)); return this;
        }
        Builder param(String key, String label, String def) {
            params.add(new Param(key, label, def)); return this;
        }
        Builder step(String s) { steps.add(s); return this; }
        Builder out(String ext, String mime) { outputExt = ext; outputMime = mime; return this; }
        Builder outDir() { outputKind = OutputKind.DIRECTORY; outputExt = ""; outputMime = "image/jpeg"; return this; }
        Builder noOut() { outputKind = OutputKind.NONE; outputExt = ""; outputMime = ""; return this; }
        Recipe build() { return new Recipe(this); }
    }

    public static final String TRANSCODE = "Transcode";
    public static final String COMPRESSION = "Compression & quality";
    public static final String AUDIO = "Audio";
    public static final String GIF = "GIF";
    public static final String CUT = "Cut & join";
    public static final String SCALE = "Scale, crop, overlay";
    public static final String FRAMES = "Frames & streaming";
    public static final String CUSTOM = "Custom";

    public static final List<Recipe> ALL = Arrays.asList(
        // --- TRANSCODE ---------------------------------------------------------------------
        new Builder("to_mp4", TRANSCODE, "Anything → MP4 (H.264 + AAC)")
            .desc("Plays everywhere.")
            .orig("ffmpeg -i input.avi -c:v libx264 -c:a aac output.mp4")
            .video()
            .step("-i <input> -c:v libx264 -c:a aac <output>")
            .build(),
        new Builder("to_webm", TRANSCODE, "MP4 → WebM (VP9 + Opus)")
            .desc("For the web. VP9 software encoding is slow on phones.")
            .orig("ffmpeg -i input.mp4 -c:v libvpx-vp9 -b:v 2M -c:a libopus output.webm")
            .video()
            .param("vbitrate", "Video bitrate", "2M")
            .step("-i <input> -c:v libvpx-vp9 -b:v {vbitrate} -c:a libopus <output>")
            .out("webm", "video/webm")
            .build(),
        new Builder("remux", TRANSCODE, "Remux only (change container)")
            .desc("No re-encoding, no quality loss.")
            .orig("ffmpeg -i input.mkv -c copy output.mp4")
            .video()
            .param("format", "Output container (mp4, mkv, mov…)", "mp4")
            .step("-i <input> -c copy <output>")
            .out("{format}", "video/*")
            .build(),

        // --- COMPRESSION -------------------------------------------------------------------
        new Builder("crf", COMPRESSION, "Compress with CRF")
            .desc("CRF 18 = high quality, 28 = small file, 23 = sweet spot.")
            .orig("ffmpeg -i input.mp4 -c:v libx264 -crf 23 -preset medium -c:a aac -b:a 128k output.mp4")
            .video()
            .param("crf", "CRF (0-51)", "23")
            .param("preset", "Preset", "medium")
            .param("abitrate", "Audio bitrate", "128k")
            .step("-i <input> -c:v libx264 -crf {crf} -preset {preset} -c:a aac -b:a {abitrate} <output>")
            .build(),
        new Builder("crf_slow", COMPRESSION, "Compress with slow preset")
            .desc("Slower preset = smaller file at same quality.")
            .orig("ffmpeg -i input.mp4 -c:v libx264 -crf 23 -preset slow output.mp4")
            .video()
            .param("crf", "CRF (0-51)", "23")
            .param("preset", "Preset", "slow")
            .step("-i <input> -c:v libx264 -crf {crf} -preset {preset} <output>")
            .build(),
        new Builder("two_pass", COMPRESSION, "Two-pass encoding (target bitrate)")
            .desc("Pass 1 analyses (no audio, output discarded), pass 2 hits a target size precisely. Both passes run in sequence.")
            .orig("ffmpeg -y -i input.mp4 -c:v libx264 -b:v 1M -pass 1 -an -f null /dev/null\n"
                + "ffmpeg -i input.mp4 -c:v libx264 -b:v 1M -pass 2 -c:a aac -b:a 128k output.mp4")
            .video()
            .param("vbitrate", "Video bitrate", "1M")
            .param("abitrate", "Audio bitrate", "128k")
            .step("-y -i <input> -c:v libx264 -b:v {vbitrate} -pass 1 -passlogfile <passlog> -an -f null /dev/null")
            .step("-i <input> -c:v libx264 -b:v {vbitrate} -pass 2 -passlogfile <passlog> -c:a aac -b:a {abitrate} <output>")
            .build(),

        // --- AUDIO -------------------------------------------------------------------------
        new Builder("audio_mp3", AUDIO, "Extract audio as MP3")
            .desc("-vn = no video, -q:a 2 = high VBR quality.")
            .orig("ffmpeg -i input.mp4 -vn -c:a libmp3lame -q:a 2 audio.mp3")
            .video()
            .param("q", "VBR quality (0 best - 9 worst)", "2")
            .step("-i <input> -vn -c:a libmp3lame -q:a {q} <output>")
            .out("mp3", "audio/mpeg")
            .build(),
        new Builder("audio_m4a", AUDIO, "Extract audio as M4A (AAC)")
            .orig("ffmpeg -i input.mp4 -vn -c:a aac -b:a 192k audio.m4a")
            .video()
            .param("abitrate", "Audio bitrate", "192k")
            .step("-i <input> -vn -c:a aac -b:a {abitrate} <output>")
            .out("m4a", "audio/mp4")
            .build(),
        new Builder("audio_copy", AUDIO, "Extract audio without re-encoding")
            .desc("Fastest. The extension must fit the source codec (aac, mp3, opus, ac3…).")
            .orig("ffmpeg -i input.mp4 -vn -c:a copy audio.aac")
            .video()
            .param("format", "Output extension", "aac")
            .step("-i <input> -vn -c:a copy <output>")
            .out("{format}", "audio/*")
            .build(),
        new Builder("replace_audio", AUDIO, "Replace a video's audio track")
            .orig("ffmpeg -i video.mp4 -i new_audio.mp3 -c:v copy -c:a aac -map 0:v:0 -map 1:a:0 -shortest output.mp4")
            .in("input", "Video", "video/*")
            .in("audio", "New audio", "audio/*")
            .step("-i <input> -i <audio> -c:v copy -c:a aac -map 0:v:0 -map 1:a:0 -shortest <output>")
            .build(),

        // --- GIF ---------------------------------------------------------------------------
        new Builder("gif", GIF, "Video → optimized GIF")
            .desc("Step 1 generates an optimized colour palette, step 2 renders the GIF with it.")
            .orig("ffmpeg -i input.mp4 -vf \"fps=10,scale=480:-1:flags=lanczos,palettegen\" palette.png\n"
                + "ffmpeg -i input.mp4 -i palette.png -filter_complex \"fps=10,scale=480:-1:flags=lanczos[x];[x][1:v]paletteuse\" output.gif")
            .video()
            .param("fps", "Frames per second", "10")
            .param("width", "Width (px)", "480")
            .step("-i <input> -vf \"fps={fps},scale={width}:-1:flags=lanczos,palettegen\" <palette>")
            .step("-i <input> -i <palette> -filter_complex \"fps={fps},scale={width}:-1:flags=lanczos[x];[x][1:v]paletteuse\" <output>")
            .out("gif", "image/gif")
            .build(),

        // --- CUT & JOIN --------------------------------------------------------------------
        new Builder("cut_fast", CUT, "Fast cut (stream copy)")
            .desc("-ss before -i seeks fast; cuts land on keyframes.")
            .orig("ffmpeg -ss 00:01:30 -i input.mp4 -t 00:00:30 -c copy clip.mp4")
            .video()
            .param("start", "Start", "00:01:30")
            .param("duration", "Duration", "00:00:30")
            .param("format", "Output container", "mp4")
            .step("-ss {start} -i <input> -t {duration} -c copy <output>")
            .out("{format}", "video/*")
            .build(),
        new Builder("cut_accurate", CUT, "Frame-accurate cut (re-encode)")
            .desc("Re-encodes, slower but exact.")
            .orig("ffmpeg -i input.mp4 -ss 00:01:30 -t 00:00:30 -c:v libx264 -c:a aac clip.mp4")
            .video()
            .param("start", "Start", "00:01:30")
            .param("duration", "Duration", "00:00:30")
            .step("-i <input> -ss {start} -t {duration} -c:v libx264 -c:a aac <output>")
            .build(),
        new Builder("concat", CUT, "Join videos losslessly")
            .desc("Pick the clips in order. They must share the same codecs. The list file is generated for you.")
            .orig("ffmpeg -f concat -safe 0 -i list.txt -c copy joined.mp4")
            .inMulti("videos", "Videos (in order)", "video/*")
            .param("format", "Output container", "mp4")
            .step("-f concat -safe 0 -i <list> -c copy <output>")
            .out("{format}", "video/*")
            .build(),

        // --- SCALE, CROP, OVERLAY ----------------------------------------------------------
        new Builder("scale_exact", SCALE, "Scale to exact size")
            .orig("ffmpeg -i input.mp4 -vf \"scale=1280:720\" -c:a copy output.mp4")
            .video()
            .param("width", "Width", "1280")
            .param("height", "Height", "720")
            .step("-i <input> -vf \"scale={width}:{height}\" -c:a copy <output>")
            .build(),
        new Builder("scale_width", SCALE, "Scale by width, keep aspect ratio")
            .desc("-2 = even height (H.264 needs even dimensions).")
            .orig("ffmpeg -i input.mp4 -vf \"scale=640:-2\" output.mp4")
            .video()
            .param("width", "Width", "640")
            .step("-i <input> -vf \"scale={width}:-2\" <output>")
            .build(),
        new Builder("crop", SCALE, "Crop a region")
            .orig("ffmpeg -i input.mp4 -vf \"crop=640:480:100:50\" output.mp4")
            .video()
            .param("w", "Width", "640")
            .param("h", "Height", "480")
            .param("x", "X offset", "100")
            .param("y", "Y offset", "50")
            .step("-i <input> -vf \"crop={w}:{h}:{x}:{y}\" <output>")
            .build(),
        new Builder("watermark", SCALE, "Watermark (logo overlay)")
            .desc("Default position: top-right corner with a 10px margin.")
            .orig("ffmpeg -i input.mp4 -i logo.png -filter_complex \"overlay=W-w-10:10\" -c:a copy output.mp4")
            .video()
            .in("logo", "Logo image", "image/*")
            .param("pos", "Overlay position", "W-w-10:10")
            .step("-i <input> -i <logo> -filter_complex \"overlay={pos}\" -c:a copy <output>")
            .build(),
        new Builder("subtitles", SCALE, "Burn subtitles into the video")
            .desc("Uses libass with the system fonts.")
            .orig("ffmpeg -i input.mp4 -vf \"subtitles=subs.srt\" -c:a copy output.mp4")
            .video()
            .inCopy("subs", "Subtitle file (.srt/.ass)", "*/*")
            .step("-i <input> -vf \"subtitles=<subs>\" -c:a copy <output>")
            .build(),

        // --- FRAMES & STREAMING ------------------------------------------------------------
        new Builder("poster", FRAMES, "Extract one frame (thumbnail)")
            .orig("ffmpeg -ss 00:00:05 -i input.mp4 -vframes 1 poster.jpg")
            .video()
            .param("time", "Time", "00:00:05")
            .step("-ss {time} -i <input> -vframes 1 <output>")
            .out("jpg", "image/jpeg")
            .build(),
        new Builder("frames", FRAMES, "Dump frames as JPEGs")
            .desc("frame_001.jpg, frame_002.jpg, … Save them to a folder afterwards.")
            .orig("ffmpeg -i input.mp4 -vf fps=1 frame_%03d.jpg")
            .video()
            .param("fps", "Frames per second", "1")
            .step("-i <input> -vf fps={fps} <outdir>/frame_%03d.jpg")
            .outDir()
            .build(),
        new Builder("rtmp", FRAMES, "Push to an RTMP server")
            .desc("-re = read at native frame rate. Keep the app open while streaming.")
            .orig("ffmpeg -re -i input.mp4 -c:v libx264 -preset veryfast -b:v 2500k -c:a aac -b:a 128k -f flv rtmp://live.example.com/app/STREAM_KEY")
            .video()
            .param("url", "RTMP URL", "rtmp://live.example.com/app/STREAM_KEY")
            .param("vbitrate", "Video bitrate", "2500k")
            .param("abitrate", "Audio bitrate", "128k")
            .step("-re -i <input> -c:v libx264 -preset veryfast -b:v {vbitrate} -c:a aac -b:a {abitrate} -f flv {url}")
            .noOut()
            .build(),

        // --- CUSTOM ------------------------------------------------------------------------
        new Builder("custom", CUSTOM, "Custom command")
            .desc("Edit the command freely. <input> is the picked file, <output> the result file.")
            .video()
            .param("format", "Output extension", "mp4")
            .step("-i <input> -c:v libx264 -c:a aac <output>")
            .out("{format}", "*/*")
            .build()
    );

    public static Recipe byId(String id) {
        for (Recipe r : ALL) if (r.id.equals(id)) return r;
        return null;
    }
}
