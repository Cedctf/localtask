package com.example.assignment2;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;
import java.util.Map;

public class FCMTokenManager {
    
    private static final String TAG = "FCMTokenManager";
    private FirebaseFirestore db;
    private SessionManager sessionManager;
    
    public FCMTokenManager(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.sessionManager = new SessionManager(context);
    }
    
    /**
     * Get FCM token and save it to Firestore for the current user
     */
    public void getAndSaveToken() {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "User ID not available, cannot save FCM token");
            return;
        }
        
        Log.d(TAG, "Requesting FCM token for user: " + userId);
        
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    Log.d(TAG, "FCM Registration Token retrieved: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null"));

                    if (token != null && !token.isEmpty()) {
                        // Save token to Firestore
                        saveTokenToFirestore(userId, token);
                    } else {
                        Log.w(TAG, "FCM token is null or empty");
                    }
                });
    }
    
    /**
     * Save FCM token to Firestore for a specific user
     */
    public void saveTokenToFirestore(String userId, String token) {
        if (userId == null || userId.isEmpty() || token == null || token.isEmpty()) {
            Log.w(TAG, "Invalid userId or token, cannot save to Firestore");
            return;
        }
        
        Log.d(TAG, "Saving FCM token to Firestore for user: " + userId);
        
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("fcmToken", token);
        tokenData.put("tokenUpdatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        
        // Try both users and hirers collections to handle both user types
        saveTokenToCollection("users", userId, tokenData, token);
    }
    
    /**
     * Save token to a specific collection with fallback logic
     */
    private void saveTokenToCollection(String collection, String userId, Map<String, Object> tokenData, String token) {
        db.collection(collection).document(userId)
                .update(tokenData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "FCM token saved successfully to " + collection + " collection for user: " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to update FCM token in " + collection + " collection, trying to set with merge", e);
                    
                    // If update fails, try to set the data with merge (in case document doesn't exist)
                    db.collection(collection).document(userId)
                            .set(tokenData, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d(TAG, "FCM token set successfully in " + collection + " collection for user: " + userId);
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e(TAG, "Failed to set FCM token in " + collection + " collection for user: " + userId, e2);
                                
                                // If this is users collection and it failed, try hirers collection
                                if ("users".equals(collection)) {
                                    Log.d(TAG, "Trying hirers collection as fallback for user: " + userId);
                                    saveTokenToCollection("hirers", userId, tokenData, token);
                                }
                            });
                });
    }
    
    /**
     * Delete FCM token from Firestore (useful for logout)
     */
    public void deleteTokenFromFirestore() {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "User ID not available, cannot delete FCM token");
            return;
        }
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", com.google.firebase.firestore.FieldValue.delete());
        updates.put("tokenUpdatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        
        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "FCM token deleted successfully from Firestore for user: " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete FCM token from Firestore for user: " + userId, e);
                });
    }
} 