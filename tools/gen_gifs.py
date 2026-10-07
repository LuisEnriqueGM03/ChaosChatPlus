"""
GIFs animados de serie (pixel art propio, a partir de los emojis): assets/chaoschatplus/gifs/*.gif + index.json.
Uso: python tools/gen_gifs.py   (antes: python tools/gen_emojis.py)
"""
import json
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
EMOJI = os.path.join(ROOT, "src", "main", "resources", "assets", "chaoschatplus", "textures", "emoji")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "chaoschatplus", "gifs")
SIZE = 64  # 16 px de emoji x4


def emoji(name):
    return Image.open(os.path.join(EMOJI, name + ".png")).convert("RGBA")


def canvas():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def place(img, scale=3.0, dx=0, dy=0, sx=None, sy=None):
    """Emoji escalado (sin suavizar) y centrado con desplazamiento."""
    sx = sx or scale
    sy = sy or scale
    w, h = int(16 * sx), int(16 * sy)
    out = canvas()
    out.alpha_composite(img.resize((w, h), Image.NEAREST), ((SIZE - w) // 2 + dx, (SIZE - h) // 2 + dy))
    return out


def to_p(rgba):
    alpha = rgba.getchannel("A")
    p = rgba.convert("RGB").convert("P", palette=Image.ADAPTIVE, colors=255)
    mask = Image.eval(alpha, lambda a: 255 if a <= 128 else 0)
    p.paste(255, mask=mask)
    p.info["transparency"] = 255
    return p


def save(name, frames, duration):
    path = os.path.join(OUT, name + ".gif")
    ps = [to_p(f) for f in frames]
    ps[0].save(path, save_all=True, append_images=ps[1:], duration=duration, loop=0,
               transparency=255, disposal=2, optimize=False)
    # Pillow junta los frames repetidos y a veces deja bloques que el lector de GIF de Java no entiende:
    # se vuelve a guardar a partir de los frames ya juntados, que sí se lee bien.
    im = Image.open(path)
    merged, durations = [], []
    for k in range(im.n_frames):
        im.seek(k)
        merged.append(to_p(im.convert("RGBA")))
        durations.append(im.info.get("duration", duration))
    merged[0].save(path, save_all=True, append_images=merged[1:], duration=durations, loop=0,
                   transparency=255, disposal=2, optimize=False)


def creeper_baile():
    c = emoji("creeper")
    frames = []
    for k in range(8):
        t = k / 8 * 2 * math.pi
        sq = 1 + 0.12 * math.sin(t * 2)
        frames.append(place(c, sx=3.2 / sq, sy=3.2 * sq, dx=int(6 * math.sin(t)), dy=int(-4 * abs(math.sin(t)))))
    return frames


def fuego():
    f = emoji("fuego")
    frames = []
    for k in range(6):
        img = place(f, sx=3.4 + 0.15 * (k % 2), sy=3.4 + 0.25 * ((k * 7) % 3), dy=-(k % 3))
        frames.append(img)
    return frames


def tnt_boom():
    t = emoji("tnt")
    frames = []
    for k in range(6):
        img = place(t, 3.2)
        if k % 2:
            white = Image.new("RGBA", img.size, (255, 255, 255, 0))
            mask = img.getchannel("A").point(lambda a: 160 if a else 0)
            white.putalpha(mask)
            img.alpha_composite(white)
        frames.append(img)
    for r in (10, 18, 26, 31, 31):
        img = canvas()
        d = ImageDraw.Draw(img)
        d.ellipse([32 - r, 32 - r, 32 + r, 32 + r], fill=(255, 152, 0, 255))
        d.ellipse([32 - r * 0.65, 32 - r * 0.65, 32 + r * 0.65, 32 + r * 0.65], fill=(255, 235, 59, 255))
        d.ellipse([32 - r * 0.3, 32 - r * 0.3, 32 + r * 0.3, 32 + r * 0.3], fill=(255, 255, 255, 255))
        frames.append(img)
    frames.append(canvas())
    return frames


def corazon_latido():
    h = emoji("corazon")
    return [place(h, s) for s in (3.0, 3.4, 3.8, 3.4, 3.0, 3.0)]


SKIN = (255, 202, 40, 255)
SKIN_D = (224, 160, 0, 255)
LINE = (138, 90, 0, 255)
CUFF = (66, 165, 245, 255)
CUFF_D = (21, 101, 192, 255)


def hand(flip):
    """Mano de lado (palma hacia la derecha) a 32 px: dedos, pulgar y puño de la manga."""
    img = Image.new("RGBA", (14, 30), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([1, 2, 11, 22], radius=5, fill=SKIN, outline=LINE)
    for x in (4, 6, 8):  # separación de los dedos
        d.line([(x, 3), (x, 8)], fill=SKIN_D)
    d.line([(10, 10), (10, 20)], fill=SKIN_D)  # sombra del canto
    d.ellipse([7, 11, 13, 19], fill=SKIN, outline=LINE)  # pulgar
    d.line([(9, 13), (11, 16)], fill=SKIN_D)
    d.rectangle([2, 22, 10, 28], fill=CUFF, outline=CUFF_D)
    return img.transpose(Image.FLIP_LEFT_RIGHT) if flip else img


def aplausos():
    """Dos manos que se juntan y aplauden, con chispas al chocar (dibujado a 32 px y ampliado x2)."""
    left = hand(False)
    right = hand(True)
    spark = (255, 238, 88, 255)
    frames = []
    for gap, sparks in ((8, False), (4, False), (0, True), (0, True), (4, False), (8, False)):
        small = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
        small.alpha_composite(left, (16 - 13 - gap // 2, 1))
        small.alpha_composite(right, (16 - 1 + gap // 2, 1))
        if sparks:
            d = ImageDraw.Draw(small)
            for (x0, y0, x1, y1) in ((15, 0, 15, 2), (10, 1, 8, -1), (20, 1, 22, -1), (4, 6, 2, 5), (26, 6, 28, 5)):
                d.line([(x0, y0), (x1, y1)], fill=spark)
        frames.append(small.resize((SIZE, SIZE), Image.NEAREST))
    return frames


def gg():
    base = emoji("gg")
    colors = [(229, 57, 53), (251, 140, 0), (253, 216, 53), (67, 160, 71), (30, 136, 229), (142, 36, 170)]
    frames = []
    for c in colors:
        img = place(base, 3.6)
        px = img.load()
        for y in range(SIZE):
            for x in range(SIZE):
                r, g, b, a = px[x, y]
                if a and not (r > 230 and g > 230 and b > 230):  # todo menos las letras blancas
                    px[x, y] = (*c, a)
        frames.append(img)
    return frames


def risa():
    f = emoji("llorar_risa")
    return [place(f, 3.4, dx=dx, dy=dy) for dx, dy in ((0, 0), (-3, -1), (0, -2), (3, -1))]


def ojos():
    e = emoji("ojos")
    return [place(e, 3.4), place(e, 3.4), place(e.transpose(Image.FLIP_LEFT_RIGHT), 3.4),
            place(e.transpose(Image.FLIP_LEFT_RIGHT), 3.4)]


GIFS = [
    ("creeper_baile", "Creeper bailando", creeper_baile, 90, ["baile", "dance", "minecraft"]),
    ("fuego", "Fuego", fuego, 110, ["fire", "lit"]),
    ("tnt_boom", "TNT boom", tnt_boom, 120, ["explosion", "boom", "minecraft"]),
    ("corazon_latido", "Corazón latiendo", corazon_latido, 100, ["amor", "love", "heart"]),
    ("aplausos", "Aplausos", aplausos, 90, ["clap", "bravo"]),
    ("gg", "GG", gg, 150, ["good game", "ganar"]),
    ("risa", "Risa", risa, 90, ["laugh", "jaja", "lol"]),
    ("ojos", "Ojos", ojos, 400, ["mirar", "eyes"]),
]


# ---------------------------------------------------------------- GIFs animados a partir de los emojis

def anim_bounce(img):
    return [place(img, 3.2, dy=dy, sx=3.2 * sx, sy=3.2 / sx)
            for dy, sx in ((0, 1.08), (-6, 0.96), (-10, 0.94), (-6, 0.96), (0, 1.08), (2, 1.14))]


def anim_shake(img):
    return [place(img, 3.3, dx=dx) for dx in (0, -4, 4, -3, 3, 0)]


def anim_pulse(img):
    return [place(img, s) for s in (2.9, 3.2, 3.6, 3.2, 2.9, 2.9)]


def anim_wobble(img):
    frames = []
    for angle in (0, 12, 18, 12, 0, -12, -18, -12):
        big = img.resize((52, 52), Image.NEAREST).rotate(angle, resample=Image.NEAREST, expand=False)
        out = canvas()
        out.alpha_composite(big, (6, 6))
        frames.append(out)
    return frames


def anim_float(img):
    return [place(img, 3.2, dy=dy) for dy in (0, -2, -4, -5, -4, -2, 0, 1)]


def anim_spin(img):
    frames = []
    for k in range(8):
        sx = abs(math.cos(k / 8 * math.pi)) * 3.4
        frames.append(place(img if k < 4 else img.transpose(Image.FLIP_LEFT_RIGHT), sx=max(0.4, sx), sy=3.4))
    return frames


def anim_blink(img):
    """Aparece y desaparece con brillo (para símbolos)."""
    frames = []
    for a in (1.0, 1.0, 0.7, 0.4, 0.7, 1.0):
        f = place(img, 3.3)
        alpha = f.getchannel("A").point(lambda v: int(v * a))
        f.putalpha(alpha)
        frames.append(f)
    return frames


EFFECTS = {
    "bounce": (anim_bounce, 90, "saltando"),
    "shake": (anim_shake, 70, "temblando"),
    "pulse": (anim_pulse, 110, "latiendo"),
    "wobble": (anim_wobble, 80, "bailando"),
    "float": (anim_float, 110, "flotando"),
    "spin": (anim_spin, 70, "girando"),
    "blink": (anim_blink, 120, "brillando"),
}

# Efecto por categoría de emoji, y qué emojis se animan (100 en total).
CATEGORY_EFFECTS = {
    "caras": ["bounce", "shake", "wobble"],
    "corazones": ["pulse"],
    "gestos": ["wobble"],
    "animales": ["bounce", "wobble"],
    "comida": ["spin", "float"],
    "naturaleza": ["float", "spin"],
    "objetos": ["float", "spin"],
    "simbolos": ["blink", "pulse"],
    "minecraft": ["float", "spin", "bounce"],
}
SKIP = {"n0", "n1", "n2", "n3", "n4", "n5", "n6", "n7", "n8", "n9", "n10", "arriba", "abajo", "izquierda", "derecha",
        "mas", "menos", "new", "ok_simbolo", "sos", "gg", "fuego", "creeper", "tnt", "ojos", "aplausos", "corazon",
        "llorar_risa"}
EXTRA_COUNT = 100


def emoji_gifs():
    with open(os.path.join(EMOJI, "..", "..", "emojis.json"), encoding="utf-8") as f:
        emojis = json.load(f)
    out = []
    used = {}
    for e in emojis:
        if len(out) >= EXTRA_COUNT:
            break
        if e["name"] in SKIP or e["category"] not in CATEGORY_EFFECTS:
            continue
        effects = CATEGORY_EFFECTS[e["category"]]
        n = used.get(e["category"], 0)
        used[e["category"]] = n + 1
        effect = effects[n % len(effects)]
        fn, duration, label = EFFECTS[effect]
        title = e["name"].replace("_", " ").capitalize() + " " + label
        tags = [e["name"].replace("_", " "), e["category"], label] + e["aliases"]
        out.append(("e_" + e["name"], title, (lambda img=emoji(e["name"]), fn=fn: fn(img)), duration, tags))
    return out


def main():
    os.makedirs(OUT, exist_ok=True)
    # Los GIFs "x_*" (troll y bromas) se hicieron aparte: se conservan con su entrada del índice.
    kept = []
    index_path = os.path.join(OUT, "index.json")
    if os.path.exists(index_path):
        with open(index_path, encoding="utf-8") as f:
            kept = [e for e in json.load(f) if e["id"].startswith("x_")]
    for f in os.listdir(OUT):
        if f.endswith(".gif") and not f.startswith("x_"):
            os.remove(os.path.join(OUT, f))
    from gen_gifs_extra import all_extra
    index = list(kept)
    # Primero los textos y efectos (sin anime ni emojis): salen antes en "Tendencias" y en las categorías.
    for gid, name, fn, tags in all_extra():
        frames, duration = fn()
        save(gid, frames, duration)
        index.append({"id": gid, "name": name, "tags": tags})
    for gid, name, fn, duration, tags in GIFS + emoji_gifs():
        save(gid, fn(), duration)
        index.append({"id": gid, "name": name, "tags": tags})
    with open(os.path.join(OUT, "index.json"), "w", encoding="utf-8") as f:
        json.dump(index, f, ensure_ascii=False, indent="\t")
        f.write("\n")
    print(f"{len(index)} GIFs generados")


if __name__ == "__main__":
    main()
