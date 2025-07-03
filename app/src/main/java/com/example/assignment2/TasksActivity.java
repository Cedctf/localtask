package com.example.assignment2;

import android.os.Bundle;
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

        // Initialize chatbot
        chatbotManager = new ChatbotManager(this);
        chatbotManager.addChatbotButton(this);

        // Get user data from intent
        userName = getIntent().getStringExtra("USER_NAME");
        userType = getIntent().getStringExtra("USER_TYPE");
        userEmail = getIntent().getStringExtra("USER_EMAIL");

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                selectedFragment = TasksFragment.newInstance();
            } else if (itemId == R.id.navigation_leaderboard) {
                selectedFragment = LeaderboardFragment.newInstance();
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
                case "leaderboard":
                    initialFragment = LeaderboardFragment.newInstance();
                    selectedNavItem = R.id.navigation_leaderboard;
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
                    initialFragment = TasksFragment.newInstance();
                    selectedNavItem = R.id.navigation_home;
                    break;
            }
        } else {
            // Default to TasksFragment (Home)
            initialFragment = TasksFragment.newInstance();
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
} 