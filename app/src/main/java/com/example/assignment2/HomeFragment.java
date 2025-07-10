package com.example.assignment2;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.android.material.button.MaterialButton;
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
    private SharedPreferences walletPrefs;
    
    // Header components
    private TextView welcomeText;
    
    // Cash Balance display (read-only, managed in BadgesFragment)
    private TextView cashBalanceAmount;
    private MaterialButton addCashButton, cashOutButton;
    
    // Feature cards
    private MaterialCardView leaderboardCard, searchCard, myTasksCard, badgesCard;
    
    // Quick stats
    private TextView activeTasksCount, completedTasksCount;
    private TextView pointsCount, levelCount;
    private TextView leaderboardSubtext;
    
    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        sessionManager = new SessionManager(requireContext());
        db = FirebaseFirestore.getInstance();
        walletPrefs = requireActivity().getSharedPreferences("badges_prefs", 0);
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupUserContent();
        setupWalletDisplay();
        setupClickHandlers();
        loadDashboardData();
    }

    private void initializeViews(View view) {
        // Header
        welcomeText = view.findViewById(R.id.welcomeText);
        
        // Cash Balance section (display only)
        cashBalanceAmount = view.findViewById(R.id.cashBalanceAmount);
        addCashButton = view.findViewById(R.id.addCashButton);
        cashOutButton = view.findViewById(R.id.cashOutButton);
        
        // Feature cards
        leaderboardCard = view.findViewById(R.id.leaderboardCard);
        searchCard = view.findViewById(R.id.searchCard);
        myTasksCard = view.findViewById(R.id.myTasksCard);
        badgesCard = view.findViewById(R.id.badgesCard);
        
        // Quick stats
        activeTasksCount = view.findViewById(R.id.activeTasksCount);
        completedTasksCount = view.findViewById(R.id.completedTasksCount);
        pointsCount = view.findViewById(R.id.pointsCount);
        levelCount = view.findViewById(R.id.levelCount);
        leaderboardSubtext = view.findViewById(R.id.leaderboardSubtext);
    }

    private void setupUserContent() {
        String userName = sessionManager.getUserName();
        String userType = sessionManager.getUserType();
        
        // Update header based on user type
        if ("Hirer".equals(userType)) {
            welcomeText.setText("Business");
            if (leaderboardSubtext != null) {
                leaderboardSubtext.setText("Find top talent");
            }
        } else {
            welcomeText.setText("Money");
            if (leaderboardSubtext != null) {
                leaderboardSubtext.setText("View rankings");
            }
        }
    }
    
    private void setupWalletDisplay() {
        // Display wallet amount (read-only, managed in BadgesFragment)
        loadWalletAmount();
        
        // Redirect buttons to BadgesFragment where wallet is managed
        if (addCashButton != null) {
            addCashButton.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Go to Badges tab to manage wallet", Toast.LENGTH_SHORT).show();
                navigateToBadges();
            });
        }
        
        if (cashOutButton != null) {
            cashOutButton.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Go to Badges tab to manage wallet", Toast.LENGTH_SHORT).show();
                navigateToBadges();
            });
        }
    }
    
    private void loadWalletAmount() {
        int walletAmount = walletPrefs.getInt("wallet_amount", 0);
        updateWalletDisplay(walletAmount);
    }
    
    private void updateWalletDisplay(int amount) {
        if (cashBalanceAmount != null) {
            cashBalanceAmount.setText("RM" + String.format("%.2f", (double) amount));
        }
    }

    private void setupClickHandlers() {
        // Feature cards navigation
        leaderboardCard.setOnClickListener(v -> navigateToLeaderboard());
        searchCard.setOnClickListener(v -> navigateToSearch());
        myTasksCard.setOnClickListener(v -> navigateToMyTasks());
        badgesCard.setOnClickListener(v -> navigateToBadges());
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

    private void loadHirerDashboardData(String hirerId) {
        // Load posted tasks count
        TaskManager.getTaskCountByHirer(hirerId, new TaskManager.TaskCountCallback() {
            @Override
            public void onSuccess(int count) {
                if (activeTasksCount != null) {
                activeTasksCount.setText(String.valueOf(count));
                }
            }

            @Override
            public void onError(String error) {
                if (activeTasksCount != null) {
                activeTasksCount.setText("0");
                }
            }
        });

        // Load completed tasks by this hirer's posted tasks
        db.collection("tasks")
            .whereEqualTo("hirerId", hirerId)
            .whereEqualTo("status", "completed")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (completedTasksCount != null) {
                completedTasksCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                }
            })
            .addOnFailureListener(e -> {
                if (completedTasksCount != null) {
                    completedTasksCount.setText("0");
                }
            });

        // Load hirer points
        db.collection("hirers")
            .whereEqualTo("name", sessionManager.getUserName())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    int points = queryDocumentSnapshots.getDocuments().get(0).getLong("points") != null ?
                            queryDocumentSnapshots.getDocuments().get(0).getLong("points").intValue() : 0;
                    if (pointsCount != null) {
                    pointsCount.setText(String.valueOf(points));
                    }
                    
                    // Calculate level based on points
                    int level = calculateLevel(points);
                    if (levelCount != null) {
                    levelCount.setText(String.valueOf(level));
                    }
                } else {
                    if (pointsCount != null) pointsCount.setText("0");
                    if (levelCount != null) levelCount.setText("1");
                }
            })
            .addOnFailureListener(e -> {
                if (pointsCount != null) pointsCount.setText("0");
                if (levelCount != null) levelCount.setText("1");
            });
    }

    private void loadWorkerDashboardData(String userId) {
        // Load active tasks
        db.collection("tasks")
            .whereEqualTo("assignedTo", sessionManager.getUserName())
            .whereIn("status", List.of("pending", "in_progress"))
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (activeTasksCount != null) {
                activeTasksCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                }
            })
            .addOnFailureListener(e -> {
                if (activeTasksCount != null) {
                    activeTasksCount.setText("0");
                }
            });

        // Load completed tasks
        db.collection("tasks")
            .whereEqualTo("assignedTo", sessionManager.getUserName())
            .whereEqualTo("status", "completed")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (completedTasksCount != null) {
                completedTasksCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                }
            })
            .addOnFailureListener(e -> {
                if (completedTasksCount != null) {
                    completedTasksCount.setText("0");
                }
            });

        // Load user points
        db.collection("users")
            .whereEqualTo("name", sessionManager.getUserName())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    int points = queryDocumentSnapshots.getDocuments().get(0).getLong("points") != null ?
                            queryDocumentSnapshots.getDocuments().get(0).getLong("points").intValue() : 0;
                    if (pointsCount != null) {
                    pointsCount.setText(String.valueOf(points));
                    }
                    
                    // Calculate level based on points
                    int level = calculateLevel(points);
                    if (levelCount != null) {
                    levelCount.setText(String.valueOf(level));
                    }
                } else {
                    if (pointsCount != null) pointsCount.setText("0");
                    if (levelCount != null) levelCount.setText("1");
                }
            })
            .addOnFailureListener(e -> {
                if (pointsCount != null) pointsCount.setText("0");
                if (levelCount != null) levelCount.setText("1");
            });
    }

    private int calculateLevel(int points) {
        // Simple level calculation: every 100 points = 1 level
        return Math.max(1, points / 100 + 1);
    }

    private void setDefaultValues() {
        if (activeTasksCount != null) activeTasksCount.setText("0");
        if (completedTasksCount != null) completedTasksCount.setText("0");
        if (pointsCount != null) pointsCount.setText("0");
        if (levelCount != null) levelCount.setText("1");
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
    
    private void navigateToBadges() {
        // Navigate to badges tab using bottom navigation
        if (getActivity() instanceof TasksActivity) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.navigation_badges);
            }
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh wallet amount display when returning to this fragment
        loadWalletAmount();
    }
} 