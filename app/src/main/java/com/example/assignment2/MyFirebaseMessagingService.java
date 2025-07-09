package com.example.assignment2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    
    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "task_notifications";
    private static final String CHANNEL_NAME = "Task Notifications";
    private static final String CHANNEL_DESC = "Notifications for task updates";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        
        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            
            // Get additional data
            String taskId = remoteMessage.getData().get("taskId");
            String type = remoteMessage.getData().get("type");
            
            Log.d(TAG, "Notification - Title: " + title + ", Body: " + body + ", TaskId: " + taskId);
            
            // Show notification
            showNotification(title, body, taskId, type);
        }
        
        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataPayload(remoteMessage.getData());
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        
        // Save the new FCM token to Firestore
        saveTokenToFirestore(token);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null);
            
            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(String title, String body, String taskId, String type) {
        // Create an explicit intent for an Activity in your app
        Intent intent;
        if (taskId != null && !taskId.isEmpty()) {
            // Open TaskDetailsActivity with the specific task
            intent = new Intent(this, TaskDetailsActivity.class);
            intent.putExtra("task_id", taskId);
        } else {
            // Open main activity
            intent = new Intent(this, TasksActivity.class);
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 
                0, 
                intent, 
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // Set notification sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // You'll need to create this icon
                .setContentTitle(title != null ? title : "LocalTask")
                .setContentText(body != null ? body : "You have a new notification")
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        // Set notification style for longer text
        if (body != null && body.length() > 50) {
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(body));
        }

        // Add action buttons for task completion notifications
        if ("task_completed".equals(type) && taskId != null) {
            // Add "View Task" action
            Intent viewIntent = new Intent(this, TaskDetailsActivity.class);
            viewIntent.putExtra("task_id", taskId);
            PendingIntent viewPendingIntent = PendingIntent.getActivity(
                    this, 
                    1, 
                    viewIntent, 
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
            );
            notificationBuilder.addAction(R.drawable.ic_menu, "View Task", viewPendingIntent);
        }

        // Show notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        try {
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
            Log.d(TAG, "Notification displayed successfully");
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for showing notification", e);
        }
    }

    private void handleDataPayload(Map<String, String> data) {
        // Handle any additional data processing here
        String taskId = data.get("taskId");
        String type = data.get("type");
        String hirerId = data.get("hirerId");
        
        Log.d(TAG, "Handling data payload - TaskId: " + taskId + ", Type: " + type + ", HirerId: " + hirerId);
        
        // You can add custom logic here based on the notification type
        if ("task_completed".equals(type)) {
            // Handle task completion notification
            Log.d(TAG, "Processing task completion notification for task: " + taskId);
        }
    }

    private void saveTokenToFirestore(String token) {
        SessionManager sessionManager = new SessionManager(this);
        String userId = sessionManager.getUserId();
        
        if (userId != null && !userId.isEmpty()) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("fcmToken", token);
            tokenData.put("tokenUpdatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
            
            db.collection("users").document(userId)
                    .update(tokenData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "FCM token updated successfully in Firestore");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update FCM token in Firestore", e);
                    });
        } else {
            Log.w(TAG, "User ID not available, cannot save FCM token to Firestore");
        }
    }
} 