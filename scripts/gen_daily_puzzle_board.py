#!/usr/bin/env python3
"""生成 daily_puzzle.board_json（15×15，0 空 1 黑 2 白）。

用法一 — 直接用行列下标（0～14）与颜色:
  python scripts/gen_daily_puzzle_board.py 7,7,1 7,8,1

用法二 — 棋谱坐标（列 A～O + 行号 1～15，行 1 为最上行）:
  python scripts/gen_daily_puzzle_board.py chess \\
    --black "H8,G9,I9,F10,J10,E11,K11,D12,L12,C13,M13,B14,N14,A15,O15" \\
    --white "G8,H9,I8,F11,J9,E10,K10,D11,L11,C12,M12,B13,N13,A14,O14,C14,M14,B15,N15"
"""
import argparse
import json
import sys

SIZE = 15
EMPTY = 0
BLACK = 1
WHITE = 2
LETTERS = "ABCDEFGHIJKLMNO"


def parse_chess_cell(token: str):
    t = token.strip().upper().replace(" ", "")
    if not t:
        return None
    col = LETTERS.index(t[0])
    row = int(t[1:]) - 1
    if row < 0 or row >= SIZE or col < 0 or col >= SIZE:
        raise ValueError(f"越界: {token!r} -> row={row} col={col}")
    return row, col


def build_from_chess(black_csv: str, white_csv: str):
    b = [[EMPTY] * SIZE for _ in range(SIZE)]
    for part in black_csv.replace(",", " ").split():
        r, c = parse_chess_cell(part)
        if b[r][c] != EMPTY:
            raise ValueError(f"重复落子: {part} 已有子 {b[r][c]}")
        b[r][c] = BLACK
    for part in white_csv.replace(",", " ").split():
        r, c = parse_chess_cell(part)
        if b[r][c] != EMPTY:
            raise ValueError(f"黑白冲突: {part}")
        b[r][c] = WHITE
    return b


def main():
    if len(sys.argv) >= 2 and sys.argv[1] == "chess":
        p = argparse.ArgumentParser(prog="gen_daily_puzzle_board.py chess")
        p.add_argument("--black", required=True, help="逗号或空格分隔，如 H8,G9")
        p.add_argument("--white", required=True)
        args = p.parse_args(sys.argv[2:])
        b = build_from_chess(args.black, args.white)
        print(json.dumps(b, separators=(",", ":")))
        return

    b = [[EMPTY] * SIZE for _ in range(SIZE)]
    for arg in sys.argv[1:]:
        parts = arg.split(",")
        if len(parts) != 3:
            print("bad arg (need r,c,color):", arg, file=sys.stderr)
            sys.exit(1)
        r, c, color = int(parts[0]), int(parts[1]), int(parts[2])
        b[r][c] = color
    print(json.dumps(b, separators=(",", ":")))


if __name__ == "__main__":
    main()
