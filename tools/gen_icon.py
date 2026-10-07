"""Icono del mod (128x128): burbuja de chat con un emoji, en pixel art. Uso: python tools/gen_icon.py"""
import os

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
EMOJI = os.path.join(ROOT, "src", "main", "resources", "assets", "chaoschatplus", "textures", "emoji")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "chaoschatplus", "icon.png")

S = 32
img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
# Fondo: cuadrado redondeado oscuro con borde azul.
d.rounded_rectangle([0, 0, S - 1, S - 1], radius=6, fill=(24, 26, 33, 255), outline=(95, 134, 232, 255))
# Burbuja de chat.
d.rounded_rectangle([4, 5, 27, 21], radius=4, fill=(122, 162, 255, 255))
d.polygon([(8, 21), (8, 26), (13, 21)], fill=(122, 162, 255, 255))
for x in (9, 15, 21):
    d.rectangle([x, 12, x + 1, 13], fill=(255, 255, 255, 255))
img = img.resize((128, 128), Image.NEAREST)
# Emoji abajo a la derecha.
smile = Image.open(os.path.join(EMOJI, "risa.png")).resize((56, 56), Image.NEAREST)
img.alpha_composite(smile, (66, 66))
img.save(OUT)
print("Icono:", OUT)
