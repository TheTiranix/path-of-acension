"""Empaqueta el video del menu principal (bosque con tormenta) para reproducirlo cuadro por cuadro desde el mod (client/MenuScene.java).

El mod no puede usar un mp4 (no hay decodificador en Minecraft), asi que el video se convierte a una secuencia de JPEG en un solo archivo.
Sin audio: el sonido (lluvia y truenos) lo pone el mod, sincronizado con los rayos que se detectan aca por el brillo de cada cuadro.

Formato de storm.bin (enteros de 4 bytes, big endian):
  ancho, alto, fps*1000, cuadros, cantidad_de_rayos, [cuadro de cada rayo]..., [offset de cada cuadro]... (cuadros+1), datos JPEG

Uso:  python tools/gen_menu_storm.py [ruta_al_mp4] [ancho] [cuadro_inicial] [cuadro_final]
Salida: src/main/resources/assets/tcorigenes/menu/storm.bin
"""
import os
import struct
import sys

import cv2
import numpy as np

SRC = sys.argv[1] if len(sys.argv) > 1 else "C:/Users/agust/Downloads/bosque_tormenta.mp4"
OUT_W = int(sys.argv[2]) if len(sys.argv) > 2 else 1280
QUALITY = 80
# Tramo que se repite en bucle (cuadros del video original, 24 por segundo): los primeros 3 segundos. El video entero tiene la camara acercandose
# a la criatura y un corte brusco al reiniciar; en estos 72 cuadros el principio y el final son oscuros y el empalme no se nota.
START = int(sys.argv[3]) if len(sys.argv) > 3 else 0
END = int(sys.argv[4]) if len(sys.argv) > 4 else 72
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "tcorigenes", "menu", "storm.bin")


def main():
    cap = cv2.VideoCapture(SRC)
    fps = cap.get(cv2.CAP_PROP_FPS)
    in_w, in_h = int(cap.get(3)), int(cap.get(4))
    out_h = int(round(OUT_W * in_h / in_w / 2.0)) * 2
    frames, lum = [], []
    index = -1
    while True:
        ok, fr = cap.read()
        if not ok:
            break
        index += 1
        if not (START <= index < END):
            continue
        lum.append(float(cv2.resize(fr, (96, 54)).mean()))
        small = cv2.resize(fr, (OUT_W, out_h), interpolation=cv2.INTER_AREA)
        ok, buf = cv2.imencode(".jpg", small, [cv2.IMWRITE_JPEG_QUALITY, QUALITY])
        frames.append(buf.tobytes())
    lum = np.array(lum)
    thr = lum.mean() + 2.2 * lum.std()
    flashes, last = [], -999
    for k, v in enumerate(lum):  # un rayo = el cuadro mas brillante de cada pico (separados al menos 0.8 s)
        if v > thr and k - last > fps * 0.8:
            flashes.append(k)
            last = k
    n = len(frames)
    header = [OUT_W, out_h, int(round(fps * 1000)), n, len(flashes)] + flashes
    offsets, pos = [], 0
    for f in frames:
        offsets.append(pos)
        pos += len(f)
    offsets.append(pos)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "wb") as fh:
        fh.write(struct.pack(">%di" % len(header), *header))
        fh.write(struct.pack(">%di" % len(offsets), *offsets))
        for f in frames:
            fh.write(f)
    print("cuadros", n, "fps", round(fps, 2), "tamaño", OUT_W, "x", out_h, "| rayos en los cuadros", flashes,
          "| segundos", [round(f / fps, 2) for f in flashes], "| MB", round(os.path.getsize(OUT) / 1e6, 1))


if __name__ == "__main__":
    main()
