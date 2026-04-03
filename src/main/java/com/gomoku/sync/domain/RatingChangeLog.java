package com.gomoku.sync.domain;

/**
 * rating_change_log 行
 */
public class RatingChangeLog {

    private long userId;
    private String roomId;
    private long opponentUserId;
    private int eloBefore;
    private int eloAfter;
    private int delta;
    private int totalSteps;
    private boolean runaway;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public long getOpponentUserId() {
        return opponentUserId;
    }

    public void setOpponentUserId(long opponentUserId) {
        this.opponentUserId = opponentUserId;
    }

    public int getEloBefore() {
        return eloBefore;
    }

    public void setEloBefore(int eloBefore) {
        this.eloBefore = eloBefore;
    }

    public int getEloAfter() {
        return eloAfter;
    }

    public void setEloAfter(int eloAfter) {
        this.eloAfter = eloAfter;
    }

    public int getDelta() {
        return delta;
    }

    public void setDelta(int delta) {
        this.delta = delta;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public boolean isRunaway() {
        return runaway;
    }

    public void setRunaway(boolean runaway) {
        this.runaway = runaway;
    }
}
