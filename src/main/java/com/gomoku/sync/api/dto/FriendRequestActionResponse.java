package com.gomoku.sync.api.dto;

/** POST accept / reject 幂等结果 */
public class FriendRequestActionResponse {

    private String result;

    public FriendRequestActionResponse() {
    }

    public FriendRequestActionResponse(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
