"""Descarga GIFs de anime de nekos.best a gifs-anime/<categoría>/ (fuera del jar y de git).

Primero reutiliza los que ya bajaron los clientes de prueba (run/, run2/), luego pide los que falten
con calma (1 petición cada 2 s, respetando Retry-After). Uso: python tools/download_nekos.py
Ojo: los términos de nekos.best no permiten volver a publicarlos; no los metas en el mod publicado.
"""
import hashlib
import json
import re
import shutil
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "gifs-anime"
TARGETS = {
    "laugh": 30, "dance": 30, "happy": 24, "smile": 24, "hug": 24, "wave": 24, "thumbsup": 20,
    "cry": 24, "clap": 18, "kiss": 18, "angry": 18, "shocked": 18, "facepalm": 14, "sleep": 14,
}
UA = "ChaosChatPlus/1.2 (tools/download_nekos.py)"
GAP = 2.0
last = 0.0


def slug(s):
    return re.sub(r"[^a-z0-9]+", "_", s.lower()).strip("_")[:32] or "anime"


def get(url):
    global last
    while True:
        wait = GAP - (time.time() - last)
        if wait > 0:
            time.sleep(wait)
        last = time.time()
        req = urllib.request.Request(url, headers={"User-Agent": UA})
        try:
            with urllib.request.urlopen(req, timeout=20) as r:
                return r.read()
        except urllib.error.HTTPError as e:
            if e.code == 429:
                secs = min(int(e.headers.get("Retry-After", "120")), 3600)
                print(f"  429: espero {secs} s")
                time.sleep(secs)
                continue
            print(f"  HTTP {e.code}: {url}")
            return None
        except Exception as e:  # noqa: BLE001
            print(f"  error {e}: {url}")
            return None


def main():
    OUT.mkdir(exist_ok=True)
    index_file = OUT / "fuentes.json"
    index = json.loads(index_file.read_text(encoding="utf-8")) if index_file.exists() else {}

    def save_index():
        tmp = index_file.with_suffix(".tmp")
        tmp.write_text(json.dumps(index, indent=2, ensure_ascii=False), encoding="utf-8")
        tmp.replace(index_file)

    def have(cat):
        return sum(1 for v in index.values() if v["category"] == cat and (OUT / v["file"]).exists())

    def store(cat, url, anime, data):
        name = f"{cat}/{slug(anime)}_{url.rsplit('/', 1)[-1][:8]}.gif"
        (OUT / cat).mkdir(exist_ok=True)
        (OUT / name).write_bytes(data)
        index[url] = {"category": cat, "anime": anime, "file": name}

    # 1) Reutilizar la caché de los clientes de prueba.
    for run in ("run", "run2"):
        cache = ROOT / run / "chaoschatplus" / "cache" / "gifs"
        idx = cache / "nekos.json"
        if not idx.exists():
            continue
        try:
            saved = json.loads(idx.read_text(encoding="utf-8"))
        except Exception:  # noqa: BLE001
            continue
        for cat, entries in saved.items():
            for e in entries:
                url = e["url"]
                src = cache / (hashlib.sha1(url.encode()).hexdigest() + ".gif")
                if url in index or cat not in TARGETS or have(cat) >= TARGETS[cat] or not src.exists():
                    continue
                store(cat, url, e.get("anime", "anime"), src.read_bytes())
    save_index()
    print("Desde la caché:", len(index))

    # 2) Pedir los que falten.
    for cat, target in TARGETS.items():
        tries = 0
        while have(cat) < target and tries < 15:
            tries += 1
            need = target - have(cat)
            body = get(f"https://nekos.best/api/v2/{cat}?amount={min(20, need + 2)}")
            if not body:
                continue
            for r in json.loads(body).get("results", []):
                url = r.get("url", "")
                if have(cat) >= target or not url.endswith(".gif") or url in index:
                    continue
                data = get(url)
                if data and len(data) <= 4 * 1024 * 1024:
                    store(cat, url, r.get("anime_name", "anime"), data)
                    save_index()
            print(f"{cat}: {have(cat)}/{target}")
    print("Total:", len(index))


if __name__ == "__main__":
    main()
