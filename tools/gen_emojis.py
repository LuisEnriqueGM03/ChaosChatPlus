"""
Genera los emojis por defecto de Chaos ChatPlus en pixel art de 16x16 (dibujo propio, sin licencias de terceros):

  src/main/resources/assets/chaoschatplus/textures/emoji/<nombre>.png
  src/main/resources/assets/chaoschatplus/font/emoji.json   (fuente bitmap de los emojis por defecto)
  src/main/resources/assets/chaoschatplus/emojis.json       (nombre, alias, categoría, codepoint y emojis Unicode)

El orden de EMOJIS fija los codepoints (U+E000 + índice): añade los nuevos al final para no cambiar los existentes.
Uso: python tools/gen_emojis.py [--preview]   (--preview deja build/emoji_preview.png ampliado x8)
"""

import json
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "chaoschatplus")
FIRST_CODEPOINT = 0xE000
S = 16


def rgb(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


# ---------------------------------------------------------------- paleta

INK = rgb("2B1D14")
WHITE = rgb("FFFFFF")
TEAR = rgb("4FC3F7")
TEAR_D = rgb("2A8BC0")
TONGUE = rgb("E8495F")
MOUTH = rgb("6B2A1E")
BLUSH = rgb("FF8A80")
HEART = rgb("E53935")

FACE = {
    "yellow": (rgb("FFD54A"), rgb("F2A900"), rgb("FFF2A8"), rgb("B57A00")),
    "red": (rgb("F2643D"), rgb("C6361B"), rgb("FFA182"), rgb("8E2310")),
    "green": (rgb("9CCC65"), rgb("6E9E3A"), rgb("CDE7A8"), rgb("4A6B25")),
    "blue": (rgb("7FC8F8"), rgb("4B9BD6"), rgb("C6E8FF"), rgb("2E6A99")),
    "purple": (rgb("A974E0"), rgb("7C47B8"), rgb("D2B5F2"), rgb("55297F")),
}

# Círculo de 16 px: columnas ocupadas por fila.
CIRCLE = [(5, 10), (3, 12), (2, 13), (1, 14), (1, 14), (0, 15), (0, 15), (0, 15),
          (0, 15), (0, 15), (0, 15), (1, 14), (1, 14), (2, 13), (3, 12), (5, 10)]


def new():
    return Image.new("RGBA", (S, S), (0, 0, 0, 0))


def px(img, x, y, c):
    if 0 <= x < S and 0 <= y < S:
        img.putpixel((x, y), c)


def rect(img, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(img, x, y, c)


def pts(img, c, *points):
    for x, y in points:
        px(img, x, y, c)


def grid(img, rows, colors, ox=0, oy=0):
    """Dibuja un mapa de caracteres: cada letra es un color de `colors`, '.' = vacío."""
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != "." and ch != " ":
                px(img, ox + x, oy + y, colors[ch])


def in_circle(x, y):
    return 0 <= y < S and CIRCLE[y][0] <= x <= CIRCLE[y][1]


def face(color="yellow"):
    base, shade, light, line = FACE[color]
    img = new()
    for y in range(S):
        for x in range(CIRCLE[y][0], CIRCLE[y][1] + 1):
            edge = not (in_circle(x - 1, y) and in_circle(x + 1, y) and in_circle(x, y - 1) and in_circle(x, y + 1))
            if edge:
                c = line
            elif x + y >= 21:
                c = shade
            else:
                c = base
            px(img, x, y, c)
    pts(img, light, (4, 2), (5, 2), (3, 3), (2, 4), (2, 5))
    return img


# ---- ojos

def eyes_dot(img, c=INK):
    rect(img, 4, 5, 5, 7, c)
    rect(img, 10, 5, 11, 7, c)


def eyes_happy(img, c=INK):  # ^ ^
    pts(img, c, (3, 7), (4, 6), (5, 6), (6, 7), (9, 7), (10, 6), (11, 6), (12, 7))


def eyes_closed(img, c=INK):  # > <
    pts(img, c, (3, 5), (4, 6), (5, 7), (4, 8), (3, 9))
    pts(img, c, (12, 5), (11, 6), (10, 7), (11, 8), (12, 9))


def eyes_line(img, c=INK):
    rect(img, 3, 6, 6, 6, c)
    rect(img, 9, 6, 12, 6, c)


def eyes_wink(img, c=INK):
    rect(img, 4, 5, 5, 7, c)
    pts(img, c, (9, 7), (10, 6), (11, 6), (12, 7))


def eyes_big(img):  # ojos grandes y brillantes
    for ox in (3, 9):
        rect(img, ox, 5, ox + 3, 8, INK)
        rect(img, ox + 1, 5, ox + 2, 6, WHITE)
        px(img, ox + 3, 8, WHITE)


def eyes_x(img, c=INK):
    for ox in (3, 9):
        pts(img, c, (ox, 5), (ox + 2, 5), (ox + 1, 6), (ox, 7), (ox + 2, 7))


def eyes_side(img):  # mirando de reojo
    for ox in (3, 9):
        rect(img, ox, 6, ox + 3, 7, WHITE)
        rect(img, ox + 2, 6, ox + 3, 7, INK)


def eyes_up(img):  # ojos en blanco
    for ox in (3, 9):
        rect(img, ox, 5, ox + 3, 8, WHITE)
        rect(img, ox + 1, 5, ox + 2, 6, INK)


def eyes_angry(img, c=INK):
    pts(img, c, (3, 4), (4, 5), (5, 5), (6, 6), (12, 4), (11, 5), (10, 5), (9, 6))
    rect(img, 4, 7, 5, 8, c)
    rect(img, 10, 7, 11, 8, c)


def eyes_sad(img, c=INK):
    pts(img, c, (3, 6), (4, 5), (5, 5), (12, 6), (11, 5), (10, 5))
    rect(img, 4, 7, 5, 8, c)
    rect(img, 10, 7, 11, 8, c)


def eyes_hearts(img):
    for ox in (2, 9):
        grid(img, [".h.h.", "hhhhh", ".hhh.", "..h.."], {"h": HEART}, ox, 4)


def eyes_stars(img):
    star = rgb("FFF176")
    edge = rgb("F57F17")
    for ox in (2, 9):
        grid(img, ["..e..", ".eye.", "eyyye", ".e.e."], {"e": edge, "y": star}, ox, 4)


def eyes_money(img):
    g = rgb("2E7D32")
    for ox in (3, 9):
        grid(img, [".g..", "gggg", "g...", "ggg.", "...g", "gggg", ".g.."], {"g": g}, ox, 3)


def sunglasses(img):
    k = rgb("1A1A1A")
    l = rgb("4A4A4A")
    rect(img, 1, 5, 14, 5, k)
    rect(img, 2, 5, 6, 8, k)
    rect(img, 9, 5, 13, 8, k)
    pts(img, l, (3, 6), (10, 6))


def glasses(img):
    k = rgb("1A1A1A")
    for ox in (2, 9):
        rect(img, ox, 4, ox + 4, 4, k)
        rect(img, ox, 8, ox + 4, 8, k)
        rect(img, ox, 4, ox, 8, k)
        rect(img, ox + 4, 4, ox + 4, 8, k)
        rect(img, ox + 2, 6, ox + 2, 7, INK)
    rect(img, 7, 5, 8, 5, k)


# ---- bocas

def mouth_smile(img, c=INK):
    pts(img, c, (4, 10), (11, 10))
    rect(img, 5, 11, 10, 11, c)


def mouth_grin(img):
    rect(img, 4, 10, 11, 10, INK)
    rect(img, 4, 11, 11, 11, WHITE)
    pts(img, INK, (3, 10), (12, 10), (4, 12), (11, 12))
    rect(img, 5, 12, 10, 12, MOUTH)
    rect(img, 5, 13, 10, 13, INK)
    pts(img, INK, (3, 11), (12, 11))


def mouth_open(img):  # boca abierta riendo
    rect(img, 4, 10, 11, 10, INK)
    rect(img, 4, 11, 11, 11, MOUTH)
    rect(img, 5, 12, 10, 12, MOUTH)
    rect(img, 6, 12, 9, 12, TONGUE)
    rect(img, 6, 13, 9, 13, INK)
    pts(img, INK, (3, 10), (12, 10), (3, 11), (12, 11), (4, 12), (11, 12), (5, 13), (10, 13))


def mouth_flat(img, c=INK):
    rect(img, 5, 11, 10, 11, c)


def mouth_sad(img, c=INK):
    rect(img, 5, 10, 10, 10, c)
    pts(img, c, (4, 11), (11, 11))


def mouth_o(img, small=False):
    if small:
        rect(img, 7, 10, 8, 11, INK)
        return
    rect(img, 6, 9, 9, 9, INK)
    rect(img, 5, 10, 5, 12, INK)
    rect(img, 10, 10, 10, 12, INK)
    rect(img, 6, 13, 9, 13, INK)
    rect(img, 6, 10, 9, 12, MOUTH)


def mouth_tongue(img):
    mouth_smile(img)
    rect(img, 8, 12, 9, 13, TONGUE)
    px(img, 8, 12, rgb("B71C3C"))


def mouth_smirk(img):
    pts(img, INK, (6, 11), (7, 11), (8, 11), (9, 11), (10, 10), (11, 9))


def mouth_wavy(img, c=INK):
    pts(img, c, (4, 11), (5, 10), (6, 11), (7, 10), (8, 11), (9, 10), (10, 11), (11, 10))


def mouth_kiss(img):
    pts(img, INK, (7, 9), (8, 10), (7, 11), (8, 12), (7, 13))


def mouth_teeth(img):  # dientes apretados
    rect(img, 4, 10, 11, 12, WHITE)
    rect(img, 3, 10, 3, 12, INK)
    rect(img, 12, 10, 12, 12, INK)
    rect(img, 4, 9, 11, 9, INK)
    rect(img, 4, 13, 11, 13, INK)
    rect(img, 4, 11, 11, 11, rgb("BDBDBD"))


def blush(img):
    pts(img, BLUSH, (2, 9), (3, 9), (12, 9), (13, 9))


def tear(img, x, y):
    px(img, x, y, TEAR)
    rect(img, x - 1, y + 1, x + 1, y + 2, TEAR)
    px(img, x, y + 3, TEAR_D)


# ---------------------------------------------------------------- caras

def e_sonrisa():
    i = face(); eyes_dot(i); mouth_smile(i); return i


def e_feliz():
    i = face(); eyes_happy(i); mouth_smile(i); blush(i); return i


def e_risa():
    i = face(); eyes_dot(i); mouth_grin(i); return i


def e_jaja():
    i = face(); eyes_closed(i); mouth_open(i); return i


def e_llorar_risa():
    i = face(); eyes_happy(i); mouth_open(i)
    tear(i, 1, 7); tear(i, 14, 7)
    return i


def e_sudor():
    i = face(); eyes_happy(i); mouth_grin(i)
    pts(i, TEAR, (13, 2), (12, 3), (13, 3), (12, 4), (13, 4)); px(i, 13, 5, TEAR_D)
    return i


def e_guino():
    i = face(); eyes_wink(i); mouth_tongue(i); return i


def e_enamorado():
    i = face(); eyes_hearts(i); mouth_smile(i); return i


def e_estrellas():
    i = face(); eyes_stars(i); mouth_grin(i); return i


def e_beso():
    i = face(); eyes_wink(i); mouth_kiss(i)
    grid(i, ["h.h", "hhh", ".h."], {"h": HEART}, 11, 10)
    return i


def e_lengua():
    i = face(); eyes_dot(i); mouth_tongue(i); return i


def e_loco():
    i = face(); eyes_x(i); mouth_tongue(i); return i


def e_cool():
    i = face(); sunglasses(i); mouth_smirk(i); return i


def e_nerd():
    i = face(); glasses(i); mouth_grin(i); return i


def e_angel():
    i = face()
    halo = rgb("FFF59D"); halo_d = rgb("FBC02D")
    eyes_happy(i); mouth_smile(i)
    img = new()
    img.paste(i.crop((0, 0, S, S)).resize((14, 14), Image.NEAREST), (1, 2))
    rect(img, 3, 0, 12, 0, halo_d); rect(img, 2, 1, 13, 1, halo)
    pts(img, halo_d, (2, 0), (13, 0))
    return img


def e_neutral():
    i = face(); eyes_dot(i); mouth_flat(i); return i


def e_inexpresivo():
    i = face(); eyes_line(i); mouth_flat(i); return i


def e_aburrido():
    i = face(); eyes_side(i); mouth_sad(i); return i


def e_ojos_blanco():
    i = face(); eyes_up(i); mouth_flat(i); return i


def e_picaro():
    i = face(); eyes_side(i); mouth_smirk(i); return i


def e_pensativo():
    i = face(); eyes_side(i); mouth_wavy(i)
    hand = rgb("FFC107"); hand_d = rgb("B57A00")
    grid(i, ["..hh", ".hhh", "hhhd", "hhd.", "dd.."], {"h": hand, "d": hand_d}, 2, 11)
    return i


def e_triste():
    i = face(); eyes_sad(i); mouth_sad(i); return i


def e_llorar():
    i = face(); eyes_sad(i); mouth_sad(i); tear(i, 4, 9); return i


def e_llanto():
    i = face(); eyes_closed(i); mouth_o(i)
    rect(i, 3, 9, 4, 15, TEAR); rect(i, 11, 9, 12, 15, TEAR)
    return i


def e_suplicar():
    i = face(); eyes_big(i); mouth_sad(i); return i


def e_enojado():
    i = face(); eyes_angry(i); mouth_sad(i); return i


def e_furioso():
    i = face("red"); eyes_angry(i); mouth_teeth(i); return i


def e_sorpresa():
    i = face(); eyes_big(i); mouth_o(i); return i


def e_asombro():
    i = face(); eyes_dot(i); mouth_o(i, small=True); return i


def e_grito():
    i = face("blue"); eyes_big(i); mouth_o(i)
    return i


def e_dormido():
    i = face(); eyes_line(i); mouth_o(i, small=True)
    z = rgb("5C6BC0")
    grid(i, ["zzz", "..z", ".z.", "zzz"], {"z": z}, 12, 0)
    return i


def e_mareado():
    i = face(); eyes_x(i); mouth_wavy(i); return i


def e_enfermo():
    i = face("green"); eyes_line(i); mouth_wavy(i, rgb("2E5016")); return i


def e_frio():
    i = face("blue"); eyes_sad(i); mouth_teeth(i)
    pts(i, WHITE, (2, 2), (13, 3), (1, 10))
    return i


def e_calor():
    i = face("red"); eyes_sad(i); mouth_tongue(i)
    tear(i, 13, 2)
    return i


def e_dinero():
    i = face(); eyes_money(i); mouth_tongue(i); return i


def e_fiesta():
    i = face(); eyes_happy(i); mouth_open(i)
    hat = rgb("AB47BC"); hat_l = rgb("FFEB3B")
    grid(i, ["...y", "..hh", ".hyh", "hhhh"], {"h": hat, "y": hat_l}, 9, 0)
    return i


def e_diablo():
    i = face("purple"); eyes_angry(i); mouth_smirk(i)
    horn = rgb("55297F")
    pts(i, horn, (1, 0), (2, 1), (2, 2), (14, 0), (13, 1), (13, 2))
    return i


def e_calavera():
    i = new()
    b = rgb("ECEFF1"); s = rgb("B0BEC5"); k = rgb("263238")
    grid(i, [
        "....kkkkkkkk....",
        "..kkbbbbbbbbkk..",
        ".kbbbbbbbbbbbbk.",
        "kbbbbbbbbbbbbbbk",
        "kbbbbbbbbbbbbbbk",
        "kbkkkbbbbbbkkkbk",
        "kbkkkkbbbbkkkkbk",
        "kbkkkkbbbbkkkkbk",
        "kbbkkbbbbbbkkbbk",
        ".kbbbbbkkbbbbbk.",
        ".kssbbbkkbbbssk.",
        "..kssbbbbbbssk..",
        "...kbkbkkbkbk...",
        "...kbkbbbbkbk...",
        "....kbkbbkbk....",
        ".....kkkkkk.....",
    ], {"k": k, "b": b, "s": s})
    return i


def e_fantasma():
    i = new()
    b = rgb("F5F5F5"); s = rgb("CFD8DC"); k = rgb("37474F")
    grid(i, [
        ".....kkkkkk.....",
        "...kkbbbbbbkk...",
        "..kbbbbbbbbbbk..",
        ".kbbbbbbbbbbbbk.",
        ".kbbkkbbbbkkbbk.",
        ".kbbkkbbbbkkbbk.",
        "kbbbbbbbbbbbbbbk",
        "kbbbbbbkkbbbbbbk",
        "kbbbbbbkkbbbbbbk",
        "kbbbbbbbbbbbbbbk",
        "kbbbbbbbbbbbbssk",
        "kbbbbbbbbbbbsssk",
        "kbbbbbbbbbbsssk.",
        "kbbskbbbskbbsk..",
        "kbsk.kbsk.kbsk..",
        ".kk...kk...kk...",
    ], {"k": k, "b": b, "s": s})
    return i


def e_payaso_caca():
    i = new()
    b = rgb("8D6E63"); d = rgb("5D4037"); l = rgb("A1887F")
    grid(i, [
        "........d.......",
        ".......dbd......",
        "......dbbbd.....",
        ".....dbbbbbd....",
        "....ddddddddd...",
        "...dbbbbbbbbbd..",
        "...dblbbbbbbbd..",
        "..ddddddddddddd.",
        ".dbbbbbbbbbbbbbd",
        ".dbllbbbbbbbbbbd",
        "dddddddddddddddd",
        "dbbbbbbbbbbbbbbd",
        "dbbbbbbbbbbbbbbd",
        ".dddddddddddddd.",
    ], {"d": d, "b": b, "l": l}, 0, 2)
    rect(i, 5, 7, 6, 8, WHITE); rect(i, 10, 7, 11, 8, WHITE)
    pts(i, INK, (6, 8), (11, 8))
    pts(i, INK, (5, 11), (10, 11)); rect(i, 6, 12, 9, 12, INK)
    return i


# ---------------------------------------------------------------- corazones

HEART_ROWS = [
    "................",
    "..kkkk....kkkk..",
    ".kllcck..kcccck.",
    "kllcccckkcccccdk",
    "klcccccccccccccdk"[:16],
    "kcccccccccccccdk",
    "kcccccccccccccdk",
    ".kcccccccccccdk.",
    "..kcccccccccdk..",
    "...kcccccccdk...",
    "....kcccccdk....",
    ".....kcccdk.....",
    "......kcdk......",
    ".......kk.......",
    "................",
    "................",
]


def heart(main, dark, light, line):
    i = new()
    grid(i, HEART_ROWS, {"k": line, "c": main, "d": dark, "l": light}, 0, 1)
    return i


def e_corazon(): return heart(rgb("E53935"), rgb("B71C1C"), rgb("FF8A80"), rgb("7F0000"))
def e_corazon_naranja(): return heart(rgb("FB8C00"), rgb("E65100"), rgb("FFCC80"), rgb("8A3A00"))
def e_corazon_amarillo(): return heart(rgb("FDD835"), rgb("F9A825"), rgb("FFF59D"), rgb("8A6D00"))
def e_corazon_verde(): return heart(rgb("43A047"), rgb("1B5E20"), rgb("A5D6A7"), rgb("0D3B10"))
def e_corazon_azul(): return heart(rgb("1E88E5"), rgb("0D47A1"), rgb("90CAF9"), rgb("082C66"))
def e_corazon_morado(): return heart(rgb("8E24AA"), rgb("4A148C"), rgb("CE93D8"), rgb("2E0A57"))
def e_corazon_negro(): return heart(rgb("37474F"), rgb("1C262B"), rgb("78909C"), rgb("0B0F12"))
def e_corazon_blanco(): return heart(rgb("FAFAFA"), rgb("CFD8DC"), rgb("FFFFFF"), rgb("607D8B"))


def e_corazon_roto():
    i = e_corazon()
    for x, y in [(8, 2), (7, 4), (8, 5), (9, 6), (8, 7), (7, 8), (8, 9), (8, 10), (7, 11), (8, 12), (8, 13)]:
        px(i, x, y, (0, 0, 0, 0))
    return i


# ---------------------------------------------------------------- gestos

SKIN = {"s": rgb("FFCA28"), "d": rgb("E0A000"), "k": rgb("8A5A00")}


def e_pulgar_arriba():
    i = new()
    grid(i, [
        "......kk........",
        ".....ksk........",
        ".....ksk........",
        "....ksdk........",
        "...ksdk.........",
        "kkkksskkkkkk....",
        "ksskssssssssk...",
        "ksskskkkkkkskk..",
        "ksskssssssssk...",
        "ksskskkkkkkskk..",
        "ksskssssssssk...",
        "ksskskkkkkkk....",
        "ksskssssssdk....",
        "kkkkkkkkkkk.....",
    ], SKIN, 1, 1)
    return i


def e_pulgar_abajo():
    return e_pulgar_arriba().transpose(Image.FLIP_TOP_BOTTOM)


def e_ok():
    i = new()
    grid(i, [
        "......kk..kk....",
        ".....ksk.ksk.kk.",
        ".....ksk.ksk.ksk",
        "..kk.ksk.ksk.ksk",
        ".kssk.ksk.ksk.sk",
        ".ksksksssssssssk",
        ".ks..ksssssssssk",
        ".ksk.kssssssssdk",
        "..ksksssssssssk.",
        "...kssssssssssk.",
        "....ksssssssdk..",
        ".....kssssssk...",
        "......kkkkkk....",
    ], SKIN, 0, 2)
    return i


def e_aplausos():
    i = new()
    grid(i, [
        "..y.....y.......",
        "y...kk....y.....",
        "...kssk.kk......",
        "..ksssskssk.....",
        "..kssssssssk....",
        ".kssssksssssk...",
        ".ksssksssssssk..",
        ".kssksssssssssk.",
        "..ksksssssssssk.",
        "...kssssssssssk.",
        "....ksssssssddk.",
        ".....kssssssdk..",
        "......kssssdk...",
        ".......kkkkk....",
    ], {**SKIN, "y": rgb("FFEE58")}, 0, 1)
    return i


def e_rezar():
    i = new()
    grid(i, [
        "......kk.kk.....",
        ".....ksskssk....",
        ".....ksskssk....",
        "....ksssksssk...",
        "....ksssksssk...",
        "....ksssksssk...",
        "...kssssksssdk..",
        "...kssssksssdk..",
        "..ksssssksssdk..",
        "..ksssssksssdk..",
        ".kssssskksssdk..",
        ".kkkkkkkkkkkkk..",
        ".kbbbbbbbbbbbk..",
        ".kkkkkkkkkkkkk..",
    ], {**SKIN, "b": rgb("90CAF9")}, 1, 1)
    return i


def e_saludo():
    i = new()
    grid(i, [
        "...kk.kk........",
        "..ksskssk.kk....",
        "..ksskssk.ksk...",
        "..ksskssk.ksk...",
        "..kssksskksskk..",
        "..ksssssssssskk.",
        "kk.kssssssssssk.",
        "ksk.ksssssssssk.",
        ".kskksssssssssk.",
        "..ksssssssssdk..",
        "...ksssssssdk...",
        "....kssssssk....",
        ".....kkkkkk.....",
    ], SKIN, 0, 2)
    pts(i, rgb("90CAF9"), (14, 1), (15, 3), (13, 0))
    return i


def e_musculo():
    i = new()
    grid(i, [
        ".....kkk........",
        "....ksssk.......",
        "....kssdk.......",
        "....ksssk.......",
        ".....ksk........",
        ".....ksk...kkk..",
        ".....kssk.kssdk.",
        ".....ksssksssdk.",
        "....kssssssssdk.",
        "..kkssssssssdk..",
        ".ksssssssssdk...",
        "kssssssssddk....",
        "kkkkkkkkkkk.....",
    ], SKIN, 0, 2)
    return i


def e_ojos():
    i = new()
    for ox in (1, 8):
        grid(i, [
            ".kkkk.",
            "kwwwwk",
            "kwwwwk",
            "kwwkkk",
            "kwwkkk",
            "kwwwwk",
            "kwwwwk",
            ".kkkk.",
        ], {"k": INK, "w": WHITE}, ox, 4)
    return i


# ---------------------------------------------------------------- símbolos

def e_fuego():
    i = new()
    grid(i, [
        "......r.........",
        "......rr........",
        ".....rrr...r....",
        ".....rorr..rr...",
        "....rrorr..rr...",
        "...rrooorrrrr...",
        "...rroooorrorr..",
        "..rrooyoooroor..",
        "..rrooyyooooorr.",
        "..rooyyyyooyoor.",
        "..rooyywyyyyoor.",
        "..roooywwyyyoor.",
        "..rroooyyyyoorr.",
        "...rrooooooorr..",
        "....rrrrrrrrr...",
    ], {"r": rgb("D84315"), "o": rgb("FF8F00"), "y": rgb("FFD54F"), "w": rgb("FFF8E1")}, 0, 1)
    return i


def e_estrella():
    i = new()
    grid(i, [
        ".......kk.......",
        "......kyyk......",
        "......kyyk......",
        ".....kyyyyk.....",
        "kkkkkkywyyykkkkk",
        "kyyyyywyyyyyyyok",
        ".kyyyyyyyyyyyok.",
        "..kyyyyyyyyyok..",
        "...kyyyyyyyok...",
        "...kyyyyyyyok...",
        "..kyyyyookyyok..",
        "..kyyyokkkyyok..",
        ".kyyokk...kkyok.",
        ".kook.......kook",
        ".kkk.........kkk",
    ], {"k": rgb("C77800"), "y": rgb("FFD740"), "o": rgb("FFA000"), "w": rgb("FFF8E1")}, 0, 0)
    return i


def e_brillos():
    i = new()
    y = rgb("FFD740"); w = rgb("FFF8E1")
    grid(i, ["...y...", "...y...", "..yyy..", "yyywyyy", "..yyy..", "...y...", "...y..."], {"y": y, "w": w}, 1, 6)
    grid(i, ["..y..", ".yyy.", "yywyy", ".yyy.", "..y.."], {"y": y, "w": w}, 9, 1)
    grid(i, [".y.", "yyy", ".y."], {"y": y}, 11, 10)
    return i


def e_cien():
    i = new()
    r = rgb("E53935")
    grid(i, [
        ".r..rrr..rrr.",
        "rr.r...rr...r",
        ".r.r...rr...r",
        ".r.r...rr...r",
        ".r.r...rr...r",
        ".r.r...rr...r",
        "rrr.rrr..rrr.",
    ], {"r": r}, 1, 3)
    rect(i, 1, 12, 14, 12, r)
    rect(i, 2, 14, 13, 14, r)
    return i


def e_check():
    i = new()
    g = rgb("43A047"); d = rgb("1B5E20"); w = WHITE
    rect(i, 1, 1, 14, 14, g)
    rect(i, 1, 14, 14, 14, d); rect(i, 14, 1, 14, 14, d)
    pts(i, (0, 0, 0, 0), (1, 1), (14, 1), (1, 14), (14, 14))
    grid(i, ["........ww", ".......www", "......www.", "ww...www..", "www.www...", ".wwwww....", "..www....."], {"w": w}, 3, 4)
    return i


def e_cruz():
    i = new()
    r = rgb("E53935"); d = rgb("8E1B1B")
    grid(i, [
        ".dd........dd.",
        "drrd......drrd",
        "drrrd....drrrd",
        ".drrrd..drrrd.",
        "..drrrddrrrd..",
        "...drrrrrrd...",
        "....drrrrd....",
        "....drrrrd....",
        "...drrrrrrd...",
        "..drrrddrrrd..",
        ".drrrd..drrrd.",
        "drrrd....drrrd",
        "drrd......drrd",
        ".dd........dd.",
    ], {"r": r, "d": d}, 1, 1)
    return i


def e_alerta():
    i = new()
    y = rgb("FFC107"); k = rgb("3E2723"); d = rgb("C77800")
    grid(i, [
        ".......dd.......",
        "......dyyd......",
        "......dyyd......",
        ".....dyyyyd.....",
        ".....dykkyd.....",
        "....dyykkyyd....",
        "....dyykkyyd....",
        "...dyyykkyyyd...",
        "...dyyykkyyyd...",
        "..dyyyykkyyyyd..",
        "..dyyyyyyyyyyd..",
        ".dyyyyykkyyyyyd.",
        ".dyyyyykkyyyyyd.",
        "dddddddddddddddd",
    ], {"y": y, "k": k, "d": d}, 0, 1)
    return i


def e_pregunta():
    i = new()
    r = rgb("E53935"); d = rgb("8E1B1B")
    grid(i, [
        "...dddddd...",
        "..drrrrrrd..",
        ".drrrddrrrd.",
        ".drrd..drrd.",
        "..dd...drrd.",
        "......drrd..",
        ".....drrd...",
        "....drrd....",
        "....drrd....",
        ".....dd.....",
        "....dddd....",
        "....drrd....",
        "....drrd....",
        ".....dd.....",
    ], {"r": r, "d": d}, 2, 1)
    return i


def e_exclamacion():
    i = new()
    r = rgb("E53935"); d = rgb("8E1B1B")
    grid(i, [
        ".dddd.",
        "drrrrd",
        "drrrrd",
        "drrrrd",
        "drrrrd",
        ".drrd.",
        ".drrd.",
        ".drrd.",
        "..dd..",
        "......",
        ".dddd.",
        ".drrd.",
        ".drrd.",
        "..dd..",
    ], {"r": r, "d": d}, 5, 1)
    return i


def e_zzz():
    i = new()
    b = rgb("5C6BC0"); l = rgb("9FA8DA")
    grid(i, ["bbbbbb", "....bb", "...bb.", "..bb..", ".bb...", "bbbbbb"], {"b": b}, 1, 9)
    grid(i, ["llll", "..l.", ".l..", "llll"], {"l": l}, 8, 4)
    grid(i, ["bbb", ".b.", "bbb"], {"b": l}, 12, 0)
    return i


def e_musica():
    i = new()
    k = rgb("5E35B1"); l = rgb("9575CD")
    grid(i, [
        "....kkkkkkkkk",
        "....kkkkkkkkk",
        "....kl.....lk",
        "....k......k.",
        "....k......k.",
        "....k......k.",
        "....k......k.",
        "....k......k.",
        ".kkkk...kkkk.",
        "kkkkk..kkkkk.",
        "kkkkk..kkkkk.",
        ".kkk....kkk..",
    ], {"k": k, "l": l}, 1, 2)
    return i


def e_rayo():
    i = new()
    y = rgb("FFD600"); d = rgb("F57F17")
    grid(i, [
        "......ddddd",
        ".....dyyyyd",
        ".....dyyyd.",
        "....dyyyd..",
        "....dyyd...",
        "...dyyyddd.",
        "...dyyyyyyd",
        "..dddyyyyd.",
        ".....dyyd..",
        "....dyyd...",
        "....dyd....",
        "...dyd.....",
        "...dd......",
        "..dd.......",
        "..d........",
    ], {"y": y, "d": d}, 2, 0)
    return i


def e_corona():
    i = new()
    g = rgb("FFC107"); d = rgb("B57A00"); r = rgb("E53935"); b = rgb("1E88E5")
    grid(i, [
        "d......dd......d",
        "dd....dggd....dd",
        "dgd..dggggd..dgd",
        "dggddggggggddggd",
        "dggggggggggggggd",
        "dggrggggbgggrggd",
        "dggggggggggggggd",
        "dddddddddddddddd",
        "dggggggggggggggd",
        "dddddddddddddddd",
    ], {"g": g, "d": d, "r": r, "b": b}, 0, 3)
    return i


def e_trofeo():
    i = new()
    g = rgb("FFC107"); d = rgb("B57A00"); l = rgb("FFF59D"); w = rgb("6D4C41")
    grid(i, [
        "..dddddddddddd..",
        "dddgllgggggggddd",
        "d.dgllgggggggd.d",
        "d.dglggggggggd.d",
        ".ddgggggggggggdd",
        "...dgggggggggd..",
        "....dgggggggd...",
        ".....ddgggdd....",
        ".......dgd......",
        ".......dgd......",
        ".....ddgggdd....",
        "....dwwwwwwwd...",
        "....dwwwwwwwd...",
        "....ddddddddd...",
    ], {"g": g, "d": d, "l": l, "w": w}, 0, 1)
    return i


def e_regalo():
    i = new()
    r = rgb("E53935"); d = rgb("8E1B1B"); y = rgb("FFD54F"); o = rgb("F9A825")
    grid(i, [
        "...oo....oo.....",
        "..oyyo..oyyo....",
        "...oyyooyyo.....",
        ".dddddooodddddd.",
        ".drrrrryyrrrrrd.",
        ".drrrrryyrrrrrd.",
        ".dddddooodddddd.",
        "..drrrryyrrrrd..",
        "..drrrryyrrrrd..",
        "..drrrryyrrrrd..",
        "..drrrryyrrrrd..",
        "..drrrryyrrrrd..",
        "..dddddoodddd...",
    ], {"r": r, "d": d, "y": y, "o": o}, 0, 2)
    return i


def e_pastel():
    i = new()
    p = rgb("F8BBD0"); m = rgb("EC407A"); c = rgb("8D6E63"); w = WHITE; f = rgb("FFB300"); r = rgb("E53935")
    grid(i, [
        ".......f........",
        ".......r........",
        ".......w........",
        "....mmmmmmmm....",
        "...mpppppppppm..",
        "..mpwpppwpppwpm.",
        "..mmmmmmmmmmmmm.",
        "..cccccccccccccc",
        "..cppppppppppppc",
        "..cccccccccccccc",
        "..cccccccccccccc",
        "..mmmmmmmmmmmmmm",
    ], {"p": p, "m": m, "c": c, "w": w, "f": f, "r": r}, 0, 2)
    return i


def e_cafe():
    i = new()
    w = rgb("ECEFF1"); d = rgb("90A4AE"); c = rgb("6D4C41"); s = rgb("CFD8DC")
    grid(i, [
        "....s..s........",
        "...s..s.........",
        "....s..s........",
        "................",
        ".dddddddddd.....",
        ".dccccccccdddd..",
        ".dwwwwwwwwd..d..",
        ".dwwwwwwwwd..d..",
        ".dwwwwwwwwdddd..",
        ".dwwwwwwwwd.....",
        "..dwwwwwwd......",
        "dddddddddddddd..",
        ".dddddddddddd...",
    ], {"w": w, "d": d, "c": c, "s": s}, 0, 2)
    return i


def e_pizza():
    i = new()
    c = rgb("FFCA28"); k = rgb("A1662F"); r = rgb("D32F2F"); d = rgb("C77800")
    grid(i, [
        "kkkkkkkkkkkkkkk.",
        "kkkkkkkkkkkkkkk.",
        ".dcccccccccccd..",
        ".dccrrcccccccd..",
        "..dcrrccccrrd...",
        "..dcccccccrrd...",
        "...dcccccccd....",
        "...dcccrrccd....",
        "....dccrrcd.....",
        "....dcccccd.....",
        ".....dcccd......",
        ".....dcccd......",
        "......dcd.......",
        "......dd........",
    ], {"c": c, "k": k, "r": r, "d": d}, 0, 1)
    return i


def e_confeti():
    i = new()
    k = rgb("7E57C2"); y = rgb("FFD54F"); r = rgb("EF5350"); g = rgb("66BB6A"); b = rgb("42A5F5")
    grid(i, [
        "......r....b....",
        "..y.......r...y.",
        "........g.......",
        "....b......y....",
        "..........r..g..",
        "......yk.......b",
        ".....kkk..g.....",
        "....kkkky.......",
        "...kkykkk...r...",
        "..kkkkkk........",
        ".kkkykk......y..",
        ".kkkkk..........",
        "kkykk...........",
        "kkkk............",
        "kk..............",
    ], {"k": k, "y": y, "r": r, "g": g, "b": b}, 0, 0)
    return i


def e_camara():
    i = new()
    k = rgb("37474F"); b = rgb("546E7A"); l = rgb("B0BEC5"); g = rgb("4FC3F7"); w = WHITE
    grid(i, [
        ".....kkkkk......",
        "....kbbbbbk.....",
        "kkkkkkkkkkkkkkkk",
        "kbbbbbbbbbbbbbbk",
        "kbwbbbkkkkbbbbbk",
        "kbbbbkllllkbbbbk",
        "kbbbkllgggkkbbbk",
        "kbbbklgwggklbbbk",
        "kbbbklggggklbbbk",
        "kbbbkkggglklbbbk",
        "kbbbbkkllkkbbbbk",
        "kbbbbbkkkkbbbbbk",
        "kkkkkkkkkkkkkkkk",
    ], {"k": k, "b": b, "l": l, "g": g, "w": w}, 0, 2)
    return i


def e_chat():
    i = new()
    w = rgb("FFFFFF"); k = rgb("546E7A"); d = rgb("90A4AE")
    grid(i, [
        "..kkkkkkkkkkkk..",
        ".kwwwwwwwwwwwwk.",
        "kwwwwwwwwwwwwwwk",
        "kwwddwwddwwddwwk",
        "kwwddwwddwwddwwk",
        "kwwwwwwwwwwwwwwk",
        ".kwwwwwwwwwwwwk.",
        "..kkkwwkkkkkkk..",
        "....kwk.........",
        "....kk..........",
    ], {"w": w, "k": k, "d": d}, 0, 3)
    return i


def e_gg():
    i = new()
    g = rgb("43A047"); d = rgb("1B5E20")
    rect(i, 0, 3, 15, 12, d)
    rect(i, 1, 4, 14, 11, g)
    grid(i, [
        ".www..www.",
        "w....w....",
        "w.ww.w.ww.",
        "w..w.w..w.",
        ".www..www.",
    ], {"w": WHITE}, 3, 5)
    return i


# ---------------------------------------------------------------- minecraft (dibujos propios)

def e_diamante():
    i = new()
    c = rgb("4DD0E1"); l = rgb("B2EBF2"); d = rgb("00838F"); k = rgb("004D55")
    grid(i, [
        "....kkkkkkkk....",
        "...kllccllcck...",
        "..kllccllccccd..",
        ".kllccllccccccdk",
        "kkkkkkkkkkkkkkkk",
        "kcclccccccccccdk",
        ".kcclccccccccdk.",
        "..kcclccccccdk..",
        "...kcclccccdk...",
        "....kcclccdk....",
        ".....kclcdk.....",
        "......kcdk......",
        ".......kk.......",
    ], {"c": c, "l": l, "d": d, "k": k}, 0, 2)
    return i


def e_esmeralda():
    i = new()
    c = rgb("2ECC71"); l = rgb("A9F5C8"); d = rgb("1E8449"); k = rgb("0B4D27")
    grid(i, [
        "......kkkk......",
        ".....kllcck.....",
        "....klcccccdk...",
        "...klccccccdk...",
        "...kcccllccdk...",
        "...kcccllccdk...",
        "...kccccccccdk..",
        "...kccccccccdk..",
        "...kccccccccdk..",
        "...kcccccccddk..",
        "....kccccccdk...",
        ".....kcccddk....",
        "......kkkkk.....",
    ], {"c": c, "l": l, "d": d, "k": k}, 0, 1)
    return i


def e_oro():
    i = new()
    g = rgb("FFD54F"); l = rgb("FFF59D"); d = rgb("F9A825"); k = rgb("8D6E00")
    grid(i, [
        "......kkkkkkkkk.",
        ".....kllllllldk.",
        "....kllgggggddk.",
        "...kkkkkkkkkdk..",
        "..klgggggggdk...",
        ".klgggggggddk...",
        "kkkkkkkkkkkk....",
    ], {"g": g, "l": l, "d": d, "k": k}, 0, 5)
    return i


def e_espada():
    i = new()
    b = rgb("4DD0E1"); l = rgb("E0F7FA"); k = rgb("004D55"); h = rgb("6D4C41"); g = rgb("FFC107")
    grid(i, [
        "............kkk.",
        "...........klbk.",
        "..........klbk..",
        ".........klbk...",
        "........klbk....",
        ".......klbk.....",
        "..k...klbk......",
        "..kk.klbk.......",
        "...kklbk........",
        "....kgk.........",
        "...khkgk........",
        "..khk.kk........",
        ".khk............",
        "kkk.............",
    ], {"b": b, "l": l, "k": k, "h": h, "g": g}, 1, 1)
    return i


def e_pico():
    i = new()
    b = rgb("4DD0E1"); l = rgb("E0F7FA"); k = rgb("004D55"); h = rgb("8D6E63"); hd = rgb("5D4037")
    grid(i, [
        "....kkkkkk......",
        "...klllbbbkk....",
        "....kkkkbbbbk...",
        "........khbbk...",
        ".......khk.kbk..",
        "......khk...kbk.",
        ".....khk.....kk.",
        "....khk.........",
        "...khk..........",
        "..khk...........",
        ".kdk............",
        ".kk.............",
    ], {"b": b, "l": l, "k": k, "h": h, "d": hd}, 1, 2)
    return i


def e_tnt():
    i = new()
    r = rgb("D32F2F"); d = rgb("8E1B1B"); w = rgb("ECEFF1"); k = rgb("212121"); f = rgb("BDBDBD")
    grid(i, [
        "................",
        "dddddddddddddddd",
        "drrdrrdrrdrrdrrd",
        "drrdrrdrrdrrdrrd",
        "dwwwwwwwwwwwwwwd",
        "dwkkkwkwwkwkkkwd",
        "dwwkwwkkwkwwkwwd",
        "dwwkwwkwkkwwkwwd",
        "dwwkwwkwwkwwkwwd",
        "dwwwwwwwwwwwwwwd",
        "drrdrrdrrdrrdrrd",
        "drrdrrdrrdrrdrrd",
        "dddddddddddddddd",
    ], {"r": r, "d": d, "w": w, "k": k, "f": f}, 0, 1)
    return i


def e_creeper():
    i = new()
    g = rgb("4CAF50"); l = rgb("81C784"); d = rgb("2E7D32"); k = rgb("1B1B1B")
    rect(i, 1, 1, 14, 14, g)
    for (x, y) in [(2, 2), (6, 3), (11, 2), (3, 12), (12, 11), (9, 13), (2, 8), (13, 6)]:
        px(i, x, y, l)
    for (x, y) in [(4, 2), (9, 4), (12, 3), (2, 10), (13, 13), (7, 13)]:
        px(i, x, y, d)
    rect(i, 3, 4, 6, 7, k)
    rect(i, 9, 4, 12, 7, k)
    rect(i, 6, 8, 9, 10, k)
    rect(i, 5, 9, 5, 12, k)
    rect(i, 10, 9, 10, 12, k)
    rect(i, 6, 11, 9, 11, k)
    rect(i, 0, 0, 15, 0, d); rect(i, 0, 15, 15, 15, d); rect(i, 0, 0, 0, 15, d); rect(i, 15, 0, 15, 15, d)
    return i


def e_manzana():
    i = new()
    r = rgb("E53935"); d = rgb("9A1B1B"); l = rgb("FF8A80"); s = rgb("5D4037"); g = rgb("66BB6A")
    grid(i, [
        ".......s..gg....",
        ".......s.ggg....",
        "...ddd.sddd.....",
        "..drrrrrrrrrd...",
        ".drllrrrrrrrrd..",
        ".drlrrrrrrrrrd..",
        "drrrrrrrrrrrrrd.",
        "drrrrrrrrrrrrrd.",
        "drrrrrrrrrrrrrd.",
        "drrrrrrrrrrrrdd.",
        ".drrrrrrrrrrdd..",
        ".ddrrrrrrrrrdd..",
        "..ddrrrddrrdd...",
        "...ddd..dddd....",
    ], {"r": r, "d": d, "l": l, "s": s, "g": g}, 0, 1)
    return i


def e_cama():
    i = new()
    r = rgb("E53935"); d = rgb("8E1B1B"); w = rgb("ECEFF1"); wd = rgb("B0BEC5"); b = rgb("6D4C41")
    grid(i, [
        "wwwwwrrrrrrrrrrr",
        "wwwwwrrrrrrrrrrr",
        "wdddwrrrrrrrrrrr",
        "dddddddddddddddd",
        "bbbbbbbbbbbbbbbb",
        "b..............b",
        "b..............b",
    ], {"r": r, "d": d, "w": w, "b": b}, 0, 6)
    return i


def e_portal():
    i = new()
    o = rgb("1A0F2E"); p = rgb("8E24AA"); l = rgb("CE93D8"); m = rgb("5E1F8A")
    rect(i, 1, 0, 14, 15, o)
    rect(i, 3, 2, 12, 13, p)
    for (x, y) in [(4, 3), (7, 5), (10, 4), (5, 8), (9, 9), (6, 11), (11, 12), (4, 12)]:
        px(i, x, y, l)
    for (x, y) in [(6, 3), (11, 7), (3, 6), (8, 12), (10, 2), (5, 5)]:
        px(i, x, y, m)
    return i


# ---------------------------------------------------------------- índice

# (nombre, función, alias, categoría, emojis Unicode que se convierten en este)
EMOJIS = [
    ("sonrisa", e_sonrisa, ["smile", "smiley"], "caras", ["🙂", "😀", "😃"]),
    ("feliz", e_feliz, ["blush", "happy"], "caras", ["😊", "☺"]),
    ("risa", e_risa, ["grin", "grinning"], "caras", ["😁", "😄"]),
    ("jaja", e_jaja, ["laughing", "xd", "lol"], "caras", ["😆"]),
    ("llorar_risa", e_llorar_risa, ["joy", "rofl"], "caras", ["😂", "🤣"]),
    ("sudor", e_sudor, ["sweat_smile"], "caras", ["😅"]),
    ("guino", e_guino, ["wink"], "caras", ["😉", "😜"]),
    ("enamorado", e_enamorado, ["heart_eyes", "love"], "caras", ["😍", "🥰"]),
    ("estrellas", e_estrellas, ["star_struck", "wow"], "caras", ["🤩"]),
    ("beso", e_beso, ["kiss", "kissing_heart"], "caras", ["😘", "😗"]),
    ("lengua", e_lengua, ["tongue", "yum"], "caras", ["😛", "😋"]),
    ("loco", e_loco, ["crazy", "zany"], "caras", ["🤪"]),
    ("cool", e_cool, ["sunglasses", "gafas"], "caras", ["😎"]),
    ("nerd", e_nerd, ["nerd_face"], "caras", ["🤓"]),
    ("angel", e_angel, ["innocent", "halo"], "caras", ["😇"]),
    ("neutral", e_neutral, ["neutral_face"], "caras", ["😐"]),
    ("inexpresivo", e_inexpresivo, ["expressionless"], "caras", ["😑"]),
    ("aburrido", e_aburrido, ["unamused", "meh"], "caras", ["😒"]),
    ("ojos_blanco", e_ojos_blanco, ["rolling_eyes", "roll"], "caras", ["🙄"]),
    ("picaro", e_picaro, ["smirk"], "caras", ["😏"]),
    ("pensativo", e_pensativo, ["thinking", "hmm"], "caras", ["🤔"]),
    ("triste", e_triste, ["sad", "pensive"], "caras", ["😔", "🙁", "☹"]),
    ("llorar", e_llorar, ["cry"], "caras", ["😢"]),
    ("llanto", e_llanto, ["sob"], "caras", ["😭"]),
    ("suplicar", e_suplicar, ["pleading", "plis", "please"], "caras", ["🥺"]),
    ("enojado", e_enojado, ["angry"], "caras", ["😠", "😤"]),
    ("furioso", e_furioso, ["rage"], "caras", ["😡", "🤬"]),
    ("sorpresa", e_sorpresa, ["open_mouth", "surprised"], "caras", ["😮", "😲"]),
    ("asombro", e_asombro, ["hushed", "o_o"], "caras", ["😯"]),
    ("grito", e_grito, ["scream"], "caras", ["😱"]),
    ("dormido", e_dormido, ["sleeping", "sleepy"], "caras", ["😴"]),
    ("mareado", e_mareado, ["dizzy_face", "dizzy"], "caras", ["😵", "😵‍💫"]),
    ("enfermo", e_enfermo, ["sick", "nauseated"], "caras", ["🤢", "🤮"]),
    ("frio", e_frio, ["cold", "cold_face"], "caras", ["🥶"]),
    ("calor", e_calor, ["hot", "hot_face"], "caras", ["🥵"]),
    ("dinero", e_dinero, ["money", "money_mouth"], "caras", ["🤑"]),
    ("fiesta", e_fiesta, ["party", "partying"], "caras", ["🥳"]),
    ("diablo", e_diablo, ["devil", "smiling_imp"], "caras", ["😈"]),
    ("calavera", e_calavera, ["skull", "dead"], "caras", ["💀", "☠"]),
    ("fantasma", e_fantasma, ["ghost"], "caras", ["👻"]),
    ("caca", e_payaso_caca, ["poop"], "caras", ["💩"]),

    ("corazon", e_corazon, ["heart", "red_heart"], "corazones", ["❤", "❤️", "♥"]),
    ("corazon_naranja", e_corazon_naranja, ["orange_heart"], "corazones", ["🧡"]),
    ("corazon_amarillo", e_corazon_amarillo, ["yellow_heart"], "corazones", ["💛"]),
    ("corazon_verde", e_corazon_verde, ["green_heart"], "corazones", ["💚"]),
    ("corazon_azul", e_corazon_azul, ["blue_heart"], "corazones", ["💙"]),
    ("corazon_morado", e_corazon_morado, ["purple_heart"], "corazones", ["💜"]),
    ("corazon_negro", e_corazon_negro, ["black_heart"], "corazones", ["🖤"]),
    ("corazon_blanco", e_corazon_blanco, ["white_heart"], "corazones", ["🤍"]),
    ("corazon_roto", e_corazon_roto, ["broken_heart"], "corazones", ["💔"]),

    ("pulgar_arriba", e_pulgar_arriba, ["thumbsup", "like", "+1"], "gestos", ["👍"]),
    ("pulgar_abajo", e_pulgar_abajo, ["thumbsdown", "dislike", "-1"], "gestos", ["👎"]),
    ("ok", e_ok, ["ok_hand"], "gestos", ["👌"]),
    ("aplausos", e_aplausos, ["clap"], "gestos", ["👏"]),
    ("rezar", e_rezar, ["pray", "gracias", "thanks"], "gestos", ["🙏"]),
    ("saludo", e_saludo, ["wave", "hola", "hi"], "gestos", ["👋"]),
    ("musculo", e_musculo, ["muscle", "strong"], "gestos", ["💪"]),
    ("ojos", e_ojos, ["eyes"], "gestos", ["👀"]),

    ("fuego", e_fuego, ["fire", "lit"], "simbolos", ["🔥"]),
    ("estrella", e_estrella, ["star"], "simbolos", ["⭐", "🌟"]),
    ("brillos", e_brillos, ["sparkles"], "simbolos", ["✨"]),
    ("cien", e_cien, ["100"], "simbolos", ["💯"]),
    ("check", e_check, ["white_check_mark", "si", "yes"], "simbolos", ["✅", "✔"]),
    ("cruz", e_cruz, ["x", "no"], "simbolos", ["❌", "✖"]),
    ("alerta", e_alerta, ["warning"], "simbolos", ["⚠", "⚠️"]),
    ("pregunta", e_pregunta, ["question"], "simbolos", ["❓"]),
    ("exclamacion", e_exclamacion, ["exclamation"], "simbolos", ["❗"]),
    ("zzz", e_zzz, ["zz"], "simbolos", ["💤"]),
    ("musica", e_musica, ["music", "notes"], "simbolos", ["🎵", "🎶"]),
    ("rayo", e_rayo, ["zap", "lightning"], "simbolos", ["⚡"]),
    ("gg", e_gg, ["good_game"], "simbolos", []),
    ("chat", e_chat, ["speech", "speech_balloon"], "simbolos", ["💬"]),

    ("corona", e_corona, ["crown"], "objetos", ["👑"]),
    ("trofeo", e_trofeo, ["trophy"], "objetos", ["🏆"]),
    ("regalo", e_regalo, ["gift"], "objetos", ["🎁"]),
    ("pastel", e_pastel, ["cake", "birthday"], "objetos", ["🎂", "🍰"]),
    ("cafe", e_cafe, ["coffee"], "objetos", ["☕"]),
    ("pizza", e_pizza, [], "objetos", ["🍕"]),
    ("confeti", e_confeti, ["tada", "confetti"], "objetos", ["🎉", "🎊"]),
    ("camara", e_camara, ["camera"], "objetos", ["📷", "📸"]),

    ("diamante", e_diamante, ["diamond"], "minecraft", ["💎"]),
    ("esmeralda", e_esmeralda, ["emerald"], "minecraft", []),
    ("oro", e_oro, ["gold", "gold_ingot"], "minecraft", []),
    ("espada", e_espada, ["sword"], "minecraft", ["🗡", "⚔"]),
    ("pico", e_pico, ["pickaxe"], "minecraft", ["⛏"]),
    ("tnt", e_tnt, ["dynamite", "boom"], "minecraft", ["🧨", "💥"]),
    ("creeper", e_creeper, [], "minecraft", []),
    ("manzana", e_manzana, ["apple"], "minecraft", ["🍎"]),
    ("cama", e_cama, ["bed"], "minecraft", ["🛏"]),
    ("portal", e_portal, ["nether_portal"], "minecraft", []),
]


def main():
    from gen_emojis_extra import EXTRA, gui_icons
    all_emojis = EMOJIS + EXTRA
    gui_dir = os.path.join(ASSETS, "textures", "gui")
    os.makedirs(gui_dir, exist_ok=True)
    for name, img in gui_icons().items():
        img.save(os.path.join(gui_dir, name + ".png"))
    tex_dir = os.path.join(ASSETS, "textures", "emoji")
    os.makedirs(tex_dir, exist_ok=True)
    os.makedirs(os.path.join(ASSETS, "font"), exist_ok=True)
    for f in os.listdir(tex_dir):
        if f.endswith(".png"):
            os.remove(os.path.join(tex_dir, f))

    index = []
    providers = []
    images = []
    seen = set()
    for n, (name, fn, aliases, category, unicode) in enumerate(all_emojis):
        assert name not in seen, name
        seen.add(name)
        img = fn()
        assert img.size == (S, S), name
        img.save(os.path.join(tex_dir, name + ".png"))
        images.append((name, img))
        cp = FIRST_CODEPOINT + n
        index.append({"name": name, "aliases": aliases, "category": category, "unicode": unicode,
                      "codepoint": cp})
        providers.append({"type": "bitmap", "file": "chaoschatplus:emoji/" + name + ".png",
                          "height": 8, "ascent": 7, "chars": [chr(cp)]})

    with open(os.path.join(ASSETS, "emojis.json"), "w", encoding="utf-8") as f:
        json.dump(index, f, ensure_ascii=False, indent="\t")
        f.write("\n")
    with open(os.path.join(ASSETS, "font", "emoji.json"), "w", encoding="utf-8") as f:
        json.dump({"providers": providers}, f, ensure_ascii=True, indent="\t")
        f.write("\n")
    print(f"{len(index)} emojis generados")

    if "--preview" in sys.argv:
        cols = 12
        rows = (len(images) + cols - 1) // cols
        scale = 6
        sheet = Image.new("RGBA", (cols * (S + 4) * scale, rows * (S + 4) * scale), rgb("2B2D31"))
        for k, (name, img) in enumerate(images):
            x = (k % cols) * (S + 4) * scale + 2 * scale
            y = (k // cols) * (S + 4) * scale + 2 * scale
            sheet.paste(img.resize((S * scale, S * scale), Image.NEAREST), (x, y), img.resize((S * scale, S * scale), Image.NEAREST))
        out = os.path.join(ROOT, "build", "emoji_preview.png")
        os.makedirs(os.path.dirname(out), exist_ok=True)
        sheet.save(out)
        print("Vista previa:", out)


if __name__ == "__main__":
    main()
