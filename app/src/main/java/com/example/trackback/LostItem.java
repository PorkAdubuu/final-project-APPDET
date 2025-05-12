package com.example.trackback;

public class LostItem {

    private String itemLostText;
    private String category;
    private String brandText;
    private String dateText;
    private String timeText;
    private String additionalInfoText;
    private String lastSeenText;
    private String moreInfoText;
    private String firstNameText;
    private String lastNameText;
    private String phoneNumber;
    private String profileUrl;  // Field for the profile image URL

    // Default constructor required for Firestore
    public LostItem() {
    }

    // Constructor to easily create a LostItem object
    public LostItem(String itemLostText, String category, String brandText, String dateText, String timeText,
                    String additionalInfoText, String lastSeenText, String moreInfoText,
                    String firstNameText, String lastNameText, String phoneNumber, String profileUrl) {
        this.itemLostText = itemLostText;
        this.category = category;
        this.brandText = brandText;
        this.dateText = dateText;
        this.timeText = timeText;
        this.additionalInfoText = additionalInfoText;
        this.lastSeenText = lastSeenText;
        this.moreInfoText = moreInfoText;
        this.firstNameText = firstNameText;
        this.lastNameText = lastNameText;
        this.phoneNumber = phoneNumber;
        this.profileUrl = profileUrl;  // Initialize the profileUrl
    }

    // Getter and setter methods
    public String getProfileUrl() {  // Updated getter method
        return profileUrl;
    }

    public String getItemLostText() {
        return itemLostText;
    }

    public void setItemLostText(String itemLostText) {
        this.itemLostText = itemLostText;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBrandText() {
        return brandText;
    }

    public void setBrandText(String brandText) {
        this.brandText = brandText;
    }

    public String getDateText() {
        return dateText;
    }

    public void setDateText(String dateText) {
        this.dateText = dateText;
    }

    public String getTimeText() {
        return timeText;
    }

    public void setTimeText(String timeText) {
        this.timeText = timeText;
    }

    public String getAdditionalInfoText() {
        return additionalInfoText;
    }

    public void setAdditionalInfoText(String additionalInfoText) {
        this.additionalInfoText = additionalInfoText;
    }

    public String getLastSeenText() {
        return lastSeenText;
    }

    public void setLastSeenText(String lastSeenText) {
        this.lastSeenText = lastSeenText;
    }

    public String getMoreInfoText() {
        return moreInfoText;
    }

    public void setMoreInfoText(String moreInfoText) {
        this.moreInfoText = moreInfoText;
    }

    public String getFirstNameText() {
        return firstNameText;
    }

    public void setFirstNameText(String firstNameText) {
        this.firstNameText = firstNameText;
    }

    public String getLastNameText() {
        return lastNameText;
    }

    public void setLastNameText(String lastNameText) {
        this.lastNameText = lastNameText;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
