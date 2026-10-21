#!/usr/bin/env python3
"""Arma el zip del modpack para subir a CurseForge (formato manifest.json + overrides).

Los mods salen del registro de ATLauncher de la instancia (instance.json: id de proyecto y de archivo de CurseForge de cada jar que esta en
mods/ y no esta desactivado). Nuestro mod (pathofascension) no esta en ese registro: si se pasa --core-file-id (el id del archivo que ya subiste al
proyecto del mod) va como referencia; si no, el jar de releases/ va dentro de overrides/mods (CurseForge puede objetarlo: lo mejor es subir primero
el mod y volver a correr esto con el id). Los overrides son los de la release (build/release/PathOfAscension-configs-<version>.zip).

Uso:  python tools/make_curseforge_pack.py <version> [--core-file-id N] [carpeta_de_la_instancia]
Salida: build/release/PathOfAscension-CurseForge-<version>.zip
"""
import json
import os
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_INSTANCE = "C:/Users/agust/AppData/Roaming/ATLauncher/instances/PRUEBAS"
CORE_PROJECT_ID = 1742020
OWN_JAR = "pathofascension-0.1.0.jar"
# mods instalados desde Modrinth (sin ids de CurseForge en el registro): archivo -> (proyecto, archivo) de CurseForge, buscados a mano
MANUAL = {
    "Controlling-forge-1.20.1-12.0.2.jar": (250398, 4646682),
    "Searchables-forge-1.20.1-1.0.3.jar": (858542, 5284015),
}


def main():
    args = sys.argv[1:]
    version = args[0].lstrip("v")
    core_file = None
    if "--core-file-id" in args:
        core_file = int(args[args.index("--core-file-id") + 1])
    rest = [a for i, a in enumerate(args[1:], 1) if a != "--core-file-id" and (i < 2 or args[i - 1] != "--core-file-id")]
    instance = rest[0] if rest else DEFAULT_INSTANCE

    d = json.load(open(instance + "/instance.json", encoding="utf-8"))
    loader = d["launcher"]["loaderVersion"]["rawVersion"].split("-", 1)[1]
    present = set(os.listdir(instance + "/mods"))
    files, skipped = {}, []
    for m in d["launcher"]["mods"]:
        f = m.get("file")
        if m.get("disabled") or f not in present or f == OWN_JAR:
            continue
        pid, fid = m.get("curseForgeProjectId"), m.get("curseForgeFileId")
        if not (pid and fid):
            pid, fid = MANUAL.get(f, (None, None))
        if pid and fid:
            files[pid] = {"projectID": pid, "fileID": fid, "required": True}
        else:
            skipped.append(f)
    if core_file:
        files[CORE_PROJECT_ID] = {"projectID": CORE_PROJECT_ID, "fileID": core_file, "required": True}

    manifest = {
        "minecraft": {"version": "1.20.1", "modLoaders": [{"id": "forge-" + loader, "primary": True}]},
        "manifestType": "minecraftModpack", "manifestVersion": 1, "name": "Path of Ascension", "version": version,
        "author": "TheTiranix", "files": list(files.values()), "overrides": "overrides",
    }
    configs = os.path.join(ROOT, "build", "release", "PathOfAscension-configs-v%s.zip" % version)
    out = os.path.join(ROOT, "build", "release", "PathOfAscension-CurseForge-v%s.zip" % version)
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("manifest.json", json.dumps(manifest, indent=2))
        zf.writestr("modlist.html", "<ul>\n" + "".join("<li>%d</li>\n" % f["projectID"] for f in files.values()) + "</ul>\n")
        with zipfile.ZipFile(configs) as cz:
            for name in cz.namelist():
                if not name.endswith("/"):
                    zf.writestr("overrides/" + name, cz.read(name))
        if not core_file:
            zf.write(os.path.join(ROOT, "releases", OWN_JAR), "overrides/mods/" + OWN_JAR)
    print(out)
    print("mods de CurseForge:", len(files), "| forge", loader, "| nuestro mod:", "como referencia (archivo %d)" % core_file if core_file else "dentro de overrides/mods")
    if skipped:
        print("SIN id de CurseForge (no entran):", skipped)


if __name__ == "__main__":
    main()
