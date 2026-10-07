"""
Segunda tanda de emojis (1.0.1) e iconos de la barra del chat. La importa gen_emojis.py; van detrás de los de la
primera tanda para que sus codepoints no cambien.
"""

from PIL import Image

from gen_emojis import (BLUSH, FACE, HEART, INK, MOUTH, S, TEAR, TEAR_D, TONGUE, WHITE, eyes_big, eyes_closed,
                        eyes_dot, eyes_happy, eyes_line, eyes_side, eyes_wink, eyes_x, face, grid, mouth_flat,
                        mouth_o, mouth_smile, mouth_teeth, mouth_wavy, new, pts, px, rect, rgb, tear)


def circle(img, cx, cy, r, c):
    for y in range(S):
        for x in range(S):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                px(img, x, y, c)


def ring(img, cx, cy, r, c, w=1.0):
    for y in range(S):
        for x in range(S):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if r - w < d <= r:
                px(img, x, y, c)


# Letras y números de 3x5 para los símbolos.
GLYPHS = {
    "0": ["###", "#.#", "#.#", "#.#", "###"], "1": [".#.", "##.", ".#.", ".#.", "###"],
    "2": ["###", "..#", "###", "#..", "###"], "3": ["###", "..#", ".##", "..#", "###"],
    "4": ["#.#", "#.#", "###", "..#", "..#"], "5": ["###", "#..", "###", "..#", "###"],
    "6": ["###", "#..", "###", "#.#", "###"], "7": ["###", "..#", ".#.", ".#.", ".#."],
    "8": ["###", "#.#", "###", "#.#", "###"], "9": ["###", "#.#", "###", "..#", "###"],
    "N": ["#.#", "###", "###", "###", "#.#"], "E": ["###", "#..", "##.", "#..", "###"],
    "W": ["#.#", "#.#", "#.#", "###", "#.#"], "O": ["###", "#.#", "#.#", "#.#", "###"],
    "K": ["#.#", "##.", "#..", "##.", "#.#"], "S": ["###", "#..", "###", "..#", "###"],
    "T": ["###", ".#.", ".#.", ".#.", ".#."], "P": ["###", "#.#", "###", "#..", "#.."],
}


def text(img, s, x, y, c):
    for ch in s:
        rows = GLYPHS[ch]
        for yy, row in enumerate(rows):
            for xx, v in enumerate(row):
                if v == "#":
                    px(img, x + xx, y + yy, c)
        x += 4


def width(s):
    return len(s) * 4 - 1


def badge(label, fill, dark, fg=WHITE):
    i = new()
    rect(i, 1, 2, 14, 13, dark)
    rect(i, 1, 2, 14, 12, fill)
    pts(i, (0, 0, 0, 0), (1, 2), (14, 2), (1, 13), (14, 13))
    text(i, label, 8 - width(label) // 2 - (width(label) % 2), 5, fg)
    return i


# ---------------------------------------------------------------- caras

def e_timido():
    i = face(); eyes_side(i); mouth_wavy(i)
    pts(i, BLUSH, (2, 9), (3, 9), (4, 9), (11, 9), (12, 9), (13, 9))
    return i


def e_guino_sonrisa():
    i = face(); eyes_wink(i); mouth_smile(i); return i


def e_nervioso():
    i = face(); eyes_big(i); mouth_teeth(i)
    pts(i, TEAR, (13, 2), (12, 3), (13, 3)); px(i, 13, 4, TEAR_D)
    return i


def e_bostezo():
    i = face(); eyes_line(i); mouth_o(i)
    return i


def e_zombi():
    i = face("green"); eyes_x(i); mouth_flat(i, rgb("2E5016"))
    pts(i, rgb("4A6B25"), (3, 12), (4, 13), (12, 3), (11, 2))
    return i


def e_robot():
    i = new()
    g = rgb("B0BEC5"); d = rgb("546E7A"); k = rgb("263238"); r = rgb("EF5350")
    rect(i, 7, 0, 8, 2, d); px(i, 7, 0, r); px(i, 8, 0, r)
    rect(i, 1, 3, 14, 14, d); rect(i, 2, 4, 13, 13, g)
    rect(i, 3, 6, 6, 8, k); rect(i, 9, 6, 12, 8, k)
    px(i, 4, 7, rgb("4FC3F7")); px(i, 10, 7, rgb("4FC3F7"))
    for x in range(4, 12, 2):
        rect(i, x, 10, x, 12, k)
    rect(i, 4, 10, 11, 10, k); rect(i, 4, 12, 11, 12, k)
    rect(i, 0, 7, 0, 10, d); rect(i, 15, 7, 15, 10, d)
    return i


def e_alien():
    i = new()
    g = rgb("9CCC65"); d = rgb("558B2F"); k = rgb("1B1B1B")
    grid(i, [
        "....dddddddd....",
        "..ddggggggggdd..",
        ".dggggggggggggd.",
        "dggggggggggggggd",
        "dgkkkkggggkkkkgd",
        "dgkkkkkggkkkkkgd",
        "dggkkkkggkkkkggd",
        ".dggkkggggkkggd.",
        ".dgggggggggggggd",
        "..dgggggggggggd.",
        "...dggggdgggd...",
        "....dgggggggd...",
        ".....ddgggdd....",
        ".......ddd......",
    ], {"g": g, "d": d, "k": k}, 0, 1)
    pts(i, WHITE, (3, 5), (11, 5))
    return i


def e_payaso():
    i = face(); eyes_dot(i)
    w = rgb("FFFFFF"); r = rgb("E53935"); h = rgb("42A5F5")
    rect(i, 3, 4, 6, 8, w); rect(i, 9, 4, 12, 8, w)
    eyes_dot(i)
    circle(i, 7.5, 9, 1.6, r)
    mouth_smile(i, r)
    for x, y in [(0, 3), (1, 2), (2, 1), (13, 1), (14, 2), (15, 3), (0, 4), (15, 4)]:
        px(i, x, y, h)
    return i


def e_gato_feliz():
    i = new()
    y = rgb("FFD54A"); d = rgb("B57A00"); p = rgb("FF8A80")
    grid(i, [
        "dd..........dd..",
        "dyd........dyd..",
        "dypd......dpyd..",
        "dyyddddddddyyd..",
        "dyyyyyyyyyyyyd..",
        "dyyyyyyyyyyyyd..",
        "dyykyyyyyykyyd..",
        "dyyykyyyykyyyd..",
        "dyyyyyyyyyyyyd..",
        "ddyyyyyppyyyydd.",
        "dyyykyykkyykyyd.",
        ".dyyykkyykkyyd..",
        "..ddyyyyyyydd...",
        "....ddddddd.....",
    ], {"y": y, "d": d, "p": p, "k": INK}, 1, 1)
    return i


def e_sin_boca():
    i = face(); eyes_dot(i); return i


def e_mascarilla():
    i = face(); eyes_dot(i)
    w = rgb("FAFAFA"); g = rgb("B0BEC5")
    rect(i, 3, 9, 12, 13, w)
    rect(i, 3, 10, 12, 10, g)
    pts(i, g, (1, 8), (2, 9), (14, 8), (13, 9))
    return i


def e_uwu():
    i = face(); eyes_happy(i)
    pts(i, INK, (6, 10), (7, 11), (8, 10), (9, 11), (10, 10))
    pts(i, BLUSH, (2, 9), (3, 9), (12, 9), (13, 9))
    return i


def e_al_reves():
    i = face(); eyes_dot(i); mouth_smile(i)
    return i.rotate(180)


def e_vaquero():
    i = face(); eyes_dot(i); mouth_smile(i)
    h = rgb("8D6E63"); d = rgb("5D4037")
    out = new()
    out.paste(i.resize((14, 14), Image.NEAREST), (1, 2))
    rect(out, 4, 0, 11, 2, h); rect(out, 0, 3, 15, 3, d); rect(out, 5, 2, 10, 2, d)
    return out


def e_explota():
    i = face(); eyes_big(i); mouth_o(i)
    o = rgb("FF7043"); y = rgb("FFD54F")
    for y0 in range(0, 4):
        rect(i, 3, y0, 12, y0, (0, 0, 0, 0))
    pts(i, o, (3, 2), (5, 0), (7, 1), (9, 0), (11, 2), (6, 3), (10, 3), (4, 3), (12, 3))
    pts(i, y, (6, 1), (8, 2), (10, 1), (5, 2), (7, 3), (9, 3))
    return i


# ---------------------------------------------------------------- animales

def head(rows, colors):
    i = new()
    grid(i, rows, colors, 0, 1)
    return i


def e_gato():
    return head([
        ".kk........kk...",
        ".kok......kok...",
        ".kook....kook...",
        ".kooookkoooook..",
        "koooooooooooook.",
        "koowkooooookwook",
        "kookkooooookkook",
        "koooooopppooooko",
        "kwwoooookooooowk",
        ".kooookwwkoooook",
        "..kkoooooooookk.",
        "....kkkkkkkkk...",
    ], {"k": rgb("4E342E"), "o": rgb("FFA726"), "w": WHITE, "p": rgb("F48FB1")})


def e_perro():
    return head([
        "..bbb......bbb..",
        ".bbbbcccccccbbbb",
        ".bbbcccccccccbbb",
        ".bbcckcccccckcbb",
        ".bbccccccccccbbb",
        "..bccwwwwwwwccb.",
        "...cwwwkkkwwwc..",
        "...cwwwwkwwwwc..",
        "...cwwwkwkwwwc..",
        "....cwwwpwwwc...",
        ".....ccwwwcc....",
        ".......ccc......",
    ], {"b": rgb("6D4C41"), "c": rgb("BCAAA4"), "w": rgb("EFEBE9"), "k": INK, "p": rgb("E57373")})


def e_cerdo():
    i = new()
    p = rgb("F8BBD0"); d = rgb("EC407A"); n = rgb("F48FB1")
    rect(i, 1, 2, 14, 14, d); rect(i, 2, 3, 13, 13, p)
    pts(i, d, (1, 1), (2, 1), (13, 1), (14, 1))
    rect(i, 3, 5, 4, 6, INK); rect(i, 11, 5, 12, 6, INK)
    rect(i, 5, 8, 10, 11, d); rect(i, 6, 9, 9, 10, n)
    px(i, 6, 9, rgb("880E4F")); px(i, 9, 9, rgb("880E4F"))
    return i


def e_vaca():
    i = new()
    w = rgb("FAFAFA"); k = rgb("212121"); p = rgb("F8BBD0"); g = rgb("9E9E9E")
    rect(i, 1, 1, 14, 14, k); rect(i, 2, 2, 13, 13, w)
    rect(i, 2, 2, 5, 5, k); rect(i, 10, 3, 13, 4, k)
    pts(i, g, (0, 1), (15, 1), (0, 2), (15, 2))
    rect(i, 4, 6, 4, 7, k); rect(i, 11, 6, 11, 7, k)
    rect(i, 3, 9, 12, 13, p); px(i, 5, 11, k); px(i, 10, 11, k)
    return i


def e_gallina():
    i = new()
    w = rgb("FAFAFA"); g = rgb("CFD8DC"); r = rgb("E53935"); y = rgb("FFB300")
    rect(i, 3, 2, 12, 15, g); rect(i, 4, 3, 11, 14, w)
    rect(i, 6, 0, 9, 1, r)
    rect(i, 5, 6, 5, 7, INK); rect(i, 10, 6, 10, 7, INK)
    rect(i, 6, 8, 9, 9, y); rect(i, 7, 10, 8, 11, r)
    return i


def e_rana():
    i = new()
    g = rgb("66BB6A"); d = rgb("2E7D32"); w = WHITE
    circle(i, 3.5, 4, 3, d); circle(i, 11.5, 4, 3, d)
    circle(i, 3.5, 4, 2, w); circle(i, 11.5, 4, 2, w)
    rect(i, 3, 3, 4, 5, INK); rect(i, 11, 3, 12, 5, INK)
    rect(i, 1, 7, 14, 14, d); rect(i, 2, 7, 13, 13, g)
    rect(i, 3, 10, 12, 10, d); pts(i, d, (2, 9), (13, 9))
    pts(i, BLUSH, (3, 12), (12, 12))
    return i


def e_abeja():
    i = new()
    y = rgb("FFD54F"); k = rgb("212121"); w = rgb("E1F5FE"); b = rgb("90CAF9")
    circle(i, 5, 3.5, 3, b); circle(i, 10, 3.5, 3, b)
    circle(i, 5, 3.5, 2, w); circle(i, 10, 3.5, 2, w)
    for yy in range(6, 15):
        for xx in range(2, 14):
            if (xx - 7.5) ** 2 / 36 + (yy - 10) ** 2 / 20 <= 1:
                px(i, xx, yy, y if (xx // 2) % 2 == 0 else k)
    rect(i, 0, 9, 1, 10, k)
    px(i, 12, 9, WHITE); px(i, 12, 10, k)
    return i


def e_pez():
    return head([
        "......bbb.......",
        ".....bbbbb......",
        "b...bbbbbbbb....",
        "bb.bblbbbbbbb...",
        "bbbbllbbbbbwkb..",
        "bbbbbbbbbbbwkbb.",
        "bbbbbbbbbbbbbbbb",
        "bbbbddbbbbbbbbb.",
        "bb.bbddbbbbbbb..",
        "b...bbbbbbbbb...",
        ".....bbbbb......",
        "......bbb.......",
    ], {"b": rgb("29B6F6"), "l": rgb("B3E5FC"), "d": rgb("0277BD"), "w": WHITE, "k": INK})


def e_oso():
    i = new()
    b = rgb("8D6E63"); d = rgb("5D4037"); c = rgb("D7CCC8")
    circle(i, 3, 3, 2.6, d); circle(i, 12, 3, 2.6, d)
    circle(i, 3, 3, 1.3, c); circle(i, 12, 3, 1.3, c)
    circle(i, 7.5, 9, 6.5, d); circle(i, 7.5, 9, 5.5, b)
    rect(i, 4, 7, 5, 8, INK); rect(i, 10, 7, 11, 8, INK)
    circle(i, 7.5, 11.5, 2.5, c); rect(i, 7, 10, 8, 11, INK)
    return i


def e_zorro():
    return head([
        "oo..........oo..",
        "ooo........ooo..",
        "owoo......oowo..",
        "owooooooooooowo.",
        "ooooooooooooooo.",
        "ookkooooooookkoo",
        "oooooooooooooooo",
        ".wwwoooooooowww.",
        "..wwwwwooowwww..",
        "...wwwwkkwwww...",
        "....wwwwwwww....",
        ".....wwwwww.....",
        "......wwww......",
    ], {"o": rgb("FB8C00"), "w": rgb("FFF3E0"), "k": INK})


def e_panda():
    i = new()
    w = rgb("FAFAFA"); k = rgb("212121"); g = rgb("B0BEC5")
    circle(i, 3, 3, 2.6, k); circle(i, 12, 3, 2.6, k)
    circle(i, 7.5, 9, 6.5, g); circle(i, 7.5, 9, 5.5, w)
    rect(i, 3, 6, 6, 9, k); rect(i, 9, 6, 12, 9, k)
    px(i, 5, 7, w); px(i, 10, 7, w)
    rect(i, 7, 11, 8, 11, k); pts(i, k, (6, 13), (9, 13), (7, 12), (8, 12))
    return i


def e_pinguino():
    i = new()
    k = rgb("263238"); w = rgb("FAFAFA"); o = rgb("FF9800")
    circle(i, 7.5, 8, 7, k)
    circle(i, 5.5, 9, 3.4, w); circle(i, 9.5, 9, 3.4, w)
    rect(i, 5, 7, 5, 8, k); rect(i, 10, 7, 10, 8, k)
    rect(i, 6, 10, 9, 11, o); rect(i, 7, 12, 8, 12, o)
    return i


# ---------------------------------------------------------------- comida

def e_pan():
    return head([
        "................",
        "....dddddddd....",
        "..ddbbbbbbbbdd..",
        ".dbbblbbbblbbbd.",
        "dbbbbllbbbbllbbd",
        "dbbbbbllbbbbllbd",
        "dbbbbbbbbbbbbbbd",
        "dbbbbbbbbbbbbbbd",
        ".dbbbbbbbbbbbbd.",
        "..dddddddddddd..",
    ], {"d": rgb("8D5524"), "b": rgb("D9A05B"), "l": rgb("F2D29B")})


def e_galleta():
    i = new()
    b = rgb("C68642"); d = rgb("8D5524"); k = rgb("4E342E")
    circle(i, 7.5, 7.5, 7, d); circle(i, 7.5, 7.5, 6, b)
    pts(i, k, (4, 4), (5, 4), (10, 3), (8, 7), (9, 7), (4, 9), (11, 10), (6, 12), (7, 12))
    return i


def e_sandia():
    i = new()
    g = rgb("2E7D32"); l = rgb("A5D6A7"); r = rgb("E53935"); k = INK
    for y in range(4, 15):
        for x in range(S):
            d = ((x - 7.5) ** 2 + (y - 3) ** 2) ** 0.5
            if y >= 4 and d <= 8:
                px(i, x, y, g if d > 7 else l if d > 6 else r)
    pts(i, k, (5, 6), (8, 7), (11, 6), (6, 9), (10, 9), (8, 11))
    return i


def e_zanahoria():
    return head([
        "..........g.g...",
        "...........gg.g.",
        "..........ggg...",
        ".........oogg...",
        "........ooooo...",
        ".......ooooo....",
        "......oodoo.....",
        ".....ooooo......",
        "....ooodo.......",
        "...ooooo........",
        "..oodoo.........",
        ".oooo...........",
        "oo..............",
    ], {"g": rgb("43A047"), "o": rgb("FB8C00"), "d": rgb("E65100")})


def e_huevo():
    i = new()
    w = rgb("FAFAFA"); g = rgb("CFD8DC"); y = rgb("FFB300"); l = rgb("FFE082")
    for y0 in range(S):
        for x in range(S):
            if (x - 7.5) ** 2 / 49 + (y0 - 8) ** 2 / 40 <= 1:
                px(i, x, y0, w)
    ring(i, 7.5, 8, 6.9, g)
    circle(i, 7.5, 8.5, 3.2, y); px(i, 6, 7, l); px(i, 7, 7, l)
    return i


def e_hamburguesa():
    return head([
        "....bbbbbbbb....",
        "..bbbwbbbbwbbb..",
        ".bbbbbbbwbbbbbb.",
        "bbwbbbbbbbbbwbbb",
        "gggggggggggggggg",
        "rrrrrrrrrrrrrrrr",
        "yyyyyyyyyyyyyyyy",
        "mmmmmmmmmmmmmmmm",
        "mmmmmmmmmmmmmmmm",
        "bbbbbbbbbbbbbbbb",
        ".bbbbbbbbbbbbbb.",
    ], {"b": rgb("E0A458"), "w": rgb("FFF3E0"), "g": rgb("7CB342"), "r": rgb("E53935"), "y": rgb("FFD54F"),
        "m": rgb("6D4C41")})


def e_papas():
    return head([
        "...y.y..y.y.....",
        "...yy.y.yy.y....",
        "..yyyyy.yyyy....",
        "..yyyyyyyyyy....",
        ".rrrrrrrrrrrrr..",
        ".rrrrrrrrrrrrr..",
        "..rrrrwrrrrrr...",
        "..rrrwwwrrrrr...",
        "..rrrrwrrrrrr...",
        "...rrrrrrrrr....",
        "...rrrrrrrrr....",
    ], {"y": rgb("FFD54F"), "r": rgb("E53935"), "w": WHITE})


def e_helado():
    return head([
        ".....pppp.......",
        "....pppwpp......",
        "...pppppppp.....",
        "...wwwwwwww.....",
        "..wwwwwcwwww....",
        "..wwcwwwwwww....",
        "..cccccccccc....",
        "...cbcbcbcb.....",
        "...bcbcbcbc.....",
        "....cbcbcb......",
        "....bcbcbc......",
        ".....cbcb.......",
        "......cb........",
    ], {"p": rgb("F48FB1"), "w": rgb("FFF8E1"), "c": rgb("D9A05B"), "b": rgb("A1662F")})


def e_dona():
    i = new()
    b = rgb("D9A05B"); p = rgb("F06292"); s = [rgb("FFEB3B"), rgb("4FC3F7"), WHITE, rgb("81C784")]
    circle(i, 7.5, 7.5, 7, b)
    circle(i, 7.5, 7, 6.2, p)
    circle(i, 7.5, 7.5, 2.2, (0, 0, 0, 0))
    for n, (x, y) in enumerate([(4, 3), (10, 3), (3, 8), (12, 7), (6, 11), (10, 10), (7, 2)]):
        px(i, x, y, s[n % 4])
    return i


def e_manzana_dorada():
    from gen_emojis import e_manzana
    i = e_manzana()
    out = i.copy()
    for y in range(S):
        for x in range(S):
            r, g, b, a = i.getpixel((x, y))
            if a and r > 120 and g < 120:  # rojo -> dorado
                out.putpixel((x, y), (min(255, r + 20), min(255, g + 150), 40, a))
            elif a and r > 80 and g < 60:
                out.putpixel((x, y), (180, 120, 0, a))
    pts(out, rgb("FFF59D"), (3, 6), (4, 5))
    return out


# ---------------------------------------------------------------- naturaleza

def e_sol():
    i = new()
    y = rgb("FFD54F"); o = rgb("FFA000")
    for x, yy in [(7, 0), (8, 0), (7, 15), (8, 15), (0, 7), (0, 8), (15, 7), (15, 8), (2, 2), (13, 2), (2, 13), (13, 13)]:
        px(i, x, yy, o)
    circle(i, 7.5, 7.5, 5, o); circle(i, 7.5, 7.5, 4, y)
    return i


def e_luna():
    i = new()
    y = rgb("FFF59D"); d = rgb("FBC02D")
    circle(i, 7.5, 7.5, 7, d); circle(i, 7.5, 7.5, 6, y)
    circle(i, 11, 5, 6, (0, 0, 0, 0))
    return i


def nube(img, oy=0, c=None, d=None):
    c = c or rgb("FAFAFA"); d = d or rgb("B0BEC5")
    circle(img, 5, 6 + oy, 3.3, d); circle(img, 10, 5 + oy, 4.3, d); circle(img, 13, 8 + oy, 2.5, d)
    rect(img, 2, 8 + oy, 14, 10 + oy, d)
    circle(img, 5, 6 + oy, 2.3, c); circle(img, 10, 5 + oy, 3.3, c); circle(img, 13, 8 + oy, 1.5, c)
    rect(img, 3, 8 + oy, 13, 9 + oy, c)


def e_nube():
    i = new(); nube(i, 2); return i


def e_lluvia():
    i = new(); nube(i, -1, rgb("CFD8DC"), rgb("78909C"))
    for x, y in [(3, 12), (6, 13), (9, 12), (12, 13), (4, 15), (10, 15)]:
        px(i, x, y, TEAR); px(i, x, y - 1, TEAR)
    return i


def e_nieve():
    i = new()
    b = rgb("81D4FA"); w = rgb("E1F5FE")
    for k in range(-6, 7):
        px(i, 7 + k, 7, b); px(i, 7, 7 + k, b); px(i, 7 + k, 7 + k, w); px(i, 7 + k, 7 - k, w)
    pts(i, b, (5, 2), (9, 2), (5, 12), (9, 12), (2, 5), (2, 9), (12, 5), (12, 9))
    return i


def e_arcoiris():
    i = new()
    colors = [rgb("E53935"), rgb("FB8C00"), rgb("FDD835"), rgb("43A047"), rgb("1E88E5"), rgb("8E24AA")]
    for n, c in enumerate(colors):
        for y in range(S):
            for x in range(S):
                d = ((x - 7.5) ** 2 + (y - 13) ** 2) ** 0.5
                if 7.5 - n - 1 < d <= 7.5 - n and y <= 12:
                    px(i, x, y, c)
    w = rgb("FAFAFA")
    for cx in (1.5, 13.5):
        circle(i, cx, 12, 2.2, rgb("B0BEC5")); circle(i, cx, 12, 1.4, w)
    return i


def e_arbol():
    i = new()
    g = rgb("43A047"); d = rgb("2E7D32"); l = rgb("81C784"); t = rgb("6D4C41")
    circle(i, 7.5, 6, 6, d); circle(i, 7, 5.5, 5, g)
    pts(i, l, (5, 3), (6, 2), (4, 5))
    rect(i, 6, 11, 9, 15, t)
    return i


def e_flor():
    i = new()
    p = rgb("F06292"); d = rgb("C2185B"); y = rgb("FFD54F"); g = rgb("43A047")
    rect(i, 7, 9, 8, 15, g); rect(i, 9, 11, 11, 12, g)
    for cx, cy in [(7.5, 2.5), (4, 5.5), (11, 5.5), (5.5, 9), (9.5, 9)]:
        circle(i, cx, cy, 2.3, d); circle(i, cx, cy, 1.5, p)
    circle(i, 7.5, 6, 1.8, y)
    return i


def e_hoja():
    return head([
        ".............gg.",
        "..........gggg..",
        ".......ggglgg...",
        ".....gggglggg...",
        "....ggggglgggg..",
        "...gggglggggg...",
        "...gggglgggg....",
        "..ggglggggg.....",
        "..gglgggg.......",
        "..glggg.........",
        ".dl.............",
        "d...............",
    ], {"g": rgb("66BB6A"), "l": rgb("C5E1A5"), "d": rgb("6D4C41")})


def e_seta():
    i = new()
    r = rgb("E53935"); d = rgb("B71C1C"); w = rgb("FAFAFA"); s = rgb("EFEBE9")
    for y in range(1, 9):
        for x in range(S):
            if (x - 7.5) ** 2 / 56 + (y - 8) ** 2 / 50 <= 1:
                px(i, x, y, r)
    rect(i, 1, 8, 14, 8, d)
    pts(i, w, (4, 4), (5, 4), (4, 5), (10, 3), (11, 3), (8, 6), (12, 6))
    rect(i, 5, 9, 10, 14, s); rect(i, 5, 15, 10, 15, rgb("BCAAA4"))
    return i


def e_cactus():
    i = new()
    g = rgb("43A047"); d = rgb("2E7D32"); p = rgb("A1662F")
    rect(i, 6, 1, 9, 12, d); rect(i, 7, 1, 8, 12, g)
    rect(i, 2, 4, 3, 8, d); rect(i, 2, 8, 6, 9, d)
    rect(i, 12, 3, 13, 7, d); rect(i, 9, 7, 13, 8, d)
    rect(i, 4, 12, 11, 15, p)
    return i


# ---------------------------------------------------------------- objetos

def e_bomba():
    i = new()
    k = rgb("263238"); g = rgb("546E7A"); f = rgb("FFB300"); r = rgb("E53935")
    circle(i, 7, 9.5, 5.6, k); pts(i, g, (4, 7), (5, 6), (4, 8))
    rect(i, 9, 3, 10, 4, g); pts(i, rgb("A1887F"), (11, 2), (12, 1))
    pts(i, f, (13, 0), (14, 1)); pts(i, r, (13, 1))
    return i


def e_llave():
    i = new()
    y = rgb("FFC107"); d = rgb("B57A00")
    ring(i, 4.5, 4.5, 3.6, d, 1.6); ring(i, 4.5, 4.5, 2.6, y, 0.8)
    for k in range(7, 15):
        px(i, k, k, d); px(i, k + 1, k, y)
    pts(i, d, (11, 13), (12, 12), (13, 15), (14, 14))
    return i


def e_candado():
    i = new()
    y = rgb("FFC107"); d = rgb("B57A00"); g = rgb("90A4AE")
    ring(i, 7.5, 6, 4, g, 1.6)
    rect(i, 2, 7, 13, 15, d); rect(i, 3, 8, 12, 14, y)
    rect(i, 7, 10, 8, 12, d)
    return i


def e_campana():
    return head([
        ".......k........",
        "......kyk.......",
        ".....kyyyk......",
        "....kyylyyk.....",
        "....kylyyyk.....",
        "....kylyyyk.....",
        "...kyyyyyyyk....",
        "...kyyyyyyyk....",
        "..kyyyyyyyyyk...",
        ".kkkkkkkkkkkkk..",
        "......kok.......",
        ".......k........",
    ], {"k": rgb("B57A00"), "y": rgb("FFC107"), "l": rgb("FFF59D"), "o": rgb("8D6E63")})


def e_libro():
    i = new()
    r = rgb("C62828"); d = rgb("8E1B1B"); p = rgb("FFF8E1"); g = rgb("FFC107")
    rect(i, 2, 1, 13, 14, d); rect(i, 3, 1, 13, 13, r)
    rect(i, 3, 12, 13, 13, p); rect(i, 3, 13, 13, 13, rgb("E0D6C2"))
    rect(i, 6, 4, 11, 5, g); rect(i, 7, 7, 10, 7, g)
    return i


def e_lapiz():
    return head([
        ".............kk.",
        "............kppk",
        "...........kgggk",
        "..........kyyyk.",
        ".........kyyyk..",
        "........kyyyk...",
        ".......kyyyk....",
        "......kyyyk.....",
        ".....kyyyk......",
        "....kyyyk.......",
        "...kwwwk........",
        "..kwwwk.........",
        "..kwkk..........",
        "..kk............",
    ], {"k": rgb("5D4037"), "p": rgb("F48FB1"), "g": rgb("B0BEC5"), "y": rgb("FFC107"), "w": rgb("F5DEB3")})


def e_bombilla():
    i = new()
    y = rgb("FFEB3B"); l = rgb("FFF9C4"); d = rgb("F9A825"); g = rgb("90A4AE")
    circle(i, 7.5, 6, 5.4, d); circle(i, 7.5, 6, 4.5, y)
    pts(i, l, (5, 3), (5, 4), (6, 3))
    rect(i, 5, 11, 10, 11, d)
    rect(i, 5, 12, 10, 14, g); rect(i, 5, 13, 10, 13, rgb("607D8B")); rect(i, 6, 15, 9, 15, rgb("607D8B"))
    return i


def e_reloj():
    i = new()
    k = rgb("37474F"); w = rgb("FAFAFA"); r = rgb("E53935")
    circle(i, 7.5, 7.5, 7, k); circle(i, 7.5, 7.5, 6, w)
    rect(i, 7, 3, 8, 8, k); rect(i, 8, 7, 11, 8, k); px(i, 8, 9, r)
    pts(i, k, (7, 1), (8, 1), (13, 7), (13, 8), (2, 7), (2, 8), (7, 13), (8, 13))
    return i


def e_bolsa_dinero():
    return head([
        ".....kkkkk......",
        "......kgk.......",
        ".....kkkkk......",
        "....kggggk......",
        "...kgggggggk....",
        "..kgggg$ggggk...",
        ".kgggg$$$ggggk..",
        ".kggg$g$gggggk..",
        ".kgggg$$$ggggk..",
        ".kggggg$g$gggk..",
        ".kgggg$$$ggggk..",
        "..kggggg$gggk...",
        "...kkkkkkkkk....",
    ], {"k": rgb("6D4C41"), "g": rgb("D7B377"), "$": rgb("2E7D32")})


def e_moneda():
    i = new()
    y = rgb("FFC107"); d = rgb("B57A00"); l = rgb("FFF59D")
    circle(i, 7.5, 7.5, 7, d); circle(i, 7.5, 7.5, 6, y); ring(i, 7.5, 7.5, 4.6, d)
    rect(i, 7, 5, 8, 10, d); pts(i, l, (4, 4), (5, 3))
    return i


def e_escudo():
    return head([
        "kkkkkkkkkkkkkk..",
        "kbbbbbbwwwwwwk..",
        "kbbbbbbwwwwwwk..",
        "kbbbbbbwwwwwwk..",
        "kbbbbbbwwwwwwk..",
        "kwwwwwwbbbbbbk..",
        "kwwwwwwbbbbbbk..",
        ".kwwwwwbbbbbk...",
        ".kwwwwwbbbbbk...",
        "..kwwwwbbbbk....",
        "...kwwwbbbk.....",
        "....kkkkkk......",
    ], {"k": rgb("5D4037"), "b": rgb("1E88E5"), "w": rgb("ECEFF1")})


def e_arco():
    return head([
        "....kkk.........",
        "......kks.......",
        ".......k.s......",
        "........k.s.....",
        "........k..s....",
        "........k...s...",
        "........k...s...",
        "........k..s....",
        "........k.s.....",
        ".......k.s......",
        "......kks.......",
        "....kkk.........",
    ], {"k": rgb("8D6E63"), "s": rgb("ECEFF1")})


def e_flecha():
    return head([
        ".........gggg...",
        "..........ggg...",
        ".........g.gg...",
        "........k...g...",
        ".......k........",
        "......k.........",
        ".....k..........",
        "....k...........",
        "w..k............",
        ".wk.............",
        "ww..............",
    ], {"g": rgb("90A4AE"), "k": rgb("8D6E63"), "w": WHITE})


def e_pocion():
    i = new()
    g = rgb("CFD8DC"); p = rgb("E91E63"); l = rgb("F8BBD0"); c = rgb("8D6E63")
    rect(i, 6, 0, 9, 2, c)
    rect(i, 6, 3, 9, 5, g)
    circle(i, 7.5, 10.5, 5, g); circle(i, 7.5, 10.5, 4, p)
    rect(i, 3, 7, 12, 8, g)
    pts(i, l, (5, 9), (5, 10))
    return i


def e_mapa():
    i = new()
    p = rgb("F5E6C8"); d = rgb("A1887F"); g = rgb("81C784"); b = rgb("64B5F6"); r = rgb("E53935")
    rect(i, 1, 2, 14, 13, d); rect(i, 2, 3, 13, 12, p)
    rect(i, 3, 4, 6, 7, g); rect(i, 9, 8, 12, 11, b)
    pts(i, r, (10, 4), (11, 5), (11, 4), (10, 5))
    pts(i, d, (5, 9), (6, 10), (7, 10), (8, 9))
    return i


def e_brujula():
    i = new()
    k = rgb("546E7A"); w = rgb("ECEFF1"); r = rgb("E53935"); b = rgb("1E88E5")
    circle(i, 7.5, 7.5, 7, k); circle(i, 7.5, 7.5, 6, w)
    for n in range(4):
        px(i, 7, 3 + n, r); px(i, 8, 3 + n, r)
        px(i, 7, 8 + n, b); px(i, 8, 8 + n, b)
    return i


def e_antorcha():
    return head([
        "......y.........",
        ".....yoy........",
        ".....ooo........",
        "......w.........",
        "......k.........",
        "......k.........",
        "......k.........",
        "......k.........",
        "......k.........",
        "......k.........",
        "......k.........",
    ], {"y": rgb("FFEB3B"), "o": rgb("FF9800"), "w": rgb("FFF8E1"), "k": rgb("6D4C41")})


# ---------------------------------------------------------------- símbolos

def arrow(direction):
    i = new()
    b = rgb("1E88E5"); d = rgb("0D47A1")
    rect(i, 1, 1, 14, 14, d); rect(i, 1, 1, 14, 13, b)
    pts(i, (0, 0, 0, 0), (1, 1), (14, 1), (1, 14), (14, 14))
    rows = ["....w....", "...www...", "..wwwww..", ".wwwwwww.", "wwwwwwwww", "...www...", "...www...", "...www..."]
    tmp = new()
    grid(tmp, rows, {"w": WHITE}, 3, 3)
    tmp = tmp.rotate({"up": 0, "left": 90, "down": 180, "right": -90}[direction])
    i.alpha_composite(tmp)
    return i


def e_mas():
    i = new(); g = rgb("43A047")
    rect(i, 6, 2, 9, 13, g); rect(i, 2, 6, 13, 9, g); return i


def e_menos():
    i = new(); rect(i, 2, 6, 13, 9, rgb("E53935")); return i


def number(n):
    return badge(str(n), rgb("546E7A"), rgb("37474F"))


def e_stop():
    i = new()
    r = rgb("E53935"); d = rgb("8E1B1B")
    for y in range(S):
        for x in range(S):
            if abs(x - 7.5) + abs(y - 7.5) <= 10.5 and abs(x - 7.5) <= 7.5 and abs(y - 7.5) <= 7.5:
                px(i, x, y, d)
            if abs(x - 7.5) + abs(y - 7.5) <= 9.5 and abs(x - 7.5) <= 6.5 and abs(y - 7.5) <= 6.5:
                px(i, x, y, r)
    rect(i, 3, 7, 12, 8, WHITE)
    return i


def e_prohibido():
    i = new()
    r = rgb("E53935")
    ring(i, 7.5, 7.5, 7.2, r, 1.8)
    for k in range(3, 13):
        px(i, k, k, r); px(i, k + 1, k, r)
    return i


# ---------------------------------------------------------------- minecraft (dibujos propios)

def e_slime():
    i = new()
    g = rgb("7CC56B"); d = rgb("4E9A3F"); l = rgb("B5E3A8")
    rect(i, 1, 1, 14, 14, d); rect(i, 2, 2, 13, 13, g)
    rect(i, 3, 5, 5, 7, rgb("2E5E25")); rect(i, 10, 5, 12, 7, rgb("2E5E25"))
    rect(i, 7, 10, 8, 10, rgb("2E5E25"))
    pts(i, l, (3, 3), (4, 3), (3, 4))
    return i


def e_zombie():
    i = new()
    g = rgb("5F9E4E"); d = rgb("3F7034"); k = rgb("1B1B1B")
    rect(i, 1, 1, 14, 14, g)
    for (x, y) in [(2, 2), (12, 3), (4, 12), (10, 11), (6, 2)]:
        px(i, x, y, d)
    rect(i, 3, 6, 6, 7, k); rect(i, 9, 6, 12, 7, k)
    rect(i, 5, 10, 10, 11, rgb("2E4F25"))
    return i


def e_esqueleto():
    i = new()
    w = rgb("CFCFCF"); g = rgb("9E9E9E"); k = rgb("1B1B1B")
    rect(i, 1, 1, 14, 14, w)
    for (x, y) in [(2, 2), (12, 3), (4, 12), (11, 12)]:
        px(i, x, y, g)
    rect(i, 3, 5, 6, 7, k); rect(i, 9, 5, 12, 7, k)
    rect(i, 7, 8, 8, 9, k)
    rect(i, 4, 11, 11, 11, k); pts(i, k, (5, 12), (7, 12), (9, 12))
    return i


def e_enderman():
    i = new()
    k = rgb("161616"); p = rgb("E040FB"); l = rgb("F3B6FF")
    rect(i, 1, 1, 14, 14, k)
    rect(i, 2, 7, 6, 8, p); rect(i, 9, 7, 13, 8, p)
    px(i, 4, 7, l); px(i, 11, 7, l)
    return i


def e_perla():
    i = new()
    t = rgb("1E8C7E"); d = rgb("0D4F47"); l = rgb("7EE0D2")
    circle(i, 7.5, 7.5, 6, d); circle(i, 7.5, 7.5, 5, t)
    circle(i, 7.5, 7.5, 2, rgb("0A3A34")); pts(i, l, (5, 4), (4, 5), (5, 5))
    return i


def e_ojo_ender():
    i = new()
    g = rgb("6FBE8A"); d = rgb("2E6B48"); k = rgb("102018"); y = rgb("D4E157")
    circle(i, 7.5, 7.5, 6, d); circle(i, 7.5, 7.5, 5, g)
    rect(i, 6, 5, 9, 10, y); rect(i, 7, 5, 8, 10, k)
    return i


def e_estrella_nether():
    i = new()
    w = rgb("FFFDE7"); y = rgb("FFF176"); d = rgb("C0A000")
    for k in range(-6, 7):
        px(i, 7 + k, 7, y); px(i, 7, 7 + k, y)
    for k in range(-4, 5):
        px(i, 7 + k, 7 + k, d); px(i, 7 + k, 7 - k, d)
    circle(i, 7, 7, 2, w)
    return i


def e_totem():
    return head([
        "....ggggggg.....",
        "...gyyyyyyyg....",
        "...gykyyykyg....",
        "...gyyyyyyyg....",
        "...ggyykyygg....",
        ".gggyyyyyyyggg..",
        "gyygggkgkgggyyg.",
        "gyyg.gyyyg..gyyg",
        ".gg..ggggg...gg.",
        ".....gyyyg......",
        ".....gy.yg......",
        ".....gg.gg......",
    ], {"g": rgb("B57A00"), "y": rgb("FFC107"), "k": rgb("2E7D32")})


def e_cubo():
    i = new()
    g = rgb("B0BEC5"); d = rgb("607D8B"); w = rgb("42A5F5")
    rect(i, 2, 4, 13, 5, d); rect(i, 3, 5, 12, 14, g); rect(i, 3, 14, 12, 14, d)
    rect(i, 3, 5, 12, 6, w)
    ring(i, 7.5, 4, 6, d)
    return i


def e_cofre():
    i = new()
    w = rgb("A1662F"); d = rgb("5D3A1A"); l = rgb("C68642"); m = rgb("B0BEC5")
    rect(i, 1, 2, 14, 14, d); rect(i, 2, 3, 13, 7, l); rect(i, 2, 8, 13, 13, w)
    rect(i, 1, 7, 14, 7, d)
    rect(i, 7, 6, 8, 9, m)
    return i


def e_mesa_crafteo():
    i = new()
    t = rgb("A1662F"); d = rgb("5D3A1A"); g = rgb("8D6E63"); s = rgb("B0BEC5")
    rect(i, 1, 1, 14, 14, d); rect(i, 2, 2, 13, 13, t)
    for k in range(2, 14, 4):
        rect(i, k, 2, k, 13, g); rect(i, 2, k, 13, k, g)
    pts(i, s, (4, 4), (5, 5), (10, 4), (11, 5))
    return i


def e_cesped():
    i = new()
    g = rgb("6AAE3E"); d = rgb("4A8A2A"); b = rgb("8B5A2B"); l = rgb("A87445")
    rect(i, 1, 1, 14, 14, b)
    rect(i, 1, 1, 14, 4, g)
    for x in range(1, 15, 2):
        px(i, x, 5, g)
    for (x, y) in [(3, 2), (8, 3), (12, 2)]:
        px(i, x, y, d)
    for (x, y) in [(4, 8), (10, 10), (6, 12), (12, 7)]:
        px(i, x, y, l)
    return i


def e_obsidiana():
    i = new()
    k = rgb("140E1E"); p = rgb("3C2A5A"); l = rgb("7A5BB0")
    rect(i, 1, 1, 14, 14, k)
    for (x, y) in [(3, 3), (4, 4), (9, 2), (11, 8), (5, 10), (6, 11), (12, 12)]:
        px(i, x, y, p)
    pts(i, l, (4, 3), (10, 8))
    return i


def e_lava():
    i = new()
    o = rgb("FF7A00"); y = rgb("FFC93C"); r = rgb("C83A00")
    rect(i, 1, 1, 14, 14, o)
    for (x, y0) in [(3, 3), (4, 3), (9, 5), (10, 6), (5, 10), (12, 11), (7, 13)]:
        px(i, x, y0, y)
    for (x, y0) in [(2, 7), (8, 2), (12, 3), (3, 13), (10, 9)]:
        px(i, x, y0, r)
    return i


def e_agua():
    i = new()
    b = rgb("3F76E4"); l = rgb("7DA6F5"); d = rgb("2B55B0")
    rect(i, 1, 1, 14, 14, b)
    for y in (3, 8, 12):
        for x in range(2, 14, 4):
            px(i, x, y, l); px(i, x + 1, y, l)
    pts(i, d, (6, 5), (11, 10), (3, 10))
    return i


def ingot(main, light, dark):
    return head([
        "................",
        "................",
        "......kkkkkkkkk.",
        ".....kllllllldk.",
        "....kllmmmmmddk.",
        "...kkkkkkkkkdk..",
        "..klmmmmmmmdk...",
        ".klmmmmmmmddk...",
        "kkkkkkkkkkkk....",
    ], {"k": dark, "l": light, "m": main, "d": dark})


def e_hierro():
    return ingot(rgb("D8D8D8"), rgb("FFFFFF"), rgb("7A7A7A"))


def e_netherita():
    return ingot(rgb("4A4146"), rgb("7A6E73"), rgb("231E21"))


def e_redstone():
    i = new()
    r = rgb("E53935"); d = rgb("8E1B1B"); l = rgb("FF8A80")
    for (x, y) in [(6, 3), (7, 3), (5, 4), (6, 4), (7, 4), (8, 4), (4, 6), (5, 6), (9, 7), (10, 7), (10, 8), (6, 9),
                   (7, 9), (8, 10), (5, 11), (6, 11), (11, 11), (9, 12), (3, 9), (12, 5)]:
        px(i, x, y, r)
    for (x, y) in [(7, 5), (6, 10), (10, 9), (4, 7)]:
        px(i, x, y, d)
    pts(i, l, (6, 3), (9, 7))
    return i


def e_orbe_xp():
    i = new()
    g = rgb("B5E61D"); y = rgb("F0FF6E"); d = rgb("6F9E00")
    circle(i, 7.5, 7.5, 5.5, d); circle(i, 7.5, 7.5, 4.5, g); circle(i, 6.5, 6.5, 2, y)
    return i


def e_spawner():
    i = new()
    k = rgb("1E2A33"); g = rgb("455A64"); f = rgb("FF7043")
    rect(i, 1, 1, 14, 14, k)
    for v in (1, 5, 10, 14):
        rect(i, v, 1, v, 14, g); rect(i, 1, v, 14, v, g)
    circle(i, 7.5, 8, 2.2, f)
    return i


# ---------------------------------------------------------------- lista (se añade al final de EMOJIS)

EXTRA = [
    ("timido", e_timido, ["shy"], "caras", []),
    ("guino_sonrisa", e_guino_sonrisa, ["wink_smile"], "caras", []),
    ("nervioso", e_nervioso, ["grimacing", "nervous"], "caras", ["😬"]),
    ("bostezo", e_bostezo, ["yawn"], "caras", ["🥱"]),
    ("zombi", e_zombi, ["zombie_face"], "caras", ["🧟"]),
    ("robot", e_robot, ["bot"], "caras", ["🤖"]),
    ("alien", e_alien, ["alien_face"], "caras", ["👽"]),
    ("payaso", e_payaso, ["clown"], "caras", ["🤡"]),
    ("gato_feliz", e_gato_feliz, ["smiley_cat"], "caras", ["😺", "😸"]),
    ("sin_boca", e_sin_boca, ["no_mouth"], "caras", ["😶"]),
    ("mascarilla", e_mascarilla, ["mask"], "caras", ["😷"]),
    ("uwu", e_uwu, ["owo"], "caras", []),
    ("al_reves", e_al_reves, ["upside_down"], "caras", ["🙃"]),
    ("vaquero", e_vaquero, ["cowboy"], "caras", ["🤠"]),
    ("explota", e_explota, ["exploding_head", "mind_blown"], "caras", ["🤯"]),

    ("gato", e_gato, ["cat"], "animales", ["🐱"]),
    ("perro", e_perro, ["dog"], "animales", ["🐶"]),
    ("cerdo", e_cerdo, ["pig"], "animales", ["🐷"]),
    ("vaca", e_vaca, ["cow"], "animales", ["🐮"]),
    ("gallina", e_gallina, ["chicken"], "animales", ["🐔"]),
    ("rana", e_rana, ["frog"], "animales", ["🐸"]),
    ("abeja", e_abeja, ["bee"], "animales", ["🐝"]),
    ("pez", e_pez, ["fish"], "animales", ["🐟", "🐠"]),
    ("oso", e_oso, ["bear"], "animales", ["🐻"]),
    ("zorro", e_zorro, ["fox"], "animales", ["🦊"]),
    ("panda", e_panda, [], "animales", ["🐼"]),
    ("pinguino", e_pinguino, ["penguin"], "animales", ["🐧"]),

    ("pan", e_pan, ["bread"], "comida", ["🍞"]),
    ("galleta", e_galleta, ["cookie"], "comida", ["🍪"]),
    ("sandia", e_sandia, ["watermelon"], "comida", ["🍉"]),
    ("zanahoria", e_zanahoria, ["carrot"], "comida", ["🥕"]),
    ("huevo", e_huevo, ["egg"], "comida", ["🥚", "🍳"]),
    ("hamburguesa", e_hamburguesa, ["burger", "hamburger"], "comida", ["🍔"]),
    ("papas", e_papas, ["fries", "patatas"], "comida", ["🍟"]),
    ("helado", e_helado, ["ice_cream"], "comida", ["🍦"]),
    ("dona", e_dona, ["donut"], "comida", ["🍩"]),
    ("manzana_dorada", e_manzana_dorada, ["golden_apple"], "comida", []),

    ("sol", e_sol, ["sun", "sunny"], "naturaleza", ["☀", "🌞"]),
    ("luna", e_luna, ["moon"], "naturaleza", ["🌙"]),
    ("nube", e_nube, ["cloud"], "naturaleza", ["☁"]),
    ("lluvia", e_lluvia, ["rain"], "naturaleza", ["🌧"]),
    ("nieve", e_nieve, ["snowflake", "snow"], "naturaleza", ["❄"]),
    ("arcoiris", e_arcoiris, ["rainbow"], "naturaleza", ["🌈"]),
    ("arbol", e_arbol, ["tree"], "naturaleza", ["🌳"]),
    ("flor", e_flor, ["flower"], "naturaleza", ["🌸", "🌺"]),
    ("hoja", e_hoja, ["leaf"], "naturaleza", ["🍃", "🌿"]),
    ("seta", e_seta, ["mushroom"], "naturaleza", ["🍄"]),
    ("cactus", e_cactus, [], "naturaleza", ["🌵"]),

    ("bomba", e_bomba, ["bomb"], "objetos", ["💣"]),
    ("llave", e_llave, ["key"], "objetos", ["🔑"]),
    ("candado", e_candado, ["lock"], "objetos", ["🔒"]),
    ("campana", e_campana, ["bell"], "objetos", ["🔔"]),
    ("libro", e_libro, ["book"], "objetos", ["📕", "📖"]),
    ("lapiz", e_lapiz, ["pencil"], "objetos", ["✏"]),
    ("bombilla", e_bombilla, ["bulb", "idea"], "objetos", ["💡"]),
    ("reloj", e_reloj, ["clock"], "objetos", ["⏰", "🕒"]),
    ("bolsa_dinero", e_bolsa_dinero, ["moneybag"], "objetos", ["💰"]),
    ("moneda", e_moneda, ["coin"], "objetos", ["🪙"]),
    ("escudo", e_escudo, ["shield"], "objetos", ["🛡"]),
    ("arco", e_arco, ["bow"], "objetos", ["🏹"]),
    ("flecha", e_flecha, ["arrow"], "objetos", []),
    ("pocion", e_pocion, ["potion"], "objetos", ["🧪"]),
    ("mapa", e_mapa, ["map"], "objetos", ["🗺"]),
    ("brujula", e_brujula, ["compass"], "objetos", ["🧭"]),
    ("antorcha", e_antorcha, ["torch"], "objetos", []),

    ("arriba", lambda: arrow("up"), ["up"], "simbolos", ["⬆"]),
    ("abajo", lambda: arrow("down"), ["down"], "simbolos", ["⬇"]),
    ("izquierda", lambda: arrow("left"), ["left"], "simbolos", ["⬅"]),
    ("derecha", lambda: arrow("right"), ["right"], "simbolos", ["➡"]),
    ("mas", e_mas, ["plus"], "simbolos", ["➕"]),
    ("menos", e_menos, ["minus"], "simbolos", ["➖"]),
] + [
    ("n" + str(n), (lambda k: (lambda: number(k)))(n), ["num" + str(n)], "simbolos", [])
    for n in range(11)
] + [
    ("new", lambda: badge("NEW", rgb("1E88E5"), rgb("0D47A1")), ["nuevo"], "simbolos", ["🆕"]),
    ("ok_simbolo", lambda: badge("OK", rgb("43A047"), rgb("1B5E20")), ["ok_button"], "simbolos", ["🆗"]),
    ("sos", lambda: badge("SOS", rgb("E53935"), rgb("8E1B1B")), [], "simbolos", ["🆘"]),
    ("stop", e_stop, ["alto"], "simbolos", ["🛑"]),
    ("prohibido", e_prohibido, ["no_entry", "forbidden"], "simbolos", ["🚫", "⛔"]),

    ("slime", e_slime, [], "minecraft", []),
    ("zombie", e_zombie, [], "minecraft", []),
    ("esqueleto", e_esqueleto, ["skeleton"], "minecraft", []),
    ("enderman", e_enderman, [], "minecraft", []),
    ("perla", e_perla, ["ender_pearl"], "minecraft", []),
    ("ojo_ender", e_ojo_ender, ["eye_of_ender"], "minecraft", []),
    ("estrella_nether", e_estrella_nether, ["nether_star"], "minecraft", []),
    ("totem", e_totem, [], "minecraft", []),
    ("cubo", e_cubo, ["bucket"], "minecraft", ["🪣"]),
    ("cofre", e_cofre, ["chest"], "minecraft", []),
    ("mesa_crafteo", e_mesa_crafteo, ["crafting_table"], "minecraft", []),
    ("cesped", e_cesped, ["grass_block", "grass"], "minecraft", []),
    ("obsidiana", e_obsidiana, ["obsidian"], "minecraft", []),
    ("lava", e_lava, [], "minecraft", ["🌋"]),
    ("agua", e_agua, ["water"], "minecraft", ["💧", "🌊"]),
    ("hierro", e_hierro, ["iron", "iron_ingot"], "minecraft", []),
    ("netherita", e_netherita, ["netherite"], "minecraft", []),
    ("redstone", e_redstone, [], "minecraft", []),
    ("orbe_xp", e_orbe_xp, ["xp", "exp"], "minecraft", []),
    ("spawner", e_spawner, [], "minecraft", []),
]


# ---------------------------------------------------------------- iconos de la barra (blanco y negro)

def gui_icons():
    """Iconos monocromos (blanco con sombra gris) para los botones de la barra del chat."""
    w = rgb("F2F2F2")
    s = rgb("6E7480")
    icons = {}

    i = new()
    ring(i, 7.5, 7.5, 7, w, 1.4)
    rect(i, 5, 5, 5, 7, w); rect(i, 10, 5, 10, 7, w)
    pts(i, w, (4, 10), (11, 10), (5, 11), (10, 11)); rect(i, 6, 12, 9, 12, w)
    icons["btn_emoji"] = i

    i = new()
    for k in range(4, 14):
        px(i, k, 15 - k, w); px(i, k + 1, 15 - k, w)
    pts(i, w, (2, 10), (3, 11), (4, 12), (5, 13), (3, 9), (6, 14))
    pts(i, w, (2, 13), (1, 14), (3, 12))
    icons["btn_item"] = i

    i = new()
    rect(i, 5, 2, 10, 3, w)
    rect(i, 1, 4, 14, 13, w); rect(i, 2, 5, 13, 12, (0, 0, 0, 0))
    ring(i, 7.5, 8.5, 3.2, w, 1.3)
    px(i, 12, 6, w)
    icons["btn_shot"] = i

    i = new()
    for y in range(S):
        for x in range(S):
            # Flecha de enviar (triángulo de papel apuntando a la derecha).
            if x >= 2 and abs(y - 7.5) <= (14 - x) * 0.55 and not (x < 7 and abs(y - 7.5) < 1.2):
                px(i, x, y, w)
    icons["btn_send"] = i

    i = new()
    rect(i, 1, 3, 14, 12, w)
    rect(i, 2, 4, 13, 11, (0, 0, 0, 0))
    for k in range(6):
        px(i, 2 + k, 4 + k, w)
        px(i, 13 - k, 4 + k, w)
    icons["btn_dm"] = i

    out = {}
    for name, img in icons.items():
        shadow = new()
        for y in range(S):
            for x in range(S):
                if img.getpixel((x, y))[3] and y + 1 < S and not img.getpixel((x, y + 1))[3]:
                    shadow.putpixel((x, y + 1), s)
        shadow.alpha_composite(img)
        out[name] = shadow
    return out
