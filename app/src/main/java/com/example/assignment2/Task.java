package com.example.assignment2;

public class Task {
    private String title;
    private String description;
    private String hirerId;
    private String hirerName;
    private double payment;
    private String status;
    private long timestamp;
    private String dueDate;
    private String location;
    private String assignedTo;
    private Object startTime;
    private Object completionTime;
    private Object paymentTime;

    // Empty constructor for Firestore
    public Task() {}

    public Task(String title, String description, String hirerId, String hirerName, double payment, String dueDate, String location) {
        this.title = title;
        this.description = description;
        this.hirerId = hirerId;
        this.hirerName = hirerName;
        this.payment = payment;
        this.status = "open";
        this.timestamp = System.currentTimeMillis();
        this.dueDate = dueDate;
        this.location = location;
    }

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getHirerId() { return hirerId; }
    public void setHirerId(String hirerId) { this.hirerId = hirerId; }
    
    public String getHirerName() { return hirerName; }
    public void setHirerName(String hirerName) { this.hirerName = hirerName; }
    
    public double getPayment() { return payment; }
    public void setPayment(double payment) { this.payment = payment; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    
    public Object getStartTime() { return startTime; }
    public void setStartTime(Object startTime) { this.startTime = startTime; }
    
    public Object getCompletionTime() { return completionTime; }
    public void setCompletionTime(Object completionTime) { this.completionTime = completionTime; }
    
    public Object getPaymentTime() { return paymentTime; }
    public void setPaymentTime(Object paymentTime) { this.paymentTime = paymentTime; }
} 