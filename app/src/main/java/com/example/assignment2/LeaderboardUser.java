package com.example.assignment2;

public class LeaderboardUser {
    private String name;
    private String email;
    private String phone;
    private int tasksCompleted;
    private double totalEarnings;
    private double averageRating;
    private double efficiencyScore; // tasks completed per day
    private long lastTaskCompletionTime;
    private int rank;
    private boolean isPreferred; // For company preferred workers

    // Default constructor for Firestore
    public LeaderboardUser() {}

    public LeaderboardUser(String name, String email, String phone, int tasksCompleted, 
                          double totalEarnings, double averageRating, double efficiencyScore,
                          long lastTaskCompletionTime) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.tasksCompleted = tasksCompleted;
        this.totalEarnings = totalEarnings;
        this.averageRating = averageRating;
        this.efficiencyScore = efficiencyScore;
        this.lastTaskCompletionTime = lastTaskCompletionTime;
        this.isPreferred = false;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public int getTasksCompleted() { return tasksCompleted; }
    public void setTasksCompleted(int tasksCompleted) { this.tasksCompleted = tasksCompleted; }

    public double getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(double totalEarnings) { this.totalEarnings = totalEarnings; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public double getEfficiencyScore() { return efficiencyScore; }
    public void setEfficiencyScore(double efficiencyScore) { this.efficiencyScore = efficiencyScore; }

    public long getLastTaskCompletionTime() { return lastTaskCompletionTime; }
    public void setLastTaskCompletionTime(long lastTaskCompletionTime) { 
        this.lastTaskCompletionTime = lastTaskCompletionTime; 
    }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public boolean isPreferred() { return isPreferred; }
    public void setPreferred(boolean preferred) { this.isPreferred = preferred; }
} 