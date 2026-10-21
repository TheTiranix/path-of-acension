"""Genera el sonido de lluvia (bucle sin cortes) del menu principal. Se reproduce junto con el video de la tormenta (client/MenuScene.java).

Todo se sintetiza por codigo (ruido filtrado + gotas), sin muestras de ningun juego. Requiere `pip install soundfile scipy`.
Uso:  python tools/gen_menu_rain.py
Salida: src/main/resources/assets/tcorigenes/sounds/menu_rain.ogg
"""
import os

import numpy as np
import soundfile as sf
from scipy.signal import butter, sosfilt

FS = 32000
SECONDS = 14
FADE = 2.0  # segundos de cruce entre el final y el principio para que el bucle no se corte
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "tcorigenes", "sounds", "menu_rain.ogg")
rng = np.random.default_rng(20261020)


def band(x, lo, hi):
    return sosfilt(butter(4, [lo, hi], btype="bandpass", fs=FS, output="sos"), x)


def channel(n):
    hiss = band(rng.normal(0, 1, n), 1200, 9000)  # la lluvia en general
    body = band(rng.normal(0, 1, n), 300, 1400) * 0.55  # gotas en el suelo
    rumble = band(rng.normal(0, 1, n), 40, 180) * 0.35  # tormenta lejana
    drops = np.zeros(n)
    for _ in range(int(n / FS * 90)):  # gotas sueltas, mas fuertes
        i = rng.integers(0, n - 400)
        ln = int(rng.integers(40, 300))
        env = np.exp(-np.linspace(0, 7, ln))
        drops[i:i + ln] += band(rng.normal(0, 1, ln + 64), 1800, 7500)[:ln] * env * rng.uniform(0.4, 1.4)
    swell = 1.0 + 0.18 * np.sin(np.linspace(0, 2 * np.pi * 3, n) + rng.uniform(0, 6))  # sube y baja suave
    return (hiss + body + rumble + drops * 0.9) * swell


def main():
    n = int((SECONDS + FADE) * FS)
    sig = np.stack([channel(n), channel(n)], axis=1)
    sig /= np.abs(sig).max()
    f = int(FADE * FS)
    ramp = np.linspace(0, 1, f)[:, None]
    loop = sig[:-f].copy()
    loop[:f] = loop[:f] * ramp + sig[-f:] * (1 - ramp)  # el final se funde con el principio
    loop *= 0.55
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    sf.write(OUT, loop.astype(np.float32), FS, format="OGG", subtype="VORBIS")
    print("ok", round(os.path.getsize(OUT) / 1e3), "KB,", round(len(loop) / FS, 1), "s")


if __name__ == "__main__":
    main()
