"""Validate a completed world save across Minecraft's old and new log formats."""
import gzip
from pathlib import Path


def clean_save(home: Path) -> bool:
    log = (home / 'server/logs/latest.log').read_text(encoding='utf-8', errors='replace')
    if 'All dimensions are saved' in log or 'All chunks are saved' in log:
        return True
    shutdown = log.rsplit('Stopping server', 1)
    if len(shutdown) != 2 or 'Saving players' not in shutdown[1] or 'Saving worlds' not in shutdown[1]:
        return False
    if not all("Saving chunks for level 'ServerLevel[world]'/minecraft:" + dimension in shutdown[1]
               for dimension in ['overworld', 'the_nether', 'the_end']):
        return False
    markers = [p for p in (home / 'signals').iterdir() if p.suffix in ['.done', '.camera']]
    world = home / 'server/world/level.dat'
    if not markers or not world.exists() or world.stat().st_mtime_ns < max(p.stat().st_mtime_ns for p in markers):
        return False
    # Decompression checks the gzip CRC and complete stream, then the NBT root boundary.
    data = gzip.decompress(world.read_bytes())
    return data.startswith(b'\x0a\x00\x00') and data.endswith(b'\x00')
