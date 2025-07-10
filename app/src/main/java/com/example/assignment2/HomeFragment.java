package com.example.assignment2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private SessionManager sessionManager;
    private FirebaseFirestore db;
    
    // Dashboard UI Components
    private TextView welcomeText, dashboardSubtitle;
    private TextView activeTasksCount, completedTasksCount;
    private TextView pointsCount, levelCount;
    private TextView leaderboardSubtext;
    
    // Navigation cards
    private MaterialCardView searchCard, myTasksCard, leaderboardCard;
    
    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        sessionManager = new SessionManager(requireContext());
        db = FirebaseFirestore.getInstance();
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupUserContent();
        setupClickHandlers();
        loadDashboardData();
        loadTaskProgress();
    }

    private void initializeViews(View view) {
        // Dashboard components
        welcomeText = view.findViewById(R.id.welcomeText);
        dashboardSubtitle = view.findViewById(R.id.dashboardSubtitle);
        
        // Stats cards
        activeTasksCount = view.findViewById(R.id.activeTasksCount);
        completedTasksCount = view.findViewById(R.id.completedTasksCount);
        pointsCount = view.findViewById(R.id.pointsCount);
        levelCount = view.findViewById(R.id.levelCount);
        
        // Leaderboard banner
        leaderboardSubtext = view.findViewById(R.id.leaderboardSubtext);
        
        // Navigation cards
        searchCard = view.findViewById(R.id.searchCard);
        myTasksCard = view.findViewById(R.id.myTasksCard);
        leaderboardCard = view.findViewById(R.id.leaderboardCard);
    }

    private void setupUserContent() {
        String userName = sessionManager.getUserName();
        String userType = sessionManager.getUserType();
        
        // Dashboard content
        welcomeText.setText("Today");
        
        if ("Hirer".equals(userType)) {
            // Hirer-specific content
            dashboardSubtitle.setText("Manage your posted tasks, " + userName);
        } else {
            // Worker-specific content
            dashboardSubtitle.setText("Your task progress, " + userName);
        }
    }

    private void setupClickHandlers() {
        // Navigation cards
        searchCard.setOnClickListener(v -> navigateToSearch());
        myTasksCard.setOnClickListener(v -> navigateToMyTasks());
        leaderboardCard.setOnClickListener(v -> navigateToLeaderboard());
    }

    private void loadDashboardData() {
        String userType = sessionManager.getUserType();
        String userId = sessionManager.getUserId();
        
        if (userId != null) {
            if ("Hirer".equals(userType)) {
                loadHirerDashboardData(userId);
            } else {
                loadWorkerDashboardData(userId);
            }
        } else {
            // Set default values if no user data
            setDefaultValues();
        }
    }

    private void loadTaskProgress() {
        String userName = sessionManager.getUserName();
        if (userName != null) {
            // Fetch tasks that user has applied to and are in-progress
            db.collection("tasks")
                .whereEqualTo("assignedTo", userName)
                .whereEqualTo("status", "in_progress")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int inProgressCount = queryDocumentSnapshots.size();
                    updateTaskProgressDisplay(inProgressCount);
                })
                .addOnFailureListener(e -> {
                    // Handle error - show default
                    updateTaskProgressDisplay(0);
                });
        } else {
            updateTaskProgressDisplay(0);
        }
    }

    private void updateTaskProgressDisplay(int inProgressTasks) {
        // Update the task progress section with actual data
        // For now, let's update the subtitle to include this information
        String userName = sessionManager.getUserName();
        String userType = sessionManager.getUserType();
        
        if ("Hirer".equals(userType)) {
            dashboardSubtitle.setText("Manage your posted tasks, " + userName);
        } else {
            if (inProgressTasks > 0) {
                dashboardSubtitle.setText("You have " + inProgressTasks + " tasks in progress, " + userName);
            } else {
                dashboardSubtitle.setText("No active tasks in progress, " + userName);
            }
        }
    }

    private void loadHirerDashboardData(String hirerId) {
        // Load posted tasks count
        TaskManager.getTaskCountByHirer(hirerId, new TaskManager.TaskCountCallback() {
            @Override
            public void onSuccess(int count) {
                activeTasksCount.setText(String.valueOf(count));
            }

            @Override
            public void onError(String error) {
                activeTasksCount.setText("0");
            }
        });

        // Load completed tasks by this hirer's posted tasks
        db.collection("tasks")
            .whereEqualTo("hirerId", hirerId)
            .whereEqualTo("status", "completed")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                completedTasksCount.setText(String.valueOf(queryDocumentSnapshots.size()));
            })
            .addOnFailureListener(e -> completedTasksCount.setText("0"));

        // Load hirer points
        db.collection("hirers")
            .whereEqualTo("name", sessionManager.getUserName())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    int points = queryDocumentSnapshots.getDocuments().get(0).getLong("points") != null ?
                            queryDocumentSnapshots.getDocuments().get(0).getLong("points").intValue() : 0;
                    pointsCount.setText(String.valueOf(points));
                    
                    // Calculate level based on points
                    int level = calculateLevel(points);
                    levelCount.setText(String.valueOf(level));
                } else {
                    pointsCount.setText("0");
                    levelCount.setText("1");
                }
            })
            .addOnFailureListener(e -> {
                pointsCount.setText("0");
                levelCount.setText("1");
            });
    }

    private void loadWorkerDashboardData(String userId) {
        // Load active tasks
        db.collection("tasks")
            .whereEqualTo("assignedTo", sessionManager.getUserName())
            .whereIn("status", List.of("pending", "in_progress"))
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                activeTasksCount.setText(String.valueOf(queryDocumentSnapshots.size()));
            })
            .addOnFailureListener(e -> activeTasksCount.setText("0"));

        // Load completed tasks
        db.collection("tasks")
            .whereEqualTo("assignedTo", sessionManager.getUserName())
            .whereEqualTo("status", "completed")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                completedTasksCount.setText(String.valueOf(queryDocumentSnapshots.size()));
            })
            .addOnFailureListener(e -> completedTasksCount.setText("0"));

        // Load user points
        db.collection("users")
            .whereEqualTo("name", sessionManager.getUserName())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    int points = queryDocumentSnapshots.getDocuments().get(0).getLong("points") != null ?
                            queryDocumentSnapshots.getDocuments().get(0).getLong("points").intValue() : 0;
                    pointsCount.setText(String.valueOf(points));
                    
                    // Calculate level based on points
                    int level = calculateLevel(points);
                    levelCount.setText(String.valueOf(level));
                } else {
                    pointsCount.setText("0");
                    levelCount.setText("1");
                }
            })
            .addOnFailureListener(e -> {
                pointsCount.setText("0");
                levelCount.setText("1");
            });
    }

    private int calculateLevel(int points) {
        // Simple level calculation: every 100 points = 1 level
        return Math.max(1, points / 100 + 1);
    }

    private void setDefaultValues() {
        activeTasksCount.setText("0");
        completedTasksCount.setText("0");
        pointsCount.setText("0");
        levelCount.setText("1");
    }

    private void navigateToSearch() {
        // Navigate to search tab using bottom navigation
        if (getActivity() instanceof TasksActivity) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.navigation_search);
            }
        }
    }

    private void navigateToMyTasks() {
        // Navigate to my tasks tab using bottom navigation
        if (getActivity() instanceof TasksActivity) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.navigation_my_tasks);
            }
        }
    }

    private void navigateToLeaderboard() {
        // Navigate to LeaderboardFragment using fragment transaction
        if (getActivity() != null) {
            LeaderboardFragment leaderboardFragment = LeaderboardFragment.newInstance();
            getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, leaderboardFragment)
                .addToBackStack(null)
                .commit();
        }
    }
} 