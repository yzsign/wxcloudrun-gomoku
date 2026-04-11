package com.gomoku.sync.domain;

import java.util.Date;

/**
 * 对局归档 games 表
 */
public class GameRecord {

    private Long id;
    private String roomId;
    private int matchRound;
    private long blackUserId;
    private long whiteUserId;
    private int totalSteps;
    private String outcome;
    private Long runawayUserId;
    private int blackEloBefore;
    private int whiteEloBefore;
    private int blackEloAfter;
    private int whiteEloAfter;
    private int blackEloDelta;
    private int whiteEloDelta;
    /** JSON 数组：[{r,c,color}, ...]，与 GameRoomStateSnapshot.MoveRecord 一致 */
    private String movesJson;
    /** 终局时黑方佩戴棋子皮肤，供回放展示 */
    private String blackPieceSkinId;
    /** 终局时白方佩戴棋子皮肤 */
    private String whitePieceSkinId;
    private Date createdAt;

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

    public Long getRunawayUserId() {
        return runawayUserId;
    }

    public void setRunawayUserId(Long runawayUserId) {
        this.runawayUserId = runawayUserId;
    }

    public int getBlackEloBefore() {
        return blackEloBefore;
    }

    public void setBlackEloBefore(int blackEloBefore) {
        this.blackEloBefore = blackEloBefore;
    }

    public int getWhiteEloBefore() {
        return whiteEloBefore;
    }

    public void setWhiteEloBefore(int whiteEloBefore) {
        this.whiteEloBefore = whiteEloBefore;
    }

    public int getBlackEloAfter() {
        return blackEloAfter;
    }

    public void setBlackEloAfter(int blackEloAfter) {
        this.blackEloAfter = blackEloAfter;
    }

    public int getWhiteEloAfter() {
        return whiteEloAfter;
    }

    public void setWhiteEloAfter(int whiteEloAfter) {
        this.whiteEloAfter = whiteEloAfter;
    }

    public int getBlackEloDelta() {
        return blackEloDelta;
    }

    public void setBlackEloDelta(int blackEloDelta) {
        this.blackEloDelta = blackEloDelta;
    }

    public int getWhiteEloDelta() {
        return whiteEloDelta;
    }

    public void setWhiteEloDelta(int whiteEloDelta) {
        this.whiteEloDelta = whiteEloDelta;
    }

    public String getMovesJson() {
        return movesJson;
    }

    public void setMovesJson(String movesJson) {
        this.movesJson = movesJson;
    }

    public String getBlackPieceSkinId() {
        return blackPieceSkinId;
    }

    public void setBlackPieceSkinId(String blackPieceSkinId) {
        this.blackPieceSkinId = blackPieceSkinId;
    }

    public String getWhitePieceSkinId() {
        return whitePieceSkinId;
    }

    public void setWhitePieceSkinId(String whitePieceSkinId) {
        this.whitePieceSkinId = whitePieceSkinId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
