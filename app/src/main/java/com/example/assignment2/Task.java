package com.example.assignment2;

public class Task {
    private String id;
    private String title;
    private String description;
    private String status;
    private int points;
    private String assignedTo;
    private String createdBy;

    // Empty constructor for Firestore
    public Task() {}

    public Task(String id, String title, String description, String status, int points, 
                String assignedTo, String createdBy) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.points = points;
        this.assignedTo = assignedTo;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
} 