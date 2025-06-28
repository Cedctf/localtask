package com.example.assignment2;

public class Hirer {
    private String name;
    private String email;
    private String phone;
    private String passwordHash;
    private String userType;
    private long timestamp;

    // Default constructor required for Firestore
    public Hirer() {
    }

    public Hirer(String name, String email, String phone, String password, String userType) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.passwordHash = PasswordUtils.hashPassword(password);
        this.userType = userType;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    // Method to verify password
    public boolean verifyPassword(String password) {
        return PasswordUtils.verifyPassword(password, this.passwordHash);
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
} 