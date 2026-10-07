"""Copia gifs-anime/ a la carpeta de GIFs de un servidor (por defecto run-server), con categorías en español.

Uso: python tools/install_anime.py [ruta/al/servidor]
"""
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
NAMES = {
    "laugh": "risa", "dance": "baile", "happy": "feliz", "smile": "sonrisa", "hug": "abrazo", "wave": "saludo",
    "thumbsup": "pulgar", "cry": "llorar", "clap": "aplausos", "kiss": "beso", "angry": "enojo",
    "shocked": "sorpresa", "facepalm": "facepalm", "sleep": "dormir",
}

server = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "run-server"
dest = server / "config" / "chaoschatplus" / "gifs" / "anime"
n = 0
for cat_dir in sorted((ROOT / "gifs-anime").iterdir()):
    if not cat_dir.is_dir():
        continue
    out = dest / NAMES.get(cat_dir.name, cat_dir.name)
    out.mkdir(parents=True, exist_ok=True)
    for gif in cat_dir.glob("*.gif"):
        if not (out / gif.name).exists():
            shutil.copy2(gif, out / gif.name)
        n += 1
print(f"{n} GIFs en {dest}")
