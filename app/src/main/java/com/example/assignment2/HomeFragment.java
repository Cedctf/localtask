package com.example.assignment2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.NumberFormat;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private SessionManager sessionManager;
    private FirebaseFirestore db;
    private TextView activeTasksCount, totalEarningsText, totalEarningsLabel;
    
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
        setupDashboard();
        setupClickHandlers(view);
        loadDashboardStats();
    }

    private void initializeViews(View view) {
        activeTasksCount = view.findViewById(R.id.activeTasksCount);
        totalEarningsText = view.findViewById(R.id.totalEarningsText);
        
        // Find the label TextView to update it based on user type
        totalEarningsLabel = view.findViewById(R.id.totalEarningsLabel);
        
        // Update the label based on user type
        String userType = sessionManager.getUserType();
        if ("Hirer".equals(userType)) {
            totalEarningsLabel.setText("Total Spent");
        } else {
            totalEarningsLabel.setText("Total Earnings");
        }
    }

    private void setupDashboard() {
        TextView welcomeText = getView().findViewById(R.id.welcomeText);
        
        if (welcomeText != null) {
            String userName = sessionManager.getUserName();
            String userType = sessionManager.getUserType();
            welcomeText.setText("Welcome back, " + userName + "!");
        }
    }

    private void setupClickHandlers(View view) {
        MaterialCardView cardViewTasks = view.findViewById(R.id.cardViewTasks);
        MaterialCardView cardViewProfile = view.findViewById(R.id.cardViewProfile);
        MaterialCardView cardViewBadges = view.findViewById(R.id.cardViewBadges);

        cardViewTasks.setOnClickListener(v -> navigateToSearch());
        cardViewProfile.setOnClickListener(v -> navigateToProfile());
        cardViewBadges.setOnClickListener(v -> navigateToBadges());
    }

    private void loadDashboardStats() {
        String userId = sessionManager.getUserId();
        String userType = sessionManager.getUserType();
        
        if (userId == null) return;

        if ("Hirer".equals(userType)) {
            loadHirerStats(userId);
        } else {
            loadWorkerStats(userId);
        }
    }

    private void loadHirerStats(String hirerId) {
        // Load active tasks count
        db.collection("tasks")
            .whereEqualTo("hirerId", hirerId)
            .whereEqualTo("status", "open")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (activeTasksCount != null) {
                    activeTasksCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                }
            });

        // Load total spent from user document
        db.collection("users").document(hirerId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                double totalSpent = 0;
                if (documentSnapshot.exists() && documentSnapshot.contains("totalSpent")) {
                    Number spentNumber = documentSnapshot.getDouble("totalSpent");
                    totalSpent = spentNumber != null ? spentNumber.doubleValue() : 0;
                }
                
                if (totalEarningsText != null) {
                    NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
                    totalEarningsText.setText(format.format(totalSpent));
                }
            })
            .addOnFailureListener(e -> {
                if (totalEarningsText != null) {
                    totalEarningsText.setText("$0.00");
                }
            });
    }

    private void loadWorkerStats(String userId) {
        // Load assigned tasks count
        db.collection("tasks")
            .whereEqualTo("assignedTo", userId)
            .whereEqualTo("status", "in_progress")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (activeTasksCount != null) {
                    activeTasksCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                }
            });

        // Load total earnings from completed tasks
        db.collection("tasks")
            .whereEqualTo("assignedTo", userId)
            .whereEqualTo("status", "completed")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                double totalEarnings = 0;
                for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Task task = document.toObject(Task.class);
                    totalEarnings += task.getPayment();
                }
                
                if (totalEarningsText != null) {
                    NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
                    totalEarningsText.setText(format.format(totalEarnings));
                }
            });
    }

    private void navigateToSearch() {
        if (getActivity() instanceof TasksActivity) {
            ((TasksActivity) getActivity()).findViewById(R.id.bottom_navigation)
                .findViewById(R.id.navigation_search).performClick();
        }
    }

    private void navigateToProfile() {
        if (getActivity() instanceof TasksActivity) {
            ((TasksActivity) getActivity()).findViewById(R.id.bottom_navigation)
                .findViewById(R.id.navigation_profile).performClick();
        }
    }

    private void navigateToBadges() {
        if (getActivity() instanceof TasksActivity) {
            ((TasksActivity) getActivity()).findViewById(R.id.bottom_navigation)
                .findViewById(R.id.navigation_badges).performClick();
        }
    }
} 