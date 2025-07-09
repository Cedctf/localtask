package com.example.assignment2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class NotificationPermissionHelper {
    
    private static final String TAG = "NotificationPermissionHelper";
    public static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    
    /**
     * Check if notification permission is granted
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                    context, 
                    Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        }
        // For Android 12 and below, notifications are enabled by default
        return true;
    }
    
    /**
     * Request notification permission (for Android 13+)
     */
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(activity)) {
                Log.d(TAG, "Requesting notification permission for Android 13+");
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            } else {
                Log.d(TAG, "Notification permission already granted");
            }
        } else {
            Log.d(TAG, "Android version below 13, notification permission not required");
        }
    }
    
    /**
     * Check if we should show rationale for notification permission
     */
    public static boolean shouldShowNotificationRationale(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, 
                    Manifest.permission.POST_NOTIFICATIONS
            );
        }
        return false;
    }
    
    /**
     * Handle permission result
     */
    public static void handlePermissionResult(int requestCode, String[] permissions, 
                                            int[] grantResults, PermissionCallback callback) {
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted");
                if (callback != null) callback.onPermissionGranted();
            } else {
                Log.w(TAG, "Notification permission denied");
                if (callback != null) callback.onPermissionDenied();
            }
        }
    }
    
    /**
     * Interface for permission callbacks
     */
    public interface PermissionCallback {
        void onPermissionGranted();
        void onPermissionDenied();
    }
} 