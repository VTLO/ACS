#!/usr/bin/env python3
"""Fail if a LOAD segment of a packaged .so would be mapped wrongly.

Offset and address must agree modulo the alignment, and 64-bit ABIs (the only ones
with 16 KB-page devices) need at least 16 KB alignment.
"""
import subprocess, sys, tempfile, zipfile, os, glob

bad = 0
for apk in sys.argv[1:]:
    tmp = tempfile.mkdtemp()
    with zipfile.ZipFile(apk) as z:
        z.extractall(tmp, [n for n in z.namelist() if n.startswith("lib/") and n.endswith(".so")])
    for so in glob.glob(f"{tmp}/lib/*/*.so"):
        for line in subprocess.run(["readelf", "-lW", so], capture_output=True, text=True, check=True).stdout.splitlines():
            p = line.split()
            if p and p[0] == "LOAD":
                off, vaddr, align = int(p[1], 16), int(p[2], 16), int(p[-1], 16)
                min_align = 0x4000 if "64" in so.split("/lib/")[-1].split("/")[0] else 0x1000
                if align < min_align or off % align != vaddr % align:
                    bad += 1
                    print(f"BAD {os.path.basename(apk)} {os.path.relpath(so, tmp)}: {line.strip()}")
print("alignment OK" if not bad else f"{bad} misaligned segments")
sys.exit(1 if bad else 0)
