package com.gomoku.sync.domain;

import java.util.Date;

/**
 * games + 对手用户行，供历史列表查询映射。
 */
public class GameHistoryQueryRow {

    private Long id;
    private String roomId;
    private int matchRound;
    private int totalSteps;
    private String outcome;
    private Date createdAt;
    private long blackUserId;
    private long whiteUserId;
    private String opponentNickname;
    private boolean opponentBot;
    private String opponentAvatarUrl;
    /** 对手 users.gender：0 未知 1 男 2 女；可能为 null（旧数据或未上报） */
    private Integer opponentGender;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public int getMatchRound() {
        return matchRound;
    }

    public void setMatchRound(int matchRound) {
        this.matchRound = matchRound;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public long getBlackUserId() {
        return blackUserId;
    }

    public void setBlackUserId(long blackUserId) {
        this.blackUserId = blackUserId;
    }

    public long getWhiteUserId() {
        return whiteUserId;
    }

    public void setWhiteUserId(long whiteUserId) {
        this.whiteUserId = whiteUserId;
    }

    public String getOpponentNickname() {
        return opponentNickname;
    }

    public void setOpponentNickname(String opponentNickname) {
        this.opponentNickname = opponentNickname;
    }

    public boolean isOpponentBot() {
        return opponentBot;
    }

    public void setOpponentBot(boolean opponentBot) {
        this.opponentBot = opponentBot;
    }

    public String getOpponentAvatarUrl() {
        return opponentAvatarUrl;
    }

    public void setOpponentAvatarUrl(String opponentAvatarUrl) {
        this.opponentAvatarUrl = opponentAvatarUrl;
    }

    public Integer getOpponentGender() {
        return opponentGender;
    }

    public void setOpponentGender(Integer opponentGender) {
        this.opponentGender = opponentGender;
    }
}
