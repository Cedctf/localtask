package com.example.assignment2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {
    private static final String ARG_USER_NAME = "userName";
    private static final String ARG_USER_TYPE = "userType";
    private static final String ARG_USER_EMAIL = "userEmail";

    private String userName;
    private String userType;
    private String userEmail;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    // Tab buttons - only Dashboard and Settings
    private TextView tabDashboard, tabSettings;
    
    // Content views - only Dashboard and Settings
    private ScrollView dashboardContent, settingsContent;

    public static ProfileFragment newInstance(String userName, String userType, String userEmail) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_NAME, userName);
        args.putString(ARG_USER_TYPE, userType);
        args.putString(ARG_USER_EMAIL, userEmail);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userName = getArguments().getString(ARG_USER_NAME);
            userType = getArguments().getString(ARG_USER_TYPE);
            userEmail = getArguments().getString(ARG_USER_EMAIL);
        }
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize tab buttons
        setupTabButtons(view);
        
        // Initialize content views
        setupContentViews(view);

        // Set user information
        TextView nameText = view.findViewById(R.id.profileName);
        TextView typeText = view.findViewById(R.id.profileType);
        TextView emailText = view.findViewById(R.id.profileEmail);

        nameText.setText(userName);
        typeText.setText(userType);
        emailText.setText(userEmail);

        // Load statistics for dashboard tab
        loadUserStatistics(view);

        // Setup buttons in different tabs
        setupTabContent(view);

        // Show dashboard by default
        showDashboard();

        return view;
    }

    private void setupTabButtons(View view) {
        tabDashboard = view.findViewById(R.id.tabDashboard);
        tabSettings = view.findViewById(R.id.tabSettings);

        tabDashboard.setOnClickListener(v -> showDashboard());
        tabSettings.setOnClickListener(v -> showSettings());
    }

    private void setupContentViews(View view) {
        dashboardContent = view.findViewById(R.id.dashboardContent);
        settingsContent = view.findViewById(R.id.settingsContent);
    }

    private void setupTabContent(View view) {
        // Setup dashboard content buttons
        Button shareProgressButton = view.findViewById(R.id.btnShareProgress);
        shareProgressButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Share Progress coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Setup settings content buttons
        Button editProfileButton = view.findViewById(R.id.btnEditProfile);
        Button notificationSettingsButton = view.findViewById(R.id.btnNotificationSettings);
        Button privacySettingsButton = view.findViewById(R.id.btnPrivacySettings);
        Button logoutButton = view.findViewById(R.id.btnLogout);

        editProfileButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Edit Profile coming soon!", Toast.LENGTH_SHORT).show();
        });

        notificationSettingsButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Notification Settings coming soon!", Toast.LENGTH_SHORT).show();
        });

        privacySettingsButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Privacy Settings coming soon!", Toast.LENGTH_SHORT).show();
        });

        logoutButton.setOnClickListener(v -> logout());
    }

    private void showDashboard() {
        updateTabSelection(tabDashboard);
        showContent(dashboardContent);
    }

    private void showSettings() {
        updateTabSelection(tabSettings);
        showContent(settingsContent);
    }

    private void updateTabSelection(TextView selectedTab) {
        // Reset all tabs to unselected state
        tabDashboard.setAlpha(0.7f);
        tabDashboard.setTypeface(null, android.graphics.Typeface.NORMAL);
        
        tabSettings.setAlpha(0.7f);
        tabSettings.setTypeface(null, android.graphics.Typeface.NORMAL);

        // Highlight selected tab
        selectedTab.setAlpha(1.0f);
        selectedTab.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void showContent(ScrollView selectedContent) {
        // Hide all content views
        dashboardContent.setVisibility(View.GONE);
        settingsContent.setVisibility(View.GONE);

        // Show selected content
        selectedContent.setVisibility(View.VISIBLE);
    }

    private void loadUserStatistics(View view) {
        // Load dashboard stats
        TextView totalTasksText = view.findViewById(R.id.totalTasksCount);
        TextView longestStreakText = view.findViewById(R.id.longestStreakCount);
        TextView currentStreakText = view.findViewById(R.id.currentStreakCount);

        // Load stats for dashboard tab stats section
        TextView tasksCompletedText = view.findViewById(R.id.tasksCompleted);
        TextView totalPointsText = view.findViewById(R.id.totalPoints);
        TextView badgesEarnedText = view.findViewById(R.id.badgesEarned);

        if ("Hirer".equals(userType)) {
            // For hirers, show tasks uploaded instead of tasks completed
            loadHirerStatistics(view, totalTasksText, tasksCompletedText, totalPointsText, badgesEarnedText);
        } else {
            // For users, show original statistics
            loadRegularUserStatistics(totalTasksText, tasksCompletedText, totalPointsText, badgesEarnedText);
        }

        // Set default streak values (these would be calculated from actual data in a real app)
        longestStreakText.setText("1 day");
        currentStreakText.setText("1 day");
    }

    private void loadHirerStatistics(View view, TextView totalTasksText, TextView tasksUploadedText, TextView totalPointsText, TextView badgesEarnedText) {
        String userId = sessionManager.getUserId();
        if (userId != null) {
            // Load tasks uploaded by this hirer
            TaskManager.getTaskCountByHirer(userId, new TaskManager.TaskCountCallback() {
                @Override
                public void onSuccess(int count) {
                    totalTasksText.setText(String.valueOf(count));
                    tasksUploadedText.setText(String.valueOf(count));
                }

                @Override
                public void onError(String error) {
                    totalTasksText.setText("0");
                    tasksUploadedText.setText("0");
                }
            });
        }

        // Load total points for hirer
        db.collection("hirers")
            .whereEqualTo("name", userName)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    int points = queryDocumentSnapshots.getDocuments().get(0).getLong("points") != null ?
                            queryDocumentSnapshots.getDocuments().get(0).getLong("points").intValue() : 0;
                    totalPointsText.setText(String.valueOf(points));
                }
            });

        // Load badges earned by hirer
        db.collection("hirer_badges")
            .whereEqualTo("userName", userName)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                badgesEarnedText.setText(String.valueOf(queryDocumentSnapshots.size()));
            });
    }

    private void loadRegularUserStatistics(TextView totalTasksText, TextView tasksCompletedText, TextView totalPointsText, TextView badgesEarnedText) {
        // Load tasks completed
        db.collection("tasks")
            .whereEqualTo("assignedTo", userName)
            .whereEqualTo("status", "completed")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int completedCount = queryDocumentSnapshots.size();
                totalTasksText.setText(String.valueOf(completedCount));
                tasksCompletedText.setText(String.valueOf(completedCount));
            });

        // Load total points
        db.collection("users")
            .whereEqualTo("name", userName)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    int points = queryDocumentSnapshots.getDocuments().get(0).getLong("points") != null ?
                            queryDocumentSnapshots.getDocuments().get(0).getLong("points").intValue() : 0;
                    totalPointsText.setText(String.valueOf(points));
                }
            });

        // Load badges earned
        db.collection("user_badges")
            .whereEqualTo("userName", userName)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                badgesEarnedText.setText(String.valueOf(queryDocumentSnapshots.size()));
            });
    }

    private void logout() {
        // Clear session
        sessionManager.logoutUser();

        // Navigate to login activity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
} 