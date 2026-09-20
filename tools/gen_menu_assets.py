"""Genera los recursos originales del menu principal tetrico (fondo, logo, botones y ambiente sonoro).

El sonido va como OGG dentro del mod (lo reproduce MenuAmbience); requiere `pip install soundfile`.

Todo se dibuja/sintetiza por codigo: no se usa arte ni musica de ningun juego existente.
Uso:  python tools/gen_menu_assets.py
Salida: modpack-config/fancymenu/assets/
"""
import math
import os
import random
import wave

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.join(os.path.dirname(__file__), "..", "modpack-config", "fancymenu", "assets")
os.makedirs(ROOT, exist_ok=True)
rng = np.random.default_rng(1666)
random.seed(1666)


# ------------------------------------------------------------------ utilidades
def fbm(w, h, octaves=((6, 4, 1.0), (12, 7, 0.55), (24, 14, 0.3), (48, 27, 0.16), (96, 54, 0.08))):
    """Ruido de nubes/niebla: suma de octavas de ruido suavizado."""
    total = np.zeros((h, w), dtype=np.float32)
    for gw, gh, amp in octaves:
        small = Image.fromarray((rng.random((gh, gw)) * 255).astype(np.uint8))
        big = small.resize((w, h), Image.BICUBIC)
        total += np.asarray(big, dtype=np.float32) / 255.0 * amp
    total -= total.min()
    total /= total.max()
    return total


def font(size, bold=True):
    path = "C:/Windows/Fonts/" + ("georgiab.ttf" if bold else "georgia.ttf")
    try:
        return ImageFont.truetype(path, size)
    except OSError:
        return ImageFont.load_default()


# ---------------------------------------------------------------------- fondo
def make_background():
    w, h = 1920, 1080
    base = np.zeros((h, w, 3), dtype=np.float32)
    base[:] = (7, 5, 6)

    # cielo: degradado casi negro con un resplandor rojo apagado detras de la luna
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    mx, my = w * 0.68, h * 0.30
    dist = np.sqrt((xx - mx) ** 2 + (yy - my) ** 2)
    glow = np.exp(-(dist / 380.0) ** 2)
    base += glow[..., None] * np.array([70, 10, 12], dtype=np.float32)

    # niebla roja y gris que sube desde el suelo
    fog1 = fbm(w, h) ** 2.2
    fog2 = fbm(w, h, ((4, 3, 1.0), (9, 5, 0.6), (20, 11, 0.3), (40, 22, 0.15)))
    ground = np.clip((yy - h * 0.35) / (h * 0.65), 0, 1) ** 1.3
    base += (fog1 * ground)[..., None] * np.array([120, 16, 18], dtype=np.float32) * 0.55
    base += (fog2 * ground)[..., None] * np.array([70, 66, 70], dtype=np.float32) * 0.35

    img = Image.fromarray(np.clip(base, 0, 255).astype(np.uint8))
    draw = ImageDraw.Draw(img, "RGBA")

    # luna: disco palido rojizo, velado por la niebla
    r = 96
    draw.ellipse((mx - r, my - r, mx + r, my + r), fill=(150, 60, 58, 255))
    draw.ellipse((mx - r + 14, my - r + 10, mx + r - 6, my + r - 12), fill=(176, 84, 78, 255))
    for cx, cy, cr in ((mx - 30, my - 20, 16), (mx + 34, my + 26, 10), (mx + 8, my - 44, 8)):
        draw.ellipse((cx - cr, cy - cr, cx + cr, cy + cr), fill=(120, 44, 44, 255))
    img = img.filter(ImageFilter.GaussianBlur(3))

    # suelo irregular y arboles muertos en silueta
    sil = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    sd = ImageDraw.Draw(sil)
    horizon = int(h * 0.78)
    pts = [(0, h)]
    yv = horizon
    for x in range(0, w + 20, 20):
        yv += random.randint(-6, 6)
        yv = min(max(yv, horizon - 30), horizon + 30)
        pts.append((x, yv))
    pts.append((w, h))
    sd.polygon(pts, fill=(3, 2, 3, 255))

    def branch(x, y, ang, length, width, depth):
        if depth == 0 or length < 6:
            return
        x2 = x + math.cos(ang) * length
        y2 = y - math.sin(ang) * length
        sd.line((x, y, x2, y2), fill=(3, 2, 3, 255), width=max(1, int(width)))
        for da in (random.uniform(0.35, 0.7), -random.uniform(0.35, 0.7)):
            if random.random() < 0.9:
                branch(x2, y2, ang + da + random.uniform(-0.15, 0.15), length * random.uniform(0.62, 0.78), width * 0.68, depth - 1)

    for tx, tl, tw in ((140, 230, 15), (420, 150, 10), (1330, 190, 12), (1610, 260, 17), (1810, 140, 9)):
        ty = horizon + random.randint(-10, 20)
        branch(tx, ty, math.pi / 2 + random.uniform(-0.12, 0.12), tl, tw, 8)

    # una horca lejana
    gx = int(w * 0.52)
    sd.rectangle((gx, horizon - 190, gx + 9, horizon + 6), fill=(3, 2, 3, 255))
    sd.rectangle((gx, horizon - 190, gx + 120, horizon - 181), fill=(3, 2, 3, 255))
    sd.line((gx + 104, horizon - 181, gx + 104, horizon - 120), fill=(3, 2, 3, 255), width=3)
    sd.ellipse((gx + 98, horizon - 124, gx + 110, horizon - 108), fill=(3, 2, 3, 255))
    sd.line((gx + 104, horizon - 108, gx + 104, horizon - 62), fill=(3, 2, 3, 255), width=5)
    sd.line((gx + 104, horizon - 96, gx + 90, horizon - 66), fill=(3, 2, 3, 255), width=3)
    sd.line((gx + 104, horizon - 96, gx + 119, horizon - 66), fill=(3, 2, 3, 255), width=3)
    sil = sil.filter(ImageFilter.GaussianBlur(1.4))
    img.paste(sil, (0, 0), sil)

    # niebla baja por delante de las siluetas
    arr = np.asarray(img, dtype=np.float32)
    front = fbm(w, h, ((5, 3, 1.0), (11, 6, 0.6), (26, 14, 0.25))) ** 1.8
    band = np.exp(-((yy - horizon - 20) / 130.0) ** 2)
    arr += (front * band)[..., None] * np.array([90, 40, 42], dtype=np.float32) * 0.9

    # goteo de sangre en el borde superior
    drips = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    dd = ImageDraw.Draw(drips)
    x = 0
    while x < w:
        x += random.randint(26, 90)
        ln = random.choice((14, 22, 40, 70, 120, 34))
        wd = random.randint(2, 6)
        dd.rectangle((x, 0, x + wd, ln), fill=(70, 0, 4, 200))
        dd.ellipse((x - 1, ln - wd, x + wd + 1, ln + wd), fill=(70, 0, 4, 200))
    dd.rectangle((0, 0, w, 10), fill=(60, 0, 3, 210))
    drips = drips.filter(ImageFilter.GaussianBlur(1.2))
    base_img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8)).convert("RGBA")
    base_img.alpha_composite(drips)
    arr = np.asarray(base_img.convert("RGB"), dtype=np.float32)

    # vineta fuerte y grano
    nx = (xx - w / 2) / (w / 2)
    ny = (yy - h / 2) / (h / 2)
    vign = 1.0 - np.clip((nx ** 2 * 0.8 + ny ** 2 * 1.1), 0, 1) ** 1.25 * 0.92
    arr *= vign[..., None]
    arr *= 0.78
    arr += rng.normal(0, 5.0, (h, w, 1)).astype(np.float32)
    lum = arr.mean(axis=2, keepdims=True)
    arr = lum + (arr - lum) * 0.8  # menos saturacion, mas sucio
    Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8)).save(os.path.join(ROOT, "menu_bg.png"))


# ----------------------------------------------------------------------- logo
def make_logo():
    w, h = 1200, 210
    title = "PATH OF ASCENSION"
    spacing = 10
    size = 150
    while size > 40:
        f = font(size)
        total = sum(f.getlength(c) for c in title) + spacing * (len(title) - 1)
        if total < w - 90:
            break
        size -= 4
    f = font(size)
    total = sum(f.getlength(c) for c in title) + spacing * (len(title) - 1)
    mask = Image.new("L", (w, h), 0)
    md = ImageDraw.Draw(mask)
    ty = 24
    x = (w - total) / 2
    for c in title:
        md.text((x, ty), c, font=f, fill=255)
        x += f.getlength(c) + spacing
    tw = total

    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    glow = mask.filter(ImageFilter.GaussianBlur(14))
    glow_layer = Image.new("RGBA", (w, h), (90, 0, 0, 0))
    glow_layer.putalpha(glow.point(lambda p: int(p * 0.9)))
    out.alpha_composite(glow_layer)
    shadow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    shadow.putalpha(mask.point(lambda p: int(p * 0.95)))
    out.alpha_composite(shadow, (4, 5))
    grad = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    gp = grad.load()
    y0, y1 = ty + 10, ty + size + 20
    for y in range(h):
        t = min(max((y - y0) / max(1, (y1 - y0)), 0), 1)
        col = (int(175 - 120 * t), int(22 - 16 * t), int(24 - 18 * t))
        for x in range(w):
            gp[x, y] = col + (255,)
    grad.putalpha(mask)
    out.alpha_composite(grad)
    edge = mask.filter(ImageFilter.MaxFilter(3))
    edge_layer = Image.new("RGBA", (w, h), (12, 0, 2, 0))
    edge_layer.putalpha(ImageChops_subtract(edge, mask))
    out.alpha_composite(edge_layer)

    d = ImageDraw.Draw(out)
    marr = np.asarray(mask)
    for _ in range(26):
        x = random.randint(int((w - tw) / 2) + 10, int((w + tw) / 2) - 10)
        ys = np.where(marr[:, x] > 200)[0]
        if len(ys) == 0:
            continue
        yb = int(ys.max())
        ln = random.choice((10, 16, 26, 40, 60))
        wd = random.choice((2, 3, 4))
        d.rectangle((x, yb - 2, x + wd, yb + ln), fill=(120, 6, 8, 255))
        d.ellipse((x - 1, yb + ln - wd, x + wd + 1, yb + ln + wd), fill=(120, 6, 8, 255))
    out.save(os.path.join(ROOT, "menu_logo.png"))


def ImageChops_subtract(a, b):
    from PIL import ImageChops
    return ImageChops.subtract(a, b)


# ---------------------------------------------------------------------- botones
def make_buttons():
    def button(name, fill, border, hi, noise=6):
        w, h = 200, 20
        arr = np.zeros((h, w, 3), dtype=np.float32)
        arr[:] = fill
        arr += rng.normal(0, noise, (h, w, 1)).astype(np.float32)
        img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8)).convert("RGBA")
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, w - 1, h - 1), outline=border)
        d.line((1, 1, w - 2, 1), fill=hi)
        d.line((1, h - 2, w - 2, h - 2), fill=(0, 0, 0, 255))
        # esquinas "rotas"
        for cx, cy in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
            d.point((cx, cy), fill=(0, 0, 0, 0))
        img.save(os.path.join(ROOT, name))

    button("btn_normal.png", (22, 16, 18), (84, 22, 22, 255), (52, 34, 36, 255))
    button("btn_hover.png", (58, 12, 14), (190, 44, 40, 255), (110, 40, 38, 255), noise=9)
    button("btn_inactive.png", (15, 13, 14), (40, 34, 36, 255), (26, 22, 24, 255), noise=3)


# ------------------------------------------------------------------- sonido
def make_ambience(seconds=60, fs=22050):
    """Ambiente turbio en bucle perfecto: todo son ciclos enteros y el reverb es una convolucion circular."""
    n = seconds * fs
    t = np.arange(n) / fs

    def snap(f):
        return round(f * seconds) / seconds  # ciclos enteros en el bucle

    def sine(f, phase=0.0):
        return np.sin(2 * math.pi * snap(f) * t + phase)

    def lfo(f, phase=0.0):
        return 0.5 + 0.5 * np.sin(2 * math.pi * (round(f * seconds) / seconds) * t + phase)

    # 1) zumbido grave y disonante (segunda menor + tritono)
    drone = (
        0.9 * np.tanh(1.6 * sine(41.2)) + 0.6 * sine(43.65, 1.0)
        + 0.35 * sine(82.4) * lfo(1 / 30.0) + 0.3 * sine(87.3, 2.0) * lfo(1 / 20.0, 1.3)
        + 0.22 * sine(116.5) * lfo(1 / 12.0, 0.7) + 0.2 * sine(123.5, 0.2) * lfo(1 / 15.0, 2.1)
    )
    drone *= 0.55 + 0.45 * lfo(1 / 60.0)

    # 2) coro de cuerdas desafinadas (armonicos de sierra, se filtran mas abajo)
    pad = np.zeros(n)
    for base, amp, ph in ((220.0, 0.10, 0.0), (233.1, 0.09, 1.7), (277.2, 0.07, 3.1), (349.2, 0.05, 0.6)):
        for k in range(1, 9):
            pad += amp / k * np.sin(2 * math.pi * snap(base * k) * t + ph * k)
    pad *= (0.15 + 0.85 * lfo(1 / 15.0, 0.4) ** 2) * (0.5 + 0.5 * lfo(1 / 60.0, 2.0))

    # 3) aliento / viento sucio (ruido filtrado en frecuencia, modulado lento)
    def band_noise(lo, hi):
        spec = np.fft.rfft(rng.normal(0, 1, n))
        f = np.fft.rfftfreq(n, 1 / fs)
        spec *= ((f > lo) & (f < hi)) * (1.0 / np.sqrt(np.maximum(f, 1.0)))
        out = np.fft.irfft(spec, n)
        return out / np.abs(out).max()

    wind = band_noise(120, 900) * (0.25 + 0.75 * lfo(1 / 12.0, 1.0) ** 2) * 0.8
    hiss = band_noise(2500, 7500) * (0.1 + 0.9 * lfo(1 / 20.0, 2.6) ** 3) * 0.22
    rumble = band_noise(20, 120) * 0.7

    # 4) lamentos y raspaduras lejanas
    wails = np.zeros(n)
    for _ in range(7):
        start = random.randint(0, n - 1)
        dur = int(random.uniform(3.0, 6.0) * fs)
        f0 = random.choice((480, 620, 733, 910))
        glide = random.uniform(-0.25, 0.35)
        tt = np.arange(dur) / fs
        freq = f0 * (1 + glide * tt / (dur / fs))
        phase = 2 * math.pi * np.cumsum(freq) / fs + 3 * np.sin(2 * math.pi * 5.1 * tt) / 5.1 * 0.2
        env = np.sin(math.pi * tt / (dur / fs)) ** 2
        tone = np.sin(phase) * env * 0.09 + np.sin(phase * 2.01) * env * 0.03
        idx = (start + np.arange(dur)) % n
        wails[idx] += tone

    # 5) latido lejano muy bajo (1.6 s por ciclo -> 37.5 ciclos en 60 s; se ajusta a 60/1.5=40)
    beat = np.zeros(n)
    period = int(1.5 * fs)
    for b in range(0, n, period):
        for off, amp in ((0, 1.0), (int(0.32 * fs), 0.7)):
            i0 = b + off
            L = int(0.22 * fs)
            tt = np.arange(L) / fs
            thump = np.sin(2 * math.pi * 52 * tt) * np.exp(-tt * 22) * amp
            idx = (i0 + np.arange(L)) % n
            beat[idx] += thump
    beat *= 0.5

    dry = 0.9 * drone + 0.55 * pad + 0.5 * wind + 0.35 * hiss + 0.5 * rumble + 1.0 * wails + 0.45 * beat

    # 6) reverb: respuesta larga (4 s) aplicada circularmente para no romper el bucle
    def impulse(seed):
        r = np.random.default_rng(seed)
        m = int(4.0 * fs)
        ir = r.normal(0, 1, m) * np.exp(-np.arange(m) / (1.1 * fs))
        ir[: int(0.03 * fs)] *= np.linspace(0, 1, int(0.03 * fs))
        ir /= np.sqrt((ir ** 2).sum())
        full = np.zeros(n)
        full[:m] = ir
        return np.fft.rfft(full)

    spec = np.fft.rfft(dry)
    wet_l = np.fft.irfft(spec * impulse(1), n)
    wet_r = np.fft.irfft(spec * impulse(2), n)
    left = 0.62 * dry + 0.55 * wet_l
    right = 0.62 * dry + 0.55 * wet_r
    # leve descorrelacion estereo del viento
    left += 0.12 * band_noise(150, 700) * lfo(1 / 10.0, 0.0) * 0.4
    right += 0.12 * band_noise(150, 700) * lfo(1 / 10.0, 3.1) * 0.4

    stereo = np.stack([left, right], axis=1)
    stereo /= np.abs(stereo).max()
    stereo *= 0.8
    import soundfile as sf
    sounds = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "tcorigenes", "sounds")
    os.makedirs(sounds, exist_ok=True)
    path = os.path.join(sounds, "menu_ambient.ogg")
    data = stereo.astype("float32")
    with sf.SoundFile(path, "w", samplerate=fs, channels=2, format="OGG", subtype="VORBIS") as out:
        for i in range(0, len(data), fs):  # por bloques: escribir todo de una vez cuelga libsndfile
            out.write(data[i:i + fs])
    print("audio", path, os.path.getsize(path) // 1024, "KB", "peak", float(np.abs(stereo).max()),
          "rms", float(np.sqrt((stereo ** 2).mean())))


if __name__ == "__main__":
    make_background()
    print("bg ok")
    make_logo()
    print("logo ok")
    make_buttons()
    print("buttons ok")
    make_ambience()
