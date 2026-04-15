package com.gomoku.sync.api.dto;

import java.util.List;

public class FriendsListResponse {

    private List<FriendListItemDto> friends;

    public FriendsListResponse() {
    }

    public FriendsListResponse(List<FriendListItemDto> friends) {
        this.friends = friends;
    }

    public List<FriendListItemDto> getFriends() {
        return friends;
    }

    public void setFriends(List<FriendListItemDto> friends) {
        this.friends = friends;
    }
}
