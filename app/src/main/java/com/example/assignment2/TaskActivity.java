package com.example.assignment2;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class TaskActivity extends AppCompatActivity {

    private String userName;
    private String userType;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        // Get user data from intent
        userName = getIntent().getStringExtra("USER_NAME");
        userType = getIntent().getStringExtra("USER_TYPE");
        userEmail = getIntent().getStringExtra("USER_EMAIL");

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                selectedFragment = HomeFragment.newInstance(userName, userType);
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

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, HomeFragment.newInstance(userName, userType))
                .commit();
        }
    }
} 