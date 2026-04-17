package com.gomoku.sync.api.dto;

public class SendFriendMessageBody {

    private Long peerUserId;
    private String text;

    public Long getPeerUserId() {
        return peerUserId;
    }

    public void setPeerUserId(Long peerUserId) {
        this.peerUserId = peerUserId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
