package com.gomoku.sync.api.dto;

/**
 * 单步落子，与 {@link com.gomoku.sync.domain.GameRoomStateSnapshot.MoveRecord} 字段一致。
 */
public class GameMoveDto {

    private int r;
    private int c;
    /** {@link com.gomoku.sync.domain.Stone#BLACK} / {@link com.gomoku.sync.domain.Stone#WHITE} */
    private int color;

    public GameMoveDto() {}

    public GameMoveDto(int r, int c, int color) {
        this.r = r;
        this.c = c;
        this.color = color;
    }

    public int getR() {
        return r;
    }

    public void setR(int r) {
        this.r = r;
    }

    public int getC() {
        return c;
    }

    public void setC(int c) {
        this.c = c;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
