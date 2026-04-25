package com.gomoku.sync.domain;

/**
 * 房间与双方用户（DB 持久化，供结算校验）
 */
public class RoomParticipant {

    private String roomId;
    private long blackUserId;
    private String blackToken;
    private Long whiteUserId;
    private String whiteToken;
    /** 非残局 PvP 好友观战，与 Gomoku WS token 参数一致；多实例时须持久化 */
    private String friendWatchToken;
    private boolean whiteIsBot;
    private boolean blackIsBot;
    private Integer botSearchDepthMin;
    private Integer botSearchDepthMax;
    /** 人机棋风，见 BotAiStyle.ordinal；NULL 表示由 white_user_id 推导 */
    private Integer botAiStyle;
    /** 与 black 同用户时可仅连观战 WS；NULL 表示非残局好友房 */
    private Long observerUserId;
    private String observerToken;
    private boolean puzzleRoom;
    /** 随机匹配队列创建的房间（POST /api/match/random）；好友房为 false */
    private boolean randomMatch;
    /** 与 createPuzzleFriendRoom 写入一致，供好友进房时重置盘面 */
    private String puzzleInitBoardJson;
    private Integer puzzleSideToMove;

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public long getBlackUserId() {
        return blackUserId;
    }

    public void setBlackUserId(long blackUserId) {
        this.blackUserId = blackUserId;
    }

    public String getBlackToken() {
        return blackToken;
    }

    public void setBlackToken(String blackToken) {
        this.blackToken = blackToken;
    }

    public Long getWhiteUserId() {
        return whiteUserId;
    }

    public void setWhiteUserId(Long whiteUserId) {
        this.whiteUserId = whiteUserId;
    }

    public String getWhiteToken() {
        return whiteToken;
    }

    public void setWhiteToken(String whiteToken) {
        this.whiteToken = whiteToken;
    }

    public String getFriendWatchToken() {
        return friendWatchToken;
    }

    public void setFriendWatchToken(String friendWatchToken) {
        this.friendWatchToken = friendWatchToken;
    }

    public boolean isWhiteIsBot() {
        return whiteIsBot;
    }

    public void setWhiteIsBot(boolean whiteIsBot) {
        this.whiteIsBot = whiteIsBot;
    }

    public boolean isBlackIsBot() {
        return blackIsBot;
    }

    public void setBlackIsBot(boolean blackIsBot) {
        this.blackIsBot = blackIsBot;
    }

    public Integer getBotSearchDepthMin() {
        return botSearchDepthMin;
    }

    public void setBotSearchDepthMin(Integer botSearchDepthMin) {
        this.botSearchDepthMin = botSearchDepthMin;
    }

    public Integer getBotSearchDepthMax() {
        return botSearchDepthMax;
    }

    public void setBotSearchDepthMax(Integer botSearchDepthMax) {
        this.botSearchDepthMax = botSearchDepthMax;
    }

    public Integer getBotAiStyle() {
        return botAiStyle;
    }

    public void setBotAiStyle(Integer botAiStyle) {
        this.botAiStyle = botAiStyle;
    }

    public Long getObserverUserId() {
        return observerUserId;
    }

    public void setObserverUserId(Long observerUserId) {
        this.observerUserId = observerUserId;
    }

    public String getObserverToken() {
        return observerToken;
    }

    public void setObserverToken(String observerToken) {
        this.observerToken = observerToken;
    }

    public boolean isPuzzleRoom() {
        return puzzleRoom;
    }

    public void setPuzzleRoom(boolean puzzleRoom) {
        this.puzzleRoom = puzzleRoom;
    }

    public boolean isRandomMatch() {
        return randomMatch;
    }

    public void setRandomMatch(boolean randomMatch) {
        this.randomMatch = randomMatch;
    }

    public String getPuzzleInitBoardJson() {
        return puzzleInitBoardJson;
    }

    public void setPuzzleInitBoardJson(String puzzleInitBoardJson) {
        this.puzzleInitBoardJson = puzzleInitBoardJson;
    }

    public Integer getPuzzleSideToMove() {
        return puzzleSideToMove;
    }

    public void setPuzzleSideToMove(Integer puzzleSideToMove) {
        this.puzzleSideToMove = puzzleSideToMove;
    }
}
