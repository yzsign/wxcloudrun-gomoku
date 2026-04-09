# -*- coding: utf-8 -*-
"""Emit migration SQL: is_bot users get DiceBear 9.x PNG URLs (same seeds as fetch_bot_avatars.py)."""
from pathlib import Path

STYLES = ["avataaars", "lorelei", "notionists", "personas", "micah"]

ROOT = Path(__file__).resolve().parents[1]
V3 = ROOT / "src/main/resources/sql/migration-v3-bot-users.sql"
text = V3.read_text(encoding="utf-8")
import re

pat = re.compile(r"\('([^']+)', '[^']*', '[^']*', \d+, 1\)")
openids = pat.findall(text)
if len(openids) != 100:
    raise SystemExit(f"expected 100 openids, got {len(openids)}")

lines = [
    "-- 人机头像改为网络图（DiceBear 9.x，与 scripts/fetch_bot_avatars.py 种子一致）。",
    "-- 小游戏需在后台配置 downloadFile 合法域名：api.dicebear.com",
    "",
]
for i, oid in enumerate(openids, 1):
    st = STYLES[(i - 1) % len(STYLES)]
    url = f"https://api.dicebear.com/9.x/{st}/png?seed=gomoku_bot_{i:03d}&size=128"
    lines.append(
        f"UPDATE `users` SET `avatar_url` = '{url}' "
        f"WHERE `openid` = '{oid}' AND `is_bot` = 1;"
    )

out = ROOT / "src/main/resources/sql/migration-v21-bots-avatar-network-dicebear.sql"
out.write_text("\n".join(lines) + "\n", encoding="utf-8")
print("wrote", out, "lines", len(lines))
