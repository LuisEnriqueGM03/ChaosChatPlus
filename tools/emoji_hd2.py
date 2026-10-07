"""Emojis HD (32x32, estilo liso): corazones, gestos, símbolos y objetos. Ver emoji_hd.py."""
import math

from emoji_hd import (Canvas, HAND, INK, MOUTH, RED, TEAR, WHITE, alpha, heart_pts, mix, rgb, star_pts)

# ---------------------------------------------------------------- corazones


def heart(color, dark=None):
    c = Canvas()
    dark = dark or mix(color, rgb("000000"), 0.25)
    c.poly(heart_pts(16, 16.5, 15), dark)
    c.poly(heart_pts(16, 16, 14), color)
    hl = Canvas(); hl.ellipse(7.5, 8, 12.5, 12.5, alpha(WHITE, 120))
    m = Canvas(); m.poly(heart_pts(16, 16, 14), WHITE)
    c.paste_clipped(hl, m)
    return c


def e_corazon(): return heart(rgb("DD2E44")).done()
def e_corazon_naranja(): return heart(rgb("F4900C")).done()
def e_corazon_amarillo(): return heart(rgb("FDCB58")).done()
def e_corazon_verde(): return heart(rgb("78B159")).done()
def e_corazon_azul(): return heart(rgb("5DADEC")).done()
def e_corazon_morado(): return heart(rgb("AA8ED6")).done()
def e_corazon_negro(): return heart(rgb("31373D"), rgb("15191C")).done()
def e_corazon_blanco(): return heart(rgb("F5F8FA"), rgb("A7B4C0")).done()


def e_corazon_roto():
    c = Canvas()
    full = Canvas()
    full.poly(heart_pts(16, 16.5, 15), rgb("A0041E"))
    full.poly(heart_pts(16, 16, 14), rgb("DD2E44"))
    left, right = Canvas(), Canvas()
    crack = [(16, 3), (13.5, 10), (18, 15), (14, 21), (16.5, 31)]
    left.poly([(0, 0)] + crack + [(0, 32)], WHITE)
    right.poly([(32, 0)] + crack + [(32, 32)], WHITE)
    l, r = Canvas(), Canvas()
    l.paste_clipped(full, left)
    r.paste_clipped(full, right)
    c.img.alpha_composite(l.img.rotate(6, center=(128, 250)), (-10, 0))
    c.img.alpha_composite(r.img.rotate(-6, center=(128, 250)), (10, 0))
    return c.done()


# ---------------------------------------------------------------- manos

SKIN, SKIN_D = HAND


def finger(c, x0, y0, x1, y1, w):
    c.line([(x0, y0), (x1, y1)], SKIN_D, w + 0.9)
    c.line([(x0, y0), (x1, y1)], SKIN, w)


def thumb_hand(c, up=True):
    c.rrect(9, 13, 25, 29, 4, SKIN_D)
    c.rrect(9.5, 13.4, 24.5, 28.4, 3.6, SKIN)
    for y in (17.5, 21.5, 25.5):
        c.line([(15, y), (24, y)], SKIN_D, 0.8)
    finger(c, 12.5, 15, 12.5, 4, 5.2)
    c.rrect(4, 13, 9.5, 29, 1.5, rgb("5DADEC"))


def e_pulgar_arriba():
    c = Canvas(); thumb_hand(c); return c.done()


def e_pulgar_abajo():
    c = Canvas(); thumb_hand(c); return c.done().rotate(180)


def e_ok():
    c = Canvas()
    c.rrect(10, 15, 25, 30, 5, SKIN_D); c.rrect(10.5, 15.4, 24.5, 29.4, 4.6, SKIN)
    for x, top in ((15.5, 3.5), (19.5, 4.5), (23.2, 7)):
        finger(c, x, 17, x + 0.4, top, 3.6)
    c.circle(9.5, 15, 5.6, None, SKIN_D, 3.6)
    c.circle(9.5, 15, 5.4, None, SKIN, 2.8)
    return c.done()


def open_palm(fingers=True):
    h = Canvas()
    h.rrect(10, 13, 22, 29, 5, SKIN_D); h.rrect(10.5, 13.4, 21.5, 28.4, 4.6, SKIN)
    if fingers:
        for x, top in ((11.8, 4.5), (14.8, 2.8), (17.8, 3.2), (20.6, 5.5)):
            finger(h, x, 15, x, top, 2.6)
    return h


def e_aplausos():
    c = Canvas()
    from PIL import Image
    for dx, rot in ((-4, 22), (4, -22)):
        h = open_palm()
        c.img.alpha_composite(h.img.rotate(rot, center=(128, 230), resample=Image.BICUBIC), (int(dx * 8), 8))
    for a in (-155, -125, -55, -25):
        t = math.radians(a)
        c.line([(16 + 11 * math.cos(t), 9 + 11 * math.sin(t)), (16 + 14.5 * math.cos(t), 9 + 14.5 * math.sin(t))],
               rgb("FFAC33"), 1.3)
    return c.done()


def e_rezar():
    c = Canvas()
    from PIL import Image
    for rot, shift in ((-9, -2.2), (9, 2.2)):
        h = Canvas()
        h.rrect(11.5, 4, 20.5, 28, 4.5, SKIN_D); h.rrect(12, 4.4, 20, 27.4, 4.1, SKIN)
        for y in (9, 12.5, 16):
            h.line([(12.5, y), (19.5, y)], SKIN_D, 0.6)
        c.img.alpha_composite(h.img.rotate(rot, center=(128, 230), resample=Image.BICUBIC), (int(shift * 8), 0))
    c.rrect(6, 26, 26, 31, 2, rgb("5DADEC"))
    for x, y in ((4, 7), (28, 7), (3, 14), (29, 14)):
        c.line([(x, y), (x + (1.5 if x < 16 else -1.5), y + 1.5)], rgb("FFCC4D"), 1.0)
    return c.done()


def e_saludo():
    c = Canvas()
    h = Canvas()
    h.rrect(9, 13, 24, 29, 6, SKIN_D); h.rrect(9.5, 13.4, 23.5, 28.4, 5.6, SKIN)
    for x, top in ((11, 4.5), (14.5, 2.5), (18, 3), (21.5, 5.5)):
        finger(h, x + 0.5, 16, x + 0.5, top, 3.3)
    finger(h, 10, 22, 4.5, 15.5, 3.6)
    c.img.alpha_composite(h.img.rotate(-15, center=(128, 200)))
    for i, y in enumerate((5, 9.5)):
        c.arc(23 + i * 2, y - 4, 31 + i * 2, y + 4, -60, 40, rgb("FFAC33"), 1.2)
    return c.done()


def e_musculo():
    c = Canvas()
    c.ellipse(2, 18, 24, 30, SKIN_D); c.ellipse(2.5, 18.4, 23.5, 29.2, SKIN)
    c.circle(12, 18.5, 7.2, SKIN_D); c.circle(12, 18.6, 6.5, SKIN)
    c.rrect(17, 7, 25.5, 27, 4, SKIN_D); c.rrect(17.5, 7.4, 25, 26.4, 3.7, SKIN)
    c.circle(21.5, 6.5, 5.5, SKIN_D); c.circle(21.5, 6.5, 4.8, SKIN)
    c.arc(7, 13, 17, 23, 200, 330, SKIN_D, 0.9)
    c.line([(18.5, 5), (24, 5)], SKIN_D, 0.7); c.line([(18.5, 7.8), (24, 7.8)], SKIN_D, 0.7)
    return c.done()


def e_ojos():
    c = Canvas()
    for x in (9.5, 22.5):
        c.ellipse(x - 6.5, 4, x + 6.5, 28, rgb("8899A6"))
        c.ellipse(x - 6, 4.4, x + 6, 27.4, WHITE)
        c.ellipse(x - 2, 10, x + 4.5, 21, rgb("292F33"))
        c.circle(x, 12.5, 1.2, WHITE)
    return c.done()


# ---------------------------------------------------------------- símbolos

def e_fuego():
    c = Canvas()
    outer = [(16, 1), (22, 9), (26.5, 15), (27, 22), (23, 29), (16, 31), (9, 29), (5, 22), (6, 14), (10, 17),
             (11, 10)]
    c.poly(outer, rgb("F4900C"))
    mid = [(16, 9), (20.5, 16), (22.5, 21.5), (20, 27.5), (16, 29), (12, 27.5), (9.5, 22), (11.5, 18), (13.5, 20),
           (14, 14)]
    c.poly(mid, rgb("FFCC4D"))
    c.poly([(16, 18), (18.8, 22.5), (18, 27), (16, 28), (14, 27), (13.4, 23.5)], rgb("FFE8B6"))
    return c.done()


def e_estrella():
    c = Canvas()
    c.poly(star_pts(16, 17, 15.5, 6.8), rgb("F4900C"))
    c.poly(star_pts(16, 16.5, 14, 6), rgb("FFCC4D"))
    c.poly(star_pts(16, 16.5, 6, 2.6), alpha(rgb("FFE58A"), 200))
    return c.done()


def sparkle(c, cx, cy, r, color):
    c.poly([(cx, cy - r), (cx + r * 0.22, cy - r * 0.22), (cx + r, cy), (cx + r * 0.22, cy + r * 0.22),
            (cx, cy + r), (cx - r * 0.22, cy + r * 0.22), (cx - r, cy), (cx - r * 0.22, cy - r * 0.22)], color)


def e_brillos():
    c = Canvas()
    sparkle(c, 13, 18, 12, rgb("FFAC33")); sparkle(c, 13, 18, 9, rgb("FFCC4D"))
    sparkle(c, 25, 7, 6, rgb("FFAC33")); sparkle(c, 25, 7, 4.3, rgb("FFCC4D"))
    sparkle(c, 26, 25, 4, rgb("FFCC4D"))
    return c.done()


def e_cien():
    c = Canvas()
    red = rgb("DD2E44")
    c.text(16, 13, "100", 15, red, 0.6, red)
    c.line([(3, 24), (29, 22)], red, 2.2)
    c.line([(5, 28.5), (28, 26.5)], red, 2.2)
    return c.done()


def badge(c, color, shape="rrect"):
    dark = mix(color, rgb("000000"), 0.25)
    if shape == "circle":
        c.circle(16, 16.5, 15, dark); c.circle(16, 16, 14.4, color)
    else:
        c.rrect(1.5, 2, 30.5, 31, 6, dark); c.rrect(1.5, 1.5, 30.5, 30, 6, color)


def e_check():
    c = Canvas(); badge(c, rgb("77B255"))
    c.line([(8, 16.5), (13.5, 22), (24, 10)], WHITE, 3.6); return c.done()


def e_cruz():
    c = Canvas()
    red = rgb("DD2E44")
    c.line([(6, 6), (26, 26)], red, 5.2); c.line([(26, 6), (6, 26)], red, 5.2); return c.done()


def e_alerta():
    c = Canvas()
    c.poly([(16, 2), (31, 29), (1, 29)], rgb("E8A92E"))
    c.poly([(16, 3.8), (29.2, 28), (2.8, 28)], rgb("FFCC4D"))
    c.line([(16, 11), (16, 20)], rgb("292F33"), 3); c.circle(16, 24.5, 1.8, rgb("292F33"))
    return c.done()


def e_pregunta():
    c = Canvas()
    red = rgb("DD2E44")
    c.arc(8.5, 2, 23.5, 17, 180, 400, red, 4.2)
    c.line([(18.5, 15), (16, 18), (16, 21)], red, 4.2)
    c.circle(16, 27.5, 2.6, red)
    return c.done()


def e_exclamacion():
    c = Canvas()
    red = rgb("DD2E44")
    c.poly([(12.5, 2), (19.5, 2), (18, 21), (14, 21)], red)
    c.circle(16, 2.5, 3.5, red)
    c.circle(16, 27, 3, red)
    return c.done()


def e_zzz():
    c = Canvas()
    blue = rgb("5DADEC"); dark = rgb("3B88C3")
    c.text(10, 21, "Z", 15, blue, 0.6, dark)
    c.text(21, 11, "Z", 10, blue, 0.5, dark)
    c.text(27.5, 4, "z", 7, blue, 0.4, dark)
    return c.done()


def e_musica():
    c = Canvas()
    col = rgb("5DADEC")
    c.ellipse(3, 21, 12, 28, col); c.ellipse(18, 18, 27, 25, col)
    c.line([(11, 24), (11, 6)], col, 2); c.line([(26, 21), (26, 3.5)], col, 2)
    c.poly([(10, 4.5), (27, 1.5), (27, 7), (10, 10)], col)
    return c.done()


def e_rayo():
    c = Canvas()
    c.poly([(19, 1), (6, 18), (14.5, 18), (11, 31), (26, 12), (17, 12), (21.5, 1)], rgb("F4900C"))
    c.poly([(18.6, 2.6), (7.6, 17), (15.8, 17), (12.8, 28), (24.2, 13), (15.8, 13), (20, 2.6)], rgb("FFCC4D"))
    return c.done()


def e_gg():
    c = Canvas(); badge(c, rgb("7C5BB5"))
    c.text(16, 15.3, "GG", 15, WHITE, 0.5, WHITE); return c.done()


def e_chat():
    c = Canvas()
    col, dark = rgb("C3CDD5"), rgb("8899A6")
    c.ellipse(1, 3, 31, 25, dark); c.ellipse(1.5, 3, 30.5, 24, col)
    c.poly([(6, 20), (4, 30), (14, 23)], col)
    for x in (9.5, 16, 22.5):
        c.circle(x, 13.5, 2, rgb("66757F"))
    return c.done()


def arrow_badge(rot):
    c = Canvas(); badge(c, rgb("3B88C3"))
    a = Canvas()
    a.poly([(16, 5), (26, 16), (19.5, 16), (19.5, 27), (12.5, 27), (12.5, 16), (6, 16)], WHITE)
    c.img.alpha_composite(a.img.rotate(rot, center=(128, 124), resample=__import__("PIL.Image", fromlist=["x"]).BICUBIC))
    return c.done()


def e_arriba(): return arrow_badge(0)
def e_abajo(): return arrow_badge(180)
def e_izquierda(): return arrow_badge(90)
def e_derecha(): return arrow_badge(-90)


def e_mas():
    c = Canvas(); badge(c, rgb("3B88C3"))
    c.line([(16, 8), (16, 24)], WHITE, 4); c.line([(8, 16), (24, 16)], WHITE, 4); return c.done()


def e_menos():
    c = Canvas(); badge(c, rgb("3B88C3"))
    c.line([(8, 16), (24, 16)], WHITE, 4); return c.done()


def keycap(txt, size=18):
    c = Canvas(); badge(c, rgb("3B88C3"))
    c.rrect(3.5, 3.5, 28.5, 27.5, 4.5, rgb("5DADEC"))
    c.text(16, 15.5, txt, size, WHITE, 0.4, WHITE)
    return c.done()


for _n in range(11):
    globals()["e_n%d" % _n] = (lambda t=str(_n): keycap(t, 18 if len(t) == 1 else 15))


def text_badge(txt, color, size=11):
    c = Canvas(); badge(c, color)
    c.text(16, 15.5, txt, size, WHITE, 0.4, WHITE)
    return c.done()


def e_new(): return text_badge("NEW", rgb("3B88C3"), 10)
def e_ok_simbolo(): return text_badge("OK", rgb("3B88C3"), 14)
def e_sos(): return text_badge("SOS", rgb("DD2E44"), 10.5)


def e_stop():
    c = Canvas()
    pts = [(16 + 15 * math.cos(math.radians(22.5 + 45 * i)), 16 + 15 * math.sin(math.radians(22.5 + 45 * i))) for i in range(8)]
    c.poly(pts, rgb("A0041E"))
    c.poly([(16 + (x - 16) * 0.92, 16 + (y - 16) * 0.92 - 0.3) for x, y in pts], rgb("DD2E44"))
    c.text(16, 15.5, "STOP", 8.2, WHITE, 0.3, WHITE)
    return c.done()


def e_prohibido():
    c = Canvas()
    red = rgb("DD2E44")
    c.circle(16, 16, 14.5, red); c.circle(16, 16, 10.8, WHITE)
    c.line([(8.5, 8.5), (23.5, 23.5)], red, 3.6, False)
    return c.done()


# ---------------------------------------------------------------- objetos

GOLD, GOLD_D, GOLD_L = rgb("FFCC4D"), rgb("E8A92E"), rgb("FFE58A")


def e_corona():
    c = Canvas()
    pts = [(3, 26), (2, 9), (9.5, 16), (16, 4), (22.5, 16), (30, 9), (29, 26)]
    c.poly(pts, GOLD_D)
    c.poly([(4, 25), (3.4, 11.5), (9.8, 17.6), (16, 6.2), (22.2, 17.6), (28.6, 11.5), (28, 25)], GOLD)
    c.rrect(3, 24, 29, 29, 1.5, GOLD_D)
    for x, col in ((9, "DD2E44"), (16, "5DADEC"), (23, "78B159")):
        c.circle(x, 26.5, 1.6, rgb(col))
    for x, y in ((2, 9), (16, 4), (30, 9)):
        c.circle(x, y, 2, GOLD_D)
    return c.done()


def e_trofeo():
    c = Canvas()
    c.arc(2, 5, 12, 17, 90, 270, GOLD_D, 2.2); c.arc(20, 5, 30, 17, 270, 450, GOLD_D, 2.2)
    c.chord(6, -6, 26, 20, 0, 180, GOLD_D)
    c.chord(6.6, -6, 25.4, 19.2, 0, 180, GOLD)
    c.rrect(14, 18, 18, 24, 0.5, GOLD_D)
    c.rrect(9, 24, 23, 30, 1.5, rgb("C1694F"))
    c.rrect(12, 25.5, 20, 28.5, 0.8, GOLD)
    c.ellipse(10, 2, 14, 10, alpha(GOLD_L, 200))
    return c.done()


def e_regalo():
    c = Canvas()
    box, box_d, rib = rgb("DD2E44"), rgb("A0041E"), rgb("FFCC4D")
    c.rrect(4, 14, 28, 30, 1.5, box_d); c.rrect(4.5, 14, 27.5, 29.2, 1.5, box)
    c.rrect(2.5, 9, 29.5, 15, 1.5, box_d); c.rrect(2.8, 9, 29.2, 14.2, 1.5, box)
    c.rrect(14, 9, 18, 30, 0, rib)
    c.ellipse(7, 2, 16.5, 10, rib); c.ellipse(15.5, 2, 25, 10, rib)
    c.ellipse(9.5, 4.2, 14.5, 8, box_d); c.ellipse(17.5, 4.2, 22.5, 8, box_d)
    return c.done()


def e_pastel():
    c = Canvas()
    c.rrect(3, 15, 29, 30, 2.5, rgb("C1694F"))
    c.rrect(3, 15, 29, 21, 2.5, rgb("F4ABBA"))
    for x in (6, 11, 16, 21, 26):
        c.circle(x, 21, 2.4, rgb("F4ABBA"))
    c.rrect(3, 25, 29, 26.5, 0, rgb("F4ABBA"))
    for x, col in ((9, "5DADEC"), (16, "AA8ED6"), (23, "78B159")):
        c.rrect(x - 1.2, 7, x + 1.2, 15, 0.5, rgb(col))
        c.ellipse(x - 1.5, 2, x + 1.5, 6.5, rgb("FFAC33"))
    return c.done()


def e_cafe():
    c = Canvas()
    c.arc(20, 13, 30, 23, 270, 450, rgb("C3CDD5"), 2.2)
    c.rrect(4, 11, 24, 29, 4, rgb("C3CDD5")); c.rrect(4.6, 11, 23.4, 28.2, 3.6, WHITE)
    c.ellipse(4.6, 9.5, 23.4, 14, rgb("8B4A2A"))
    c.ellipse(1, 27, 28, 31, rgb("C3CDD5"))
    for x in (10, 14.5, 19):
        c.arc(x - 2, 1, x + 2, 5, 90, 270, alpha(rgb("A7B4C0"), 200), 1.0)
        c.arc(x - 2, 4.5, x + 2, 8.5, 270, 450, alpha(rgb("A7B4C0"), 200), 1.0)
    return c.done()


def e_pizza():
    c = Canvas()
    c.poly([(3, 6), (29, 6), (16, 31)], rgb("FFCC4D"))
    c.rrect(2, 2, 30, 8.5, 3.2, rgb("D99E82"))
    c.rrect(2, 2, 30, 6, 2.5, rgb("E8B48E"))
    for x, y in ((11, 12), (20, 13), (15.5, 20), (18, 25.5)):
        c.circle(x, y, 2.4, rgb("DD2E44"))
    c.circle(8.5, 9.5, 1.2, rgb("78B159")); c.circle(23, 18, 1.2, rgb("78B159"))
    return c.done()


def e_confeti():
    c = Canvas()
    c.poly([(2, 30), (10, 9), (23, 22)], rgb("FFAC33"))
    c.poly([(2, 30), (6, 19.5), (12.5, 26)], rgb("F4900C"))
    c.line([(7.5, 15.5), (16.5, 24.5)], rgb("DD2E44"), 1.5, False)
    for x, y, col in ((15, 4, "5DADEC"), (21, 9, "DD2E44"), (27, 4, "78B159"), (26, 14, "AA8ED6"), (29, 21, "FFCC4D"),
                      (19, 2, "FFCC4D")):
        c.rrect(x - 1.2, y - 1.2, x + 1.2, y + 1.2, 0.4, rgb(col))
    c.arc(13, 6, 21, 14, 200, 330, rgb("AA8ED6"), 1.2)
    c.arc(20, 14, 28, 22, 180, 300, rgb("5DADEC"), 1.2)
    return c.done()


def e_camara():
    c = Canvas()
    body, dark = rgb("66757F"), rgb("31373D")
    c.rrect(10, 4, 20, 9, 1.5, dark)
    c.rrect(1.5, 7, 30.5, 28, 4, dark); c.rrect(2, 7.4, 30, 27.2, 3.6, body)
    c.circle(16, 17.5, 8, dark); c.circle(16, 17.5, 6.2, rgb("292F33")); c.circle(16, 17.5, 4, rgb("3B88C3"))
    c.circle(14.5, 16, 1.4, alpha(WHITE, 200))
    c.rrect(24, 10, 28, 12.5, 0.8, rgb("FFCC4D"))
    return c.done()


def e_bomba():
    c = Canvas()
    c.circle(14, 19, 12, rgb("292F33")); c.circle(14, 18.5, 11.2, rgb("31373D"))
    c.ellipse(7, 11, 13, 16, alpha(WHITE, 80))
    c.rrect(19, 6, 25, 11, 1, rgb("66757F"))
    c.arc(22, 0, 30, 9, 180, 300, rgb("C1694F"), 1.3)
    sparkle(c, 29, 2.5, 3, rgb("FFAC33"))
    return c.done()


def e_llave():
    c = Canvas()
    col, dark = GOLD, GOLD_D
    c.circle(9, 9, 7.5, dark); c.circle(9, 9, 6.7, col); c.circle(9, 9, 2.8, (0, 0, 0, 0))
    hole = Canvas(); hole.circle(9, 9, 2.8, WHITE)
    from PIL import ImageChops
    c.img.putalpha(ImageChops.subtract(c.img.getchannel("A"), hole.img.getchannel("A")))
    c.line([(13.5, 13.5), (28, 28)], dark, 4, False); c.line([(13.5, 13.5), (28, 28)], col, 3, False)
    c.line([(23, 23), (19.5, 26.5)], col, 2.6, False); c.line([(26.5, 26.5), (23.5, 29.5)], col, 2.6, False)
    return c.done()


def e_candado():
    c = Canvas()
    c.arc(8, 2, 24, 20, 180, 360, rgb("A7B4C0"), 3)
    c.line([(9.5, 11), (9.5, 15)], rgb("A7B4C0"), 3); c.line([(22.5, 11), (22.5, 15)], rgb("A7B4C0"), 3)
    c.rrect(4, 14, 28, 30, 3, GOLD_D); c.rrect(4.5, 14, 27.5, 29, 3, GOLD)
    c.circle(16, 20.5, 2.2, rgb("664500")); c.rrect(15, 21, 17, 25.5, 0.5, rgb("664500"))
    return c.done()


def e_campana():
    c = Canvas()
    c.circle(16, 3.5, 2.2, GOLD_D)
    c.poly([(16, 4), (24, 9), (25.5, 20), (29, 25), (3, 25), (6.5, 20), (8, 9)], GOLD_D)
    c.poly([(16, 5), (23.2, 9.5), (24.6, 20), (27.5, 24), (4.5, 24), (7.4, 20), (8.8, 9.5)], GOLD)
    c.ellipse(9, 8, 13, 17, alpha(GOLD_L, 200))
    c.circle(16, 27.5, 3, GOLD_D)
    return c.done()


def e_libro():
    c = Canvas()
    c.rrect(3, 4, 28, 29, 2.5, rgb("A0041E"))
    c.rrect(6, 5, 29, 27, 1.5, WHITE)
    c.rrect(3, 3, 26, 27, 2.5, rgb("DD2E44"))
    c.rrect(5, 3, 7.5, 27, 0, rgb("A0041E"))
    c.rrect(11, 8, 22, 13, 1, rgb("FFCC4D"))
    return c.done()


def e_lapiz():
    c = Canvas()
    p = Canvas()
    p.rrect(12, 2, 20, 6, 1.5, rgb("F4ABBA"))
    p.rrect(12, 5, 20, 8, 0, rgb("A7B4C0"))
    p.rrect(12, 8, 20, 24, 0, rgb("FFCC4D"))
    p.rrect(16, 8, 20, 24, 0, rgb("E8A92E"))
    p.poly([(12, 24), (20, 24), (16, 31)], rgb("E8B48E"))
    p.poly([(14.6, 28.6), (17.4, 28.6), (16, 31)], rgb("292F33"))
    from PIL import Image
    c.img.alpha_composite(p.img.rotate(-45, center=(128, 128), resample=Image.BICUBIC))
    return c.done()


def e_bombilla():
    c = Canvas()
    c.circle(16, 12.5, 10.5, rgb("E8A92E")); c.circle(16, 12, 9.8, rgb("FFE58A"))
    c.poly([(10, 18), (22, 18), (20.5, 23), (11.5, 23)], rgb("FFE58A"))
    c.arc(12.5, 9, 19.5, 16, 20, 160, rgb("F4900C"), 1.0)
    c.rrect(11, 22, 21, 28, 1.5, rgb("A7B4C0"))
    for y in (24, 26):
        c.line([(11.5, y), (20.5, y)], rgb("8899A6"), 0.7, False)
    c.rrect(13.5, 28, 18.5, 30.5, 1.2, rgb("66757F"))
    c.ellipse(10, 6, 14, 11, alpha(WHITE, 200))
    return c.done()


def e_reloj():
    c = Canvas()
    c.circle(16, 16, 14.5, rgb("8899A6")); c.circle(16, 16, 12.8, WHITE)
    for i in range(12):
        a = math.radians(i * 30)
        r0 = 10.5 if i % 3 else 9.5
        c.line([(16 + r0 * math.cos(a), 16 + r0 * math.sin(a)), (16 + 11.6 * math.cos(a), 16 + 11.6 * math.sin(a))],
               rgb("66757F"), 0.9, False)
    c.line([(16, 16), (16, 7.5)], rgb("292F33"), 1.6)
    c.line([(16, 16), (21.5, 19)], rgb("292F33"), 1.6)
    c.circle(16, 16, 1.3, RED)
    return c.done()


def e_bolsa_dinero():
    c = Canvas()
    bag, dark = rgb("C9A26B"), rgb("9A7848")
    c.poly([(12, 3), (20, 3), (18, 9), (14, 9)], dark)
    c.ellipse(3, 8, 29, 31, dark); c.ellipse(3.6, 8.5, 28.4, 30, bag)
    c.line([(12, 9.5), (20, 9.5)], rgb("664500"), 1.4)
    c.text(16, 19.5, "$", 14, rgb("3B9E46"), 0.5, rgb("2E6B33"))
    return c.done()


def e_moneda():
    c = Canvas()
    c.circle(16, 16, 14.5, GOLD_D); c.circle(16, 15.5, 13.6, GOLD)
    c.circle(16, 15.5, 10, GOLD_D); c.circle(16, 15.2, 9.3, GOLD)
    c.text(16, 15, "$", 13, GOLD_D, 0.4, GOLD_D)
    c.ellipse(7, 6, 13, 12, alpha(GOLD_L, 200))
    return c.done()


def e_escudo():
    c = Canvas()
    pts = [(16, 2), (28, 6), (27, 18), (16, 30.5), (5, 18), (4, 6)]
    c.poly(pts, rgb("2E6A99"))
    c.poly([(16, 3.6), (26.4, 7.2), (25.5, 17.6), (16, 28.6), (6.5, 17.6), (5.6, 7.2)], rgb("5DADEC"))
    c.poly([(16, 3.6), (26.4, 7.2), (25.5, 17.6), (16, 28.6)], rgb("3B88C3"))
    c.poly(star_pts(16, 15.5, 6, 2.6), rgb("FFCC4D"))
    return c.done()


def e_arco():
    c = Canvas()
    c.arc(-6, 3, 26, 29, 300, 60, rgb("8B4636"), 2.8)
    c.line([(18, 4.5), (18, 27.5)], rgb("C3CDD5"), 0.8, False)
    c.line([(4, 16), (27, 16)], rgb("A7B4C0"), 1.2)
    c.poly([(30, 16), (25.5, 13), (25.5, 19)], rgb("8899A6"))
    c.poly([(4, 16), (1.5, 13), (6.5, 14.5)], rgb("DD2E44")); c.poly([(4, 16), (1.5, 19), (6.5, 17.5)], rgb("DD2E44"))
    return c.done()


def e_flecha():
    c = Canvas()
    c.line([(5, 27), (25, 7)], rgb("C1694F"), 2.2)
    c.poly([(29, 3), (20.5, 6.5), (25.5, 11.5)], rgb("8899A6"))
    c.poly([(5, 27), (2, 23.5), (7.5, 22.5)], rgb("DD2E44"))
    c.poly([(5, 27), (8.5, 30), (9.5, 24.5)], rgb("DD2E44"))
    return c.done()


def e_pocion():
    c = Canvas()
    glass = rgb("C6E8FF", 160)
    c.rrect(12.5, 2, 19.5, 6, 1, rgb("C1694F"))
    c.rrect(13, 5.5, 19, 12, 0.8, glass)
    c.circle(16, 20, 10.5, rgb("8899A6")); c.circle(16, 20, 9.8, glass)
    liquid = Canvas(); liquid.rrect(0, 17, 32, 32, 0, rgb("E040A0"))
    m = Canvas(); m.circle(16, 20, 9.6, WHITE)
    c.paste_clipped(liquid, m)
    c.circle(12.5, 21, 1.3, alpha(WHITE, 170)); c.circle(18, 25, 0.9, alpha(WHITE, 170))
    c.ellipse(9.5, 12.5, 13.5, 17, alpha(WHITE, 200))
    return c.done()


def e_mapa():
    c = Canvas()
    paper, d1 = rgb("F5E6C8"), rgb("D9C49B")
    c.poly([(2, 6), (11, 3), (21, 6), (30, 3), (30, 26), (21, 29), (11, 26), (2, 29)], d1)
    c.poly([(2.8, 6.6), (11, 3.8), (11, 25.4), (2.8, 28)], paper)
    c.poly([(11, 3.8), (21, 6.6), (21, 28.2), (11, 25.4)], mix(paper, d1, 0.4))
    c.poly([(21, 6.6), (29.2, 3.8), (29.2, 25.6), (21, 28.2)], paper)
    c.line([(5, 22), (10, 17), (15, 19), (20, 12), (25, 14)], rgb("DD2E44"), 1.0)
    c.line([(24, 9), (27, 12)], rgb("DD2E44"), 1.3); c.line([(27, 9), (24, 12)], rgb("DD2E44"), 1.3)
    return c.done()


def e_brujula():
    c = Canvas()
    c.circle(16, 16, 14.5, GOLD_D); c.circle(16, 16, 12.8, rgb("F5F8FA"))
    c.poly([(16, 4.5), (19, 16), (13, 16)], RED)
    c.poly([(16, 27.5), (19, 16), (13, 16)], rgb("8899A6"))
    c.circle(16, 16, 1.6, rgb("292F33"))
    return c.done()


def e_antorcha():
    c = Canvas()
    c.rrect(13.5, 13, 18.5, 31, 1, rgb("8B5A2B")); c.rrect(16, 13, 18.5, 31, 0, rgb("6B4420"))
    c.poly([(16, 1), (21.5, 8), (21, 13.5), (16, 15), (11, 13.5), (10.5, 8)], rgb("F4900C"))
    c.poly([(16, 5), (19, 9.5), (18.5, 13), (16, 14), (13.5, 13), (13, 9.5)], rgb("FFCC4D"))
    return c.done()


HD = {name[2:]: fn for name, fn in list(globals().items()) if name.startswith("e_") and callable(fn)}
