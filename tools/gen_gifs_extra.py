"""
Tercera tanda de GIFs de serie (sin anime): textos animados tipo sticker (GG, LOL, F, OMG, BRUH...) y efectos
(fuegos artificiales, confeti, lluvia de corazones/dinero/diamantes, nieve, carga, sirena...). Todo dibujado aquí.
La importa gen_gifs.py.
"""
import math
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
EMOJI = os.path.join(ROOT, "src", "main", "resources", "assets", "chaoschatplus", "textures", "emoji")

W_TEXT, H_TEXT = 128, 64
SIZE = 64

# Fuente pixel 5x7 (mayúsculas, números y signos).
FONT = {
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "B": ["11110", "10001", "10001", "11110", "10001", "10001", "11110"],
    "C": ["01111", "10000", "10000", "10000", "10000", "10000", "01111"],
    "D": ["11110", "10001", "10001", "10001", "10001", "10001", "11110"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "F": ["11111", "10000", "10000", "11110", "10000", "10000", "10000"],
    "G": ["01111", "10000", "10000", "10011", "10001", "10001", "01111"],
    "H": ["10001", "10001", "10001", "11111", "10001", "10001", "10001"],
    "I": ["11111", "00100", "00100", "00100", "00100", "00100", "11111"],
    "J": ["00111", "00010", "00010", "00010", "10010", "10010", "01100"],
    "K": ["10001", "10010", "10100", "11000", "10100", "10010", "10001"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
    "N": ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    "Q": ["01110", "10001", "10001", "10001", "10101", "10010", "01101"],
    "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
    "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    "U": ["10001", "10001", "10001", "10001", "10001", "10001", "01110"],
    "V": ["10001", "10001", "10001", "10001", "10001", "01010", "00100"],
    "W": ["10001", "10001", "10001", "10101", "10101", "11011", "10001"],
    "X": ["10001", "10001", "01010", "00100", "01010", "10001", "10001"],
    "Y": ["10001", "10001", "01010", "00100", "00100", "00100", "00100"],
    "Z": ["11111", "00001", "00010", "00100", "01000", "10000", "11111"],
    "0": ["01110", "10001", "10011", "10101", "11001", "10001", "01110"],
    "1": ["00100", "01100", "00100", "00100", "00100", "00100", "01110"],
    "2": ["01110", "10001", "00001", "00010", "00100", "01000", "11111"],
    "3": ["11110", "00001", "00001", "01110", "00001", "00001", "11110"],
    "4": ["00010", "00110", "01010", "10010", "11111", "00010", "00010"],
    "5": ["11111", "10000", "11110", "00001", "00001", "10001", "01110"],
    "6": ["01110", "10000", "10000", "11110", "10001", "10001", "01110"],
    "7": ["11111", "00001", "00010", "00100", "01000", "01000", "01000"],
    "8": ["01110", "10001", "10001", "01110", "10001", "10001", "01110"],
    "9": ["01110", "10001", "10001", "01111", "00001", "00001", "01110"],
    "!": ["00100", "00100", "00100", "00100", "00100", "00000", "00100"],
    "?": ["01110", "10001", "00001", "00010", "00100", "00000", "00100"],
    ".": ["00000", "00000", "00000", "00000", "00000", "00000", "00100"],
    "<": ["00010", "00100", "01000", "10000", "01000", "00100", "00010"],
    "3_": ["00000"] * 7,
    " ": ["00000"] * 7,
}

RAINBOW = [(229, 57, 53), (251, 140, 0), (253, 216, 53), (67, 160, 71), (30, 136, 229), (142, 36, 170)]
OUTLINE = (20, 20, 26, 255)


def letter_mask(ch, scale):
    rows = FONT.get(ch, FONT[" "])
    img = Image.new("L", (5 * scale, 7 * scale), 0)
    d = ImageDraw.Draw(img)
    for y, row in enumerate(rows):
        for x, v in enumerate(row):
            if v == "1":
                d.rectangle([x * scale, y * scale, x * scale + scale - 1, y * scale + scale - 1], fill=255)
    return img


def fit_scale(text):
    for scale in (5, 4, 3, 2):
        if len(text) * 6 * scale - scale <= W_TEXT - 8 and 7 * scale <= H_TEXT - 12:
            return scale
    return 2


def draw_text(text, colors, offsets=None, visible=None, jitter=None):
    """Texto centrado; colors = color por letra; offsets = (dx, dy) por letra; visible = cuántas letras se ven."""
    scale = fit_scale(text)
    total = len(text) * 6 * scale - scale
    x0 = (W_TEXT - total) // 2
    y0 = (H_TEXT - 7 * scale) // 2
    img = Image.new("RGBA", (W_TEXT, H_TEXT), (0, 0, 0, 0))
    for i, ch in enumerate(text):
        if visible is not None and i >= visible:
            break
        if ch == " ":
            continue
        dx, dy = offsets[i] if offsets else (0, 0)
        if jitter:
            dx += jitter[i][0]
            dy += jitter[i][1]
        mask = letter_mask(ch, scale)
        x = x0 + i * 6 * scale + dx
        y = y0 + dy
        # Contorno: la letra desplazada en 8 direcciones.
        for ox in (-2, 0, 2):
            for oy in (-2, 0, 2):
                if ox or oy:
                    img.paste(OUTLINE, (x + ox, y + oy), mask)
        img.paste(OUTLINE, (x + 2, y + 3), mask)
        img.paste(colors[i % len(colors)] + (255,) if len(colors[i % len(colors)]) == 3 else colors[i % len(colors)],
                  (x, y), mask)
    return img


# ---------------------------------------------------------------- efectos de texto

def fx_rainbow(text):
    frames = []
    for k in range(6):
        frames.append(draw_text(text, [RAINBOW[(i + k) % 6] for i in range(len(text))]))
    return frames, 110


def fx_wave(text, color=(255, 213, 74)):
    frames = []
    for k in range(8):
        offs = [(0, int(round(4 * math.sin((i * 0.9) - k * math.pi / 4)))) for i in range(len(text))]
        frames.append(draw_text(text, [color], offs))
    return frames, 80


def fx_bounce(text, color=(122, 162, 255)):
    frames = []
    for dy in (0, -4, -7, -4, 0, 2):
        frames.append(draw_text(text, [color], [(0, dy)] * len(text)))
    return frames, 90


def fx_shake(text, color=(255, 107, 107)):
    rnd = random.Random(text)
    frames = []
    for _ in range(6):
        jit = [(rnd.randint(-2, 2), rnd.randint(-2, 2)) for _ in text]
        frames.append(draw_text(text, [color], jitter=jit))
    return frames, 60


def fx_type(text, color=(230, 232, 238)):
    frames = [draw_text(text, [color], visible=n) for n in range(1, len(text) + 1)]
    frames += [frames[-1]] * 6
    return frames, 120


def fx_neon(text, color=(0, 229, 255)):
    dim = tuple(int(c * 0.35) for c in color)
    frames = [draw_text(text, [c]) for c in (color, color, dim, color, dim, color, color, color)]
    return frames, 110


def fx_glitch(text):
    rnd = random.Random(text + "g")
    frames = []
    for k in range(6):
        base = draw_text(text, [(255, 255, 255)])
        if k % 2:
            red = draw_text(text, [(255, 40, 80)], [(rnd.randint(-3, -1), 0)] * len(text))
            blue = draw_text(text, [(40, 200, 255)], [(rnd.randint(1, 3), 0)] * len(text))
            out = Image.new("RGBA", base.size, (0, 0, 0, 0))
            out.alpha_composite(red)
            out.alpha_composite(blue)
            out.alpha_composite(base)
            frames.append(out)
        else:
            frames.append(base)
    return frames, 90


def fx_pop(text, color=(102, 187, 106)):
    frames = []
    for s in (0.4, 0.7, 1.1, 1.0, 1.0, 1.0, 1.0, 1.0):
        full = draw_text(text, [color])
        w, h = int(W_TEXT * s), int(H_TEXT * s)
        small = full.resize((max(1, w), max(1, h)), Image.NEAREST)
        out = Image.new("RGBA", (W_TEXT, H_TEXT), (0, 0, 0, 0))
        out.alpha_composite(small, ((W_TEXT - w) // 2, (H_TEXT - h) // 2)) if s <= 1 else out.alpha_composite(
            small.crop(((w - W_TEXT) // 2, (h - H_TEXT) // 2, (w - W_TEXT) // 2 + W_TEXT, (h - H_TEXT) // 2 + H_TEXT)))
        frames.append(out)
    return frames, 90


# (texto, efecto, etiquetas para categorías y búsqueda)
TEXTS = [
    ("GG", fx_rainbow, ["gg", "good game", "aplausos", "ganar"]),
    ("EZ", fx_shake, ["ez", "facil"]),
    ("LOL", fx_bounce, ["lol", "risa", "laugh"]),
    ("XD", fx_wave, ["xd", "risa", "laugh"]),
    ("JAJAJA", fx_wave, ["jajaja", "risa", "laugh"]),
    ("F", fx_neon, ["f", "rip", "llorar", "cry"]),
    ("RIP", fx_type, ["rip", "llorar", "cry", "muerte"]),
    ("OMG", fx_pop, ["omg", "sorpresa", "wow"]),
    ("WOW", fx_rainbow, ["wow", "sorpresa"]),
    ("NICE", fx_bounce, ["nice", "like", "bien"]),
    ("POG", fx_glitch, ["pog", "sorpresa", "hype"]),
    ("BRUH", fx_shake, ["bruh", "enojado", "facepalm"]),
    ("NOOO", fx_shake, ["nooo", "llorar", "no"]),
    ("SIUU", fx_pop, ["siuu", "baile", "celebrar"]),
    ("HOLA", fx_wave, ["hola", "saludo", "hi"]),
    ("ADIOS", fx_type, ["adios", "saludo", "bye"]),
    ("GRACIAS", fx_rainbow, ["gracias", "thanks"]),
    ("BUENAS", fx_wave, ["buenas", "hola", "saludo"]),
    ("OK", fx_pop, ["ok", "like", "bien"]),
    ("SI", fx_bounce, ["si", "yes", "like"]),
    ("NO", fx_shake, ["no", "nope"]),
    ("LETS GO", fx_rainbow, ["lets go", "vamos", "hype", "baile"]),
    ("VAMOS", fx_pop, ["vamos", "hype"]),
    ("LAG", fx_glitch, ["lag", "enojado"]),
    ("AFK", fx_neon, ["afk", "dormir"]),
    ("BRB", fx_type, ["brb", "ahora vuelvo"]),
    ("GL HF", fx_wave, ["gl hf", "suerte"]),
    ("WP", fx_bounce, ["wp", "well played", "aplausos"]),
    ("CLUTCH", fx_glitch, ["clutch", "hype"]),
    ("NOOB", fx_shake, ["noob", "risa"]),
    ("PRO", fx_neon, ["pro", "like"]),
    ("EPIC", fx_rainbow, ["epic", "epico", "hype"]),
    ("WIN", fx_pop, ["win", "ganar", "aplausos"]),
    ("OOF", fx_shake, ["oof", "llorar"]),
    ("HYPE", fx_glitch, ["hype", "baile"]),
    ("TQM", fx_bounce, ["tqm", "beso", "amor", "abrazo"]),
    ("LOVE", fx_neon, ["love", "amor", "beso", "abrazo"]),
    ("QUE?", fx_pop, ["que", "sorpresa", "confundido"]),
    ("???", fx_shake, ["que", "confundido", "sorpresa"]),
    ("!!!", fx_pop, ["sorpresa", "alerta"]),
    ("100", fx_rainbow, ["100", "like", "perfecto"]),
    ("TOP 1", fx_neon, ["top 1", "ganar", "aplausos"]),
    ("MVP", fx_glitch, ["mvp", "aplausos"]),
    ("BUENO", fx_type, ["bueno", "ok"]),
    ("YA", fx_bounce, ["ya", "ok"]),
    ("UWU", fx_wave, ["uwu", "beso", "tierno"]),
    ("<3", fx_neon, ["corazon", "amor", "beso"]),
    ("SOS", fx_neon, ["sos", "ayuda"]),
    ("STOP", fx_shake, ["stop", "enojado"]),
    ("HOLA!", fx_rainbow, ["hola", "saludo"]),
]


def text_gifs():
    out = []
    for text, fx, tags in TEXTS:
        gid = "t_" + "".join(c.lower() if c.isalnum() else "_" for c in text).strip("_") or "t_x"
        gid = gid.replace("__", "_")
        if text == "???":
            gid = "t_interrogacion"
        elif text == "!!!":
            gid = "t_exclamacion"
        elif text == "<3":
            gid = "t_corazon"
        elif text == "HOLA!":
            gid = "t_hola2"
        out.append((gid, text, (lambda fx=fx, text=text: fx(text)), tags))
    return out


# ---------------------------------------------------------------- escenas y efectos (64x64)

def canvas():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def emoji(name):
    return Image.open(os.path.join(EMOJI, name + ".png")).convert("RGBA")


def sprite(name, size):
    return emoji(name).resize((size, size), Image.NEAREST)


def fireworks(color):
    rnd = random.Random(str(color))
    bursts = [(rnd.randint(16, 48), rnd.randint(14, 36)) for _ in range(3)]
    frames = []
    for k in range(12):
        img = canvas()
        d = ImageDraw.Draw(img)
        for b, (cx, cy) in enumerate(bursts):
            t = (k - b * 3) % 12
            if t < 2:  # cohete subiendo
                d.rectangle([cx - 1, 60 - t * 12, cx, 62 - t * 12], fill=(255, 236, 179, 255))
            elif t < 9:
                r = (t - 1) * 3
                fade = max(60, 255 - (t - 2) * 30)
                for a in range(12):
                    ang = a / 12 * 2 * math.pi
                    x = cx + r * math.cos(ang)
                    y = cy + r * math.sin(ang) + (t - 2) * 0.6
                    d.rectangle([x - 1, y - 1, x, y], fill=color + (fade,))
        frames.append(img)
    return frames, 80


def confetti(seed):
    rnd = random.Random(seed)
    pieces = [(rnd.randint(0, 63), rnd.randint(-64, 0), rnd.choice(RAINBOW), rnd.randint(2, 4)) for _ in range(40)]
    frames = []
    for k in range(12):
        img = canvas()
        d = ImageDraw.Draw(img)
        for x, y0, c, speed in pieces:
            y = (y0 + k * speed * 2) % 80 - 8
            xx = x + int(2 * math.sin((k + x) / 2))
            if k % 2:
                d.rectangle([xx, y, xx + 2, y + 1], fill=c + (255,))
            else:
                d.rectangle([xx, y, xx + 1, y + 2], fill=c + (255,))
        frames.append(img)
    return frames, 70


def rain_of(name, size=12, count=10, up=False):
    rnd = random.Random(name)
    spr = sprite(name, size)
    drops = [(rnd.randint(0, SIZE - size), rnd.randint(0, SIZE + size), rnd.randint(1, 2)) for _ in range(count)]
    frames = []
    n = 12
    for k in range(n):
        img = canvas()
        for x, y0, speed in drops:
            span = SIZE + size
            y = (y0 + k * span * speed // n) % span - size
            if up:
                y = SIZE - y - size
            img.alpha_composite(spr, (x, y))
        frames.append(img)
    return frames, 80


def snow():
    rnd = random.Random("snow")
    flakes = [(rnd.randint(0, 63), rnd.randint(0, 63), rnd.randint(1, 2)) for _ in range(45)]
    frames = []
    for k in range(16):
        img = canvas()
        d = ImageDraw.Draw(img)
        for x, y0, s in flakes:
            y = (y0 + k * s * 2) % 64
            xx = (x + int(2 * math.sin((k + y0) / 3))) % 64
            d.rectangle([xx, y, xx + s - 1, y + s - 1], fill=(255, 255, 255, 230))
        frames.append(img)
    return frames, 90


def rain():
    rnd = random.Random("rain")
    drops = [(rnd.randint(0, 63), rnd.randint(0, 63)) for _ in range(40)]
    frames = []
    for k in range(8):
        img = canvas()
        d = ImageDraw.Draw(img)
        for x, y0 in drops:
            y = (y0 + k * 8) % 64
            d.line([(x, y), (x - 1, y + 4)], fill=(100, 181, 246, 230))
        frames.append(img)
    return frames, 60


def spinner(color):
    frames = []
    for k in range(8):
        img = canvas()
        d = ImageDraw.Draw(img)
        for a in range(8):
            ang = a / 8 * 2 * math.pi
            x = 32 + 18 * math.cos(ang)
            y = 32 + 18 * math.sin(ang)
            alpha = 255 if (a - k) % 8 == 0 else max(40, 255 - ((k - a) % 8) * 30)
            d.ellipse([x - 4, y - 4, x + 4, y + 4], fill=color + (alpha,))
        frames.append(img)
    return frames, 90


def loading_bar():
    frames = []
    for k in range(11):
        img = canvas()
        d = ImageDraw.Draw(img)
        d.rounded_rectangle([6, 26, 57, 38], radius=4, outline=(230, 232, 238, 255), width=2)
        if k:
            d.rounded_rectangle([9, 29, 9 + int(45 * k / 10), 35], radius=2, fill=(102, 187, 106, 255))
        frames.append(img)
    frames += [frames[-1]] * 3
    return frames, 110


def typing():
    frames = []
    for k in range(6):
        img = canvas()
        d = ImageDraw.Draw(img)
        d.rounded_rectangle([6, 18, 57, 46], radius=12, fill=(42, 45, 54, 255))
        for i in range(3):
            up = 3 if (k % 3) == i else 0
            d.ellipse([16 + i * 12, 29 - up, 23 + i * 12, 36 - up], fill=(230, 232, 238, 255))
        frames.append(img)
    return frames, 160


def siren():
    frames = []
    for k in range(6):
        img = canvas()
        d = ImageDraw.Draw(img)
        c = (229, 57, 53) if k % 2 == 0 else (30, 136, 229)
        d.pieslice([4, 4, 60, 60], 200 + k * 60, 260 + k * 60, fill=c + (110,))
        d.rounded_rectangle([20, 22, 44, 50], radius=6, fill=c + (255,))
        d.rectangle([16, 50, 48, 56], fill=(84, 110, 122, 255))
        d.rectangle([26, 28, 30, 40], fill=(255, 255, 255, 180))
        frames.append(img)
    return frames, 120


def lightning():
    bolt = [(36, 2), (22, 30), (32, 30), (24, 62), (46, 24), (34, 24), (42, 2)]
    frames = []
    for k in range(8):
        img = canvas()
        d = ImageDraw.Draw(img)
        if k in (1, 2, 5):
            d.rectangle([0, 0, 63, 63], fill=(255, 255, 255, 60))
            d.polygon(bolt, fill=(255, 235, 59, 255), outline=(245, 127, 23, 255))
        elif k in (3, 6):
            d.polygon(bolt, fill=(255, 235, 59, 140))
        frames.append(img)
    return frames, 90


def explosion(core, ring):
    frames = []
    for r in (4, 10, 16, 22, 27, 30, 31):
        img = canvas()
        d = ImageDraw.Draw(img)
        d.ellipse([32 - r, 32 - r, 32 + r, 32 + r], fill=ring + (220,))
        d.ellipse([32 - r * 0.6, 32 - r * 0.6, 32 + r * 0.6, 32 + r * 0.6], fill=core + (255,))
        frames.append(img)
    for a in (160, 90, 30):
        img = canvas()
        d = ImageDraw.Draw(img)
        d.ellipse([4, 4, 60, 60], fill=(120, 120, 120, a))
        frames.append(img)
    frames.append(canvas())
    return frames, 70


def bubbles():
    rnd = random.Random("bubbles")
    bs = [(rnd.randint(4, 58), rnd.randint(0, 63), rnd.randint(2, 5)) for _ in range(14)]
    frames = []
    for k in range(12):
        img = canvas()
        d = ImageDraw.Draw(img)
        for x, y0, r in bs:
            y = (y0 - k * 5) % 72 - 4
            d.ellipse([x - r, y - r, x + r, y + r], outline=(129, 212, 250, 255))
            d.point((x - r // 2, y - r // 2), fill=(255, 255, 255, 255))
        frames.append(img)
    return frames, 80


def spin_sprite(name):
    spr = emoji(name)
    frames = []
    for k in range(10):
        sx = abs(math.cos(k / 10 * math.pi)) * 3.4
        w = max(2, int(16 * sx))
        img = canvas()
        s = (spr if k < 5 else spr.transpose(Image.FLIP_LEFT_RIGHT)).resize((w, 54), Image.NEAREST)
        img.alpha_composite(s, ((SIZE - w) // 2, 5))
        frames.append(img)
    return frames, 70


def shine_sprite(name):
    spr = sprite(name, 48)
    frames = []
    for k in range(10):
        img = canvas()
        img.alpha_composite(spr, (8, 8))
        mask = img.getchannel("A")
        glare = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
        d = ImageDraw.Draw(glare)
        x = -24 + k * 12
        d.polygon([(x, 64), (x + 10, 64), (x + 34, 0), (x + 24, 0)], fill=(255, 255, 255, 150))
        glare.putalpha(Image.composite(glare.getchannel("A"), Image.new("L", (SIZE, SIZE), 0), mask))
        img.alpha_composite(glare)
        frames.append(img)
    return frames, 70


def stonks(up):
    pts = [(4, 52), (14, 44), (22, 48), (32, 34), (40, 38), (52, 16)]
    if not up:
        pts = [(x, 68 - y) for x, y in pts]
    color = (67, 160, 71) if up else (229, 57, 53)
    frames = []
    for k in range(1, len(pts) + 3):
        img = canvas()
        d = ImageDraw.Draw(img)
        d.line([(4, 58), (60, 58)], fill=(120, 120, 120, 255))
        d.line([(4, 58), (4, 6)], fill=(120, 120, 120, 255))
        shown = pts[:min(k, len(pts))]
        if len(shown) > 1:
            d.line(shown, fill=color + (255,), width=3)
        if k >= len(pts):
            x, y = pts[-1]
            d.polygon([(x - 6, y + (6 if up else -6)), (x + 6, y + (2 if up else -2)), (x + 4, y + (-6 if up else 6))],
                      fill=color + (255,))
        frames.append(img)
    return frames, 130


def heart_break():
    h = sprite("corazon", 44)
    frames = []
    for gap in (0, 0, 0, 2, 5, 8, 11, 11):
        img = canvas()
        left = h.crop((0, 0, 22, 44))
        right = h.crop((22, 0, 44, 44))
        img.alpha_composite(left, (10 - gap, 10 + gap // 2))
        img.alpha_composite(right, (32 + gap, 10 + gap // 2))
        frames.append(img)
    return frames, 110


def campfire():
    rnd = random.Random("fire")
    frames = []
    for k in range(8):
        img = canvas()
        d = ImageDraw.Draw(img)
        d.rectangle([12, 50, 52, 55], fill=(109, 76, 65, 255))
        d.rectangle([18, 46, 46, 50], fill=(141, 110, 99, 255))
        for i in range(14):
            x = rnd.randint(20, 44)
            hgt = rnd.randint(10, 30)
            c = rnd.choice([(255, 87, 34), (255, 152, 0), (255, 213, 79)])
            d.ellipse([x - 4, 46 - hgt, x + 4, 48], fill=c + (230,))
        frames.append(img)
    return frames, 90


SCENES = [
    ("s_fuegos_rojos", "Fuegos artificiales rojos", lambda: fireworks((239, 83, 80)), ["fuegos", "fiesta", "celebrar", "baile"]),
    ("s_fuegos_dorados", "Fuegos artificiales dorados", lambda: fireworks((255, 213, 79)), ["fuegos", "fiesta", "celebrar", "baile"]),
    ("s_fuegos_azules", "Fuegos artificiales azules", lambda: fireworks((79, 195, 247)), ["fuegos", "fiesta", "celebrar"]),
    ("s_fuegos_verdes", "Fuegos artificiales verdes", lambda: fireworks((129, 199, 132)), ["fuegos", "fiesta", "celebrar"]),
    ("s_confeti", "Confeti", lambda: confetti("a"), ["confeti", "fiesta", "baile", "celebrar", "aplausos"]),
    ("s_confeti2", "Más confeti", lambda: confetti("b"), ["confeti", "fiesta", "celebrar"]),
    ("s_lluvia_corazones", "Lluvia de corazones", lambda: rain_of("corazon"), ["corazones", "amor", "beso", "abrazo"]),
    ("s_corazones_suben", "Corazones flotando", lambda: rain_of("corazon_morado", up=True), ["corazones", "amor", "abrazo", "beso"]),
    ("s_lluvia_dinero", "Lluvia de dinero", lambda: rain_of("moneda"), ["dinero", "money", "rico"]),
    ("s_lluvia_billetes", "Lluvia de bolsas de dinero", lambda: rain_of("bolsa_dinero", 14, 8), ["dinero", "money", "rico"]),
    ("s_lluvia_diamantes", "Lluvia de diamantes", lambda: rain_of("diamante"), ["diamantes", "minecraft", "rico"]),
    ("s_lluvia_esmeraldas", "Lluvia de esmeraldas", lambda: rain_of("esmeralda"), ["esmeraldas", "minecraft"]),
    ("s_lluvia_estrellas", "Lluvia de estrellas", lambda: rain_of("estrella"), ["estrellas", "like", "genial"]),
    ("s_lluvia_risa", "Lluvia de risas", lambda: rain_of("llorar_risa", 14, 9), ["risa", "jaja", "laugh"]),
    ("s_lluvia_fuego", "Lluvia de fuego", lambda: rain_of("fuego"), ["fuego", "hype"]),
    ("s_lluvia_pulgares", "Lluvia de likes", lambda: rain_of("pulgar_arriba", 14, 8), ["like", "bien", "aplausos"]),
    ("s_lluvia_tnt", "Lluvia de TNT", lambda: rain_of("tnt", 14, 8), ["tnt", "minecraft", "explosion"]),
    ("s_lluvia_pizza", "Lluvia de pizza", lambda: rain_of("pizza", 14, 8), ["pizza", "comida", "hambre"]),
    ("s_lluvia_lagrimas", "Lluvia de llanto", lambda: rain_of("llanto", 14, 8), ["llorar", "cry", "triste"]),
    ("s_lluvia_enojo", "Lluvia de enojo", lambda: rain_of("furioso", 14, 8), ["enojado", "angry"]),
    ("s_lluvia_sorpresa", "Lluvia de sorpresa", lambda: rain_of("sorpresa", 14, 8), ["sorpresa", "wow", "omg"]),
    ("s_lluvia_besos", "Lluvia de besos", lambda: rain_of("beso", 14, 8), ["beso", "kiss", "amor"]),
    ("s_saludos_suben", "Saludos flotando", lambda: rain_of("saludo", 14, 8, up=True), ["hola", "saludo", "adios"]),
    ("s_aplausos_suben", "Aplausos flotando", lambda: rain_of("aplausos", 14, 8, up=True), ["aplausos", "bravo"]),
    ("s_xp", "Orbes de experiencia", lambda: rain_of("orbe_xp", 10, 12, up=True), ["xp", "experiencia", "minecraft"]),
    ("s_nieve", "Nieve", snow, ["nieve", "frio", "invierno"]),
    ("s_lluvia", "Lluvia", rain, ["lluvia", "triste", "llorar"]),
    ("s_carga_azul", "Cargando", lambda: spinner((79, 195, 247)), ["cargando", "loading", "espera", "lag"]),
    ("s_carga_verde", "Cargando verde", lambda: spinner((129, 199, 132)), ["cargando", "loading", "espera"]),
    ("s_carga_rosa", "Cargando rosa", lambda: spinner((240, 98, 146)), ["cargando", "loading", "espera"]),
    ("s_barra_carga", "Barra de carga", loading_bar, ["cargando", "loading", "espera"]),
    ("s_escribiendo", "Escribiendo...", typing, ["escribiendo", "typing", "espera"]),
    ("s_sirena", "Sirena", siren, ["sirena", "alerta", "policia", "sorpresa"]),
    ("s_rayo", "Rayo", lightning, ["rayo", "tormenta", "sorpresa", "enojado"]),
    ("s_explosion", "Explosión", lambda: explosion((255, 235, 59), (255, 112, 67)), ["explosion", "boom", "sorpresa"]),
    ("s_explosion_azul", "Explosión azul", lambda: explosion((225, 245, 254), (79, 195, 247)), ["explosion", "boom", "magia"]),
    ("s_burbujas", "Burbujas", bubbles, ["burbujas", "agua", "relax"]),
    ("s_moneda_gira", "Moneda girando", lambda: spin_sprite("moneda"), ["moneda", "dinero", "coin"]),
    ("s_diamante_gira", "Diamante girando", lambda: spin_sprite("diamante"), ["diamante", "minecraft"]),
    ("s_esmeralda_gira", "Esmeralda girando", lambda: spin_sprite("esmeralda"), ["esmeralda", "minecraft"]),
    ("s_totem_gira", "Tótem girando", lambda: spin_sprite("totem"), ["totem", "minecraft", "salvado"]),
    ("s_trofeo_brillo", "Trofeo brillando", lambda: shine_sprite("trofeo"), ["trofeo", "ganar", "aplausos", "win"]),
    ("s_corona_brillo", "Corona brillando", lambda: shine_sprite("corona"), ["corona", "rey", "ganar"]),
    ("s_diamante_brillo", "Diamante brillando", lambda: shine_sprite("diamante"), ["diamante", "minecraft", "rico"]),
    ("s_espada_brillo", "Espada brillando", lambda: shine_sprite("espada"), ["espada", "minecraft", "pvp"]),
    ("s_stonks", "Stonks", lambda: stonks(True), ["stonks", "subir", "dinero", "ganar"]),
    ("s_not_stonks", "Not stonks", lambda: stonks(False), ["not stonks", "bajar", "perder", "llorar"]),
    ("s_corazon_roto", "Corazón roto", heart_break, ["corazon roto", "triste", "llorar"]),
    ("s_fogata", "Fogata", campfire, ["fogata", "fuego", "relax"]),
    ("s_lluvia_creepers", "Lluvia de creepers", lambda: rain_of("creeper", 14, 8), ["creeper", "minecraft"]),
]


def all_extra():
    """(id, nombre, función que da (frames, duración), etiquetas)."""
    out = [(gid, name, fn, tags + ["texto", "destacado"]) for gid, name, fn, tags in text_gifs()]
    out += [(gid, name, fn, tags + ["destacado"]) for gid, name, fn, tags in SCENES]
    return out
