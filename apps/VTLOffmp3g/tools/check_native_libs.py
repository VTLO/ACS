#!/usr/bin/env python3
"""Fail if any shared library would not dlopen on Android.

Mirrors the Android linker: every non-weak undefined symbol of a library must be
defined by one of the libraries in its DT_NEEDED closure (packaged libraries or the
NDK's API-level system stubs). Usage: check_native_libs.py <ndk> <api> <apk-or-jni-dir>
"""
import glob, os, re, subprocess, sys, tempfile, zipfile

TRIPLES = {"arm64-v8a": "aarch64-linux-android", "armeabi-v7a": "arm-linux-androideabi",
           "x86": "i686-linux-android", "x86_64": "x86_64-linux-android"}


def run(*cmd):
    return subprocess.run(cmd, capture_output=True, text=True, check=True).stdout


def needed(path):
    return re.findall(r"\(NEEDED\)\s+Shared library: \[(.+?)\]", run("readelf", "-dW", path))


def symbols(path, defined):
    flag = "--defined-only" if defined else "--undefined-only"
    out = set()
    for line in run("nm", "-D", flag, path).splitlines():
        parts = line.split()
        if not parts or (not defined and parts[0] in ("w", "v")):
            continue
        out.add(parts[-1].split("@")[0])
    return out


def main(ndk, api, target):
    tmp = None
    if target.endswith(".apk"):
        tmp = tempfile.mkdtemp()
        with zipfile.ZipFile(target) as z:
            z.extractall(tmp, [n for n in z.namelist() if n.startswith("lib/")])
        target = os.path.join(tmp, "lib")
    sysroot = glob.glob(f"{ndk}/toolchains/llvm/prebuilt/*/sysroot/usr/lib")[0]
    failures = 0
    for abi in sorted(os.listdir(target)):
        ours = {os.path.basename(p): p for p in glob.glob(f"{target}/{abi}/*.so")}
        sysdir = f"{sysroot}/{TRIPLES[abi]}/{api}"
        defs = {}

        def lookup(name):
            if name not in defs:
                path = ours.get(name) or os.path.join(sysdir, name)
                defs[name] = symbols(path, True) if os.path.exists(path) else None
            return defs[name]

        for name, path in sorted(ours.items()):
            closure, queue, missing_libs = [], list(needed(path)), []
            while queue:  # breadth-first, like the linker's load order
                lib = queue.pop(0)
                if lib in closure:
                    continue
                closure.append(lib)
                if lookup(lib) is None:
                    missing_libs.append(lib)
                elif lib in ours:
                    queue.extend(needed(ours[lib]))
            available = set().union(*(lookup(l) or set() for l in closure)) | symbols(path, True)
            unresolved = sorted(symbols(path, False) - available)
            if missing_libs or unresolved:
                failures += 1
                print(f"FAIL {abi}/{name}: missing libs {missing_libs}, "
                      f"{len(unresolved)} unresolved symbols {unresolved[:8]}")
        print(f"{abi}: {len(ours)} libraries checked")
    print("OK" if not failures else f"{failures} libraries would fail to load")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main(*sys.argv[1:4]))
