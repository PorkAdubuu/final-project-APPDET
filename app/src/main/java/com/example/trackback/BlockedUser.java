package com.example.trackback;

import com.google.firebase.Timestamp;

public class BlockedUser {
    private String blockedUserId;
    private String blockedUserName;
    private String blockedUserProfileUrl;
    private Timestamp timestamp;

    public BlockedUser() {}

    public String getBlockedUserId() {
        return blockedUserId;
    }

    public void setBlockedUserId(String blockedUserId) {
        this.blockedUserId = blockedUserId;
    }

    public String getBlockedUserName() {
        return blockedUserName;
    }

    public void setBlockedUserName(String blockedUserName) {
        this.blockedUserName = blockedUserName;
    }

    public String getBlockedUserProfileUrl() {
        return blockedUserProfileUrl;
    }

    public void setBlockedUserProfileUrl(String blockedUserProfileUrl) {
        this.blockedUserProfileUrl = blockedUserProfileUrl;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
