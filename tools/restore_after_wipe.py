"""Restaura el mundo al ultimo punto de guardado despues de que el grupo entero cayo.

Cuando eso pasa, el mod (WorldRestoreManager) avisa, escribe `pending_restore.txt` en la carpeta de
la instancia y CIERRA EL JUEGO DEL TODO (no puede tocar los archivos del mundo con seguridad mientras
sigue corriendo). Este script se corre DESPUES de que el juego se cerro y ANTES de volver a abrirlo:
lee ese archivo, reemplaza el mundo por la copia de ese punto de guardado, y borra el marcador.

Si no hay nada pendiente, no hace nada (es seguro correrlo siempre, por las dudas, antes de abrir el
juego). Ver tambien restaurar_ultimo_punto.bat, que solo llama a este script.
"""
import os
import shutil
import sys

MARKER_NAME = "pending_restore.txt"


def read_marker(marker_path):
    values = {}
    with open(marker_path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if "=" in line:
                key, _, value = line.partition("=")
                values[key.strip()] = value.strip()
    return values


def main():
    # Por defecto, la carpeta donde esta ESTE script (asi funciona con doble click al .bat que lo
    # acompaña, sin importar desde donde se lo llame); se puede pasar otra carpeta como argumento.
    instance_dir = sys.argv[1] if len(sys.argv) > 1 else os.path.dirname(os.path.abspath(__file__))
    marker_path = os.path.join(instance_dir, MARKER_NAME)

    if not os.path.isfile(marker_path):
        print("Nada que restaurar (no hay", MARKER_NAME, "). Podés abrir el juego normalmente.")
        return

    values = read_marker(marker_path)
    world_root = values.get("world_root")
    snapshot = values.get("snapshot")
    if not world_root or not snapshot:
        print("El marcador está incompleto, lo borro y sigo sin tocar el mundo:", marker_path)
        os.remove(marker_path)
        return
    if not os.path.isdir(snapshot):
        print("No encuentro la copia del punto de guardado:", snapshot)
        print("No se toca el mundo. Revisá el problema antes de reintentar.")
        return

    print("Restaurando", world_root)
    print("      desde", snapshot)
    if os.path.isdir(world_root):
        shutil.rmtree(world_root)
    shutil.copytree(snapshot, world_root)
    os.remove(marker_path)
    print("Listo: el mundo volvió al último punto de guardado. Ya podés abrir el juego.")


if __name__ == "__main__":
    main()
