#!/usr/bin/env python3
"""Arma el paquete de instalacion de Path of Ascension (todo lo que va FUERA del jar).

Uso:  python tools/make_release.py <version>   (ej. v0.2.0)

Genera build/release/PathOfAscension-configs-<version>.zip con esta estructura, lista para
descomprimir dentro de la carpeta de la instancia (se pisan los archivos existentes):
    config/toughasnails/*, config/apotheosis/*, config/ftbquests/quests/*, config/fancymenu/*, kubejs/*
El jar del mod NO va en el zip: se distribuye aparte (releases/testamentodelacarne-0.1.0.jar).
Los mods de terceros tampoco se incluyen: ver modpack-config/MODS.txt.
"""
import os
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'modpack-config')

# carpeta del repo -> ruta dentro de la instancia
MAPPING = [
    ('config', 'config'),
    ('ftbquests', 'config/ftbquests'),
    ('fancymenu', 'config/fancymenu'),
    ('legacy-kubejs', 'kubejs'),
]

# archivo del repo -> ruta dentro de la instancia (para lo que va suelto en la raiz, no en config/)
ROOT_FILES = [
    (os.path.join(ROOT, 'tools', 'restore_after_wipe.py'), 'restore_after_wipe.py'),
    (os.path.join(SRC, 'root', 'restaurar_ultimo_punto.bat'), 'restaurar_ultimo_punto.bat'),
]


def main():
    version = sys.argv[1] if len(sys.argv) > 1 else 'dev'
    out_dir = os.path.join(ROOT, 'build', 'release')
    os.makedirs(out_dir, exist_ok=True)
    out = os.path.join(out_dir, 'PathOfAscension-configs-%s.zip' % version)
    count = 0
    with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as zf:
        for folder, dest in MAPPING:
            base = os.path.join(SRC, folder)
            if not os.path.isdir(base):
                continue
            for dirpath, _, files in os.walk(base):
                for name in files:
                    full = os.path.join(dirpath, name)
                    rel = os.path.relpath(full, base).replace(os.sep, '/')
                    zf.write(full, dest + '/' + rel)
                    count += 1
        zf.write(os.path.join(SRC, 'README.md'), 'LEEME-modpack-config.md')
        zf.write(os.path.join(SRC, 'MODS.txt'), 'MODS.txt')
        for full, dest in ROOT_FILES:
            if os.path.isfile(full):
                zf.write(full, dest)
                count += 1
    print('%s (%d archivos)' % (out, count))


if __name__ == '__main__':
    main()
