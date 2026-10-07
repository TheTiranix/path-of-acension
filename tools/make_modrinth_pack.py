#!/usr/bin/env python3
"""Arma el .mrpack de Path of Ascension para Modrinth.

Modrinth solo acepta mods que ya estan en Modrinth (los identifica por hash): esos se listan con su URL de descarga y no pesan en el
archivo. Los que no estan en Modrinth NO se incluyen (se anotan en el informe). Va dentro del archivo, como overrides, lo propio del pack:
nuestro mod, config, defaultconfigs, kubejs y los resource packs propios.

Uso:  python tools/make_modrinth_pack.py <version> [carpeta_de_la_instancia]
"""
import hashlib
import json
import os
import sys
import urllib.request
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_INSTANCE = "C:/Users/agust/AppData/Roaming/ATLauncher/instances/TestamentodelaCarne"
OWN_JAR_PREFIX = "pathofascension"
OVERRIDE_DIRS = ["config", "defaultconfigs", "kubejs"]
OWN_RESOURCEPACKS = ["Testamento Sonoro"]
SKIP_SUFFIXES = (".bak", ".log", ".tmp")


def post_json(url, payload):
    req = urllib.request.Request(url, data=json.dumps(payload).encode(), headers={
        "Content-Type": "application/json", "User-Agent": "path-of-ascension-modpack/1.0"})
    return json.load(urllib.request.urlopen(req))


# Mods que estan en Modrinth con otro archivo (otra compilacion que la de CurseForge): prefijo del archivo -> slug de Modrinth.
SLUGS = {
    "aether-": "aether", "balm-forge": "balm", "caelus-forge": "caelus", "GlitchCore": "glitchcore", "SmartBrainLib": "smartbrainlib",
    "Structory_": "structory", "Valoria-": "valoria", "waystones-forge": "waystones", "FallingTree-": "fallingtree",
    "MAtmos-": "matmos-ambient-sound", "born_in_chaos": "borninchaos", "sophisticatedbackpacks": "sophisticated-backpacks",
    "sophisticatedcore": "sophisticated-core", "SereneSeasons": "serene-seasons", "alltheleaks": "alltheleaks", "cupboard": "cupboard",
    "framework-forge": "framework", "goblintraders": "goblin-traders", "bettervillage": "better-village", "macabre": "macabre",
    "twilightforest-": "twilight-forest", "ToughAsNails": "tough-as-nails", "ftb-quests": "ftb-quests-forge", "ftb-library": "ftb-library-forge",
    "ftb-teams": "ftb-teams-forge", "ftb-chunks": "ftb-chunks-forge", "bloodbits": "bloodbits", "RPG-HUD": "rpg-hud",
}


def find_by_slug(name):
    """Version de Modrinth para 1.20.1/Forge del mismo mod cuyo numero de version aparece en el nombre del archivo, o None."""
    slug = next((v for k, v in SLUGS.items() if k in name), None)
    if not slug:
        return None
    try:
        url = "https://api.modrinth.com/v2/project/%s/version?game_versions=%%5B%%221.20.1%%22%%5D&loaders=%%5B%%22forge%%22%%5D" % slug
        data = json.load(urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "path-of-ascension-modpack/1.0"})))
    except Exception:
        return None
    for v in data:
        for f in v["files"]:
            if f["filename"] == name:
                return f
    digits = [d for d in name.replace(".jar", "").replace("-", " ").replace("_", " ").split() if any(c.isdigit() for c in d)]
    for v in data:  # mismo numero de version del mod
        number = v["version_number"]
        if any(number.replace("v", "") in d or d in number for d in digits if len(d) > 3):
            return next((x for x in v["files"] if x.get("primary")), v["files"][0])
    return None


def file_hash(path, algo):
    h = hashlib.new(algo)
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def main():
    version = sys.argv[1] if len(sys.argv) > 1 else "dev"
    instance = sys.argv[2] if len(sys.argv) > 2 else DEFAULT_INSTANCE
    mods_dir = os.path.join(instance, "mods")
    jars = sorted(f for f in os.listdir(mods_dir) if f.endswith(".jar"))
    own = [f for f in jars if f.startswith(OWN_JAR_PREFIX)]
    others = [f for f in jars if f not in own]
    sha1 = {f: file_hash(os.path.join(mods_dir, f), "sha1") for f in others}
    found = post_json("https://api.modrinth.com/v2/version_files", {"hashes": list(sha1.values()), "algorithm": "sha1"})

    files, missing, substituted = [], [], []
    for name in others:
        info = found.get(sha1[name])
        if not info:
            entry = find_by_slug(name)
            if not entry:
                missing.append(name)
                continue
            substituted.append("%s -> %s" % (name, entry["filename"]))
            files.append({
                "path": "mods/" + entry["filename"],
                "hashes": {"sha1": entry["hashes"]["sha1"], "sha512": entry["hashes"]["sha512"]},
                "env": {"client": "required", "server": "required"},
                "downloads": [entry["url"]],
                "fileSize": entry["size"],
            })
            continue
        entry = next((x for x in info["files"] if x["hashes"].get("sha1") == sha1[name]), info["files"][0])
        files.append({
            "path": "mods/" + name,
            "hashes": {"sha1": entry["hashes"]["sha1"], "sha512": entry["hashes"]["sha512"]},
            "env": {"client": "required", "server": "required"},
            "downloads": [entry["url"]],
            "fileSize": entry["size"],
        })
    index = {
        "formatVersion": 1,
        "game": "minecraft",
        "versionId": version,
        "name": "Path of Ascension",
        "summary": "RPG modpack: races, classes, gods, elemental combat, epic bosses and survival that bites back.",
        "files": files,
        "dependencies": {"minecraft": "1.20.1", "forge": "47.4.0"},
    }

    out_dir = os.path.join(ROOT, "build", "release")
    os.makedirs(out_dir, exist_ok=True)
    out = os.path.join(out_dir, "PathOfAscension-modrinth-%s.mrpack" % version)
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("modrinth.index.json", json.dumps(index, indent=2))
        for name in own:  # nuestro mod: se distribuye dentro del pack
            zf.write(os.path.join(mods_dir, name), "overrides/mods/" + name)
        for folder in OVERRIDE_DIRS:
            base = os.path.join(instance, folder)
            for root, _, names in os.walk(base):
                for n in names:
                    if n.endswith(SKIP_SUFFIXES):
                        continue
                    full = os.path.join(root, n)
                    zf.write(full, "overrides/" + os.path.relpath(full, instance).replace(os.sep, "/"))
        for pack in OWN_RESOURCEPACKS:
            base = os.path.join(instance, "resourcepacks", pack)
            for root, _, names in os.walk(base):
                for n in names:
                    full = os.path.join(root, n)
                    zf.write(full, "overrides/" + os.path.relpath(full, instance).replace(os.sep, "/"))
    report = os.path.join(out_dir, "modrinth-excluidos-%s.txt" % version)
    with open(report, "w", encoding="utf-8") as f:
        f.write("Mods que NO estan en Modrinth y por eso no se incluyen (%d):\n" % len(missing))
        f.write("\n".join(missing) + "\n")
    print("pack:", out, "(%.1f MB)" % (os.path.getsize(out) / 1048576))
    print("mods de Modrinth:", len(files), "| no estan en Modrinth:", len(missing), "| informe:", report)


if __name__ == "__main__":
    main()
