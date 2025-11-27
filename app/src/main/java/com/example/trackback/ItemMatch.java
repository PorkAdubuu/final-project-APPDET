package com.example.trackback;

public class ItemMatch {
    private Item lostItem;
    private Item foundItem;
    private double similarityScore;
    private String matchReason;

    public ItemMatch() {
        // Default constructor required for Firestore
    }

    public ItemMatch(Item lostItem, Item foundItem, double similarityScore, String matchReason) {
        this.lostItem = lostItem;
        this.foundItem = foundItem;
        this.similarityScore = similarityScore;
        this.matchReason = matchReason;
    }

    // Getters
    public Item getLostItem() {
        return lostItem;
    }

    public Item getFoundItem() {
        return foundItem;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public String getMatchReason() {
        return matchReason;
    }

    // Setters
    public void setLostItem(Item lostItem) {
        this.lostItem = lostItem;
    }

    public void setFoundItem(Item foundItem) {
        this.foundItem = foundItem;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public void setMatchReason(String matchReason) {
        this.matchReason = matchReason;
    }
}