package com.example.trackback;

public class NotificationModel {
    private String documentId;
    private String fname;
    private String lastName;
    private String  profileUrl;
    private String date;
    private String time;
    private String reportType;
    private boolean read;
    private String notificationDocId;


    public NotificationModel() {
        // Required empty constructor for Firestore
    }

    public NotificationModel(String documentId, String fname, String lastName,
                             String  profileUrl, String date, String time,
                             String reportType, boolean read) {
        this.documentId = documentId;
        this.fname = fname;
        this.lastName = lastName;
        this.profileUrl =  profileUrl;
        this.date = date;
        this.time = time;
        this.reportType = reportType;
        this.read = read;
    }

    // Getters and Setters
    public String getNotificationDocId() {
        return notificationDocId;
    }
    public void setNotificationDocId(String notificationDocId) {
        this.notificationDocId = notificationDocId;
    }
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getFname() {
        return fname;
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getImageUrl() {
        return profileUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.profileUrl = imageUrl;
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

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
