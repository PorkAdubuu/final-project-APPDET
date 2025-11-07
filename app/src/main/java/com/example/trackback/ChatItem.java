package com.example.trackback;

public class ChatItem {

    private String receiverId;
    private String receiverName;
    private String profileImageUrl;
    private String lastMessage;

    public ChatItem() {
        // Empty constructor needed for Firestore
    }

    public ChatItem(String receiverId, String receiverName, String profileImageUrl, String lastMessage) {
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.profileImageUrl = profileImageUrl;
        this.lastMessage = lastMessage;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
}
