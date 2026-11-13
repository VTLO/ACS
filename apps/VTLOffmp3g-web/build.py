#!/usr/bin/env python3
"""Builds the VTLOffmp3g web app into dist/.

- exports the recipes from the Android app's Recipe.java (single source of truth),
- copies src/,
- vendors ffmpeg.wasm (@ffmpeg/ffmpeg, @ffmpeg/core) from npm,
- copies the libvpx presets and the launcher icon from the Android app.

Needs: python3, a JDK (javac/java) and npm. Run: python3 build.py
"""
import json, os, shutil, subprocess, sys, tarfile, tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
ANDROID = os.path.join(HERE, "..", "VTLOffmp3g", "app", "src", "main")
DIST = os.path.join(HERE, "dist")
PACKAGES = {"@ffmpeg/ffmpeg": "0.12.15", "@ffmpeg/core": "0.12.10"}


def run(*cmd, **kw):
    subprocess.run(cmd, check=True, **kw)


def export_recipes(tmp):
    classes = os.path.join(tmp, "classes")
    run("javac", "-d", classes,
        os.path.join(ANDROID, "java", "com", "vtlo", "offmp3g", "Recipe.java"),
        os.path.join(HERE, "tools", "ExportRecipes.java"))
    out = os.path.join(DIST, "recipes.json")
    run("java", "-cp", classes, "ExportRecipes", out)
    return len(json.load(open(out)))


def vendor(tmp):
    for name, version in PACKAGES.items():
        run("npm", "pack", f"{name}@{version}", "--silent", cwd=tmp, stdout=subprocess.DEVNULL)
        tgz = os.path.join(tmp, name.replace("@", "").replace("/", "-") + f"-{version}.tgz")
        target = os.path.join(DIST, "vendor", name.split("/")[1])
        with tarfile.open(tgz) as t:
            for m in t.getmembers():
                prefix = "package/dist/esm/"
                if m.isfile() and m.name.startswith(prefix) and not m.name.endswith((".d.ts", ".d.mts", ".map")):
                    m.name = m.name[len(prefix):]
                    t.extract(m, target, filter="data")
                elif m.isfile() and os.path.basename(m.name).upper().startswith(("LICENSE", "README")):
                    m.name = os.path.basename(m.name)
                    t.extract(m, target, filter="data")


def main():
    if os.path.isdir(DIST):
        shutil.rmtree(DIST)
    shutil.copytree(os.path.join(HERE, "src"), DIST)
    os.makedirs(os.path.join(DIST, "presets"), exist_ok=True)
    presets = os.path.join(ANDROID, "assets", "ffpresets")
    names = sorted(n for n in os.listdir(presets) if n.endswith(".ffpreset"))
    for n in names:
        shutil.copy(os.path.join(presets, n), os.path.join(DIST, "presets", n))
    json.dump(names, open(os.path.join(DIST, "presets", "index.json"), "w"))
    shutil.copy(os.path.join(ANDROID, "res", "mipmap-xxxhdpi", "ic_launcher.png"), os.path.join(DIST, "icon.png"))
    with tempfile.TemporaryDirectory() as tmp:
        count = export_recipes(tmp)
        vendor(tmp)
    size = sum(os.path.getsize(os.path.join(d, f)) for d, _, fs in os.walk(DIST) for f in fs)
    print(f"dist/ built: {count} recipes, {size / 1e6:.1f} MB")


if __name__ == "__main__":
    sys.exit(main())
