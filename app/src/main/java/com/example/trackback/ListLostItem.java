package com.example.trackback;

import java.io.Serializable;

public class ListLostItem implements Serializable {
    private String documentId;
    private String userId;  // CRITICAL: Must store the owner's user ID
    private String itemLost;
    private String date;
    private String time;
    private String lastSeen;
    private String category;
    private String brand;
    private String additionalInfo;
    private String moreInfo;
    private String itemImageUrl;
    private String reportType;

    // User information
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String profileUrl;

    public ListLostItem() {
        // Required empty constructor for Firestore
    }

    // Full constructor
    public ListLostItem(String documentId, String userId, String itemLost, String date, String time,
                        String lastSeen, String category, String brand, String additionalInfo,
                        String moreInfo, String itemImageUrl, String reportType,
                        String firstName, String lastName, String email, String phone, String profileUrl) {
        this.documentId = documentId;
        this.userId = userId;
        this.itemLost = itemLost;
        this.date = date;
        this.time = time;
        this.lastSeen = lastSeen;
        this.category = category;
        this.brand = brand;
        this.additionalInfo = additionalInfo;
        this.moreInfo = moreInfo;
        this.itemImageUrl = itemImageUrl;
        this.reportType = reportType;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.profileUrl = profileUrl;
    }

    // Getters and Setters
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getItemLost() { return itemLost; }
    public void setItemLost(String itemLost) { this.itemLost = itemLost; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getLastSeen() { return lastSeen; }
    public void setLastSeen(String lastSeen) { this.lastSeen = lastSeen; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }

    public String getMoreInfo() { return moreInfo; }
    public void setMoreInfo(String moreInfo) { this.moreInfo = moreInfo; }

    public String getItemImageUrl() { return itemImageUrl; }
    public void setItemImageUrl(String itemImageUrl) { this.itemImageUrl = itemImageUrl; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getProfileUrl() { return profileUrl; }
    public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }
}