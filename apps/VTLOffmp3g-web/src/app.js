// VTLOffmp3g for the browser: the recipes of tools/ffmpeg.sh, run locally with ffmpeg.wasm.
// Files never leave the computer: they are processed in the browser's memory.
import { FFmpeg } from "./vendor/ffmpeg/index.js";

const ICONS = {
  convert: "M6.99,11L3,15l3.99,4v-3H14v-2H6.99v-3zM21,9l-3.99,-4v3H10v2h7.01v3L21,9z",
  audio: "M12,3v10.55c-0.59,-0.34 -1.27,-0.55 -2,-0.55 -2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4V7h4V3h-6z",
  edit: "M17,15h2V7c0,-1.1 -0.9,-2 -2,-2H9v2h8v8zM7,17V1H5v4H1v2h4v10c0,1.1 0.9,2 2,2h10v4h2v-4h4v-2H7z",
  frames: "M21,19V5c0,-1.1 -0.9,-2 -2,-2H5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2zM8.5,13.5l2.5,3.01L14.5,12l4.5,6H5l3.5,-4.5z",
  arrow: "M12,4l-1.41,1.41L16.17,11H4v2h12.17l-5.58,5.59L12,20l8,-8z",
  chevron: "M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z",
  play: "M8,5v14l11,-7z",
  stop: "M6,6h12v12H6z",
  back: "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z",
  file: "M14,2H6c-1.1,0 -2,0.9 -2,2v16c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V8l-6,-6zM13,9V3.5L18.5,9H13z",
  tune: "M3,17v2h6v-2H3zM3,5v2h10V5H3zM13,21v-2h8v-2h-8v-2h-2v6h2zM7,9v2H3v2h4v2h2V9H7zM21,13v-2H11v2h10zM15,9h2V7h4V5h-4V3h-2v6z",
  help: "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM13,19h-2v-2h2v2zM15.07,11.25l-0.9,0.92C13.45,12.9 13,13.5 13,15h-2v-0.5c0,-1.1 0.45,-2.1 1.17,-2.83l1.24,-1.26c0.37,-0.36 0.59,-0.86 0.59,-1.41 0,-1.1 -0.9,-2 -2,-2s-2,0.9 -2,2H8c0,-2.21 1.79,-4 4,-4s4,1.79 4,4c0,0.88 -0.36,1.68 -0.93,2.25z",
  bolt: "M7,2v11h3v9l7,-12h-4l4,-8z",
  save: "M19,9h-4V3H9v6H5l7,7 7,-7zM5,18v2h14v-2H5z",
  copy: "M16,1H4c-1.1,0 -2,0.9 -2,2v14h2V3h12V1zM19,5H8c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h11c1.1,0 2,-0.9 2,-2V7c0,-1.1 -0.9,-2 -2,-2zM19,21H8V7h11v14z",
  timer: "M15,1H9v2h6V1zM11,14h2V8h-2v6zM19.03,7.39l1.42,-1.42c-0.43,-0.51 -0.9,-0.99 -1.41,-1.41l-1.42,1.42C16.07,4.74 14.12,4 12,4c-4.97,0 -9,4.03 -9,9s4.02,9 9,9 9,-4.03 9,-9c0,-2.12 -0.74,-4.07 -1.97,-5.61zM12,20c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7 7,3.13 7,7 -3.13,7 -7,7z",
  storage: "M2,20h20v-4H2v4zM4,17h2v2H4v-2zM2,4v4h20V4H2zM6,7H4V5h2v2zM2,14h20v-4H2v4zM4,11h2v2H4v-2z",
  movie: "M18,4l2,4h-3l-2,-4h-2l2,4h-3l-2,-4H8l2,4H7L5,4H4c-1.1,0 -1.99,0.9 -1.99,2L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V4h-4z",
  subtitles: "M20,4H4c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V6c0,-1.1 -0.9,-2 -2,-2zM4,12h4v2H4v-2zM14,18H4v-2h10v2zM20,18h-4v-2h4v2zM20,14H10v-2h10v2z",
  shield: "M12,1L3,5v6c0,5.55 3.84,10.74 9,12 5.16,-1.26 9,-6.45 9,-12V5l-9,-4z",
};
const icon = (name) => `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="${ICONS[name]}"/></svg>`;

const TABS = [
  { id: "convert", label: "Convert", sections: ["Transcode", "Compression & quality"] },
  { id: "audio", label: "Audio", sections: ["Audio"] },
  { id: "edit", label: "Edit", sections: ["Cut & join", "Scale, crop, overlay", "GIF"] },
  { id: "frames", label: "Frames", sections: ["Frames & streaming", "Custom"] },
];

const $ = (sel, root = document) => root.querySelector(sel);
const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const app = $("#app");

let RECIPES = [];
let tab = "convert";
let query = "";
const forms = new Map(); // recipe id -> { picks, values, command }
let job = null;          // the current or last job
let engine = null;       // loaded FFmpeg instance
let engineSetup = false;

// ------------------------------------------------------------------------------------------------
// Recipe helpers (same rules as the Android app)

function applyParams(template, values) {
  let s = template;
  for (const [k, v] of Object.entries(values)) s = s.split("{" + k + "}").join(v);
  return s;
}
function defaults(r) {
  const v = {};
  for (const p of r.params) v[p.key] = p.default;
  return v;
}
function buildCommand(r, values) {
  return r.steps.map((s) => "ffmpeg " + applyParams(s, values)).join("\n");
}
function badge(r) {
  if (r.outputKind === "NONE") return "FLV";
  if (r.outputKind === "DIRECTORY") return "JPG";
  return applyParams(r.outputExt, defaults(r)).toUpperCase();
}
function webSupported(r) {
  return r.outputKind !== "NONE"; // RTMP streaming needs raw sockets, which browsers do not have.
}
function form(r) {
  if (!forms.has(r.id)) {
    const values = defaults(r);
    forms.set(r.id, { picks: {}, values, command: buildCommand(r, values) });
  }
  return forms.get(r.id);
}

/** Splits a command line like a shell: whitespace separates, quotes group. */
function parseArguments(line) {
  const out = [];
  let cur = "", quote = null, has = false;
  for (const c of line) {
    if (quote) {
      if (c === quote) quote = null; else cur += c;
    } else if (c === '"' || c === "'") {
      quote = c; has = true;
    } else if (/\s/.test(c)) {
      if (cur || has) out.push(cur);
      cur = ""; has = false;
    } else {
      cur += c;
    }
  }
  if (cur || has) out.push(cur);
  return out;
}

const fmtBytes = (b) => b >= 1 << 30 ? (b / 2 ** 30).toFixed(2) + " GB" : b >= 1 << 20 ? (b / 2 ** 20).toFixed(1) + " MB" : Math.max(1, Math.round(b / 1024)) + " kB";
const fmtTime = (s) => {
  s = Math.max(0, Math.floor(s));
  return [Math.floor(s / 3600), Math.floor(s / 60) % 60, s % 60].map((n) => String(n).padStart(2, "0")).join(":");
};
const hms = (t) => {
  const m = /(\d+):(\d+):(\d+(?:\.\d+)?)/.exec(t);
  return m ? +m[1] * 3600 + +m[2] * 60 + +m[3] : 0;
};
const stem = (name) => (name.replace(/\.[^.]*$/, "").replace(/[^A-Za-z0-9._-]/g, "_") || "output");
const extOf = (name) => { const m = /\.([A-Za-z0-9]+)$/.exec(name); return m ? m[1].toLowerCase() : ""; };

// ------------------------------------------------------------------------------------------------
// Gauge and scope drawing

function arcPath(cx, cy, r, start, sweep) {
  const rad = (d) => (d * Math.PI) / 180;
  const a0 = rad(start), a1 = rad(start + sweep);
  const x0 = cx + r * Math.cos(a0), y0 = cy + r * Math.sin(a0);
  const x1 = cx + r * Math.cos(a1), y1 = cy + r * Math.sin(a1);
  if (sweep >= 359.9) {
    const xm = cx + r * Math.cos(a0 + Math.PI), ym = cy + r * Math.sin(a0 + Math.PI);
    return `M${x0},${y0} A${r},${r} 0 1 1 ${xm},${ym} A${r},${r} 0 1 1 ${x0},${y0}`;
  }
  return `M${x0},${y0} A${r},${r} 0 ${sweep > 180 ? 1 : 0} 1 ${x1},${y1}`;
}
/** SVG arc gauge: sweep 360 = ring (home), 270 = open gauge (recipe). progress -1 = busy. */
function gaugeSVG(sweep, progress, track, color, stroke) {
  const r = 50 - stroke / 2 - 1;
  const start = sweep >= 360 ? -90 : 90 + (360 - sweep) / 2;
  const len = (Math.PI * r * sweep) / 180;
  const p = progress < 0 ? 0.22 : Math.max(0, Math.min(1, progress));
  const spin = progress < 0 ? `<animateTransform attributeName="transform" type="rotate" from="0 50 50" to="${sweep >= 360 ? 360 : sweep - 60} 50 50" dur="1.4s" repeatCount="indefinite"/>` : "";
  return `<svg viewBox="0 0 100 100">
    <path d="${arcPath(50, 50, r, start, sweep)}" fill="none" stroke="${track}" stroke-width="${stroke}" stroke-linecap="round"/>
    <path d="${arcPath(50, 50, r, start, sweep)}" fill="none" stroke="${color}" stroke-width="${stroke}" stroke-linecap="round"
      stroke-dasharray="${Math.max(0.01, len * p)} ${len + 10}" style="transition: stroke-dasharray .35s">${spin}</path>
  </svg>`;
}

function niceCeil(v) {
  const exp = Math.pow(10, Math.floor(Math.log10(v)));
  for (const s of [1, 2, 2.5, 5, 10]) if (s * exp >= v) return s * exp;
  return 10 * exp;
}
const compact = (v) => v >= 10000 ? (v / 1000).toFixed(0) + "k" : v >= 1000 ? (v / 1000).toFixed(1) + "k" : v >= 10 || v === 0 ? v.toFixed(0) : v.toFixed(1);

/** Spectrum-analyser style plot: fps (yellow, left axis) and kb/s (magenta, right axis). */
function drawScope(canvas, samples) {
  const dpr = window.devicePixelRatio || 1;
  const W = canvas.clientWidth, H = canvas.clientHeight;
  canvas.width = W * dpr; canvas.height = H * dpr;
  const g = canvas.getContext("2d");
  g.scale(dpr, dpr);
  g.fillStyle = "#000"; g.fillRect(0, 0, W, H);
  const padL = 36, padR = 42, padT = 10, padB = 18, w = W - padL - padR, h = H - padT - padB;
  for (let i = 0; i <= 16; i++) {
    g.strokeStyle = i % 4 === 0 ? "#1c2420" : "rgba(28,36,32,.45)";
    const x = padL + (w * i) / 16; g.beginPath(); g.moveTo(x, padT); g.lineTo(x, padT + h); g.stroke();
  }
  for (let i = 0; i <= 8; i++) {
    g.strokeStyle = i % 2 === 0 ? "#1c2420" : "rgba(28,36,32,.45)";
    const y = padT + (h * i) / 8; g.beginPath(); g.moveTo(padL, y); g.lineTo(padL + w, y); g.stroke();
  }
  g.font = "10px " + getComputedStyle(document.body).getPropertyValue("--mono");
  g.fillStyle = "#7e8a86";
  if (samples.length < 2) {
    g.textAlign = "center"; g.fillText("NO SIGNAL", padL + w / 2, padT + h / 2); return;
  }
  let maxT = 0, maxF = 1, maxR = 1;
  for (const s of samples) { maxT = Math.max(maxT, s[0]); maxF = Math.max(maxF, s[1]); maxR = Math.max(maxR, s[2]); }
  maxF = niceCeil(maxF); maxR = niceCeil(maxR);
  const minT = samples[0][0], span = Math.max(0.001, maxT - minT);
  for (let i = 0; i <= 4; i++) {
    const y = padT + h - (h * i) / 4 + 4;
    g.textAlign = "right"; g.fillText(compact((maxF * i) / 4), padL - 5, y);
    g.textAlign = "left"; g.fillText(compact((maxR * i) / 4), padL + w + 5, y);
    const t = Math.round(minT + (span * i) / 4);
    g.textAlign = "center"; g.fillText(`${Math.floor(t / 60)}:${String(t % 60).padStart(2, "0")}`, padL + (w * i) / 4, H - 4);
  }
  const series = (idx, max, color) => {
    g.strokeStyle = color; g.lineWidth = 1.6; g.lineJoin = "round"; g.beginPath();
    samples.forEach((s, i) => {
      const x = padL + (w * (s[0] - minT)) / span, y = padT + h - h * Math.min(1, s[idx] / max);
      i ? g.lineTo(x, y) : g.moveTo(x, y);
    });
    g.stroke();
  };
  series(2, maxR, "#d21e5a");
  series(1, maxF, "#f2d21b");
  const last = samples[samples.length - 1];
  const cx = padL + (w * (last[0] - minT)) / span, cy = padT + h - h * Math.min(1, last[1] / maxF);
  g.strokeStyle = "#18c59c"; g.lineWidth = 1; g.setLineDash([4, 3]);
  g.beginPath(); g.moveTo(cx, padT); g.lineTo(cx, padT + h); g.moveTo(padL, cy); g.lineTo(padL + w, cy); g.stroke();
  g.setLineDash([]);
  g.fillStyle = "#18c59c"; g.font = "11px " + getComputedStyle(document.body).getPropertyValue("--mono");
  const right = cx > padL + w / 2;
  g.textAlign = right ? "right" : "left";
  g.fillText(`${last[1].toFixed(1)} fps  ${compact(last[2])} kb/s`, right ? cx - 6 : cx + 6, Math.max(padT + 12, cy - 6));
}

// ------------------------------------------------------------------------------------------------
// Engine and jobs

async function loadEngine(onStatus) {
  if (engine) return engine;
  onStatus("Loading the FFmpeg engine (32 MB, once)…");
  const ff = new FFmpeg();
  ff.on("log", ({ message }) => job && job.onLog(message));
  ff.on("progress", ({ time }) => job && job.onProgress(time));
  const base = new URL("./vendor/core/", import.meta.url).href;
  await ff.load({ coreURL: base + "ffmpeg-core.js", wasmURL: base + "ffmpeg-core.wasm" });
  engine = ff;
  engineSetup = false;
  return ff;
}

async function setupEngine(ff) {
  if (engineSetup) return;
  // The bundled libvpx presets (used through -fpre, see presetArgs) and a font for libass.
  await ff.createDir("/presets").catch(() => {});
  PRESETS = await (await fetch("presets/index.json")).json();
  for (const name of PRESETS) {
    const data = new Uint8Array(await (await fetch("presets/" + name)).arrayBuffer());
    await ff.writeFile("/presets/" + name, data);
  }
  await ff.createDir("/fonts").catch(() => {});
  await ff.writeFile("/fonts/DejaVuSans.ttf", new Uint8Array(await (await fetch("fonts/DejaVuSans.ttf")).arrayBuffer()));
  engineSetup = true;
}

let PRESETS = [];

/**
 * ffmpeg.wasm has no preset search path, so "-vpre NAME" (the Android app's form) becomes
 * "-fpre /presets/NAME.ffpreset", which loads the same file by path.
 */
function presetArgs(args) {
  const out = [];
  for (let i = 0; i < args.length; i++) {
    if (args[i] === "-vpre" && i + 1 < args.length && PRESETS.includes(args[i + 1] + ".ffpreset")) {
      out.push("-fpre", `/presets/${args[i + 1]}.ffpreset`);
      i++;
    } else {
      out.push(args[i]);
    }
  }
  return out;
}

let jobCounter = 0;

class Job {
  constructor(recipe, picks, commandText, outputName) {
    this.recipe = recipe;
    this.picks = picks;
    this.commandText = commandText;
    this.outputName = outputName;
    this.state = "RUNNING";
    this.status = "Starting…";
    this.log = "";
    this.samples = [];
    this.duration = 0;
    this.time = 0;
    this.speed = 0;
    this.size = 0;
    this.outputs = [];
    this.dir = "/job" + ++jobCounter;
    this.listeners = new Set();
  }

  emit() {
    if (this.pending) return;
    this.pending = requestAnimationFrame(() => {
      this.pending = 0;
      this.listeners.forEach((f) => f(this));
    });
  }

  note(s) { this.onLog("[VTLOffmp3g] " + s); }

  onLog(line) {
    if (line === "Aborted()") return; // ffmpeg.wasm prints this on every exit, successful or not
    this.log += line + "\n";
    if (this.log.length > 200000) this.log = this.log.slice(-200000);
    let m;
    if (!this.duration && (m = /Duration: (\d+:\d+:\d+(?:\.\d+)?)/.exec(line))) this.duration = hms(m[1]);
    if ((m = /time=\s*(-?\d+:\d+:\d+(?:\.\d+)?)/.exec(line))) {
      this.time = Math.max(0, hms(m[1]));
      const fps = +(/fps=\s*([\d.]+)/.exec(line) || [])[1] || 0;
      const rate = +(/bitrate=\s*([\d.]+)kbits/.exec(line) || [])[1] || 0;
      const sp = /speed=\s*([\d.]+)x/.exec(line);
      if (sp) this.speed = +sp[1];
      const sz = /size=\s*(\d+)(k|Ki)?B/.exec(line);
      if (sz) this.size = +sz[1] * 1024;
      const last = this.samples[this.samples.length - 1];
      if (last && this.time < last[0]) this.samples = []; // next step started
      this.samples.push([this.time, fps, rate]);
      if (this.samples.length > 400) this.samples = this.samples.filter((_, i) => i % 2 === 0);
    }
    this.emit();
  }

  onProgress(timeUs) {
    if (timeUs > 0) this.time = Math.max(this.time, timeUs / 1e6);
    this.emit();
  }

  progress() { return this.duration > 0 ? Math.max(0, Math.min(1, this.time / this.duration)) : -1; }

  finish(state, status) {
    this.state = state;
    this.status = status;
    this.emit();
  }

  async run() {
    try {
      const ff = await loadEngine((s) => { this.status = s; this.emit(); });
      await setupEngine(ff);
      if (this.state !== "RUNNING") return;
      const d = this.dir;
      await ff.createDir(d);
      const paths = {};
      // Inputs are written to the in-memory file system of the engine.
      for (const input of this.recipe.inputs) {
        const files = this.picks[input.key] || [];
        const written = [];
        for (let i = 0; i < files.length; i++) {
          this.status = `Reading ${files[i].name}`; this.emit();
          const ext = extOf(files[i].name);
          const p = `${d}/in_${input.key}_${i + 1}${ext ? "." + ext : ""}`;
          await ff.writeFile(p, new Uint8Array(await files[i].arrayBuffer()));
          written.push(p);
        }
        if (!written.length) continue;
        if (input.multiple) {
          const list = written.map((p) => `file '${p}'`).join("\n") + "\n";
          await ff.writeFile(`${d}/list.txt`, new TextEncoder().encode(list));
          paths.list = `${d}/list.txt`;
        } else {
          paths[input.key] = input.key === "subs" ? `${written[0]}:fontsdir=/fonts` : written[0];
        }
      }
      const kind = this.recipe.outputKind;
      if (kind === "DIRECTORY") await ff.createDir(`${d}/frames`);
      paths.output = `${d}/${this.outputName}`;
      paths.outdir = `${d}/frames`;
      paths.palette = `${d}/palette.png`;
      paths.passlog = `${d}/passlog`;

      const lines = this.commandText.split("\n").map((l) => l.trim()).filter(Boolean);
      for (let i = 0; i < lines.length; i++) {
        let args = parseArguments(lines[i]);
        if (args[0] === "ffmpeg") args = args.slice(1);
        args = presetArgs(args).map((a) => a.replace(/<(\w+)>/g, (_, name) => {
          if (!(name in paths)) throw new Error(`Pick a file for <${name}> first`);
          return paths[name];
        }));
        this.time = 0;
        this.status = lines.length > 1 ? `Running step ${i + 1}/${lines.length}` : "Running";
        this.onLog(`\n$ ffmpeg ${args.join(" ")}\n`);
        const rc = await ff.exec(args);
        if (this.state === "CANCELLED") return;
        if (rc !== 0) {
          this.finish("FAILED", `Failed (exit code ${rc})${lines.length > 1 ? " at step " + (i + 1) : ""}, see log`);
          return;
        }
      }

      if (kind === "FILE") {
        const data = await ff.readFile(paths.output);
        if (!data.length) throw new Error("ffmpeg finished but produced no output");
        this.outputs = [{ name: this.outputName, blob: new Blob([data], { type: this.recipe.outputMime.includes("*") ? "" : this.recipe.outputMime }) }];
      } else if (kind === "DIRECTORY") {
        const entries = (await ff.listDir(paths.outdir)).filter((e) => !e.isDir).map((e) => e.name).sort();
        const files = [];
        for (const name of entries) files.push({ name, data: await ff.readFile(`${paths.outdir}/${name}`) });
        if (!files.length) throw new Error("ffmpeg finished but produced no frames");
        this.outputs = [{ name: this.outputName + ".zip", blob: zip(files), count: files.length }];
      }
      this.size = this.outputs.reduce((n, o) => n + o.blob.size, 0);
      this.time = this.duration || this.time;
      this.finish("DONE", "Done");
    } catch (e) {
      if (this.state === "CANCELLED") return;
      this.onLog("ERROR: " + (e && e.message ? e.message : e));
      this.finish("FAILED", e && e.message ? e.message : String(e));
    } finally {
      // Free the memory used by this job's files.
      if (engine && this.state !== "CANCELLED") {
        try {
          for (const e of await engine.listDir(this.dir)) {
            if (e.isDir && e.name !== "." && e.name !== "..") {
              for (const f of await engine.listDir(`${this.dir}/${e.name}`)) if (!f.isDir) await engine.deleteFile(`${this.dir}/${e.name}/${f.name}`);
              await engine.deleteDir(`${this.dir}/${e.name}`);
            } else if (!e.isDir) {
              await engine.deleteFile(`${this.dir}/${e.name}`);
            }
          }
          await engine.deleteDir(this.dir);
        } catch { /* best effort */ }
      }
    }
  }

  cancel() {
    if (this.state !== "RUNNING") return;
    this.finish("CANCELLED", "Cancelled");
    if (engine) {
      engine.terminate(); // the only way to stop a running exec; the engine reloads next time
      engine = null;
    }
  }
}

// Minimal ZIP writer (stored, no compression) for frame dumps.
const CRC = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; t[n] = c >>> 0; }
  return t;
})();
function crc32(data) {
  let c = 0xffffffff;
  for (let i = 0; i < data.length; i++) c = CRC[(c ^ data[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}
function zip(files) {
  const parts = [], central = [];
  let offset = 0;
  for (const f of files) {
    const name = new TextEncoder().encode(f.name), crc = crc32(f.data), size = f.data.length;
    const local = new DataView(new ArrayBuffer(30));
    local.setUint32(0, 0x04034b50, true); local.setUint16(4, 20, true); local.setUint32(14, crc, true);
    local.setUint32(18, size, true); local.setUint32(22, size, true); local.setUint16(26, name.length, true);
    parts.push(local, name, f.data);
    const cd = new DataView(new ArrayBuffer(46));
    cd.setUint32(0, 0x02014b50, true); cd.setUint16(4, 20, true); cd.setUint16(6, 20, true); cd.setUint32(16, crc, true);
    cd.setUint32(20, size, true); cd.setUint32(24, size, true); cd.setUint16(28, name.length, true); cd.setUint32(42, offset, true);
    central.push(cd, name);
    offset += 30 + name.length + size;
  }
  const cdSize = central.reduce((n, p) => n + p.byteLength, 0);
  const end = new DataView(new ArrayBuffer(22));
  end.setUint32(0, 0x06054b50, true); end.setUint16(8, files.length, true); end.setUint16(10, files.length, true);
  end.setUint32(12, cdSize, true); end.setUint32(16, offset, true);
  return new Blob([...parts, ...central, end], { type: "application/zip" });
}

// ------------------------------------------------------------------------------------------------
// Home

function renderHome() {
  document.title = "VTLOffmp3g";
  app.innerHTML = `
  <div class="wrap">
    <div class="topbar">
      <img src="icon.png" alt="">
      <div class="spacer"></div>
      <button class="icon-btn" id="about" title="Guide">${icon("help")}</button>
    </div>
    <h1 class="display">VTLOffmp3g</h1>
    <p class="sub">FFmpeg 5.1 (WebAssembly) · ${RECIPES.length} recipes · runs in your browser, files never leave your computer</p>

    <div class="home-hero">
      <div class="ring-box" id="hero" title="Open the current job">
        <div id="ring"></div>
        <div class="ring-center"><div class="ring-value" id="heroValue"></div><div class="ring-detail" id="heroDetail"></div><div class="ring-label" id="heroLabel"></div></div>
      </div>
      <div class="hero-side">
        <div class="stats">
          <div class="stat-title" id="statsTitle"></div>
          ${stat("bolt", "statSpeed", "Speed")}${stat("timer", "statTime", "Processed")}${stat("storage", "statSize", "Output")}
        </div>
        <div class="tiles">
          ${tile("movie", "H.264", "libx264")}${tile("movie", "VP8 · VP9", "libvpx + presets")}
          ${tile("audio", "Opus · MP3", "libopus · LAME")}${tile("subtitles", "Subtitles", "libass")}
          ${tile("shield", "Private", "100% local")}
        </div>
      </div>
    </div>

    <div class="search">
      <input id="q" type="text" placeholder="Search recipes, codecs, formats" value="${esc(query)}" autocomplete="off">
      <button id="go" title="Open first match">${icon("arrow")}</button>
    </div>
    <div class="tabs">${TABS.map((t) => `<button class="tab ${t.id === tab ? "active" : ""}" data-tab="${t.id}">${icon(t.id)}${t.label}</button>`).join("")}</div>
    <div class="rows" id="rows"></div>
  </div>`;
  $("#about").onclick = () => showGuide();
  $("#hero").onclick = () => job && go("#/r/" + job.recipe.id);
  $("#q").oninput = (e) => { query = e.target.value; renderRows(); };
  $("#q").onkeydown = (e) => { if (e.key === "Enter") openFirst(); };
  $("#go").onclick = openFirst;
  app.querySelectorAll(".tab").forEach((b) => (b.onclick = () => {
    tab = b.dataset.tab; query = ""; $("#q").value = "";
    app.querySelectorAll(".tab").forEach((x) => x.classList.toggle("active", x === b));
    renderRows();
  }));
  renderRows();
  renderHero();
}
const stat = (ic, id, label) => `<div><div class="chip">${icon(ic)}</div><div class="stat-value" id="${id}">—</div><div class="stat-label">${label}</div></div>`;
const tile = (ic, value, label) => `<div class="tile">${icon(ic)}<b>${value}</b><span>${label}</span></div>`;

function visibleRecipes() {
  const q = query.trim().toLowerCase();
  if (q) {
    return RECIPES.filter((r) => {
      const hay = [r.title, r.description, r.original, r.section, badge(r)].join(" ").toLowerCase();
      return q.split(/\s+/).every((w) => hay.includes(w));
    });
  }
  const sections = TABS.find((t) => t.id === tab).sections;
  return RECIPES.filter((r) => sections.includes(r.section));
}
function renderRows() {
  const list = visibleRecipes();
  let html = "", section = null;
  for (const r of list) {
    if (r.section !== section) { section = r.section; html += `<div class="group">${esc(section)}</div>`; }
    const desc = r.original ? r.original.split("\n")[0] : r.description;
    html += `<button class="row ${webSupported(r) ? "" : "disabled"}" data-id="${r.id}">
      <div class="grow"><div class="t">${esc(r.title)}</div><div class="d">${esc(desc)}</div></div>
      <div class="badge">${badge(r)}</div>${icon("chevron")}</button>`;
  }
  $("#rows").innerHTML = html || `<div class="empty">No recipe matches</div>`;
  $("#rows").querySelectorAll(".row").forEach((b) => (b.onclick = () => go("#/r/" + b.dataset.id)));
}
function openFirst() {
  const first = query.trim() && visibleRecipes()[0];
  if (first) go("#/r/" + first.id);
}
function renderHero() {
  if (!$("#ring")) return;
  const green = "#21e36a", track = "#2f3430";
  const set = (id, v) => ($("#" + id).textContent = v);
  if (!job) {
    $("#ring").innerHTML = gaugeSVG(360, 1, track, green, 7);
    set("heroValue", "Ready"); set("heroDetail", "x264 · VP9 · Opus"); set("heroLabel", "Pick a recipe");
    set("statsTitle", "Last job"); set("statSpeed", "—"); set("statTime", "—"); set("statSize", "—");
    return;
  }
  const running = job.state === "RUNNING", p = job.progress();
  const color = job.state === "FAILED" ? "#ff7a00" : job.state === "CANCELLED" ? "#ffaa0d" : green;
  $("#ring").innerHTML = gaugeSVG(360, running ? p : job.state === "DONE" ? 1 : Math.max(0, p), track, color, 7);
  set("heroValue", running ? (p < 0 ? "…" : Math.round(p * 100) + "%") : { DONE: "Done", CANCELLED: "Stopped", FAILED: "Failed" }[job.state]);
  set("heroDetail", job.recipe.title);
  set("heroLabel", running ? job.status : "Click to open");
  set("statsTitle", running ? "Current job" : "Last job");
  set("statSpeed", job.speed ? job.speed.toFixed(2) + "x" : "—");
  set("statTime", fmtTime(job.time));
  set("statSize", job.size ? fmtBytes(job.size) : "—");
}

// ------------------------------------------------------------------------------------------------
// Recipe

function renderRecipe(r) {
  document.title = r.title + " · VTLOffmp3g";
  const f = form(r);
  const supported = webSupported(r);
  app.innerHTML = `
  <div class="wrap">
    <div class="topbar">
      <button class="icon-btn" id="back" title="Back">${icon("back")}</button>
      <div class="spacer"></div>
      <button class="icon-btn" id="reset" title="Reset parameters">${icon("tune")}</button>
      <button class="icon-btn" id="help" title="Cheatsheet line">${icon("help")}</button>
    </div>
    <h1 class="display">${esc(r.title)}</h1>
    <p class="sub">${esc(r.description)}</p>
    <div class="recipe-grid">
      <div class="col">
        <div class="card stripes gauge-card">
          <div class="gauge"><div id="gauge"></div><div class="center"><div class="score" id="score">0</div><div>Progress</div></div></div>
          <div>
            <div class="verdict" id="verdict">Ready</div>
            <div class="pill-row"><span id="pillLabel">Output</span><span class="pill" id="pill">${badge(r)}</span></div>
            <div class="detail" id="detail"></div>
          </div>
        </div>
        <div class="panel" id="scopeBox">
          <div class="panel-label"><span class="grow">TELEMETRY</span><span class="legend-fps">━ fps</span><span class="legend-rate">━ kb/s</span></div>
          <canvas class="scope" id="scope"></canvas>
        </div>
        <div class="stats card" id="stats">
          ${stat("timer", "sTime", "Processed")}${stat("bolt", "sSpeed", "Speed")}${stat("storage", "sSize", "Output")}
        </div>
        <div class="panel">
          <div class="panel-label"><span class="grow">LOG</span><button class="icon-btn" id="copyLog" title="Copy log" style="color: var(--scope-teal)">${icon("copy")}</button></div>
          <pre class="log" id="log">waiting for a run…</pre>
        </div>
      </div>
      <div class="col">
        ${r.inputs.length ? `<div class="card"><h3>Files</h3>${r.inputs.map((input) => `
          <div class="input-row">
            <div class="chip">${icon("file")}</div>
            <div class="grow"><div class="label">${esc(input.label)}</div><div class="name" data-name="${input.key}"></div>
              ${input.multiple ? `<button class="clear" data-clear="${input.key}">Clear list</button>` : ""}</div>
            <label class="btn primary small">${icon("file")}${input.multiple ? "Add" : "Pick"}
              <input type="file" hidden data-input="${input.key}" ${input.multiple ? "multiple" : ""} accept="${input.mime === "*/*" ? "" : input.mime}${input.key === "subs" ? ".srt,.ass,.ssa,.vtt" : ""}">
            </label>
          </div>`).join("")}</div>` : ""}
        ${r.params.length ? `<div class="card"><h3>Parameters</h3>${r.params.map((p) => `
          <label class="field"><span>${esc(p.label)}</span><input data-param="${p.key}" value="${esc(f.values[p.key])}" spellcheck="false"></label>`).join("")}</div>` : ""}
        <div class="panel">
          <div class="panel-label">COMMAND</div>
          <textarea class="command" id="command" spellcheck="false">${esc(f.command)}</textarea>
          <div class="hint">editable · one ffmpeg command per line · &lt;input&gt; &lt;output&gt; … become your files</div>
        </div>
        ${supported ? "" : `<p class="note">RTMP streaming needs a network socket, which a browser page does not have. Use the Android app for this recipe.</p>`}
        <button class="btn primary block" id="run" ${supported ? "" : "disabled"}>${icon("play")}Run</button>
        <button class="btn stop block" id="stop" hidden>${icon("stop")}Stop</button>
        <div class="actions" id="result" hidden></div>
        <div class="preview" id="preview"></div>
      </div>
    </div>
  </div>`;

  $("#back").onclick = () => go("#/");
  $("#help").onclick = () => alert(`From tools/ffmpeg.sh:\n\n${r.original || buildCommand(r, defaults(r))}`);
  $("#reset").onclick = () => { forms.delete(r.id); renderRecipe(r); };
  $("#copyLog").onclick = () => navigator.clipboard.writeText(job && job.recipe === r ? job.log : "");
  $("#command").oninput = (e) => (f.command = e.target.value);
  app.querySelectorAll("[data-param]").forEach((el) => (el.oninput = () => {
    f.values[el.dataset.param] = el.value.trim();
    f.command = buildCommand(r, f.values);
    $("#command").value = f.command;
  }));
  app.querySelectorAll("[data-input]").forEach((el) => (el.onchange = () => {
    const input = r.inputs.find((i) => i.key === el.dataset.input);
    const files = [...el.files];
    f.picks[input.key] = input.multiple ? [...(f.picks[input.key] || []), ...files] : files.slice(0, 1);
    el.value = "";
    renderPicks(r);
  }));
  app.querySelectorAll("[data-clear]").forEach((el) => (el.onclick = () => { delete f.picks[el.dataset.clear]; renderPicks(r); }));
  $("#run").onclick = () => startJob(r);
  $("#stop").onclick = () => job && job.cancel();
  renderPicks(r);
  renderJob(r);
}

function renderPicks(r) {
  const f = form(r);
  app.querySelectorAll("[data-name]").forEach((el) => {
    const files = f.picks[el.dataset.name] || [];
    el.textContent = files.length ? files.map((x, i) => (files.length > 1 ? `${i + 1}. ` : "") + `${x.name} · ${fmtBytes(x.size)}`).join("   ") : "nothing picked";
    el.classList.toggle("picked", files.length > 0);
  });
}

function startJob(r) {
  if (job && job.state === "RUNNING") return alert("Another job is running.");
  const f = form(r);
  const text = f.command;
  for (const name of [...new Set([...text.matchAll(/<(\w+)>/g)].map((m) => m[1]))]) {
    const input = name === "list" ? r.inputs.find((i) => i.multiple) : r.inputs.find((i) => i.key === name);
    if (input && !(f.picks[input.key] || []).length) return alert((input.multiple ? "Add: " : "Pick: ") + input.label);
  }
  const first = r.inputs.length && (f.picks[r.inputs[0].key] || [])[0];
  const base = first ? stem(first.name) + "_" + r.id : r.id;
  const ext = (applyParams(r.outputExt, f.values).replace(/[^A-Za-z0-9]/g, "").toLowerCase()) || "out";
  const name = r.outputKind === "DIRECTORY" ? base + "_frames" : base + "." + ext;
  const picks = Object.fromEntries(Object.entries(f.picks).map(([k, v]) => [k, [...v]]));
  job = new Job(r, picks, text, name);
  job.listeners.add(() => route(true));
  job.run();
  renderJob(r);
}

function renderJob(r) {
  const mine = job && job.recipe === r ? job : null;
  const running = mine && mine.state === "RUNNING";
  const busyElsewhere = job && job.state === "RUNNING" && !mine;
  $("#run").hidden = running;
  $("#stop").hidden = !running;
  if (webSupported(r)) $("#run").disabled = !!busyElsewhere;
  $("#command").disabled = running;
  const set = (id, v) => ($("#" + id).textContent = v);
  const green = "#21e36a", track = "#e4e9e5";
  if (!mine) {
    $("#gauge").innerHTML = gaugeSVG(270, 0, track, green, 8);
    set("score", "0"); $("#score").style.color = "";
    set("verdict", busyElsewhere ? "Busy" : "Ready"); $("#verdict").style.color = busyElsewhere ? "#ffaa0d" : green;
    set("pillLabel", "Output"); set("pill", badge(r));
    set("detail", busyElsewhere ? "Another job is running: " + job.recipe.title : webSupported(r) ? "Pick your files, then Run." : "Android app only.");
    $("#scopeBox").hidden = true; $("#stats").hidden = true; $("#result").hidden = true;
    return;
  }
  const p = mine.progress();
  let color = green;
  switch (mine.state) {
    case "RUNNING":
      set("verdict", p < 0 ? "Working" : "Encoding");
      set("score", p < 0 ? "…" : Math.round(p * 100));
      set("pillLabel", "Speed"); set("pill", mine.speed ? mine.speed.toFixed(2) + "x" : "—");
      set("detail", `${mine.status}\n${fmtTime(mine.time)}${mine.duration ? " / " + fmtTime(mine.duration) : ""}`);
      break;
    case "DONE":
      set("verdict", "Done"); set("score", "100");
      set("pillLabel", "Size"); set("pill", fmtBytes(mine.size));
      set("detail", mine.outputs.map((o) => o.name + (o.count ? ` (${o.count} frames)` : "")).join("\n"));
      break;
    case "CANCELLED":
      color = "#ffaa0d"; set("verdict", "Stopped"); set("score", p < 0 ? "–" : Math.round(p * 100));
      set("pillLabel", "Output"); set("pill", "none"); set("detail", mine.status);
      break;
    default:
      color = "#ff7a00"; set("verdict", "Failed"); set("score", p < 0 ? "!" : Math.round(p * 100));
      set("pillLabel", "Risk"); set("pill", "see log"); set("detail", mine.status);
  }
  $("#gauge").innerHTML = gaugeSVG(270, running ? p : mine.state === "DONE" ? 1 : Math.max(0.02, p), track, color, 8);
  $("#verdict").style.color = color;
  $("#score").style.color = mine.state === "FAILED" ? color : "";
  $("#scopeBox").hidden = false; $("#stats").hidden = false;
  drawScope($("#scope"), mine.samples);
  set("sTime", fmtTime(mine.time)); set("sSpeed", mine.speed ? mine.speed.toFixed(2) + "x" : "—"); set("sSize", mine.size ? fmtBytes(mine.size) : "—");
  const logEl = $("#log");
  const atBottom = logEl.scrollTop + logEl.clientHeight >= logEl.scrollHeight - 20;
  logEl.textContent = mine.log.slice(-12000) || "waiting for ffmpeg…";
  if (atBottom) logEl.scrollTop = logEl.scrollHeight;

  const result = $("#result");
  if (mine.state === "DONE" && !result.dataset.ready) {
    result.dataset.ready = "1";
    result.hidden = false;
    const out = mine.outputs[0];
    out.url = out.url || URL.createObjectURL(out.blob);
    result.innerHTML = `<a class="btn primary" href="${out.url}" download="${esc(out.name)}">${icon("save")}Download</a>`;
    const type = out.blob.type || "";
    const ext = extOf(out.name);
    const preview = $("#preview");
    if (/^video|^(mp4|webm|mkv|mov)$/.test(type || ext)) preview.innerHTML = `<video src="${out.url}" controls></video>`;
    else if (/^audio|^(mp3|m4a|aac|opus|ogg|wav)$/.test(type || ext)) preview.innerHTML = `<audio src="${out.url}" controls></audio>`;
    else if (/^image|^(gif|jpg|png|webp)$/.test(type || ext)) preview.innerHTML = `<img src="${out.url}" alt="">`;
  } else if (mine.state !== "DONE") {
    result.hidden = true; delete result.dataset.ready; $("#preview").innerHTML = "";
  }
}

// ------------------------------------------------------------------------------------------------
// French guide shown at the first visit (same story as the Android app's)

const GUIDE = [
  {
    title: "Bienvenue dans VTLOffmp3g",
    text: "Toutes les commandes FFmpeg de la cheatsheet, dans votre navigateur. Vos fichiers ne quittent jamais votre ordinateur.",
    art: () => `<div style="position:relative;width:220px;height:220px">
      <svg viewBox="0 0 100 100" style="width:100%;height:100%"><circle cx="50" cy="50" r="44" fill="none" stroke="#2f3430" stroke-width="7"/>
      <circle class="g-ring" cx="50" cy="50" r="44" fill="none" stroke="#21e36a" stroke-width="7" stroke-linecap="round" transform="rotate(-90 50 50)" stroke-dasharray="276.5" style="--len:276.5"/></svg>
      <img src="icon.png" alt="" class="g-pulse" style="position:absolute;left:55px;top:55px;width:110px;height:110px;border-radius:50%"></div>`,
  },
  {
    title: "Choisissez une recette",
    text: "Quatre onglets : Convert, Audio, Edit, Frames. Ou cherchez directement un format : mp3, gif, webm…",
    art: () => `<div style="width:340px">
      <div style="background:#fff;border-radius:4px;padding:12px 16px;color:#111;font-size:17px;height:46px"><span class="g-type">webm</span></div>
      ${[["MP4 → WebM (VP9 + Opus)", "WEBM"], ["Extraire l'audio en MP3", "MP3"], ["Vidéo → GIF optimisé", "GIF"]].map(([t, b], i) => `
      <div class="row g-slide ${i ? "g-dim" : ""}" style="margin-top:8px;min-height:52px;animation-delay:${i * 0.15}s${i ? `, ${i * 0.15}s` : ""}">
        <div class="grow"><div class="t" style="font-size:15px">${t}</div></div><div class="badge" style="font-size:15px">${b}</div>${icon("chevron")}</div>`).join("")}</div>`,
  },
  {
    title: "Ajoutez vos fichiers",
    text: "Cliquez Pick pour choisir une vidéo, réglez les paramètres. La commande FFmpeg reste modifiable à la main.",
    art: () => `<div style="width:340px">
      <div class="card" style="padding:16px 18px"><div class="input-row" style="margin:0">
        <div class="chip">${icon("file")}</div>
        <div class="grow"><div class="label">Vidéo source</div><div class="name picked g-appear">vacances.mp4</div></div>
        <span class="btn primary small g-press">${icon("file")}Pick</span></div>
        <div class="field"><span>CRF (0-51)</span><input value="23" readonly></div></div>
      <div class="panel" style="margin-top:10px"><div class="command" style="min-height:0;margin:0">ffmpeg -i vacances.mp4 -crf 23 out.mp4</div></div></div>`,
  },
  {
    title: "Lancez et suivez",
    text: "Cliquez Run. La jauge et la télémétrie montrent la progression, les fps et le débit en direct.",
    art: () => `<div style="width:340px">
      <div class="card stripes gauge-card" style="padding:16px">
        <div class="gauge" style="width:110px;height:110px">
          <svg viewBox="0 0 100 100"><path d="${arcPath(50, 50, 45, 135, 270)}" fill="none" stroke="#e4e9e5" stroke-width="8" stroke-linecap="round"/>
          <path class="g-ring" d="${arcPath(50, 50, 45, 135, 270)}" fill="none" stroke="#21e36a" stroke-width="8" stroke-linecap="round" stroke-dasharray="212" style="--len:212;animation-duration:4s"/></svg>
          <div class="center"><div class="score" style="font-size:26px">63</div></div></div>
        <div><div class="verdict" style="font-size:24px">Encoding</div><div class="pill-row"><span class="pill">1.8x</span></div></div></div>
      <div class="panel" style="margin-top:10px;padding:8px">
        <svg viewBox="0 0 300 90" style="width:100%;height:90px"><polyline class="g-draw" fill="none" stroke="#d21e5a" stroke-width="1.6" points="${wave(2, 50, 14)}"/>
        <polyline class="g-draw" fill="none" stroke="#f2d21b" stroke-width="1.6" points="${wave(1, 35, 10)}"/></svg></div></div>`,
  },
  {
    title: "Récupérez le résultat",
    text: "Écoutez ou regardez l'aperçu, puis téléchargez le fichier. Les captures d'images arrivent dans un .zip.",
    art: () => `<div style="text-align:center">
      <div class="g-pop" style="width:96px;height:96px;border-radius:50%;background:#21e36a;color:#06180c;font-size:54px;font-weight:700;display:grid;place-items:center;margin:0 auto">✓</div>
      <div style="display:flex;gap:8px;margin-top:28px;justify-content:center">
        <span class="btn primary small g-pop" style="animation-delay:.35s">${icon("save")}Download</span>
        <span class="btn tonal small g-pop" style="animation-delay:.47s">${icon("play")}Aperçu</span></div></div>`,
  },
];
function wave(seed, base, amp) {
  const pts = [];
  for (let i = 0; i <= 60; i++) pts.push(`${i * 5},${base + amp * Math.sin(i / (3 + seed)) + (i % 5) * seed - (i < 4 ? (4 - i) * 6 : 0)}`);
  return pts.join(" ");
}
let guideStep = 0;
function showGuide(step = 0) {
  guideStep = step;
  const g = $("#guide");
  const s = GUIDE[step];
  const last = step === GUIDE.length - 1;
  g.hidden = false;
  g.innerHTML = `<div class="guide-card" role="dialog" aria-label="Guide">
    <div class="guide-step">ÉTAPE ${step + 1} / ${GUIDE.length}</div>
    <div class="guide-art">${s.art()}</div>
    <h2>${s.title}</h2><p>${s.text}</p>
    <div class="guide-bar">
      <button class="skip" id="gSkip" ${last ? "style=visibility:hidden" : ""}>Passer</button>
      <div class="dots">${GUIDE.map((_, i) => `<span class="dot ${i === step ? "on" : ""}"></span>`).join("")}</div>
      <button class="btn primary" id="gNext">${last ? "C'est parti" : "Suivant"}</button>
    </div></div>`;
  $("#gSkip").onclick = closeGuide;
  $("#gNext").onclick = () => (last ? closeGuide() : showGuide(step + 1));
}
function closeGuide() {
  $("#guide").hidden = true;
  $("#guide").innerHTML = "";
  try { localStorage.setItem("vtlo-guide-done", "1"); } catch { /* private mode */ }
}
document.addEventListener("keydown", (e) => {
  if ($("#guide").hidden) return;
  if (e.key === "Escape") closeGuide();
  if (e.key === "ArrowRight" && guideStep < GUIDE.length - 1) showGuide(guideStep + 1);
  if (e.key === "ArrowLeft" && guideStep > 0) showGuide(guideStep - 1);
});

// ------------------------------------------------------------------------------------------------
// Routing

function go(hash) { location.hash = hash; }
let currentRoute = "";
function route(updateOnly = false) {
  const m = /^#\/r\/([\w-]+)/.exec(location.hash);
  const r = m && RECIPES.find((x) => x.id === m[1]);
  const key = r ? "r:" + r.id : "home";
  if (updateOnly && key === currentRoute) {
    if (r) renderJob(r); else renderHero();
    return;
  }
  currentRoute = key;
  if (r) renderRecipe(r); else renderHome();
  window.scrollTo(0, 0);
}
window.addEventListener("hashchange", () => route());
window.addEventListener("beforeunload", (e) => { if (job && job.state === "RUNNING") e.preventDefault(); });

RECIPES = await (await fetch("recipes.json")).json();
route();
let seen = null;
try { seen = localStorage.getItem("vtlo-guide-done"); } catch { /* private mode */ }
if (!seen) showGuide(0);
