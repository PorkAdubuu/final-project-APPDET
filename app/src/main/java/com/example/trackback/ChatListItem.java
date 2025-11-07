package com.example.trackback;

import com.google.firebase.Timestamp;

public class ChatListItem {
    private String documentId;
    private String senderId;
    private String receiverId;
    private String lastMessage;
    private String messageText;
    private Timestamp timestamp; // Use Firebase Timestamp for Firestore compatibility
    private String name; // Display name
    private String profileImageUrl;
    private boolean unread;

    private String lastSenderId;

    // Required empty constructor for Firestore
    public ChatListItem() {}

    // Constructor with Timestamp
    public ChatListItem(String senderId, String receiverId, String lastMessage, Timestamp timestamp, String name, String profileImageUrl) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.lastMessage = lastMessage;
        this.messageText = messageText;
        this.timestamp = timestamp; // fixed
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.unread = unread;
    }

    // ✅ Getters
    public String getDocumentId() {
        return documentId;
    }
    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getLastMessage() {
        return lastMessage;
    }
    public String getMessageText() {
        return messageText;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getName() {
        return name;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    // Helper for ChatActivity
    public String getUserId() {
        return receiverId;
    }
    public boolean isUnread() {
        return unread;
    }

    public String getLastSenderId() {
        return lastSenderId;
    }

    // ✅ Setters

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void setUnread(boolean unread) {
        this.unread = unread;
    }

    // Optional: convert Timestamp to long milliseconds
    public long getTimestampMillis() {
        return timestamp != null ? timestamp.toDate().getTime() : 0;
    }

    public void setLastSenderId(String lastSenderId) {
        this.lastSenderId = lastSenderId;
    }
}
