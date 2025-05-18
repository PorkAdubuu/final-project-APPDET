package com.example.trackback;

public class ListLostItem {

    private String itemLost;
    private String category;
    private String lastSeen;
    private String date;
    private String email;
    private String profileUrl;
    private String reportType;

    public ListLostItem() {}

    public ListLostItem(String itemLost, String category, String lastSeen, String date, String email, String profileUrl, String reportType) {
        this.itemLost = itemLost;
        this.category = category;
        this.lastSeen = lastSeen;
        this.date = date;
        this.email = email;
        this.profileUrl = profileUrl;
        this.reportType = reportType; 
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

    public String getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(String lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    // ✅ Getter and Setter for reportType
    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }
}
