package com.example.trackback;

import com.google.firebase.Timestamp;

public class LostItem {

    private Timestamp timestamp;
    private String itemLost;
    private String category;
    private String brand;
    private String date;
    private String time;
    private String additionalInfo;
    private String lastSeen;
    private String moreInfo;
    private String firstName;
    private String lastName;
    private String phone;
    private String profileUrl;
    private String userId;  // add userId here

    // Default constructor required for Firestore
    public LostItem() {
    }

    // Full constructor with userId
    public LostItem(String itemLost, String category, String brand, String date, String time,
                    String additionalInfo, String lastSeen, String moreInfo,
                    String firstName, String lastName, String phone, String profileUrl, String userId) {
        this.itemLost = itemLost;
        this.category = category;
        this.brand = brand;
        this.date = date;
        this.time = time;
        this.additionalInfo = additionalInfo;
        this.lastSeen = lastSeen;
        this.moreInfo = moreInfo;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.profileUrl = profileUrl;
        this.userId = userId;
        this.timestamp = Timestamp.now();
    }

    // Getters and setters

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getItemLost() {
        return itemLost;
    }

    public void setItemLost(String itemLost) {
        this.itemLost = itemLost;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public String getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(String lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getMoreInfo() {
        return moreInfo;
    }

    public void setMoreInfo(String moreInfo) {
        this.moreInfo = moreInfo;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
