package com.gomoku.sync.api.dto;

/**
 * POST /api/games/settle
 */
public class SettleGameRequest {

    private String roomId;
    /** 同房间第几局：首局 1，再来一局后递增；不传则按 1 处理（兼容旧客户端） */
    private Integer matchRound;
    /** BLACK_WIN | WHITE_WIN | DRAW */
    private String outcome;
    private int totalSteps;
    /** 逃跑/超时判负方用户 id；正常终局不传 */
    private Long runawayUserId;

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public Integer getMatchRound() {
        return matchRound;
    }

    public void setMatchRound(Integer matchRound) {
        this.matchRound = matchRound;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public Long getRunawayUserId() {
        return runawayUserId;
    }

    public void setRunawayUserId(Long runawayUserId) {
        this.runawayUserId = runawayUserId;
    }
}
