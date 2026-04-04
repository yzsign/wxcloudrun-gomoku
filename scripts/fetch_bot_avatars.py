# -*- coding: utf-8 -*-
"""
一次性生成 gomoku-minigame/images/bots/001.png … 100.png（外网 DiceBear，仅开发机执行）。
生成后提交到仓库，线上运行时不再请求外网。再运行 patch_migration_local_avatars.py 写回 SQL。
"""
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT.parent / "gomoku-minigame" / "images" / "bots"
STYLES = ["avataaars", "lorelei", "notionists", "personas", "micah"]


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for i in range(1, 101):
        st = STYLES[(i - 1) % len(STYLES)]
        url = f"https://api.dicebear.com/9.x/{st}/png?seed=gomoku_bot_{i:03d}&size=128"
        path = OUT / f"{i:03d}.png"
        urllib.request.urlretrieve(url, path)
        print(path.name, path.stat().st_size)


if __name__ == "__main__":
    main()
