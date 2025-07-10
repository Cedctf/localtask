package com.example.assignment2;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LeaderboardManager {
    
    public interface LeaderboardCallback {
        void onSuccess(List<LeaderboardUser> leaderboard);
        void onError(String error);
    }
    
    public interface ContactUserCallback {
        void onSuccess(String message);
        void onError(String error);
    }
    
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String TAG = "LeaderboardManager";
    
    /**
     * Get leaderboard data for most efficient users (sorted by tasks completed and efficiency)
     */
    public static void getLeaderboard(LeaderboardCallback callback) {
        Log.d(TAG, "Starting leaderboard data fetch...");
        
        // First, get all users
        db.collection("users")
                .get()
                .addOnSuccessListener(userSnapshots -> {
                    Log.d(TAG, "Users query successful. Found " + userSnapshots.size() + " users");
                    
                    List<LeaderboardUser> leaderboardUsers = new ArrayList<>();
                    
                    if (userSnapshots.isEmpty()) {
                        Log.d(TAG, "No users found in database");
                        callback.onSuccess(leaderboardUsers);
                        return;
                    }
                    
                    AtomicInteger pendingQueries = new AtomicInteger(userSnapshots.size());
                    Log.d(TAG, "Processing " + userSnapshots.size() + " users for task statistics");
                    
                    // For each user, calculate their statistics
                    for (QueryDocumentSnapshot userDoc : userSnapshots) {
                        String userName = userDoc.getString("name");
                        String email = userDoc.getString("email");
                        String phone = userDoc.getString("phone");
                        
                        if (userName == null || email == null) {
                            Log.w(TAG, "Skipping user with missing name or email");
                            // Decrement counter for skipped user
                            if (pendingQueries.decrementAndGet() == 0) {
                                Log.d(TAG, "All queries completed, returning " + leaderboardUsers.size() + " users");
                                sortAndRankUsers(leaderboardUsers);
                                callback.onSuccess(leaderboardUsers);
                            }
                            continue;
                        }
                        
                        Log.d(TAG, "Fetching tasks for user: " + userName);
                        
                        // Get completed tasks for this user
                        db.collection("tasks")
                                .whereEqualTo("assignedTo", userName)
                                .whereEqualTo("status", "completed")
                                .get()
                                .addOnSuccessListener(taskSnapshots -> {
                                    Log.d(TAG, "Tasks query for " + userName + " successful. Found " + taskSnapshots.size() + " completed tasks");
                                    
                                    LeaderboardUser leaderboardUser = calculateUserStats(
                                            userName, email, phone, taskSnapshots);
                                    
                                    synchronized(leaderboardUsers) {
                                        leaderboardUsers.add(leaderboardUser);
                                        Log.d(TAG, "Added user " + userName + " to leaderboard (" + leaderboardUser.getTasksCompleted() + " tasks completed)");
                                    }
                                    
                                    // Check if all queries are done
                                    if (pendingQueries.decrementAndGet() == 0) {
                                        Log.d(TAG, "All queries completed, returning " + leaderboardUsers.size() + " users");
                                        sortAndRankUsers(leaderboardUsers);
                                        callback.onSuccess(leaderboardUsers);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error fetching tasks for user " + userName, e);
                                    
                                    // Still add user with 0 tasks
                                    LeaderboardUser leaderboardUser = new LeaderboardUser(
                                            userName, email, phone, 0, 0.0, 0.0, 0.0, 0);
                                    
                                    synchronized(leaderboardUsers) {
                                        leaderboardUsers.add(leaderboardUser);
                                    }
                                    
                                    if (pendingQueries.decrementAndGet() == 0) {
                                        Log.d(TAG, "All queries completed (with some errors), returning " + leaderboardUsers.size() + " users");
                                        sortAndRankUsers(leaderboardUsers);
                                        callback.onSuccess(leaderboardUsers);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching users from database", e);
                    callback.onError("Failed to load users: " + e.getMessage());
                });
    }
    
    /**
     * Calculate statistics for a user based on their completed tasks
     */
    private static LeaderboardUser calculateUserStats(String userName, String email, String phone, 
                                                     com.google.firebase.firestore.QuerySnapshot taskSnapshots) {
        int tasksCompleted = taskSnapshots.size();
        double totalEarnings = 0;
        double totalRating = 0;
        int ratedTasks = 0;
        long earliestTask = Long.MAX_VALUE;
        long latestTask = 0;
        
        // Calculate statistics from tasks
        for (QueryDocumentSnapshot taskDoc : taskSnapshots) {
            Task task = taskDoc.toObject(Task.class);
            
            // Add earnings
            totalEarnings += task.getPayment();
            
            // Calculate rating
            if (task.getWorkerRating() > 0) {
                totalRating += task.getWorkerRating();
                ratedTasks++;
            }
            
            // Track time range for efficiency calculation
            long completionTime = task.getTimestamp();
            if (task.getCompletionTime() instanceof com.google.firebase.Timestamp) {
                completionTime = ((com.google.firebase.Timestamp) task.getCompletionTime()).getSeconds() * 1000;
            }
            
            if (completionTime < earliestTask) earliestTask = completionTime;
            if (completionTime > latestTask) latestTask = completionTime;
        }
        
        // Calculate average rating
        double averageRating = ratedTasks > 0 ? totalRating / ratedTasks : 0;
        
        // Calculate efficiency score (tasks per day)
        double efficiencyScore = 0;
        if (tasksCompleted > 0 && earliestTask < Long.MAX_VALUE) {
            long timeSpanMs = Math.max(latestTask - earliestTask, 24 * 60 * 60 * 1000); // At least 1 day
            double daySpan = timeSpanMs / (24.0 * 60 * 60 * 1000);
            efficiencyScore = tasksCompleted / daySpan;
        }
        
        return new LeaderboardUser(userName, email, phone, tasksCompleted, 
                                  totalEarnings, averageRating, efficiencyScore, latestTask);
    }
    
    /**
     * Sort users by efficiency and assign ranks
     */
    private static void sortAndRankUsers(List<LeaderboardUser> users) {
        // Sort by multiple criteria: tasks completed (primary), efficiency score (secondary), rating (tertiary)
        Collections.sort(users, new Comparator<LeaderboardUser>() {
            @Override
            public int compare(LeaderboardUser u1, LeaderboardUser u2) {
                // Primary: Tasks completed (descending)
                int taskComparison = Integer.compare(u2.getTasksCompleted(), u1.getTasksCompleted());
                if (taskComparison != 0) return taskComparison;
                
                // Secondary: Efficiency score (descending)
                int efficiencyComparison = Double.compare(u2.getEfficiencyScore(), u1.getEfficiencyScore());
                if (efficiencyComparison != 0) return efficiencyComparison;
                
                // Tertiary: Average rating (descending)
                return Double.compare(u2.getAverageRating(), u1.getAverageRating());
            }
        });
        
        // Assign ranks
        for (int i = 0; i < users.size(); i++) {
            users.get(i).setRank(i + 1);
        }
    }
    
    /**
     * Get top performers (users with more than minimum tasks completed)
     * Note: Filter on the frontend after calling getLeaderboard()
     */
    
    /**
     * Mark user as preferred for company contact
     */
    public static void markUserAsPreferred(String userName, boolean preferred, ContactUserCallback callback) {
        db.collection("users")
                .whereEqualTo("name", userName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String userId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        db.collection("users").document(userId)
                                .update("isPreferred", preferred)
                                .addOnSuccessListener(aVoid -> 
                                    callback.onSuccess("User preference updated successfully"))
                                .addOnFailureListener(e -> callback.onError(e.getMessage()));
                    } else {
                        callback.onError("User not found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
    
    /**
     * Get user contact information for company outreach
     */
    public static void getUserContactInfo(String userName, ContactUserCallback callback) {
        db.collection("users")
                .whereEqualTo("name", userName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String email = userDoc.getString("email");
                        String phone = userDoc.getString("phone");
                        
                        String contactInfo = "Email: " + email + "\nPhone: " + phone;
                        callback.onSuccess(contactInfo);
                    } else {
                        callback.onError("User not found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
} 