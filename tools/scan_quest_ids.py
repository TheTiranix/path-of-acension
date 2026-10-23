"""Indexa ids reales (item/entity/structure/biome/dimension/advancement) de los mods de PRUEBAS + el mod propio (src).
Salida: scratchpad/ids.json  {kind: [ids...]}"""
import glob
import json
import os
import re
import zipfile

MODS = "C:/Users/agust/AppData/Roaming/ATLauncher/instances/PRUEBAS/mods/"
SRC = "D:/Minecraft/CaidaDeLosDioses/src/main/resources/"
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "build", "quest_ids.json")

ids = {k: set() for k in ("item", "entity", "structure", "biome", "dimension", "advancement")}


def lang_keys(data):
    for key in data:
        m = re.match(r"^(item|block|entity)\.([a-z0-9_.-]+?)\.([a-z0-9_./-]+)$", key)
        if m:
            kind = "entity" if m.group(1) == "entity" else "item"
            ids[kind].add(m.group(2) + ":" + m.group(3).replace(".", "/") if False else m.group(2) + ":" + m.group(3))


def scan_names(names, read=None):
    for n in names:
        m = re.match(r"^data/([^/]+)/worldgen/(structure|biome)/(.+)\.json$", n)
        if m:
            ids[m.group(2)].add("%s:%s" % (m.group(1), m.group(3)))
        m = re.match(r"^data/([^/]+)/dimension/(.+)\.json$", n)
        if m:
            ids["dimension"].add("%s:%s" % (m.group(1), m.group(2)))
        m = re.match(r"^data/([^/]+)/advancements/(.+)\.json$", n)
        if m:
            ids["advancement"].add("%s:%s" % (m.group(1), m.group(2)))
        m = re.match(r"^assets/([^/]+)/models/item/(.+)\.json$", n)
        if m:
            ids["item"].add("%s:%s" % (m.group(1), m.group(2)))
        m = re.match(r"^assets/([^/]+)/lang/en_us\.json$", n)
        if m and read:
            try:
                lang_keys(json.loads(read(n)))
            except Exception:
                pass


for jar in glob.glob(MODS + "*.jar"):
    try:
        z = zipfile.ZipFile(jar)
    except Exception:
        continue
    scan_names(z.namelist(), lambda n, z=z: z.read(n).decode("utf-8", "replace"))

# mod propio (fuentes)
for root, _, files in os.walk(SRC):
    for f in files:
        rel = os.path.relpath(os.path.join(root, f), SRC).replace(os.sep, "/")
        scan_names([rel], lambda n: open(SRC + n, encoding="utf-8").read())

json.dump({k: sorted(v) for k, v in ids.items()}, open(OUT, "w"), indent=0)
print({k: len(v) for k, v in ids.items()})
