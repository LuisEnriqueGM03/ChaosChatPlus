"""
GIFs de reacciones estilo anime (chibi) dibujados aquí: ojos grandes con brillo, rubor, pelo de colores.
Arte propio del mod (sin clips de terceros). Ids «a_*» → categoría «anime». La importa gen_gifs.py.
"""
import math

from PIL import Image, ImageDraw

S = 96          # tamaño final
K = 3           # se dibuja a 3x y se reduce (bordes suaves)
SKIN = (255, 228, 206, 255)
SKIN_SHADE = (240, 200, 178, 255)
LINE = (60, 40, 50, 255)
WHITE = (255, 255, 255, 255)
BLUSH = (255, 140, 160, 150)
TEAR = (120, 200, 255, 230)

HAIRS = {
    "rosa": ((246, 143, 186), (222, 100, 150)),
    "azul": ((110, 170, 240), (70, 120, 205)),
    "rubio": ((250, 214, 110), (225, 175, 60)),
    "negro": ((70, 66, 90), (40, 36, 58)),
    "rojo": ((235, 90, 80), (190, 55, 55)),
    "plata": ((215, 220, 235), (165, 172, 195)),
}
EYES = {
    "rosa": (200, 70, 140), "azul": (40, 110, 210), "rubio": (60, 150, 90),
    "negro": (150, 60, 200), "rojo": (200, 60, 40), "plata": (90, 110, 200),
}


def c(v):
    return int(v * K)


def ellipse(d, cx, cy, rx, ry, fill, outline=None, width=1):
    d.ellipse([c(cx - rx), c(cy - ry), c(cx + rx), c(cy + ry)], fill=fill, outline=outline,
              width=c(width) if outline else 0)


def arc(d, cx, cy, rx, ry, start, end, width=1.6, fill=LINE):
    d.arc([c(cx - rx), c(cy - ry), c(cx + rx), c(cy + ry)], start, end, fill=fill, width=c(width))


def line(d, pts, width=1.6, fill=LINE):
    d.line([(c(x), c(y)) for x, y in pts], fill=fill, width=c(width), joint="curve")


def poly(d, pts, fill):
    d.polygon([(c(x), c(y)) for x, y in pts], fill=fill)


def eye(d, x, y, kind, iris, look=0.0):
    """kind: open, happy (^), closed (-), wink, heart, star, shock (puntito), angry, sad, spiral."""
    if kind == "open" or kind == "angry" or kind == "sad":
        ellipse(d, x, y, 6.2, 7.6, WHITE, LINE, 1.4)
        ellipse(d, x + look, y + 0.8, 4.6, 6.0, iris + (255,))
        ellipse(d, x + look, y + 1.6, 2.6, 3.4, tuple(max(0, v - 70) for v in iris) + (255,))
        ellipse(d, x + look - 1.8, y - 2.4, 1.8, 1.8, WHITE)
        ellipse(d, x + look + 1.8, y + 3.0, 0.9, 0.9, WHITE)
        if kind == "angry":
            line(d, [(x - 7, y - 11) if x < 48 else (x + 7, y - 11), (x + 5, y - 7) if x < 48 else (x - 5, y - 7)], 2.2)
        if kind == "sad":
            line(d, [(x - 6, y - 8) if x < 48 else (x + 6, y - 8), (x + 5, y - 11) if x < 48 else (x - 5, y - 11)], 1.8)
    elif kind == "happy":
        arc(d, x, y + 3, 6, 6, 200, 340, 2.0)
    elif kind == "closed":
        arc(d, x, y - 2, 6, 5, 20, 160, 2.0)
    elif kind == "line":
        line(d, [(x - 5, y), (x + 5, y)], 2.0)
    elif kind == "heart":
        ellipse(d, x - 2.6, y - 1.5, 3.4, 3.4, (235, 60, 100, 255))
        ellipse(d, x + 2.6, y - 1.5, 3.4, 3.4, (235, 60, 100, 255))
        poly(d, [(x - 5.8, y - 0.5), (x + 5.8, y - 0.5), (x, y + 6.5)], (235, 60, 100, 255))
        ellipse(d, x - 3.2, y - 2.6, 1.1, 1.1, WHITE)
    elif kind == "star":
        pts = []
        for i in range(10):
            r = 6.5 if i % 2 == 0 else 2.8
            a = -math.pi / 2 + i * math.pi / 5
            pts.append((x + r * math.cos(a), y + r * math.sin(a)))
        poly(d, pts, (255, 210, 60, 255))
    elif kind == "shock":
        ellipse(d, x, y, 6.2, 7.6, WHITE, LINE, 1.4)
        ellipse(d, x, y, 1.8, 1.8, LINE)
    elif kind == "spiral":
        ellipse(d, x, y, 6.2, 6.2, WHITE, LINE, 1.2)
        pts = [(x + (i / 20) * 5.5 * math.cos(i * 0.9), y + (i / 20) * 5.5 * math.sin(i * 0.9)) for i in range(21)]
        line(d, pts, 1.2)
    elif kind == "x":
        line(d, [(x - 4, y - 4), (x + 4, y + 4)], 2.0)
        line(d, [(x - 4, y + 4), (x + 4, y - 4)], 2.0)
    elif kind == "smug":
        ellipse(d, x, y + 1, 6.0, 5.0, WHITE, LINE, 1.4)
        ellipse(d, x + 1, y + 2, 3.8, 3.6, (120, 80, 160, 255))
        line(d, [(x - 7, y - 3), (x + 7, y - 3)], 2.4)


def mouth(d, kind, open_amt=1.0, y=63):
    x = 48
    if kind == "smile":
        arc(d, x, y - 3, 5, 4, 20, 160, 1.6)
    elif kind == "grin":
        h = 3 + 4 * open_amt
        d.chord([c(x - 7), c(y - h), c(x + 7), c(y + h)], 0, 180, fill=(170, 50, 70, 255), outline=LINE, width=c(1.3))
        ellipse(d, x, y + h * 0.55, 3.5, h * 0.35, (255, 130, 140, 255))
    elif kind == "o":
        ellipse(d, x, y, 2.5 + 2 * open_amt, 3 + 3 * open_amt, (150, 40, 60, 255), LINE, 1.2)
    elif kind == "flat":
        line(d, [(x - 4, y), (x + 4, y)], 1.6)
    elif kind == "frown":
        arc(d, x, y + 4, 5, 4, 200, 340, 1.6)
    elif kind == "cat":
        arc(d, x - 3, y - 1, 3, 3, 20, 160, 1.5)
        arc(d, x + 3, y - 1, 3, 3, 20, 160, 1.5)
    elif kind == "wail":
        h = 4 + 3 * open_amt
        poly(d, [(x - 8, y + h), (x + 8, y + h), (x + 4, y - h * 0.5), (x - 4, y - h * 0.5)], (150, 40, 60, 255))
    elif kind == "kiss":
        arc(d, x + 1, y - 2, 2.5, 2.2, 270, 90, 1.5)
        arc(d, x + 1, y + 2, 2.5, 2.2, 270, 90, 1.5)
    elif kind == "smirk":
        line(d, [(x - 4, y + 1), (x + 2, y), (x + 6, y - 3)], 1.6)
    elif kind == "teeth":
        d.rectangle([c(x - 7), c(y - 3), c(x + 7), c(y + 3)], fill=WHITE, outline=LINE, width=c(1.2))
        line(d, [(x - 7, y), (x + 7, y)], 0.9)


def head(d, hair, dy=0.0, tilt=0.0, ears=False):
    h1, h2 = HAIRS[hair]
    cy = 48 + dy
    # pelo de atrás
    ellipse(d, 48 + tilt, cy - 2, 33, 33, h2 + (255,))
    if hair in ("rosa", "rubio"):  # coletas
        ellipse(d, 13 + tilt, cy + 8, 9, 15, h2 + (255,))
        ellipse(d, 83 + tilt, cy + 8, 9, 15, h2 + (255,))
    # cara
    ellipse(d, 48 + tilt, cy + 4, 27, 25, SKIN, LINE, 1.4)
    # flequillo
    for i, (x0, w) in enumerate([(22, 14), (34, 15), (48, 15), (62, 15), (74, 14)]):
        poly(d, [(x0 - w / 2 + tilt, cy - 20), (x0 + w / 2 + tilt, cy - 20), (x0 + tilt + (i - 2) * 1.5, cy - 4 - (i % 2) * 4)],
             h1 + (255,))
    ellipse(d, 48 + tilt, cy - 22, 30, 13, h1 + (255,))
    ellipse(d, 38 + tilt, cy - 26, 7, 2.5, tuple(min(255, v + 45) for v in h1) + (255,))  # brillo del pelo
    if ears:  # orejitas de gato
        poly(d, [(20 + tilt, cy - 24), (26 + tilt, cy - 44), (38 + tilt, cy - 30)], h1 + (255,))
        poly(d, [(76 + tilt, cy - 24), (70 + tilt, cy - 44), (58 + tilt, cy - 30)], h1 + (255,))
        poly(d, [(25 + tilt, cy - 27), (27 + tilt, cy - 38), (33 + tilt, cy - 30)], (255, 180, 200, 255))
        poly(d, [(71 + tilt, cy - 27), (69 + tilt, cy - 38), (63 + tilt, cy - 30)], (255, 180, 200, 255))
    return cy


def blush(d, cy, tilt=0.0, strong=False):
    a = 200 if strong else 140
    ellipse(d, 30 + tilt, cy + 12, 5.5, 3, (255, 130, 150, a))
    ellipse(d, 66 + tilt, cy + 12, 5.5, 3, (255, 130, 150, a))
    if strong:
        for ox in (27, 31, 63, 67):
            line(d, [(ox + tilt, cy + 10), (ox - 1.5 + tilt, cy + 14)], 0.9, (220, 80, 110, 255))


def frame(draw_fn):
    img = Image.new("RGBA", (S * K, S * K), (0, 0, 0, 0))
    d = ImageDraw.Draw(img, "RGBA")
    draw_fn(d)
    return img.resize((S, S), Image.LANCZOS)


def face_gif(hair, eyes_seq, mouth_seq, n=8, bob=0.0, sway=0.0, blush_on=False, strong_blush=False, extra=None,
             ears=False, look=0.0):
    iris = EYES[hair]
    frames = []
    for i in range(n):
        t = i / n
        dy = bob * math.sin(t * 2 * math.pi)
        tilt = sway * math.sin(t * 2 * math.pi)

        def draw(d, i=i, t=t, dy=dy, tilt=tilt):
            cy = head(d, hair, dy, tilt, ears)
            ek = eyes_seq[i % len(eyes_seq)]
            le, re = ek if isinstance(ek, tuple) else (ek, ek)
            eye(d, 37 + tilt, cy + 4, le, iris, look * math.sin(t * 2 * math.pi))
            eye(d, 59 + tilt, cy + 4, re, iris, look * math.sin(t * 2 * math.pi))
            if blush_on:
                blush(d, cy, tilt, strong_blush)
            mk = mouth_seq[i % len(mouth_seq)]
            mk, amt = mk if isinstance(mk, tuple) else (mk, 1.0)
            d2 = d
            # boca relativa a la cabeza
            mouth(d2, mk, amt, y=cy + 17)
            if extra:
                extra(d, i, n, cy, tilt)
        frames.append(frame(draw))
    return frames


# ---------- extras ----------
def tears(d, i, n, cy, tilt):
    for side in (37, 59):
        k = (i % 4) / 4
        ellipse(d, side + tilt + (-4 if side < 48 else 4), cy + 12 + k * 18, 2.2, 3.2, TEAR)
        line(d, [(side + tilt - 3, cy + 10), (side + tilt - 3 if side < 48 else side + tilt + 3, cy + 14)], 1.2, TEAR)


def stream_tears(d, i, n, cy, tilt):
    for side in (34, 62):
        w = 3 + (i % 2)
        d.rectangle([c(side + tilt - w / 2), c(cy + 10), c(side + tilt + w / 2), c(96)], fill=TEAR)


def anger_mark(d, i, n, cy, tilt):
    s = 1.0 + 0.25 * (i % 2)
    x, y = 74 + tilt, cy - 26
    for a in range(4):
        ang = a * math.pi / 2 + math.pi / 4
        line(d, [(x + 2 * s * math.cos(ang), y + 2 * s * math.sin(ang)), (x + 6 * s * math.cos(ang), y + 6 * s * math.sin(ang))],
             2.2, (230, 40, 40, 255))


def steam(d, i, n, cy, tilt):
    anger_mark(d, i, n, cy, tilt)
    for k, x in enumerate((14, 82)):
        y = cy - 10 - ((i * 4 + k * 8) % 16)
        ellipse(d, x, y, 5, 4, (230, 230, 235, 200))


def zzz(d, i, n, cy, tilt):
    for k in range(3):
        p = ((i / n) + k / 3) % 1
        x, y, s = 70 + p * 18, cy - 14 - p * 30, 3 + p * 4
        line(d, [(x - s, y - s), (x + s, y - s), (x - s, y + s), (x + s, y + s)], 1.4, (110, 140, 220, 255))
    if i % 4 < 2:
        ellipse(d, 58 + tilt, cy + 22, 3, 3.5, (160, 210, 255, 200))  # burbuja de moco


def hearts(d, i, n, cy, tilt):
    for k in range(3):
        p = ((i / n) + k / 3) % 1
        x, y = 16 + k * 32 + 4 * math.sin(p * 6), cy - 20 - p * 30
        r = 2.5 + p * 2
        a = int(255 * (1 - p))
        ellipse(d, x - r * 0.7, y, r, r, (240, 70, 110, a))
        ellipse(d, x + r * 0.7, y, r, r, (240, 70, 110, a))
        poly(d, [(x - r * 1.6, y + 0.5), (x + r * 1.6, y + 0.5), (x, y + r * 2.2)], (240, 70, 110, a))


def sparkles(d, i, n, cy, tilt):
    for k, (x, y) in enumerate([(12, 20), (84, 26), (14, 70), (82, 72)]):
        s = 2 + 3 * abs(math.sin((i / n + k / 4) * math.pi * 2))
        line(d, [(x - s, y), (x + s, y)], 1.3, (255, 220, 80, 255))
        line(d, [(x, y - s), (x, y + s)], 1.3, (255, 220, 80, 255))


def sweat(d, i, n, cy, tilt):
    y = cy - 10 + (i % 4) * 2
    ellipse(d, 76 + tilt, y, 3, 4.5, (140, 210, 255, 230))
    poly(d, [(73 + tilt, y - 1), (79 + tilt, y - 1), (76 + tilt, y - 8)], (140, 210, 255, 230))


def shock_lines(d, i, n, cy, tilt):
    if i % 2 == 0:
        for x0, x1 in ((10, 18), (78, 86)):
            for k in range(3):
                line(d, [(x0, cy - 20 + k * 6), (x1, cy - 22 + k * 6)], 1.6)


def wave_hand(d, i, n, cy, tilt):
    a = math.sin(i / n * 2 * math.pi) * 0.5
    x, y = 82 + 6 * math.sin(a), 70 - 6 * math.cos(a)
    line(d, [(70, 92), (x, y)], 5, SKIN_SHADE)
    ellipse(d, x, y - 4, 6, 7, SKIN, LINE, 1.2)
    for k in range(3):
        line(d, [(x - 4 + k * 4, y - 9), (x - 5 + k * 4 + 2 * a, y - 15)], 2.6, SKIN)


def thumbs(d, i, n, cy, tilt):
    y = 78 - (2 if i % 2 else 0)
    ellipse(d, 80, y, 8, 7, SKIN, LINE, 1.2)
    d.rounded_rectangle([c(76), c(y - 17), c(83), c(y - 4)], radius=c(3), fill=SKIN, outline=LINE, width=c(1.2))
    sparkles(d, i, n, cy, tilt)


def clap_hands(d, i, n, cy, tilt):
    gap = 2 + 10 * abs(math.sin(i / n * 2 * math.pi))
    for side in (-1, 1):
        ellipse(d, 48 + side * gap, 84, 6, 9, SKIN, LINE, 1.2)
    if gap < 4:
        for k in range(5):
            a = -math.pi / 2 + (k - 2) * 0.5
            line(d, [(48 + 14 * math.cos(a), 74 + 14 * math.sin(a)), (48 + 20 * math.cos(a), 74 + 20 * math.sin(a))],
                 1.4, (255, 200, 60, 255))


def music(d, i, n, cy, tilt):
    for k in range(2):
        p = ((i / n) + k / 2) % 1
        x, y = (10 if k == 0 else 80) + 3 * math.sin(p * 9), 60 - p * 40
        ellipse(d, x, y, 3, 2.4, (120, 90, 220, 255))
        line(d, [(x + 2.5, y), (x + 2.5, y - 9), (x + 6, y - 7)], 1.3, (120, 90, 220, 255))


def ha_text(d, i, n, cy, tilt):
    col = (255, 120, 60, 255)
    off = 2 if i % 2 else 0
    for x, y in ((8, 14 + off), (76, 12 - off)):
        # «HA» en trazos
        line(d, [(x, y), (x, y + 8)], 1.6, col)
        line(d, [(x + 4, y), (x + 4, y + 8)], 1.6, col)
        line(d, [(x, y + 4), (x + 4, y + 4)], 1.6, col)
        line(d, [(x + 6, y + 8), (x + 8, y), (x + 10, y + 8)], 1.6, col)
        line(d, [(x + 7, y + 5), (x + 9, y + 5)], 1.4, col)


def blank(d, *a):
    pass


def combine(*fns):
    def f(d, i, n, cy, tilt):
        for fn in fns:
            fn(d, i, n, cy, tilt)
    return f


# ---------- reacciones ----------
REACTIONS = [
    # id, nombre, tags, constructor(hair) -> frames, duración
    ("risa", "Risa", ["risa", "laugh", "jaja", "lol"],
     lambda h: face_gif(h, ["happy"], [("grin", 1), ("grin", 0.4)], n=6, bob=2.5, blush_on=True, extra=ha_text), 90),
    ("llorar", "Llorando", ["llorar", "cry", "triste", "sad"],
     lambda h: face_gif(h, ["closed"], [("wail", 1), ("wail", 0.6)], n=6, sway=1.5, extra=stream_tears), 110),
    ("lagrimita", "Lagrimita", ["llorar", "triste", "sad", "cry"],
     lambda h: face_gif(h, ["sad"], ["frown"], n=8, bob=0.8, extra=tears), 140),
    ("enojo", "Enojada", ["enojo", "angry", "rabia"],
     lambda h: face_gif(h, ["angry"], ["teeth"], n=6, sway=1.8, extra=steam), 90),
    ("sonrojo", "Sonrojada", ["sonrojo", "blush", "verguenza", "uwu"],
     lambda h: face_gif(h, ["happy", "happy", "closed"], ["cat"], n=8, sway=2.0, blush_on=True, strong_blush=True), 140),
    ("amor", "Enamorada", ["amor", "love", "corazon", "beso"],
     lambda h: face_gif(h, ["heart"], ["smile"], n=8, bob=1.5, blush_on=True, extra=hearts), 120),
    ("beso", "Beso", ["beso", "kiss", "amor", "mua"],
     lambda h: face_gif(h, [("open", "happy"), ("closed", "closed")], ["kiss"], n=6, blush_on=True, extra=hearts), 160),
    ("guino", "Guiño", ["guiño", "wink", "ok"],
     lambda h: face_gif(h, ["open", "open", "open", ("open", "happy"), ("open", "happy")], ["smile", "smile", "smile", "grin", "grin"],
                        n=10, sway=1.0, blush_on=True, extra=sparkles), 120),
    ("dormir", "Durmiendo", ["dormir", "sleep", "sueño", "zzz"],
     lambda h: face_gif(h, ["closed"], [("o", 0.3), ("o", 0.6)], n=8, bob=1.2, extra=zzz), 200),
    ("sorpresa", "Sorpresa", ["sorpresa", "shock", "wow", "omg"],
     lambda h: face_gif(h, ["shock"], [("o", 1), ("o", 0.8)], n=4, bob=1.5, extra=shock_lines), 90),
    ("brillos", "Ojos brillantes", ["wow", "emocion", "estrella", "feliz"],
     lambda h: face_gif(h, ["star"], [("grin", 0.8)], n=8, bob=2.0, blush_on=True, extra=sparkles), 110),
    ("saludo", "Saludo", ["saludo", "hola", "wave", "hi", "adios"],
     lambda h: face_gif(h, ["happy"], ["grin"], n=8, sway=1.2, blush_on=True, extra=wave_hand), 100),
    ("pulgar", "Pulgar arriba", ["pulgar", "ok", "bien", "thumbsup"],
     lambda h: face_gif(h, [("open", "happy")], ["grin"], n=6, bob=1.0, extra=thumbs), 130),
    ("aplausos", "Aplausos", ["aplausos", "clap", "bravo"],
     lambda h: face_gif(h, ["happy"], [("grin", 0.8)], n=6, bob=1.0, blush_on=True, extra=clap_hands), 90),
    ("baile", "Bailando", ["baile", "dance", "fiesta"],
     lambda h: face_gif(h, ["happy", "happy", ("happy", "open"), "happy"], ["grin", "cat"], n=8, bob=3.0, sway=5.0, extra=music), 100),
    ("nervios", "Nerviosa", ["nervios", "sudor", "awkward", "uh"],
     lambda h: face_gif(h, ["open"], ["flat"], n=8, sway=0.8, extra=sweat, look=2.0), 120),
    ("mareada", "Mareada", ["mareo", "confusa", "dizzy"],
     lambda h: face_gif(h, ["spiral"], [("o", 0.5)], n=8, sway=3.0, blush_on=True), 100),
    ("presumida", "Presumida", ["smug", "presumida", "jeje"],
     lambda h: face_gif(h, ["smug"], ["smirk"], n=8, sway=1.0, extra=sparkles), 140),
    ("muerta", "Ko", ["muerto", "ko", "dead", "rip"],
     lambda h: face_gif(h, ["x"], [("o", 0.4)], n=6, sway=1.2), 160),
    ("neko", "Neko feliz", ["neko", "gato", "nya", "feliz"],
     lambda h: face_gif(h, ["happy"], ["cat"], n=8, bob=1.5, sway=1.5, blush_on=True, ears=True, extra=music), 110),
    ("neko_enojo", "Neko enojada", ["neko", "gato", "enojo", "angry"],
     lambda h: face_gif(h, ["angry"], ["cat"], n=6, sway=1.5, ears=True, extra=anger_mark), 100),
    ("serio", "Mirada seria", ["serio", "mirada", "hmm", "pensar"],
     lambda h: face_gif(h, ["line"], ["flat"], n=6, bob=0.5, extra=sweat), 200),
]


def anime_gifs():
    """(id, nombre, función que da (frames, duración), etiquetas)."""
    out = []
    for hair in HAIRS:
        for rid, name, tags, build, dur in REACTIONS:
            out.append((f"a_{rid}_{hair}", f"{name} ({hair})",
                        (lambda b=build, h=hair, du=dur: (b(h), du)), tags + ["anime", "chibi", hair]))
    return out
