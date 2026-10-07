"""
Emojis de 32x32 en estilo liso (tipo Discord): se dibujan a 256 px con formas suaves y se reducen con antialiasing.
Dibujo propio. Coordenadas en unidades de 32 (0..32). Lo usa gen_emojis.py: HD[nombre]() -> Image 32x32.
"""
import math

from PIL import Image, ImageChops, ImageDraw, ImageFont

OUT = 32
K = 8
SIZE = OUT * K


def rgb(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def mix(c1, c2, t):
    return tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(4))


def alpha(c, a):
    return c[:3] + (a,)


# ---------------------------------------------------------------- lienzo

DRAW_METHODS = {"ellipse", "circle", "poly", "line", "arc", "chord", "pieslice", "rrect", "text"}


class Canvas:
    def __init__(self, direct=False):
        self.img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img, "RGBA")
        self.direct = direct

    def __getattribute__(self, name):
        # Los colores semitransparentes se dibujan en una capa aparte y se mezclan (ImageDraw sobre RGBA los
        # pondría tal cual y dejaría manchas grises).
        attr = object.__getattribute__(self, name)
        if name in DRAW_METHODS and not object.__getattribute__(self, "direct"):
            def wrapped(*args, **kwargs):
                vals = list(args) + list(kwargs.values())
                # En pixel art no van los brillos (manchas blancas semitransparentes): le dan volumen "3D".
                if PIXEL and any(isinstance(v, tuple) and len(v) == 4 and v[3] < 255 and min(v[:3]) > 200 for v in vals):
                    return None
                if any(isinstance(v, tuple) and len(v) == 4 and v[3] < 255 for v in vals):
                    tmp = Canvas(direct=True)
                    getattr(tmp, name)(*args, **kwargs)
                    object.__getattribute__(self, "img").alpha_composite(tmp.img)
                    return None
                return attr(*args, **kwargs)
            return wrapped
        return attr

    @staticmethod
    def s(v):
        return v * K

    def box(self, x0, y0, x1, y1):
        return [self.s(x0), self.s(y0), self.s(x1), self.s(y1)]

    def ellipse(self, x0, y0, x1, y1, fill=None, outline=None, w=1.0):
        self.d.ellipse(self.box(x0, y0, x1, y1), fill=fill, outline=outline, width=int(self.s(w)) if outline else 0)

    def circle(self, cx, cy, r, fill=None, outline=None, w=1.0):
        self.ellipse(cx - r, cy - r, cx + r, cy + r, fill, outline, w)

    def poly(self, pts, fill=None, outline=None, w=1.0):
        p = [(self.s(x), self.s(y)) for x, y in pts]
        self.d.polygon(p, fill=fill)
        if outline:
            self.line(pts + [pts[0]], outline, w)

    def line(self, pts, color, w=1.0, caps=True):
        p = [(self.s(x), self.s(y)) for x, y in pts]
        self.d.line(p, fill=color, width=max(1, int(self.s(w))), joint="curve")
        if caps:
            for x, y in (pts[0], pts[-1]):
                self.circle(x, y, w / 2, color)

    def arc(self, x0, y0, x1, y1, start, end, color, w=1.0):
        self.d.arc(self.box(x0, y0, x1, y1), start, end, fill=color, width=max(1, int(self.s(w))))
        # puntas redondeadas
        cx, cy, rx, ry = (x0 + x1) / 2, (y0 + y1) / 2, (x1 - x0) / 2 - w / 2, (y1 - y0) / 2 - w / 2
        for a in (start, end):
            t = math.radians(a)
            self.circle(cx + rx * math.cos(t), cy + ry * math.sin(t), w / 2, color)

    def chord(self, x0, y0, x1, y1, start, end, fill):
        self.d.chord(self.box(x0, y0, x1, y1), start, end, fill=fill)

    def pieslice(self, x0, y0, x1, y1, start, end, fill):
        self.d.pieslice(self.box(x0, y0, x1, y1), start, end, fill=fill)

    def rrect(self, x0, y0, x1, y1, r, fill=None, outline=None, w=1.0):
        self.d.rounded_rectangle(self.box(x0, y0, x1, y1), radius=self.s(r), fill=fill, outline=outline,
                                 width=int(self.s(w)) if outline else 0)

    def text(self, cx, cy, txt, size, fill, stroke=0.0, stroke_fill=None, font=None):
        f = font or ImageFont.load_default(int(self.s(size)))
        self.d.text((self.s(cx), self.s(cy)), txt, font=f, fill=fill, anchor="mm",
                    stroke_width=int(self.s(stroke)), stroke_fill=stroke_fill)

    def layer(self):
        """Capa aparte (para recortar con una máscara)."""
        c = Canvas()
        return c

    def paste_clipped(self, layer, mask_layer):
        """Pega `layer` solo donde `mask_layer` tiene alfa."""
        a = ImageChops.multiply(layer.img.getchannel("A"), mask_layer.img.getchannel("A"))
        l = layer.img.copy()
        l.putalpha(a)
        self.img.alpha_composite(l)

    def paste(self, layer):
        self.img.alpha_composite(layer.img)

    def done(self):
        return pixelate(self.img)


PIXEL = True


def pixelate(big, colors=14, outline=True):
    """Pasa el dibujo grande a pixel art de 32x32: bordes duros, paleta corta sin degradados y contorno oscuro de 1 px
    (como los ítems de Minecraft)."""
    import numpy as np
    if not PIXEL:
        return bleed(big.resize((OUT, OUT), Image.LANCZOS))
    small = big.resize((OUT, OUT), Image.BOX)
    a = np.asarray(small).copy()
    solid = a[..., 3] >= 110
    rgb_img = Image.fromarray(a[..., :3])
    q = np.asarray(rgb_img.quantize(colors=colors, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
                   .convert("RGB"))
    out = np.zeros((OUT, OUT, 4), np.uint8)
    out[solid, :3] = q[solid]
    out[solid, 3] = 255
    if outline:
        edge = np.zeros_like(solid)
        col = np.zeros((OUT, OUT, 3), np.float32)
        cnt = np.zeros((OUT, OUT), np.float32)
        for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            sh = np.roll(np.roll(solid, dy, 0), dx, 1)
            shc = np.roll(np.roll(out[..., :3].astype(np.float32), dy, 0), dx, 1)
            m = sh & ~solid
            col[m] += shc[m]
            cnt[m] += 1
            edge |= m
        # No envolver por los bordes del lienzo.
        edge[0, :] &= solid[1, :]; edge[-1, :] &= solid[-2, :]; edge[:, 0] &= solid[:, 1]; edge[:, -1] &= solid[:, -2]
        dark = (col / np.maximum(cnt, 1)[..., None]) * 0.42
        out[edge, :3] = dark[edge].astype(np.uint8)
        out[edge, 3] = 255
    return Image.fromarray(out, "RGBA")


def bleed(img):
    """Pone en los píxeles transparentes el color del borde más cercano (su alfa sigue en 0). Así, al dibujar el
    emoji reducido con filtro suave, el borde no se mezcla con negro y no salen halos oscuros."""
    import numpy as np
    a = np.asarray(img).astype(np.float32)
    rgb_, al = a[..., :3], a[..., 3:4] / 255.0
    acc, w = rgb_ * al, al.copy()
    out = a.copy()
    filled = al[..., 0] > 0
    for _ in range(OUT):
        if filled.all():
            break
        pad_acc = np.pad(acc, ((1, 1), (1, 1), (0, 0)), mode="edge")
        pad_w = np.pad(w, ((1, 1), (1, 1), (0, 0)), mode="edge")
        s_acc = sum(pad_acc[1 + dy:1 + dy + OUT, 1 + dx:1 + dx + OUT] for dy in (-1, 0, 1) for dx in (-1, 0, 1))
        s_w = sum(pad_w[1 + dy:1 + dy + OUT, 1 + dx:1 + dx + OUT] for dy in (-1, 0, 1) for dx in (-1, 0, 1))
        new = (~filled) & (s_w[..., 0] > 0)
        col = s_acc / np.maximum(s_w, 1e-6)
        out[new, :3] = col[new]
        acc[new] = col[new]
        w[new] = 1.0
        filled = filled | new
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA")


def star_pts(cx, cy, r_out, r_in, n=5, rot=-90):
    pts = []
    for i in range(n * 2):
        r = r_out if i % 2 == 0 else r_in
        a = math.radians(rot + i * 180 / n)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def heart_pts(cx, cy, s, steps=60):
    pts = []
    for i in range(steps):
        t = 2 * math.pi * i / steps
        x = 16 * math.sin(t) ** 3
        y = 13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)
        pts.append((cx + x * s / 16, cy - y * s / 16))
    return pts


# ---------------------------------------------------------------- paleta

INK = rgb("3B2A1A")
WHITE = rgb("FFFFFF")
TEAR = rgb("5DADEC")
TEAR_D = rgb("3B88C3")
TONGUE = rgb("E8596E")
MOUTH = rgb("664500")
BLUSH = rgb("FF7B8A", 150)
RED = rgb("DD2E44")

# base, borde/sombra, brillo
SKIN = {
    "yellow": (rgb("FFCC4D"), rgb("E8A92E"), rgb("FFE58A")),
    "red": (rgb("F4664A"), rgb("C7381F"), rgb("FF9A82")),
    "green": (rgb("8CC152"), rgb("5E9634"), rgb("B5DB86")),
    "blue": (rgb("7EC5F5"), rgb("4796D1"), rgb("BDE3FF")),
    "purple": (rgb("AA8DD8"), rgb("7C5BB5"), rgb("CDB8F0")),
    "orange": (rgb("FFAC33"), rgb("E07B00"), rgb("FFD08A")),
}
HAND = (rgb("FFCC4D"), rgb("E8A92E"))


def face_base(c, color="yellow", cx=16, cy=16, r=14.6):
    base, dark, light = SKIN[color]
    c.circle(cx, cy, r, dark)
    c.circle(cx, cy - 0.5, r - 1.0, base)
    if PIXEL:
        return
    # brillo suave arriba a la izquierda
    hl = c.layer()
    hl.ellipse(cx - r * 0.75, cy - r * 0.9, cx + r * 0.3, cy - r * 0.15, alpha(light, 140))
    mask = c.layer()
    mask.circle(cx, cy - 0.5, r - 1.0, WHITE)
    c.paste_clipped(hl, mask)


# ---- ojos

def eyes_dot(c, y=13, dx=5, rx=1.7, ry=2.4, color=INK):
    for x in (16 - dx, 16 + dx):
        c.ellipse(x - rx, y - ry, x + rx, y + ry, color)


def eyes_big(c, y=13, dx=5, look=(0, 0), r=3.4):
    for x in (16 - dx, 16 + dx):
        c.circle(x, y, r, WHITE, INK, 0.6)
        c.circle(x + look[0], y + look[1], r * 0.55, INK)
        c.circle(x + look[0] - r * 0.25, y + look[1] - r * 0.25, r * 0.18, WHITE)


def eyes_happy(c, y=14, dx=5, w=1.6):
    for x in (16 - dx, 16 + dx):
        c.arc(x - 3, y - 2, x + 3, y + 3, 200, 340, INK, w)


def eyes_closed(c, y=13, dx=5, w=1.5):
    for x in (16 - dx, 16 + dx):
        c.arc(x - 3, y - 3, x + 3, y + 2, 20, 160, INK, w)


def eyes_line(c, y=13, dx=5, w=1.6):
    for x in (16 - dx, 16 + dx):
        c.line([(x - 2.5, y), (x + 2.5, y)], INK, w)


def eyes_xd(c, y=13, dx=5, w=1.6):
    c.line([(16 - dx - 2.5, y - 2.5), (16 - dx + 2, y), (16 - dx - 2.5, y + 2.5)], INK, w)
    c.line([(16 + dx + 2.5, y - 2.5), (16 + dx - 2, y), (16 + dx + 2.5, y + 2.5)], INK, w)


def eyes_x(c, y=13, dx=5, w=1.5):
    for x in (16 - dx, 16 + dx):
        c.line([(x - 2, y - 2), (x + 2, y + 2)], INK, w)
        c.line([(x - 2, y + 2), (x + 2, y - 2)], INK, w)


def brows(c, kind, y=8.5, dx=5, w=1.4):
    for side in (-1, 1):
        x = 16 + side * dx
        if kind == "angry":
            c.line([(x - side * 3, y + 1.4), (x + side * 2.2, y - 1.2)], INK, w)
        elif kind == "sad":
            c.line([(x - side * 3, y - 1), (x + side * 2.2, y + 1.2)], INK, w)
        elif kind == "up":
            c.arc(x - 3, y - 2.5, x + 3, y + 1.5, 200, 340, INK, w)


# ---- bocas

def mouth_smile(c, y=19, w=9, h=6, lw=1.6):
    c.arc(16 - w / 2, y - h / 2, 16 + w / 2, y + h / 2, 20, 160, INK, lw)


def mouth_frown(c, y=23, w=8, h=5, lw=1.6):
    c.arc(16 - w / 2, y - h / 2, 16 + w / 2, y + h / 2, 200, 340, INK, lw)


def mouth_open(c, y=18, w=13, h=10, teeth=True, tongue=True):
    c.chord(16 - w / 2, y - h / 2, 16 + w / 2, y + h / 2, 0, 180, MOUTH)
    if tongue:
        layer = c.layer()
        layer.ellipse(16 - w * 0.3, y + h * 0.15, 16 + w * 0.3, y + h * 0.6, TONGUE)
        m = c.layer()
        m.chord(16 - w / 2, y - h / 2, 16 + w / 2, y + h / 2, 0, 180, WHITE)
        c.paste_clipped(layer, m)
    if teeth:
        layer = c.layer()
        layer.rrect(16 - w / 2, y - 0.2, 16 + w / 2, y + 1.8, 0.5, WHITE)
        m = c.layer()
        m.chord(16 - w / 2, y - h / 2, 16 + w / 2, y + h / 2, 0, 180, WHITE)
        c.paste_clipped(layer, m)


def mouth_flat(c, y=21, w=8, lw=1.6):
    c.line([(16 - w / 2, y), (16 + w / 2, y)], INK, lw)


def mouth_o(c, y=21.5, rx=2.6, ry=3.2):
    c.ellipse(16 - rx, y - ry, 16 + rx, y + ry, MOUTH)


def tongue_out(c, y=21, x=16):
    c.rrect(x - 2.8, y, x + 2.8, y + 6, 2.8, TONGUE)
    c.line([(x, y + 0.5), (x, y + 3.5)], rgb("C43A50"), 0.8)


def tear(c, x, y, s=1.0):
    c.poly([(x, y - 3.2 * s), (x - 2 * s, y + 0.3 * s), (x + 2 * s, y + 0.3 * s)], TEAR)
    c.circle(x, y + 0.6 * s, 2.05 * s, TEAR)


def blush(c, y=18.5, dx=8.5):
    for x in (16 - dx, 16 + dx):
        c.ellipse(x - 2.6, y - 1.4, x + 2.6, y + 1.4, BLUSH)


def hand(c, cx, cy, s=1.0, rot=0):
    """Mano abierta simplificada (palma + dedos)."""
    base, dark = HAND
    c.ellipse(cx - 4 * s, cy - 3 * s, cx + 4 * s, cy + 4 * s, base, dark, 0.8)


# ---------------------------------------------------------------- caras

def face(color="yellow"):
    c = Canvas()
    face_base(c, color)
    return c


def e_sonrisa():
    c = face(); eyes_dot(c); mouth_smile(c, 19, 12, 9); return c.done()


def e_feliz():
    c = face(); eyes_happy(c); blush(c); mouth_smile(c, 19, 12, 9); return c.done()


def e_risa():
    c = face(); eyes_happy(c, 13); mouth_open(c, 19, 16, 11); return c.done()


def e_jaja():
    c = face(); eyes_xd(c, 12); mouth_open(c, 19, 17, 13); return c.done()


def e_llorar_risa():
    c = face(); eyes_happy(c, 12.5)
    tear(c, 5.5, 16, 1.2); tear(c, 26.5, 16, 1.2)
    mouth_open(c, 19, 16, 11); return c.done()


def e_sudor():
    c = face(); eyes_happy(c, 13); mouth_open(c, 19.5, 15, 10)
    tear(c, 25, 7, 1.15); return c.done()


def e_guino():
    c = face()
    c.ellipse(9.3, 10.6, 12.7, 15.4, INK)
    c.arc(18, 10, 24, 15, 200, 340, INK, 1.6)
    mouth_smile(c, 19, 12, 9); return c.done()


def e_enamorado():
    c = face()
    for x in (10.5, 21.5):
        c.poly(heart_pts(x, 13, 5.2), RED)
    mouth_open(c, 19.5, 13, 9, tongue=False); return c.done()


def e_estrellas():
    c = face()
    for x in (10.5, 21.5):
        c.poly(star_pts(x, 13, 5, 2.3), rgb("F4900C"))
        c.poly(star_pts(x, 13.3, 3.6, 1.6), rgb("FFD983"))
    mouth_open(c, 20, 13, 9, tongue=False); return c.done()


def e_beso():
    c = face()
    c.ellipse(9.3, 10.6, 12.7, 15.4, INK)
    c.arc(18, 10, 24, 15, 200, 340, INK, 1.6)
    c.arc(14, 17.5, 18.5, 21, 270, 90, INK, 1.4)
    c.arc(14, 20.5, 18.5, 24, 270, 90, INK, 1.4)
    c.poly(heart_pts(24.5, 23, 3.6), RED)
    return c.done()


def e_lengua():
    c = face(); eyes_dot(c)
    c.arc(9, 15, 23, 21, 20, 160, INK, 1.6)
    tongue_out(c, 19.5); return c.done()


def e_loco():
    c = face()
    c.circle(10, 12, 3.8, WHITE, INK, 0.6); c.circle(10.6, 12.6, 1.8, INK)
    c.circle(22, 13.5, 2.6, WHITE, INK, 0.6); c.circle(21.6, 13.8, 1.2, INK)
    c.arc(8, 15, 24, 23, 15, 165, INK, 1.6)
    tongue_out(c, 21, 19); return c.done()


def e_cool():
    c = face()
    glass = rgb("292F33")
    c.rrect(5, 10, 14.5, 16.5, 2.5, glass); c.rrect(17.5, 10, 27, 16.5, 2.5, glass)
    c.line([(3, 11), (29, 11)], glass, 1.4)
    c.line([(7, 11.6), (10, 11.6)], alpha(WHITE, 160), 0.8)
    c.line([(19.5, 11.6), (22.5, 11.6)], alpha(WHITE, 160), 0.8)
    c.arc(10, 16, 23, 23.5, 20, 160, INK, 1.6); return c.done()


def e_nerd():
    c = face()
    frame = rgb("292F33")
    for x in (10.5, 21.5):
        c.circle(x, 13, 4.6, alpha(WHITE, 90), frame, 1.3)
        c.circle(x, 13.3, 1.7, INK)
    c.line([(14.6, 13), (17.4, 13)], frame, 1.2)
    c.chord(10, 15, 22, 24, 0, 180, MOUTH)
    c.rrect(14.2, 19.4, 15.9, 21.6, 0.3, WHITE); c.rrect(16.1, 19.4, 17.8, 21.6, 0.3, WHITE)
    return c.done()


def e_angel():
    c = Canvas()
    face_base(c, cy=17.5, r=13.6)
    c.ellipse(7, 1, 25, 6.5, None, rgb("5DADEC"), 1.6)
    eyes_happy(c, 15.5)
    c.arc(10, 18, 22, 25.5, 20, 160, INK, 1.6); blush(c, 20.5, 8)
    return c.done()


def e_neutral():
    c = face(); eyes_dot(c); mouth_flat(c, 21.5, 10); return c.done()


def e_inexpresivo():
    c = face(); eyes_line(c, 13.5, 5.2, 1.8); mouth_flat(c, 21.5, 10); return c.done()


def e_aburrido():
    c = face()
    for x in (10.5, 21.5):
        c.chord(x - 3.6, 10.5, x + 3.6, 16.5, 0, 180, WHITE)
        c.line([(x - 3.6, 13.5), (x + 3.6, 13.5)], INK, 1.2)
        c.circle(x + 2, 14.6, 1.3, INK)
    brows(c, "sad", 9)
    mouth_frown(c, 23.5, 7, 4); return c.done()


def e_ojos_blanco():
    c = face()
    for x in (10.5, 21.5):
        c.circle(x, 13, 3.6, WHITE, INK, 0.6)
        c.circle(x + 0.6, 10.9, 1.7, INK)
    mouth_flat(c, 22, 7); return c.done()


def e_picaro():
    c = face()
    for x in (10.5, 21.5):
        c.chord(x - 3.4, 11, x + 3.4, 16.4, 0, 180, WHITE)
        c.line([(x - 3.4, 13.6), (x + 3.4, 13.6)], INK, 1.2)
        c.circle(x + 1.6, 14.5, 1.3, INK)
    c.line([(10, 21.5), (17, 21.2), (22.5, 18.5)], INK, 1.6); return c.done()


def e_pensativo():
    c = face()
    eyes_dot(c, 12.5)
    brows(c, "up", 7.5)
    c.line([(12, 20.5), (19, 19.5)], INK, 1.6)
    base, dark = HAND
    c.rrect(7.5, 22, 18.5, 27.5, 2.6, base, dark, 0.8)
    c.rrect(15, 15.5, 18.6, 24.5, 1.8, base, dark, 0.8)
    return c.done()


def e_triste():
    c = face(); eyes_closed(c, 15); brows(c, "sad", 9.5); mouth_frown(c, 23.5, 8, 4.5); return c.done()


def e_llorar():
    c = face(); eyes_dot(c, 13.5); brows(c, "sad", 8.5); mouth_frown(c, 23.5, 8, 4.5)
    tear(c, 22, 18, 1.0); return c.done()


def e_llanto():
    c = face()
    eyes_closed(c, 13)
    c.rrect(6.6, 14, 10, 30, 1.6, TEAR); c.rrect(22, 14, 25.4, 30, 1.6, TEAR)
    c.chord(10.5, 16.5, 21.5, 27, 180, 360, MOUTH)
    return c.done()


def e_suplicar():
    c = face()
    for x in (10.5, 21.5):
        c.circle(x, 14, 4.2, INK)
        c.circle(x - 1.2, 12.6, 1.5, WHITE)
        c.circle(x + 1.3, 15.6, 0.8, WHITE)
        c.arc(x - 4.5, 14.5, x + 4.5, 19.5, 30, 150, alpha(TEAR, 200), 0.8)
    brows(c, "sad", 8)
    c.arc(13, 21.5, 19, 25.5, 200, 340, INK, 1.4); return c.done()


def e_enojado():
    c = face("orange"); eyes_dot(c, 14); brows(c, "angry", 10); mouth_frown(c, 24, 9, 5); return c.done()


def e_furioso():
    c = face("red"); eyes_dot(c, 14, ry=2.0); brows(c, "angry", 10, w=1.7)
    c.chord(10, 20.5, 22, 28, 180, 360, MOUTH)
    return c.done()


def e_sorpresa():
    c = face(); eyes_dot(c, 12.5); brows(c, "up", 7.5); mouth_o(c, 22, 2.8, 3.4); return c.done()


def e_asombro():
    c = face(); eyes_big(c, 12.5, r=3.6); mouth_o(c, 22.5, 3.4, 4.2); return c.done()


def e_grito():
    c = Canvas()
    face_base(c)
    top = c.layer()
    top.rrect(0, 0, 32, 14, 0, alpha(rgb("9AA6E8"), 200))
    m = c.layer(); m.circle(16, 15.5, 13.6, WHITE)
    c.paste_clipped(top, m)
    eyes_big(c, 12, r=3.4)
    c.ellipse(13, 17.5, 19, 27.5, MOUTH)
    base, dark = HAND
    c.rrect(1, 17, 7, 29, 3, base, dark, 0.8)
    c.rrect(25, 17, 31, 29, 3, base, dark, 0.8)
    return c.done()


def e_dormido():
    c = face(); eyes_closed(c, 15.5); mouth_o(c, 22.5, 2.2, 2.6)
    c.text(26, 6, "z", 8, rgb("5DADEC"), 0.4, rgb("3B88C3"))
    c.text(21.5, 3.5, "z", 5.5, rgb("5DADEC"), 0.3, rgb("3B88C3"))
    return c.done()


def e_mareado():
    c = face()
    for x in (10.5, 21.5):
        pts = [(x + 0.28 * t * math.cos(t), 13 + 0.28 * t * math.sin(t)) for t in [i * 0.25 for i in range(52)]]
        c.line(pts, INK, 1.1)
    mouth_o(c, 22.5, 2.4, 2.8); return c.done()


def e_enfermo():
    c = face("green"); eyes_closed(c, 13.5); brows(c, "sad", 8.5)
    c.line([(10, 22), (13, 20.5), (16, 22), (19, 20.5), (22, 22)], INK, 1.5)
    blush(c, 18, 9); return c.done()


def e_frio():
    c = face("blue")
    eyes_dot(c, 13, color=rgb("1C4E80")); brows(c, "sad", 8.5)
    c.rrect(9.5, 19, 22.5, 24.5, 1.5, WHITE, rgb("1C4E80"), 0.9)
    for x in (12.7, 16, 19.3):
        c.line([(x, 19.2), (x, 24.3)], rgb("1C4E80"), 0.7, False)
    for x, y in ((6, 6), (26, 25)):
        c.line([(x - 2, y), (x + 2, y)], WHITE, 0.8); c.line([(x, y - 2), (x, y + 2)], WHITE, 0.8)
    return c.done()


def e_calor():
    c = face("red")
    eyes_closed(c, 13.5); brows(c, "sad", 8.5)
    c.chord(10, 15.5, 22, 24.5, 0, 180, MOUTH)
    tongue_out(c, 19.5)
    tear(c, 27, 9, 1.0); tear(c, 5.5, 11, 0.8); return c.done()


def e_dinero():
    c = face()
    green = rgb("3B9E46")
    for x in (10.5, 21.5):
        c.text(x, 13, "$", 9, green, 0.4, green)
    c.chord(9, 15.5, 23, 25, 0, 180, MOUTH)
    c.rrect(11, 19.5, 21, 24.5, 1.5, rgb("8CC152"))
    c.text(16, 22, "$", 4.5, rgb("2E6B33"))
    return c.done()


def e_fiesta():
    c = Canvas()
    face_base(c, cy=17.5, r=13.6)
    c.poly([(19, 7), (27.5, 0.5), (26, 10.5)], rgb("AA8DD8"))
    c.line([(21, 6.5), (26.5, 4)], rgb("FFCC4D"), 0.9)
    c.circle(27.5, 0.9, 1.6, rgb("FFCC4D"))
    eyes_happy(c, 15.5)
    c.arc(10, 18, 18, 25, 20, 160, INK, 1.5)
    c.poly([(16, 22), (26, 18.5), (27, 21.5)], rgb("DD2E44"))
    c.circle(26.8, 20, 1.6, rgb("FFCC4D"))
    for x, y, col in ((4, 6, "5DADEC"), (7, 3, "DD2E44"), (29, 14, "8CC152"), (3, 13, "FFCC4D")):
        c.rrect(x - 0.8, y - 0.8, x + 0.8, y + 0.8, 0.3, rgb(col))
    return c.done()


def horns(c, color):
    c.poly([(4, 9), (3, 0.5), (11, 5)], color)
    c.poly([(28, 9), (29, 0.5), (21, 5)], color)


def e_diablo():
    c = Canvas()
    horns(c, rgb("7C5BB5"))
    face_base(c, "purple", cy=17, r=14)
    for side in (-1, 1):
        x = 16 + side * 5
        c.circle(x, 14.5, 2, INK)
    brows(c, "angry", 10.5, w=1.6)
    c.arc(9, 15, 23, 25, 20, 160, INK, 1.7)
    return c.done()


def e_calavera():
    c = Canvas()
    bone, shade = rgb("E8EDF2"), rgb("A7B4C0")
    c.circle(16, 13.5, 12.5, shade)
    c.circle(16, 13, 11.8, bone)
    c.rrect(9.5, 18, 22.5, 28.5, 3, bone, shade, 0.8)
    for x in (11, 21):
        c.ellipse(x - 3.6, 10, x + 3.6, 17, rgb("292F33"))
    c.poly([(16, 17), (14.4, 20.4), (17.6, 20.4)], rgb("292F33"))
    for x in (13, 16, 19):
        c.line([(x, 23.5), (x, 28)], shade, 0.9, False)
    return c.done()


def e_fantasma():
    c = Canvas()
    body, shade = rgb("F5F8FA"), rgb("B8C4CE")
    pts = [(5, 15)]
    for i in range(41):
        t = math.pi + math.pi * i / 40
        pts.append((16 + 11 * math.cos(t), 15 + 12 * math.sin(t)))
    pts += [(27, 28), (24, 25.5), (21, 29), (18, 26), (15, 29.5), (12, 26), (9, 29), (5, 27)]
    c.poly(pts, shade)
    c.poly([(x + (0.6 if x < 16 else -0.6), y - 0.6) for x, y in pts], body)
    c.ellipse(9, 10.5, 13.5, 17.5, rgb("292F33")); c.ellipse(18.5, 10.5, 23, 17.5, rgb("292F33"))
    c.ellipse(13, 18.5, 18, 23.5, rgb("292F33"))
    c.line([(26.5, 16), (30.5, 12.5)], shade, 2.5)
    return c.done()


def e_caca():
    c = Canvas()
    b1, b2, b3 = rgb("A0663B"), rgb("7A4A26"), rgb("C08654")
    c.ellipse(3, 20, 29, 30.5, b2); c.ellipse(3.6, 19.5, 28.4, 29.5, b1)
    c.ellipse(6, 13, 26, 22.5, b2); c.ellipse(6.6, 12.5, 25.4, 21.5, b1)
    c.ellipse(9.5, 6.5, 22.5, 15, b2); c.ellipse(10, 6, 22, 14, b1)
    c.poly([(14, 7.5), (19, 1), (19.5, 8)], b1)
    c.ellipse(11.5, 7.5, 16, 10, alpha(b3, 200))
    for x in (12.2, 19.8):
        c.circle(x, 17, 2.8, WHITE); c.circle(x + 0.4, 17.4, 1.4, INK)
    c.chord(10, 19.5, 22, 27.5, 0, 180, rgb("4A2A12"))
    return c.done()


def e_timido():
    c = face(); eyes_closed(c, 14); blush(c, 18, 9)
    c.arc(12, 18, 20, 23, 20, 160, INK, 1.4); return c.done()


def e_guino_sonrisa():
    c = face()
    c.ellipse(9.3, 10.6, 12.7, 15.4, INK)
    c.line([(18.5, 10.5), (23, 13), (18.5, 15.5)], INK, 1.6)
    c.arc(9, 15, 23, 21, 20, 160, INK, 1.6)
    tongue_out(c, 19.5, 18); return c.done()


def e_nervioso():
    c = face(); eyes_dot(c, 12.5); brows(c, "up", 7.5)
    c.rrect(8.5, 18, 23.5, 24.5, 2.5, WHITE, INK, 1.0)
    c.line([(8.8, 21.25), (23.2, 21.25)], INK, 0.8, False)
    for x in (12.2, 16, 19.8):
        c.line([(x, 18.3), (x, 24.2)], INK, 0.8, False)
    return c.done()


def e_bostezo():
    c = face(); eyes_closed(c, 12.5); brows(c, "sad", 7.5)
    c.ellipse(11, 16.5, 21, 27, MOUTH)
    base, dark = HAND
    c.rrect(15, 20.5, 27, 29, 3.5, base, dark, 0.8)
    for y in (22.8, 25.2):
        c.line([(16.5, y), (25, y)], dark, 0.7)
    return c.done()


def e_zombi():
    c = Canvas()
    base, dark, light = rgb("8DB580"), rgb("5E8255"), rgb("B5D3AA")
    c.circle(16, 16, 14.6, dark); c.circle(16, 15.5, 13.6, base)
    c.circle(10.5, 13, 3, WHITE); c.circle(11, 13.5, 1.2, INK)
    c.circle(21.5, 13, 2.2, WHITE); c.circle(21.2, 13.4, 0.9, INK)
    c.line([(9, 22), (23, 21)], rgb("3E5A38"), 1.6)
    for x in (12, 15.5, 19):
        c.line([(x, 19.8), (x + 0.3, 23.4)], rgb("3E5A38"), 0.8)
    c.line([(18.5, 4.5), (25.5, 7.5)], rgb("8E4A4A"), 0.9)
    for x in (20, 22, 24):
        c.line([(x, 4.5), (x - 0.4, 7.8)], rgb("8E4A4A"), 0.7)
    return c.done()


def e_robot():
    c = Canvas()
    metal, dark, light = rgb("B9C6D1"), rgb("6D7F8E"), rgb("E1E8ED")
    c.line([(16, 2), (16, 6)], dark, 1.4); c.circle(16, 2.5, 2, RED)
    c.rrect(1.5, 13, 5, 20, 1.2, dark); c.rrect(27, 13, 30.5, 20, 1.2, dark)
    c.rrect(4, 6, 28, 28, 5, dark); c.rrect(4.6, 6.4, 27.4, 27, 4.6, metal)
    c.rrect(7, 9, 25, 18, 3, rgb("292F33"))
    c.circle(11.5, 13.5, 2.4, rgb("5DADEC")); c.circle(20.5, 13.5, 2.4, rgb("5DADEC"))
    c.circle(11, 13, 0.8, WHITE); c.circle(20, 13, 0.8, WHITE)
    c.rrect(9, 20.5, 23, 25, 1.5, rgb("292F33"))
    for x in (12.5, 16, 19.5):
        c.line([(x, 20.8), (x, 24.7)], metal, 0.7, False)
    return c.done()


def e_alien():
    c = Canvas()
    skin, dark = rgb("B4C3CC"), rgb("7F929E")
    pts = []
    for i in range(60):
        t = 2 * math.pi * i / 60
        r = 1 if math.sin(t) < 0 else 1 - 0.38 * math.sin(t) ** 1.5
        pts.append((16 + 12.5 * math.cos(t) * r, 14 + 13.5 * math.sin(t) * (1.15 if math.sin(t) > 0 else 1)))
    c.poly(pts, dark)
    c.poly([(16 + (x - 16) * 0.94, 14 + (y - 14) * 0.95 - 0.4) for x, y in pts], skin)
    c.poly([(5, 11), (14, 15), (12.5, 19), (7, 17)], rgb("292F33"))
    c.poly([(27, 11), (18, 15), (19.5, 19), (25, 17)], rgb("292F33"))
    c.line([(14.5, 24), (17.5, 24)], rgb("292F33"), 0.9)
    return c.done()


def e_payaso():
    c = Canvas()
    white, shade = rgb("F5F8FA"), rgb("C3CDD5")
    for x, y in ((4, 9), (28, 9), (6, 4), (26, 4)):
        c.circle(x, y, 4.2, rgb("E95E5E"))
    c.circle(16, 16.5, 13.6, shade); c.circle(16, 16, 13, white)
    for x in (10.5, 21.5):
        c.line([(x, 7.5), (x, 18.5)], alpha(rgb("5DADEC"), 230), 1.0)
        c.line([(x - 3.5, 13), (x + 3.5, 13)], alpha(rgb("5DADEC"), 230), 1.0)
        c.ellipse(x - 1.5, 11, x + 1.5, 15, INK)
    c.circle(16, 18, 2.8, RED); c.circle(15.2, 17.2, 0.8, alpha(WHITE, 200))
    c.chord(9, 18.5, 23, 27, 0, 180, RED)
    c.chord(11, 21, 21, 25, 0, 180, WHITE)
    return c.done()


def e_gato_feliz():
    c = Canvas()
    fur, dark, inner = rgb("FFCC4D"), rgb("E8A92E"), rgb("FF9F9F")
    c.poly([(3, 15), (4, 1.5), (14, 7)], dark); c.poly([(29, 15), (28, 1.5), (18, 7)], dark)
    c.poly([(5, 12), (5.6, 4.5), (11.5, 7.8)], inner); c.poly([(27, 12), (26.4, 4.5), (20.5, 7.8)], inner)
    c.ellipse(1.5, 4, 30.5, 30.5, dark); c.ellipse(2.3, 4.5, 29.7, 29.5, fur)
    eyes_happy(c, 16.5, 5.5)
    c.poly([(14.6, 19), (17.4, 19), (16, 20.5)], rgb("E06A6A"))
    c.arc(11.5, 19, 16, 24, 20, 160, INK, 1.2); c.arc(16, 19, 20.5, 24, 20, 160, INK, 1.2)
    for s in (-1, 1):
        for dy in (-1, 1.2):
            c.line([(16 + s * 6, 21 + dy * 0.6), (16 + s * 12.5, 20 + dy * 1.8)], alpha(INK, 170), 0.6)
    return c.done()


def e_sin_boca():
    c = face(); eyes_dot(c, 14, 5, 1.8, 2.6); return c.done()


def e_mascarilla():
    c = face(); eyes_closed(c, 12); brows(c, "sad", 7.5)
    c.line([(3, 16), (9, 18.5)], WHITE, 0.8); c.line([(29, 16), (23, 18.5)], WHITE, 0.8)
    c.rrect(7, 16, 25, 27, 3.5, rgb("F5F8FA"), rgb("A7B4C0"), 0.8)
    for y in (19.5, 22.5):
        c.line([(9, y), (23, y)], rgb("C3CDD5"), 0.7)
    return c.done()


def e_uwu():
    c = face(); blush(c, 17.5, 9)
    for x in (10.5, 21.5):
        c.arc(x - 3, 10.5, x + 3, 15.5, 200, 340, INK, 1.6)
    c.arc(11.5, 17.5, 16, 22, 20, 160, INK, 1.4); c.arc(16, 17.5, 20.5, 22, 20, 160, INK, 1.4)
    return c.done()


def e_al_reves():
    img = Image.open  # noqa: F841 (solo para que el linter no se queje)
    c = face(); eyes_dot(c); mouth_smile(c, 19, 12, 9)
    return c.done().rotate(180)


def e_vaquero():
    c = Canvas()
    face_base(c, cy=18.5, r=13)
    hat, hat_d = rgb("C1694F"), rgb("8B4636")
    c.ellipse(0.5, 7, 31.5, 13.5, hat_d)
    c.rrect(8, 0.5, 24, 10.5, 3.5, hat)
    c.ellipse(1, 6.5, 31, 12, hat)
    c.line([(8.3, 8), (23.7, 8)], hat_d, 1.4, False)
    eyes_dot(c, 16.5, 5, 1.6, 2.2)
    c.arc(9.5, 17, 22.5, 26, 20, 160, INK, 1.6)
    return c.done()


def e_explota():
    c = Canvas()
    face_base(c, cy=19, r=12.5)
    cloud = rgb("E8EDF2")
    for x, y, r in ((9, 6, 4.5), (16, 3.5, 5), (23, 6, 4.5), (5, 10, 3), (27, 10, 3)):
        c.circle(x, y, r, rgb("F4900C"))
        c.circle(x, y, r * 0.65, cloud)
    c.poly(star_pts(16, 7, 7, 3, 8), alpha(rgb("FFCC4D"), 230))
    eyes_big(c, 17, 4.6, r=2.8)
    c.ellipse(13.5, 22, 18.5, 28, MOUTH)
    return c.done()


HD = {name[2:]: fn for name, fn in list(globals().items()) if name.startswith("e_") and callable(fn)}
# El de serie se llama "caca" pero en gen_emojis su función es e_payaso_caca.


# ---------------------------------------------------------------- iconos de la barra (blancos, 32x32)

def gui_icons():
    w = rgb("F2F2F2")
    icons = {}

    c = Canvas()
    c.circle(16, 16, 13, None, w, 2.6)
    c.ellipse(10.5, 10, 13.5, 15, w); c.ellipse(18.5, 10, 21.5, 15, w)
    c.arc(9, 12, 23, 24, 25, 155, w, 2.6)
    icons["btn_emoji"] = c.done()

    c = Canvas()
    s = Canvas(direct=True)
    s.poly([(14.2, 2), (17.8, 2), (17.8, 20), (16, 22.5), (14.2, 20)], w)
    s.rrect(9.5, 20.5, 22.5, 23.5, 1.2, w)
    s.rrect(14.6, 23, 17.4, 29, 1, w)
    s.circle(16, 30, 1.8, w)
    c.img.alpha_composite(s.img.rotate(-45, center=(128, 128), resample=Image.BICUBIC))
    icons["btn_item"] = c.done()

    c = Canvas()
    c.rrect(10, 4.5, 22, 9, 1.5, w)
    c.rrect(2.5, 7.5, 29.5, 27, 4, None, w, 2.6)
    c.circle(16, 17, 5.6, None, w, 2.6)
    c.circle(24.5, 12.2, 1.5, w)
    icons["btn_shot"] = c.done()

    c = Canvas()
    c.rrect(2.5, 6.5, 29.5, 25.5, 3.5, None, w, 2.6)
    c.line([(4, 8.5), (16, 17.5), (28, 8.5)], w, 2.6)
    icons["btn_dm"] = c.done()

    c = Canvas()
    c.poly([(3, 4), (30, 16), (3, 28), (7.5, 16)], w)
    icons["btn_send"] = c.done()
    return icons
