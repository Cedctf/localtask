package com.example.assignment2;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class TasksActivity extends AppCompatActivity {

    private String userName;
    private String userType;
    private String userEmail;
    private ChatbotManager chatbotManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        // Configure status bar
        StatusBarHelper.configureStatusBar(this);

        // Initialize chatbot
        chatbotManager = new ChatbotManager(this);
        chatbotManager.addChatbotButton(this);

        // Get user data from intent
        userName = getIntent().getStringExtra("USER_NAME");
        userType = getIntent().getStringExtra("USER_TYPE");
        userEmail = getIntent().getStringExtra("USER_EMAIL");

        // Request notification permission for Android 13+
        requestNotificationPermission();

        // Get and save FCM token on app launch (for already logged in users)
        saveUserFCMToken();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                selectedFragment = HomeFragment.newInstance();
            } else if (itemId == R.id.navigation_my_tasks) {
                selectedFragment = MyTasksFragment.newInstance();
            } else if (itemId == R.id.navigation_search) {
                selectedFragment = SearchFragment.newInstance();
            } else if (itemId == R.id.navigation_badges) {
                selectedFragment = BadgesFragment.newInstance();
            } else if (itemId == R.id.navigation_profile) {
                selectedFragment = ProfileFragment.newInstance(userName, userType, userEmail);
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            }
            return true;
        });

        // Check if we need to show a specific fragment (from TaskDetailsActivity navigation)
        String showFragment = getIntent().getStringExtra("SHOW_FRAGMENT");
        Fragment initialFragment;
        int selectedNavItem;

        if (showFragment != null) {
            switch (showFragment) {
                case "my_tasks":
                    initialFragment = MyTasksFragment.newInstance();
                    selectedNavItem = R.id.navigation_my_tasks;
                    break;
                case "search":
                    initialFragment = SearchFragment.newInstance();
                    selectedNavItem = R.id.navigation_search;
                    break;
                case "badges":
                    initialFragment = BadgesFragment.newInstance();
                    selectedNavItem = R.id.navigation_badges;
                    break;
                case "profile":
                    initialFragment = ProfileFragment.newInstance(userName, userType, userEmail);
                    selectedNavItem = R.id.navigation_profile;
                    break;
                default:
                    initialFragment = HomeFragment.newInstance();
                    selectedNavItem = R.id.navigation_home;
                    break;
            }
        } else {
            // Default to HomeFragment (Dashboard)
            initialFragment = HomeFragment.newInstance();
            selectedNavItem = R.id.navigation_home;
        }

        // Set the correct navigation item and fragment
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(selectedNavItem);
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, initialFragment)
                .commit();
        }
    }

    private void requestNotificationPermission() {
        if (!NotificationPermissionHelper.hasNotificationPermission(this)) {
            // Request permission
            NotificationPermissionHelper.requestNotificationPermission(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        NotificationPermissionHelper.handlePermissionResult(requestCode, permissions, grantResults, 
            new NotificationPermissionHelper.PermissionCallback() {
                @Override
                public void onPermissionGranted() {
                    Toast.makeText(TasksActivity.this, "Notifications enabled! You'll receive updates about your tasks.", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onPermissionDenied() {
                    Toast.makeText(TasksActivity.this, "Notifications disabled. You may miss important task updates.", Toast.LENGTH_LONG).show();
                }
            });
    }

    private void saveUserFCMToken() {
        // Save FCM token for logged in users on app launch
        FCMTokenManager fcmTokenManager = new FCMTokenManager(this);
        fcmTokenManager.getAndSaveToken();
    }
} 