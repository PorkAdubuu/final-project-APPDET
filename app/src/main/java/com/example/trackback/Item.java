package com.example.trackback;

import com.google.firebase.Timestamp;

public class Item {
    private String id;
    private String userId;
    private String type;
    private String title;
    private String description;
    private String category;
    private String color;
    private String location;
    private Timestamp timestamp;
    private String imageUrl;

    public Item() {
    }

    public Item(String userId, String type, String title, String description,
                String category, String color, String location, Timestamp timestamp, String imageUrl) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.description = description;
        this.category = category;
        this.color = color;
        this.location = location;
        this.timestamp = timestamp;
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getColor() {
        return color;
    }

    public String getLocation() {
        return location;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}