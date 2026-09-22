"""Respaldo del mundo, aparte del autoguardado normal de Minecraft (por si el modpack crashea).

No es un mod: copiar/zipear el mundo entero desde DENTRO del servidor (en un tick handler) provocaria
lag serio con un mundo grande. Esto corre COMO PROCESO APARTE, fuera del juego.

Comprime la carpeta del mundo en saves/<world>/ hacia <instancia>/crash_backups/<fecha>.zip: esa carpeta
NO esta dentro de saves/, asi que no aparece en la lista de mundos del launcher a menos que alguien mueva
el zip (descomprimido) ahi a mano.

Uso manual:
    python tools/backup_world.py "C:/Users/agust/AppData/Roaming/ATLauncher/instances/TestamentodelaCarne" "New Worlxzd"

Para que corra solo, programalo con el Programador de tareas de Windows (una vez):
    schtasks /create /tn "PathOfAscension backup" /sc minutely /mo 30 /tr ^
        "python D:\\Minecraft\\CaidaDeLosDioses\\tools\\backup_world.py \"<instancia>\" \"<mundo>\""
Ajusta el intervalo (/mo 30 = cada 30 minutos) a gusto. Guarda los ultimos KEEP backups y borra el resto.
"""
import shutil
import sys
import time
from pathlib import Path

KEEP = 10


def main():
    if len(sys.argv) != 3:
        print("Uso: backup_world.py <carpeta de la instancia> <nombre del mundo>")
        sys.exit(1)

    instance = Path(sys.argv[1])
    world = sys.argv[2]
    world_dir = instance / "saves" / world
    if not world_dir.is_dir():
        print(f"No existe el mundo: {world_dir}")
        sys.exit(1)

    backups_dir = instance / "crash_backups"
    backups_dir.mkdir(exist_ok=True)

    stamp = time.strftime("%Y-%m-%d_%H-%M-%S")
    dest_base = backups_dir / f"{world}_{stamp}"
    archive = shutil.make_archive(str(dest_base), "zip", root_dir=world_dir)
    print("Backup creado:", archive)

    existing = sorted(backups_dir.glob(f"{world}_*.zip"), key=lambda p: p.stat().st_mtime)
    for old in existing[:-KEEP]:
        old.unlink()
        print("Borrado (viejo):", old.name)


if __name__ == "__main__":
    main()
