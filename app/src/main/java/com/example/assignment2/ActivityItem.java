package com.example.assignment2;

public class ActivityItem {
    private String title;
    private String description;
    private String amount;
    private String timeAgo;
    private String iconType;
    private long timestamp;

    public ActivityItem() {
        // Default constructor required for Firestore
    }

    public ActivityItem(String title, String description, String amount, String timeAgo, String iconType, long timestamp) {
        this.title = title;
        this.description = description;
        this.amount = amount;
        this.timeAgo = timeAgo;
        this.iconType = iconType;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }

    public String getIconType() {
        return iconType;
    }

    public void setIconType(String iconType) {
        this.iconType = iconType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
} 