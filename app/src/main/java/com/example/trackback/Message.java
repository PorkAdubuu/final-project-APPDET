package com.example.trackback;

import com.google.firebase.Timestamp;

public class Message {

    private String senderId;
    private String receiverId;
    private String messageText;
    private String imageUrl;
    private Timestamp timestamp;
    private Timestamp readAt;
    private boolean isSystemMessage; // NEW
    private boolean isDelivered;

    public Message() {} // Required for Firestore

    public Message(String senderId, String receiverId, String message, Timestamp timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageText = message;
        this.timestamp = timestamp;
        this.readAt = null;
        this.isSystemMessage = false;
        this.isDelivered = false;
    }

    // NEW: Constructor for system messages
    public Message(String message, Timestamp timestamp, boolean isSystemMessage) {
        this.messageText = message;
        this.timestamp = timestamp;
        this.isSystemMessage = isSystemMessage;
        this.senderId = "system";
        this.receiverId = "system";
        this.isDelivered = true;
    }

    // All getters and setters
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public Timestamp getReadAt() { return readAt; }
    public void setReadAt(Timestamp readAt) { this.readAt = readAt; }

    public boolean isSystemMessage() { return isSystemMessage; }
    public void setSystemMessage(boolean systemMessage) { isSystemMessage = systemMessage; }

    public boolean isDelivered() { return isDelivered; }
    public void setDelivered(boolean delivered) { isDelivered = delivered; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Message message = (Message) obj;
        return java.util.Objects.equals(senderId, message.senderId) &&
                java.util.Objects.equals(receiverId, message.receiverId) &&
                java.util.Objects.equals(messageText, message.messageText) &&
                java.util.Objects.equals(timestamp, message.timestamp);
    }

    @Override
    public int hashCode() {
        int result = senderId != null ? senderId.hashCode() : 0;
        result = 31 * result + (receiverId != null ? receiverId.hashCode() : 0);
        result = 31 * result + (messageText != null ? messageText.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }
}