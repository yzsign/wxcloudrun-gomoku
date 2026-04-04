package com.gomoku.sync.api.dto;

/**
 * 房主等待对手时轮询：对手加入后可取最终座位与 WebSocket token（先后手可能已随机交换）。
 */
public class RandomMatchPairedResponse {

    private final boolean guestJoined;
    private final int boardSize;
    private final String yourColor;
    private final String yourToken;

    public static RandomMatchPairedResponse waiting(int boardSize) {
        return new RandomMatchPairedResponse(false, boardSize, null, null);
    }

    public static RandomMatchPairedResponse paired(int boardSize, String yourColor, String yourToken) {
        return new RandomMatchPairedResponse(true, boardSize, yourColor, yourToken);
    }

    private RandomMatchPairedResponse(boolean guestJoined, int boardSize, String yourColor, String yourToken) {
        this.guestJoined = guestJoined;
        this.boardSize = boardSize;
        this.yourColor = yourColor;
        this.yourToken = yourToken;
    }

    public boolean isGuestJoined() {
        return guestJoined;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public String getYourColor() {
        return yourColor;
    }

    public String getYourToken() {
        return yourToken;
    }
}
