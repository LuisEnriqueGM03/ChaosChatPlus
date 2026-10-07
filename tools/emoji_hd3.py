"""Emojis HD (32x32, estilo liso): Minecraft, animales, comida y naturaleza. Ver emoji_hd.py."""
import math

from PIL import Image

from emoji_hd import Canvas, INK, RED, TEAR, WHITE, alpha, mix, rgb, star_pts

# ---------------------------------------------------------------- utilidades


def gem(c, cx, cy, r, base, light, dark):
    """Gema facetada (diamante/esmeralda)."""
    top = [(cx - r, cy - r * 0.25), (cx - r * 0.5, cy - r * 0.8), (cx + r * 0.5, cy - r * 0.8), (cx + r, cy - r * 0.25)]
    bottom = (cx, cy + r)
    c.poly(top + [bottom], dark)
    c.poly([(cx - r * 0.9, cy - r * 0.25), (cx - r * 0.45, cy - r * 0.72), (cx + r * 0.45, cy - r * 0.72),
            (cx + r * 0.9, cy - r * 0.25), (cx, cy + r * 0.88)], base)
    c.poly([(cx - r * 0.9, cy - r * 0.25), (cx + r * 0.9, cy - r * 0.25), (cx + r * 0.45, cy - r * 0.72),
            (cx - r * 0.45, cy - r * 0.72)], light)
    c.poly([(cx - r * 0.35, cy - r * 0.25), (cx + r * 0.35, cy - r * 0.25), (cx, cy + r * 0.88)], mix(base, light, 0.45))
    c.line([(cx - r * 0.45, cy - r * 0.6), (cx - r * 0.15, cy - r * 0.6)], alpha(WHITE, 220), r * 0.12)


def block(c, top, side, front, x0=4, y0=4, s=24, depth=5):
    """Bloque en perspectiva sencilla: cara frontal + tapa + lateral."""
    c.poly([(x0, y0 + depth), (x0 + depth, y0), (x0 + s + depth, y0), (x0 + s, y0 + depth)], top)
    c.poly([(x0 + s, y0 + depth), (x0 + s + depth, y0), (x0 + s + depth, y0 + s), (x0 + s, y0 + s + depth)], side)
    c.rrect(x0, y0 + depth, x0 + s, y0 + s + depth, 0.6, front)
    return (x0, y0 + depth, x0 + s, y0 + s + depth)


def rot(c, layer, angle):
    c.img.alpha_composite(layer.img.rotate(angle, center=(128, 128), resample=Image.BICUBIC))


# ---------------------------------------------------------------- Minecraft

def e_diamante():
    c = Canvas(); gem(c, 16, 17, 14, rgb("4FD8E8"), rgb("A8F0F7"), rgb("1B9AAA")); return c.done()


def e_esmeralda():
    c = Canvas()
    c.poly([(16, 1), (26, 8), (26, 24), (16, 31), (6, 24), (6, 8)], rgb("17904A"))
    c.poly([(16, 2.6), (24.6, 8.6), (24.6, 23.4), (16, 29.4), (7.4, 23.4), (7.4, 8.6)], rgb("41CD6E"))
    c.poly([(16, 7), (20.5, 10.5), (20.5, 21.5), (16, 25), (11.5, 21.5), (11.5, 10.5)], rgb("7EE8A0"))
    c.line([(10, 9), (13, 7)], alpha(WHITE, 220), 1.2)
    return c.done()


def ingot(c, light, base, dark):
    c.poly([(2, 22), (8, 12), (30, 12), (24, 22)], dark)
    c.poly([(3.5, 21), (8.8, 13), (28.5, 13), (23.2, 21)], base)
    c.poly([(8, 12), (11, 7), (27, 7), (30, 12)], light)
    c.poly([(2, 22), (24, 22), (24, 26), (2, 26)], mix(dark, rgb("000000"), 0.15))
    c.poly([(24, 22), (30, 12), (30, 16), (24, 26)], dark)
    c.line([(11, 9), (17, 9)], alpha(WHITE, 200), 1)


def e_oro():
    c = Canvas(); ingot(c, rgb("FFF0A0"), rgb("FFCC33"), rgb("D99A00")); return c.done()


def e_hierro():
    c = Canvas(); ingot(c, rgb("FFFFFF"), rgb("D8DEE4"), rgb("9AA5AF")); return c.done()


def e_netherita():
    c = Canvas(); ingot(c, rgb("6E6470"), rgb("4A414C"), rgb("2B252C")); return c.done()


def e_espada():
    c = Canvas()
    s = Canvas()
    s.poly([(14, 3), (18, 3), (18, 20), (16, 22), (14, 20)], rgb("4FD8E8"))
    s.poly([(16, 3), (18, 3), (18, 20), (16, 22)], rgb("2BB3C4"))
    s.poly([(16, 1), (18, 3), (14, 3)], rgb("A8F0F7"))
    s.rrect(10, 20, 22, 23, 1, rgb("8B5A2B"))
    s.rrect(14.5, 22.5, 17.5, 29, 0.8, rgb("6B4420"))
    s.circle(16, 30, 1.8, rgb("8B5A2B"))
    rot(c, s, -45)
    return c.done()


def e_pico():
    c = Canvas()
    p = Canvas()
    p.rrect(14.5, 8, 17.5, 31, 0.8, rgb("8B5A2B"))
    p.poly([(3, 12), (8, 6), (16, 4), (24, 6), (29, 12), (24, 9.5), (16, 8.5), (8, 9.5)], rgb("1B9AAA"))
    p.poly([(4.5, 11), (8.5, 6.8), (16, 5), (23.5, 6.8), (27.5, 11), (23.5, 8.6), (16, 7.6), (8.5, 8.6)], rgb("4FD8E8"))
    rot(c, p, -45)
    return c.done()


def e_tnt():
    c = Canvas()
    x0, y0, x1, y1 = block(c, rgb("D9D9D9"), rgb("A33224"), rgb("DB4433"))
    c.rrect(x0, y0 + 8, x1, y0 + 16, 0, rgb("F2F2F2"))
    c.text((x0 + x1) / 2, y0 + 12, "TNT", 7.2, rgb("292F33"), 0.25, rgb("292F33"))
    for i in range(1, 6):
        x = x0 + i * (x1 - x0) / 6
        c.line([(x, y0 + 1), (x, y0 + 7)], rgb("B5372A"), 0.6, False)
        c.line([(x, y0 + 17), (x, y1 - 1)], rgb("B5372A"), 0.6, False)
    c.line([(18, 4), (20, 0.5)], rgb("66757F"), 0.8)
    return c.done()


def mob_head(c, base, dark, light=None):
    c.rrect(3, 3, 29, 29, 4, dark)
    c.rrect(3.5, 3, 28.5, 28, 3.6, base)
    if light:
        c.rrect(5, 4.5, 15, 8, 1.5, alpha(light, 150))


def e_creeper():
    c = Canvas()
    mob_head(c, rgb("5DBB46"), rgb("3B8A2C"), rgb("9BE08A"))
    for x, y in ((8, 8), (20, 8), (11, 22), (22, 18)):
        c.rrect(x - 1.5, y - 1.5, x + 1.5, y + 1.5, 0.4, alpha(rgb("4AA336"), 200))
    dark = rgb("1E2A1A")
    c.rrect(7, 10, 13, 16, 0.8, dark); c.rrect(19, 10, 25, 16, 0.8, dark)
    c.rrect(13, 16, 19, 22, 0.6, dark)
    c.rrect(10, 19, 13.5, 26, 0.6, dark); c.rrect(18.5, 19, 22, 26, 0.6, dark)
    return c.done()


def e_zombie():
    c = Canvas()
    mob_head(c, rgb("5FA05A"), rgb("3E7A3A"))
    c.rrect(3.5, 3, 28.5, 9, 3.6, rgb("2E6B33"))
    c.rrect(7.5, 13, 13, 17, 0.6, rgb("2E4A80")); c.rrect(19, 13, 24.5, 17, 0.6, rgb("2E4A80"))
    c.rrect(10, 13, 13, 17, 0.4, rgb("1E1E1E")); c.rrect(19, 13, 22, 17, 0.4, rgb("1E1E1E"))
    c.rrect(12, 19.5, 20, 22, 0.6, rgb("2A4A26"))
    return c.done()


def e_esqueleto():
    c = Canvas()
    mob_head(c, rgb("D7D7D7"), rgb("9E9E9E"), WHITE)
    c.rrect(7.5, 11, 13.5, 17, 0.8, rgb("3A3A3A")); c.rrect(18.5, 11, 24.5, 17, 0.8, rgb("3A3A3A"))
    c.rrect(14.5, 17.5, 17.5, 20.5, 0.5, rgb("5A5A5A"))
    c.rrect(8, 22.5, 24, 25, 0.6, rgb("3A3A3A"))
    for x in (11, 14, 17, 20):
        c.line([(x + 0.5, 22.4), (x + 0.5, 25.1)], rgb("D7D7D7"), 0.7, False)
    return c.done()


def e_enderman():
    c = Canvas()
    mob_head(c, rgb("1B1B1F"), rgb("060608"))
    c.rrect(5, 15, 14, 18.5, 0.6, rgb("E58CF2")); c.rrect(18, 15, 27, 18.5, 0.6, rgb("E58CF2"))
    c.rrect(8, 15, 11, 18.5, 0.4, rgb("CC33E0")); c.rrect(21, 15, 24, 18.5, 0.4, rgb("CC33E0"))
    c.ellipse(4, 12.5, 15, 21, alpha(rgb("E58CF2"), 60)); c.ellipse(17, 12.5, 28, 21, alpha(rgb("E58CF2"), 60))
    return c.done()


def e_slime():
    c = Canvas()
    c.rrect(2, 4, 30, 30, 6, alpha(rgb("4BAA3A"), 255))
    c.rrect(2.6, 4, 29.4, 29, 5.6, alpha(rgb("7CD36A"), 230))
    c.rrect(8, 10, 24, 25, 3, alpha(rgb("5DBB46"), 220))
    c.rrect(9.5, 12, 13.5, 16, 0.6, rgb("1E3A1A")); c.rrect(18.5, 12, 22.5, 16, 0.6, rgb("1E3A1A"))
    c.rrect(15, 20, 17.5, 22, 0.4, rgb("1E3A1A"))
    c.ellipse(5, 6, 13, 9.5, alpha(WHITE, 140))
    return c.done()


def e_manzana():
    c = Canvas()
    c.circle(10.5, 19, 9.5, rgb("A0041E")); c.circle(21.5, 19, 9.5, rgb("A0041E"))
    c.circle(10.5, 18.5, 8.9, RED); c.circle(21.5, 18.5, 8.9, RED)
    c.ellipse(6, 13, 11, 19, alpha(WHITE, 140))
    c.line([(16, 11), (17, 4)], rgb("6B4420"), 1.6)
    c.ellipse(17.5, 3, 25, 8, rgb("78B159"))
    return c.done()


def e_manzana_dorada():
    c = Canvas()
    c.circle(10.5, 19, 9.5, rgb("C98A00")); c.circle(21.5, 19, 9.5, rgb("C98A00"))
    c.circle(10.5, 18.5, 8.9, rgb("FFCC33")); c.circle(21.5, 18.5, 8.9, rgb("FFCC33"))
    c.ellipse(6, 13, 11, 19, alpha(WHITE, 170))
    c.line([(16, 11), (17, 4)], rgb("6B4420"), 1.6)
    c.ellipse(17.5, 3, 25, 8, rgb("78B159"))
    for x, y in ((27, 26), (4, 9), (26, 12)):
        c.poly(star_pts(x, y, 2.6, 0.8, 4, 0), WHITE)
    return c.done()


def e_cama():
    c = Canvas()
    c.rrect(2, 18, 30, 25, 1.5, rgb("DD2E44"))
    c.rrect(2, 14, 11, 20, 2, WHITE)
    c.rrect(10, 15.5, 30, 20, 1.5, rgb("F15F6F"))
    c.rrect(2, 24, 30, 26.5, 0.5, rgb("C1694F"))
    c.rrect(2, 26, 5, 30, 0.6, rgb("8B4636")); c.rrect(27, 26, 30, 30, 0.6, rgb("8B4636"))
    return c.done()


def e_portal():
    c = Canvas()
    c.rrect(4, 1, 28, 31, 1.5, rgb("1B1324"))
    c.rrect(8, 5, 24, 27, 1, rgb("7B2FD6"))
    for i in range(5):
        y = 7 + i * 4.2
        c.arc(9, y - 2, 23, y + 3, 200, 340, alpha(rgb("C78BFF"), 220), 1.1)
    c.ellipse(9, 6, 15, 12, alpha(rgb("E2C3FF"), 140))
    return c.done()


def e_perla():
    c = Canvas()
    c.circle(16, 16, 12, rgb("0E5E56")); c.circle(16, 16, 11.2, rgb("1E8C7E"))
    c.circle(16, 16, 6.5, rgb("2BB3A1")); c.circle(16, 16, 3, rgb("0B3D38"))
    c.ellipse(9, 8, 15, 13, alpha(WHITE, 170))
    return c.done()


def e_ojo_ender():
    c = Canvas()
    c.circle(16, 16, 12.5, rgb("0E5E56")); c.circle(16, 16, 11.6, rgb("2BB3A1"))
    c.ellipse(8, 10.5, 24, 21.5, rgb("C9F2C0"))
    c.ellipse(13, 10.5, 19, 21.5, rgb("1E8C7E"))
    c.ellipse(14.6, 12, 17.4, 20, rgb("0B2E2A"))
    c.ellipse(9, 6.5, 14, 10.5, alpha(WHITE, 160))
    return c.done()


def e_estrella_nether():
    c = Canvas()
    c.poly(star_pts(16, 16.5, 15, 5.5, 4, -90), rgb("B8C9E6"))
    c.poly(star_pts(16, 16, 13.5, 4.6, 4, -90), rgb("F0F6FF"))
    c.poly(star_pts(16, 16, 8, 2.8, 4, -45), alpha(rgb("FFF7C2"), 220))
    c.circle(16, 16, 2.6, WHITE)
    return c.done()


def e_totem():
    c = Canvas()
    gold, dark = rgb("FFCC33"), rgb("C98A00")
    c.rrect(9, 2, 23, 14, 3, dark); c.rrect(9.5, 2.3, 22.5, 13.3, 2.6, gold)
    c.rrect(11.5, 6, 14.5, 9, 0.5, rgb("1B9AAA")); c.rrect(17.5, 6, 20.5, 9, 0.5, rgb("1B9AAA"))
    c.rrect(14, 10.5, 18, 12, 0.5, dark)
    c.poly([(3, 15), (29, 15), (25, 19), (7, 19)], rgb("78B159"))
    c.rrect(11, 14, 21, 29, 2, dark); c.rrect(11.5, 14.3, 20.5, 28.3, 1.8, gold)
    c.rrect(14, 18, 18, 22, 0.6, rgb("2BB3A1"))
    c.ellipse(10.5, 3.5, 14.5, 6, alpha(WHITE, 170))
    return c.done()


def e_cubo():
    c = Canvas()
    c.arc(6, 1, 26, 17, 180, 360, rgb("8899A6"), 1.6)
    c.poly([(4, 9), (28, 9), (25, 29), (7, 29)], rgb("8899A6"))
    c.poly([(5, 9.5), (27, 9.5), (24.2, 28.2), (7.8, 28.2)], rgb("C3CDD5"))
    c.ellipse(4, 6.5, 28, 12, rgb("8899A6"))
    c.ellipse(5.5, 7.4, 26.5, 11.2, rgb("5DADEC"))
    c.line([(8, 14), (9.5, 25)], alpha(WHITE, 160), 1.2)
    return c.done()


def e_cofre():
    c = Canvas()
    wood, dark, light = rgb("B97A3A"), rgb("6B4420"), rgb("D9A066")
    c.rrect(2, 6, 30, 29, 2, dark)
    c.rrect(3, 7, 29, 15, 1.5, light)
    c.rrect(3, 16, 29, 28, 1.5, wood)
    c.line([(3, 15.5), (29, 15.5)], dark, 1.2, False)
    c.rrect(14, 12, 18, 19, 0.8, rgb("C3CDD5")); c.rrect(15.2, 15, 16.8, 17.5, 0.4, rgb("66757F"))
    return c.done()


def e_mesa_crafteo():
    c = Canvas()
    x0, y0, x1, y1 = block(c, rgb("C99A5B"), rgb("8B5A2B"), rgb("B07A3E"))
    for i in (1, 2):
        c.line([(x0, y0 + i * 8), (x1, y0 + i * 8)], rgb("8B5A2B"), 0.8, False)
    c.poly([(9, 1), (13.5, 2.5), (11, 4.5)], rgb("8899A6"))
    c.line([(x0 + 4, y0 + 12), (x0 + 10, y0 + 6)], rgb("6B4420"), 1.6)
    c.rrect(x0 + 13, y0 + 4, x0 + 20, y0 + 7, 0.6, rgb("8899A6"))
    c.line([(x0 + 16.5, y0 + 7), (x0 + 16.5, y0 + 14)], rgb("6B4420"), 1.5)
    return c.done()


def e_cesped():
    c = Canvas()
    x0, y0, x1, y1 = block(c, rgb("7DC160"), rgb("6B4A2E"), rgb("8B5E3C"))
    c.rrect(x0, y0, x1, y0 + 5, 0.6, rgb("5DA642"))
    for x in range(int(x0) + 2, int(x1), 4):
        c.poly([(x, y0 + 5), (x + 2, y0 + 5), (x + 1, y0 + 8)], rgb("5DA642"))
    for x, y in ((9, 21), (17, 25), (22, 20), (12, 27)):
        c.rrect(x - 1, y - 1, x + 1, y + 1, 0.3, rgb("6B4A2E"))
    return c.done()


def e_obsidiana():
    c = Canvas()
    x0, y0, x1, y1 = block(c, rgb("3B2A55"), rgb("120C1C"), rgb("1F1530"))
    for x, y, w in ((8, 14, 4), (17, 19, 5), (11, 25, 3), (21, 13, 3)):
        c.line([(x, y), (x + w, y - 1)], alpha(rgb("7B4FB8"), 220), 0.9)
    return c.done()


def e_lava():
    c = Canvas()
    x0, y0, x1, y1 = block(c, rgb("FFAA33"), rgb("C24A0A"), rgb("F06A10"))
    for x, y, r in ((10, 16, 2.6), (20, 22, 3), (14, 26, 1.8), (22, 13, 1.8)):
        c.circle(x, y, r, rgb("FFCC4D"))
    return c.done()


def e_agua():
    c = Canvas()
    x0, y0, x1, y1 = block(c, rgb("6FB6FF"), rgb("1F5FB0"), rgb("3B86E0"))
    for y in (14, 20, 26):
        c.line([(x0 + 3, y), (x0 + 8, y - 1.5), (x0 + 13, y), (x0 + 18, y - 1.5), (x1 - 2, y)], alpha(rgb("A9D4FF"), 220), 0.9)
    return c.done()


def e_redstone():
    c = Canvas()
    pts = [(16, 3), (24, 8), (29, 16), (24, 25), (16, 29), (8, 25), (3, 16), (8, 8)]
    c.poly(pts, rgb("7A0000"))
    c.poly([(16 + (x - 16) * 0.85, 16 + (y - 16) * 0.85) for x, y in pts], rgb("D51A1A"))
    for x, y in ((12, 11), (20, 14), (14, 20), (21, 22)):
        c.circle(x, y, 2, rgb("FF5A4A"))
    c.ellipse(9, 7, 14, 10, alpha(WHITE, 140))
    return c.done()


def e_orbe_xp():
    c = Canvas()
    c.circle(16, 16, 13, alpha(rgb("C8FF6E"), 90))
    c.circle(16, 16, 10.5, rgb("5DA00A")); c.circle(16, 16, 9.6, rgb("B7F04A"))
    c.circle(16, 16, 5.5, rgb("F2FF9E"))
    c.ellipse(10, 9, 15, 13, alpha(WHITE, 190))
    return c.done()


def e_spawner():
    c = Canvas()
    x0, y0, x1, y1 = block(c, rgb("3D4A57"), rgb("16202A"), rgb("22303C"))
    c.circle((x0 + x1) / 2, (y0 + y1) / 2, 6.5, alpha(rgb("FF8A3D"), 160))
    c.circle((x0 + x1) / 2, (y0 + y1) / 2, 3.5, alpha(rgb("FFCC4D"), 220))
    for i in range(1, 4):
        x = x0 + i * (x1 - x0) / 4
        y = y0 + i * (y1 - y0) / 4
        c.line([(x, y0), (x, y1)], rgb("4B5866"), 1.0, False)
        c.line([(x0, y), (x1, y)], rgb("4B5866"), 1.0, False)
    return c.done()


# ---------------------------------------------------------------- animales

def animal_face(c, fur, dark, cx=16, cy=17, r=13):
    c.circle(cx, cy, r, dark); c.circle(cx, cy - 0.4, r - 0.8, fur)


def dot_eyes(c, y=15, dx=5, r=1.8, color=INK):
    for x in (16 - dx, 16 + dx):
        c.circle(x, y, r, color)
        c.circle(x - r * 0.35, y - r * 0.35, r * 0.35, WHITE)


def e_gato():
    c = Canvas()
    fur, dark, inner = rgb("F4A040"), rgb("C77420"), rgb("FFB3B3")
    c.poly([(4, 16), (5, 2), (14, 9)], dark); c.poly([(28, 16), (27, 2), (18, 9)], dark)
    c.poly([(6, 13), (6.4, 5.5), (11.5, 9.5)], inner); c.poly([(26, 13), (25.6, 5.5), (20.5, 9.5)], inner)
    animal_face(c, fur, dark, cy=18, r=12.5)
    for x in (10, 16, 22):
        c.line([(x, 6.5), (x, 9.5)], dark, 1.2)
    dot_eyes(c, 16.5, 5)
    c.poly([(14.6, 20.5), (17.4, 20.5), (16, 22)], rgb("E06A6A"))
    for s in (-1, 1):
        c.line([(16 + s * 5, 22.5), (16 + s * 12.5, 21.5)], alpha(INK, 150), 0.6)
        c.line([(16 + s * 5, 23.5), (16 + s * 12, 25)], alpha(INK, 150), 0.6)
    return c.done()


def e_perro():
    c = Canvas()
    fur, dark, ear = rgb("D9A066"), rgb("A87038"), rgb("8B5A2B")
    animal_face(c, fur, dark, cy=17, r=12.5)
    c.ellipse(1, 6, 9, 22, ear); c.ellipse(23, 6, 31, 22, ear)
    c.ellipse(9, 17, 23, 28, rgb("F2D2A9"))
    dot_eyes(c, 14, 5)
    c.ellipse(13, 17.5, 19, 21.5, INK)
    c.ellipse(14.5, 22.5, 17.5, 27, TONGUE := rgb("E8596E"))
    c.line([(16, 21.5), (16, 23)], INK, 0.8)
    return c.done()


def e_cerdo():
    c = Canvas()
    pink, dark = rgb("F7A8B8"), rgb("D9788E")
    c.poly([(4, 12), (6, 3), (12, 7)], dark); c.poly([(28, 12), (26, 3), (20, 7)], dark)
    animal_face(c, pink, dark, cy=17)
    c.ellipse(10, 17, 22, 25, rgb("F28BA0"))
    c.ellipse(12.5, 19.5, 14.8, 22.5, rgb("B5526A")); c.ellipse(17.2, 19.5, 19.5, 22.5, rgb("B5526A"))
    dot_eyes(c, 13, 6)
    return c.done()


def e_vaca():
    c = Canvas()
    c.poly([(3, 10), (2, 3), (9, 7)], rgb("D9C49B")); c.poly([(29, 10), (30, 3), (23, 7)], rgb("D9C49B"))
    c.ellipse(0, 9, 8, 15, rgb("2B2B2B")); c.ellipse(24, 9, 32, 15, rgb("2B2B2B"))
    animal_face(c, WHITE, rgb("A7B4C0"), cy=16, r=12.5)
    c.ellipse(5, 6, 14, 14, rgb("2B2B2B")); c.ellipse(20, 15, 27, 20, rgb("2B2B2B"))
    c.rrect(8, 19, 24, 29, 5, rgb("F4ABBA"))
    c.ellipse(11.5, 22.5, 14, 25.5, rgb("B5526A")); c.ellipse(18, 22.5, 20.5, 25.5, rgb("B5526A"))
    dot_eyes(c, 14, 5.5)
    return c.done()


def e_gallina():
    c = Canvas()
    c.circle(13, 5, 2.6, RED); c.circle(16.5, 3.8, 2.8, RED); c.circle(20, 5, 2.6, RED)
    animal_face(c, WHITE, rgb("C3CDD5"), cy=17)
    c.poly([(12.5, 16.5), (19.5, 16.5), (16, 21.5)], rgb("FFAC33"))
    c.poly([(14, 20), (18, 20), (16, 25.5)], RED)
    dot_eyes(c, 13, 5.5)
    return c.done()


def e_rana():
    c = Canvas()
    g, d = rgb("78C257"), rgb("4E9A35")
    c.circle(8.5, 9, 6, d); c.circle(23.5, 9, 6, d)
    c.circle(8.5, 9, 5.3, g); c.circle(23.5, 9, 5.3, g)
    c.ellipse(1.5, 9, 30.5, 30, d); c.ellipse(2.2, 9, 29.8, 29.2, g)
    for x in (8.5, 23.5):
        c.circle(x, 9, 3.4, WHITE); c.circle(x, 9.4, 1.8, INK)
    c.arc(7, 13, 25, 25, 20, 160, rgb("2E6B22"), 1.4)
    c.ellipse(6, 19, 10, 21.5, alpha(rgb("FF8A8A"), 150)); c.ellipse(22, 19, 26, 21.5, alpha(rgb("FF8A8A"), 150))
    return c.done()


def e_abeja():
    c = Canvas()
    c.ellipse(8, 2, 18, 13, alpha(rgb("DDEFFF"), 220)); c.ellipse(15, 1, 26, 12, alpha(rgb("DDEFFF"), 220))
    c.ellipse(3, 9, 29, 29, rgb("C98A00"))
    c.ellipse(3.6, 9.4, 28.4, 28.2, rgb("FFCC33"))
    body = Canvas()
    for x in (10, 17):
        body.rrect(x, 0, x + 3.5, 32, 0, rgb("292F33"))
    m = Canvas(); m.ellipse(3.6, 9.4, 28.4, 28.2, WHITE)
    c.paste_clipped(body, m)
    c.poly([(29, 19), (32, 18), (29, 21)], rgb("292F33"))
    c.circle(7.5, 17, 1.6, INK); c.circle(7, 16.5, 0.5, WHITE)
    return c.done()


def e_pez():
    c = Canvas()
    body, dark = rgb("5DADEC"), rgb("3B88C3")
    c.poly([(23, 16), (31, 7), (31, 25)], dark)
    c.ellipse(2, 7, 26, 25, dark); c.ellipse(2.6, 7.4, 25.4, 24.2, body)
    c.poly([(11, 8), (17, 3), (19, 9)], dark)
    c.arc(13, 9, 21, 23, 300, 60, alpha(dark, 200), 1)
    c.circle(8, 14, 2.2, WHITE); c.circle(8, 14.3, 1.2, INK)
    c.ellipse(5, 19, 13, 21, alpha(WHITE, 90))
    return c.done()


def e_oso():
    c = Canvas()
    fur, dark, snout = rgb("A0663B"), rgb("6E4220"), rgb("D9A066")
    c.circle(6.5, 7, 5, dark); c.circle(25.5, 7, 5, dark)
    c.circle(6.5, 7, 2.6, rgb("C88A5A")); c.circle(25.5, 7, 2.6, rgb("C88A5A"))
    animal_face(c, fur, dark, cy=17.5)
    c.ellipse(10, 17, 22, 27, snout)
    c.ellipse(13.5, 17.5, 18.5, 21, INK)
    c.arc(12.5, 20, 16, 24.5, 20, 160, INK, 0.9); c.arc(16, 20, 19.5, 24.5, 20, 160, INK, 0.9)
    dot_eyes(c, 14, 5.5)
    return c.done()


def e_zorro():
    c = Canvas()
    orange, dark = rgb("F4782A"), rgb("C04F12")
    c.poly([(2, 15), (4, 1), (13, 8)], dark); c.poly([(30, 15), (28, 1), (19, 8)], dark)
    c.poly([(4.5, 12), (5, 4.5), (10.5, 8.5)], rgb("292F33")); c.poly([(27.5, 12), (27, 4.5), (21.5, 8.5)], rgb("292F33"))
    c.poly([(2, 12), (16, 6), (30, 12), (26, 23), (16, 30), (6, 23)], dark)
    c.poly([(3.2, 12.4), (16, 7), (28.8, 12.4), (25.2, 22.4), (16, 29), (6.8, 22.4)], orange)
    c.poly([(6, 18), (16, 21), (26, 18), (22, 25), (16, 29), (10, 25)], WHITE)
    c.circle(16, 27, 2, INK)
    dot_eyes(c, 15, 5.5, 1.7)
    return c.done()


def e_panda():
    c = Canvas()
    c.circle(6.5, 7, 5, rgb("292F33")); c.circle(25.5, 7, 5, rgb("292F33"))
    animal_face(c, WHITE, rgb("C3CDD5"), cy=17.5)
    for x, rot_ in ((10.5, 30), (21.5, -30)):
        e = Canvas(); e.ellipse(x - 3.6, 12, x + 3.6, 20, rgb("292F33"))
        c.img.alpha_composite(e.img.rotate(rot_, center=(int(x * 8), 128), resample=Image.BICUBIC))
    for x in (10.5, 21.5):
        c.circle(x, 15.5, 1.5, WHITE); c.circle(x + 0.3, 15.8, 0.8, INK)
    c.ellipse(13.5, 20, 18.5, 23, rgb("292F33"))
    c.arc(12.5, 21.5, 16, 26, 20, 160, INK, 0.9); c.arc(16, 21.5, 19.5, 26, 20, 160, INK, 0.9)
    return c.done()


def e_pinguino():
    c = Canvas()
    c.ellipse(4, 2, 28, 31, rgb("1E2328"))
    c.ellipse(7.5, 9, 24.5, 30.5, WHITE)
    c.ellipse(9, 6, 15.5, 15, WHITE); c.ellipse(16.5, 6, 23, 15, WHITE)
    c.circle(12.5, 11, 1.6, INK); c.circle(19.5, 11, 1.6, INK)
    c.poly([(13.5, 14), (18.5, 14), (16, 18)], rgb("FFAC33"))
    c.ellipse(9, 16, 12, 19, alpha(rgb("FF8A8A"), 140)); c.ellipse(20, 16, 23, 19, alpha(rgb("FF8A8A"), 140))
    return c.done()


# ---------------------------------------------------------------- comida

def e_pan():
    c = Canvas()
    crust, dark, inner = rgb("D9902F"), rgb("A8641A"), rgb("F2C27A")
    c.ellipse(2, 8, 30, 28, dark); c.ellipse(2.6, 8.4, 29.4, 27, crust)
    for x in (9, 15, 21):
        c.line([(x, 12), (x + 3, 22)], inner, 1.6)
    c.ellipse(6, 10, 14, 14, alpha(WHITE, 110))
    return c.done()


def e_galleta():
    c = Canvas()
    c.circle(16, 16, 14, rgb("B5761F")); c.circle(16, 15.6, 13.2, rgb("E0A95A"))
    for x, y in ((10, 10), (20, 9), (15, 16), (22, 19), (9, 20), (17, 24)):
        c.ellipse(x - 1.8, y - 1.4, x + 1.8, y + 1.4, rgb("5C3415"))
    return c.done()


def e_sandia():
    c = Canvas()
    c.pieslice(1, -12, 31, 30, 20, 160, rgb("3E8E2F"))
    c.pieslice(2.6, -10, 29.4, 28, 20, 160, rgb("D9F2C2"))
    c.pieslice(4, -8.5, 28, 26.5, 20, 160, rgb("EF4B5A"))
    for x, y in ((10, 15), (16, 18), (22, 15), (13, 21), (19, 21)):
        c.ellipse(x - 0.9, y - 1.4, x + 0.9, y + 1.4, rgb("292F33"))
    return c.done()


def e_zanahoria():
    c = Canvas()
    car = Canvas()
    car.poly([(10, 4), (22, 4), (16, 31)], rgb("F4782A"))
    car.poly([(16, 4), (22, 4), (16, 31)], rgb("D95E14"))
    for y in (10, 16, 22):
        car.line([(12.5, y), (15.5, y + 0.5)], rgb("C04F12"), 0.8)
    car.poly([(16, 4), (11, -3), (14, 4)], rgb("78B159")); car.poly([(16, 4), (21, -3), (18, 4)], rgb("78B159"))
    car.poly([(16, 4), (16, -4), (17.5, 4)], rgb("5DA642"))
    c.img.alpha_composite(car.img.rotate(-35, center=(128, 128), resample=Image.BICUBIC), (0, 16))
    return c.done()


def e_huevo():
    c = Canvas()
    c.ellipse(1, 6, 31, 28, rgb("C3CDD5")); c.ellipse(1.5, 6, 30.5, 27, WHITE)
    c.circle(16, 16.5, 6.5, rgb("F4900C")); c.circle(16, 16, 6, rgb("FFCC33"))
    c.ellipse(12.5, 12.5, 16, 15.5, alpha(WHITE, 200))
    return c.done()


def e_hamburguesa():
    c = Canvas()
    c.chord(3, 3, 29, 23, 180, 360, rgb("D9902F"))
    for x, y in ((9, 8), (15, 6), (21, 8), (12, 10.5), (19, 10.5)):
        c.ellipse(x - 0.9, y - 0.5, x + 0.9, y + 0.5, rgb("FFF2D0"))
    c.poly([(3, 13), (29, 13), (27, 16), (23, 14.5), (19, 16.5), (15, 14.5), (11, 16.5), (7, 14.5), (4, 16)],
           rgb("78B159"))
    c.rrect(3, 15, 29, 18, 1, rgb("FFCC33"))
    c.rrect(2.5, 18, 29.5, 22.5, 2.2, rgb("6B3A1E"))
    c.rrect(3, 22, 29, 28, 3, rgb("D9902F"))
    return c.done()


def e_papas():
    c = Canvas()
    for x, h in ((8, 4), (11.5, 2), (15, 3.5), (18.5, 1.5), (22, 4)):
        c.rrect(x, h, x + 3, 18, 0.8, rgb("FFCC33"))
        c.rrect(x + 2, h, x + 3, 18, 0, rgb("F2A900"))
    c.poly([(5, 13), (27, 13), (24.5, 31), (7.5, 31)], rgb("C1272D"))
    c.poly([(5, 13), (27, 13), (26.4, 17), (5.6, 17)], rgb("DD2E44"))
    c.circle(16, 23.5, 3.2, rgb("FFCC33"))
    return c.done()


def e_helado():
    c = Canvas()
    c.poly([(8, 15), (24, 15), (16, 31)], rgb("D9902F"))
    for i in range(3):
        c.line([(9.5 + i * 4, 16), (16.5 + i * 2, 26)], rgb("A8641A"), 0.6, False)
        c.line([(22.5 - i * 4, 16), (15.5 - i * 2, 26)], rgb("A8641A"), 0.6, False)
    c.circle(11, 12, 6, rgb("F4ABBA")); c.circle(21, 12, 6, rgb("8B5A2B"))
    c.circle(16, 7, 6.5, rgb("FFF2D0"))
    c.circle(16, 0.8, 2, RED)
    return c.done()


def e_dona():
    c = Canvas()
    from PIL import ImageChops
    c.circle(16, 16.5, 14, rgb("C98A3A"))
    c.circle(16, 15.5, 12.8, rgb("F4ABBA"))
    for x, y, col in ((9, 10, "5DADEC"), (20, 8, "FFCC4D"), (24, 16, "78B159"), (9, 20, "FFFFFF"), (20, 23, "5DADEC"),
                      (14, 25, "FFCC4D")):
        c.rrect(x - 1.3, y - 0.5, x + 1.3, y + 0.5, 0.4, rgb(col))
    hole = Canvas(); hole.circle(16, 16, 4.2, WHITE)
    c.img.putalpha(ImageChops.subtract(c.img.getchannel("A"), hole.img.getchannel("A")))
    return c.done()


# ---------------------------------------------------------------- naturaleza

def e_sol():
    c = Canvas()
    for i in range(12):
        a = math.radians(i * 30)
        c.line([(16 + 11 * math.cos(a), 16 + 11 * math.sin(a)), (16 + 14.5 * math.cos(a), 16 + 14.5 * math.sin(a))],
               rgb("FFAC33"), 2.2)
    c.circle(16, 16, 9.5, rgb("F4900C")); c.circle(16, 15.6, 8.8, rgb("FFCC4D"))
    c.ellipse(10.5, 10, 15, 14, alpha(WHITE, 140))
    return c.done()


def e_luna():
    c = Canvas()
    from PIL import ImageChops
    c.circle(16, 16, 14, rgb("E8A92E")); c.circle(16, 15.6, 13.3, rgb("FFE58A"))
    cut = Canvas(); cut.circle(23, 11, 11.5, WHITE)
    c.img.putalpha(ImageChops.subtract(c.img.getchannel("A"), cut.img.getchannel("A")))
    c.circle(9, 20, 1.6, alpha(rgb("E8A92E"), 200)); c.circle(12, 26, 1.1, alpha(rgb("E8A92E"), 200))
    return c.done()


def cloud(c, dy=0, col=None, shade=None):
    col = col or rgb("F5F8FA"); shade = shade or rgb("A7B4C0")
    for cs, off in ((shade, 0.8), (col, 0)):
        c.circle(10, 15 + dy + off, 6.5, cs); c.circle(18, 11 + dy + off, 8, cs); c.circle(24.5, 16 + dy + off, 5.5, cs)
        c.rrect(3.5, 15 + dy + off, 29.5, 22 + dy + off, 3.5, cs)


def e_nube():
    c = Canvas(); cloud(c, 2); return c.done()


def e_lluvia():
    c = Canvas(); cloud(c, -4, rgb("C3CDD5"), rgb("8899A6"))
    for x, y in ((9, 24), (16, 27), (23, 24)):
        c.line([(x + 1.2, y - 2.5), (x - 0.5, y + 2.5)], TEAR, 1.6)
    return c.done()


def e_nieve():
    c = Canvas()
    col = rgb("8FCBFF")
    for i in range(6):
        a = math.radians(i * 60 - 90)
        x1, y1 = 16 + 13.5 * math.cos(a), 16 + 13.5 * math.sin(a)
        c.line([(16, 16), (x1, y1)], col, 2)
        for side in (-1, 1):
            b = a + side * math.radians(40)
            mx, my = 16 + 8.5 * math.cos(a), 16 + 8.5 * math.sin(a)
            c.line([(mx, my), (mx + 4 * math.cos(b), my + 4 * math.sin(b))], col, 1.6)
    c.circle(16, 16, 2.5, rgb("C6E8FF"))
    return c.done()


def e_arcoiris():
    c = Canvas()
    for i, col in enumerate(("DD2E44", "F4900C", "FFCC4D", "78B159", "5DADEC", "AA8ED6")):
        r = 15 - i * 2
        c.arc(16 - r, 25 - r, 16 + r, 25 + r, 180, 360, rgb(col), 2.1)
    c.circle(5, 25, 4, rgb("F5F8FA")); c.circle(27, 25, 4, rgb("F5F8FA"))
    c.circle(8.5, 26, 3, rgb("F5F8FA")); c.circle(23.5, 26, 3, rgb("F5F8FA"))
    return c.done()


def e_arbol():
    c = Canvas()
    c.rrect(13.5, 20, 18.5, 31, 1, rgb("8B5A2B"))
    for x, y, r in ((16, 10, 9), (9, 15, 7), (23, 15, 7), (16, 18, 7.5)):
        c.circle(x, y + 0.8, r, rgb("3E8E2F"))
    for x, y, r in ((16, 10, 8.4), (9, 15, 6.4), (23, 15, 6.4), (16, 18, 6.9)):
        c.circle(x, y, r, rgb("5DA642"))
    c.ellipse(10, 4, 16, 9, alpha(rgb("9BE08A"), 170))
    return c.done()


def e_flor():
    c = Canvas()
    c.line([(16, 18), (16, 31)], rgb("5DA642"), 2)
    c.ellipse(16, 23, 24, 27, rgb("78B159"))
    for i in range(6):
        a = math.radians(i * 60 - 90)
        c.circle(16 + 6.5 * math.cos(a), 12 + 6.5 * math.sin(a), 4.6, rgb("F772A5"))
    c.circle(16, 12, 4, rgb("FFCC4D")); c.circle(15, 11, 1.2, alpha(WHITE, 180))
    return c.done()


def e_hoja():
    c = Canvas()
    l = Canvas()
    l.ellipse(9, 2, 23, 28, rgb("3E8E2F")); l.ellipse(9.6, 2.4, 22.4, 27.2, rgb("78B159"))
    l.line([(16, 4), (16, 31)], rgb("3E8E2F"), 1.2)
    for y in (10, 15, 20):
        l.line([(16, y + 2), (12, y - 1)], rgb("3E8E2F"), 0.8); l.line([(16, y + 2), (20, y - 1)], rgb("3E8E2F"), 0.8)
    rot(c, l, -35)
    return c.done()


def e_seta():
    c = Canvas()
    c.rrect(12, 15, 20, 30, 3, rgb("F2E6D0"))
    c.chord(2, 2, 30, 26, 180, 360, rgb("A0041E"))
    c.chord(2.6, 2.4, 29.4, 25, 180, 360, RED)
    for x, y, r in ((9, 9, 2.2), (17, 6, 2.5), (24, 10, 2), (13, 12, 1.6)):
        c.circle(x, y, r, WHITE)
    return c.done()


def e_cactus():
    c = Canvas()
    g, d = rgb("5DA642"), rgb("3E8E2F")
    c.rrect(12, 3, 20, 27, 4, d); c.rrect(12.5, 3.4, 19.5, 26.5, 3.6, g)
    c.rrect(4, 10, 10, 18, 3, d); c.rrect(6, 15, 13, 19, 1.5, d)
    c.rrect(22, 7, 28, 15, 3, d); c.rrect(19, 12, 26, 16, 1.5, d)
    c.rrect(4.4, 10.4, 9.6, 17.6, 2.6, g); c.rrect(22.4, 7.4, 27.6, 14.6, 2.6, g)
    c.rrect(7, 26, 25, 31, 1.5, rgb("C1694F"))
    for x, y in ((14.5, 8), (17.5, 13), (14.5, 18), (17.5, 22)):
        c.line([(x, y), (x + 0.8, y - 0.8)], rgb("D9F2C2"), 0.6)
    return c.done()


HD = {name[2:]: fn for name, fn in list(globals().items()) if name.startswith("e_") and callable(fn)}
