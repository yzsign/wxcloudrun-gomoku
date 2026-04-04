# -*- coding: utf-8 -*-
"""Set bot avatar_url to local:images/bots/NNN.png（001–100，每人一张不重复）。"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / "src/main/resources/sql/migration-v3-bot-users.sql"
lines = path.read_text(encoding="utf-8").splitlines()
out = []
idx = 0
pat = re.compile(
    r"^(\('[^']+', '[^']*', )'[^']*'(, \d+, 1\)(?:,)?);?$"
)
for line in lines:
    stripped = line.strip()
    m = pat.match(stripped)
    if m:
        idx += 1
        url = f"local:images/bots/{idx:03d}.png"
        semi = ";" if stripped.endswith(";") else ""
        out.append(m.group(1) + "'" + url + "'" + m.group(2) + semi)
    else:
        out.append(line)
path.write_text("\n".join(out) + "\n", encoding="utf-8")
print("rows patched:", idx)
