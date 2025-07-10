package com.example.assignment2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
    
    // UI Components
    private TextView welcomeText, dashboardSubtitle;
    private TextView activeTasksCount, activeTasksLabel;
    private TextView totalEarningsText, totalEarningsLabel;
    private TextView ratingText, ratingLabel;
    private TextView completionRateText, monthlyTasksText, monthlyTasksLabel;
    private TextView performanceSectionTitle;
    private LinearLayout performanceSection;
    private MaterialButton primaryActionButton;
    
    // New UI components for modern dashboard
    private RadioGroup timePeriodToggle;
    private RadioButton radioDay, radioWeek;
    private FloatingActionButton addTaskButton;
    
    // Navigation cards
    private MaterialCardView cardViewMyTasks, cardViewProfile, cardViewMessages;
    
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
        setupUserSpecificContent();
        setupClickHandlers();
        loadAllDashboardData();
    }

    private void initializeViews(View view) {
        // Welcome section
        welcomeText = view.findViewById(R.id.welcomeText);
        dashboardSubtitle = view.findViewById(R.id.dashboardSubtitle);
        
        // Time period toggle
        timePeriodToggle = view.findViewById(R.id.timePeriodToggle);
        radioDay = view.findViewById(R.id.radioDay);
        radioWeek = view.findViewById(R.id.radioWeek);
        
        // Stats cards
        activeTasksCount = view.findViewById(R.id.activeTasksCount);
        activeTasksLabel = view.findViewById(R.id.activeTasksLabel);
        totalEarningsText = view.findViewById(R.id.totalEarningsText);
        totalEarningsLabel = view.findViewById(R.id.totalEarningsLabel);
        ratingText = view.findViewById(R.id.ratingText);
        ratingLabel = view.findViewById(R.id.ratingLabel);
        
        // Performance section
        performanceSection = view.findViewById(R.id.performanceSection);
        performanceSectionTitle = view.findViewById(R.id.performanceSectionTitle);
        completionRateText = view.findViewById(R.id.completionRateText);
        monthlyTasksText = view.findViewById(R.id.monthlyTasksText);
        monthlyTasksLabel = view.findViewById(R.id.monthlyTasksLabel);
        

        
        // New dashboard components
        addTaskButton = view.findViewById(R.id.addTaskButton);
        
        // Actions
        primaryActionButton = view.findViewById(R.id.primaryActionButton);
        cardViewMyTasks = view.findViewById(R.id.cardViewMyTasks);
        cardViewProfile = view.findViewById(R.id.cardViewProfile);
        cardViewMessages = view.findViewById(R.id.cardViewMessages);
    }

    private void setupUserSpecificContent() {
        String userName = sessionManager.getUserName();
        String userType = sessionManager.getUserType();
        
        // Update main title to "Today" and subtitle with user info
        welcomeText.setText("Today");
        
        if ("Hirer".equals(userType)) {
            // Hirer-specific content
            dashboardSubtitle.setText("Manage your posted tasks, " + userName);
            totalEarningsLabel.setText("Total Spent");
            activeTasksLabel.setText("Posted Tasks");
            ratingLabel.setText("Task Completion Rate");
            performanceSectionTitle.setText("Task Management");
            monthlyTasksLabel.setText("Posted This Month");
        } else {
            // Worker-specific content
            dashboardSubtitle.setText("Your task progress, " + userName);
            totalEarningsLabel.setText("Total Earned");
            activeTasksLabel.setText("Active Tasks");
            ratingLabel.setText("★ Your Rating");
            performanceSectionTitle.setText("Performance");
            monthlyTasksLabel.setText("Completed This Month");
        }
    }



    private void setupClickHandlers() {
        // Time period toggle
        timePeriodToggle.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioDay) {
                Toast.makeText(requireContext(), "Day view selected", Toast.LENGTH_SHORT).show();
                // Update dashboard data for day view
                loadAllDashboardData();
            } else if (checkedId == R.id.radioWeek) {
                Toast.makeText(requireContext(), "Week view selected", Toast.LENGTH_SHORT).show();
                // Update dashboard data for week view
                loadAllDashboardData();
            }
        });
        
        // Primary action button (Add Task)
        primaryActionButton.setOnClickListener(v -> {
            String userType = sessionManager.getUserType();
            if ("Hirer".equals(userType)) {
                Toast.makeText(requireContext(), "Add New Task", Toast.LENGTH_SHORT).show();
                // Navigate to Add Task screen for hirers
                navigateToMyTasks(); // This will be the add task screen
            } else {
                Toast.makeText(requireContext(), "Find Tasks", Toast.LENGTH_SHORT).show();
                // Navigate to available tasks for workers
                navigateToSearch();
            }
        });
        
        // Floating add task button
        addTaskButton.setOnClickListener(v -> {
            String userType = sessionManager.getUserType();
            if ("Hirer".equals(userType)) {
                Toast.makeText(requireContext(), "Post New Task", Toast.LENGTH_SHORT).show();
                // Navigate to post task screen
                navigateToMyTasks(); // For now, go to my tasks
            } else {
                Toast.makeText(requireContext(), "Find Tasks", Toast.LENGTH_SHORT).show();
                navigateToSearch();
            }
        });
        
        cardViewMyTasks.setOnClickListener(v -> navigateToMyTasks());
        cardViewProfile.setOnClickListener(v -> navigateToProfile());
        cardViewMessages.setOnClickListener(v -> {
            // Navigate to messages (implement this later)
            navigateToProfile(); // For now, go to profile
        });
    }

    private void loadAllDashboardData() {
        String userId = sessionManager.getUserId();
        String userType = sessionManager.getUserType();
        
        if (userId == null) return;

        if ("Hirer".equals(userType)) {
            loadHirerDashboardData(userId);
        } else {
            loadWorkerDashboardData(userId);
        }
    }

    private void loadHirerDashboardData(String hirerId) {
        // Load all posted tasks count (not just open ones)
        db.collection("tasks")
            .whereEqualTo("hirerId", hirerId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int totalPostedTasks = queryDocumentSnapshots.size();
                activeTasksCount.setText(String.valueOf(totalPostedTasks));
                
                // Calculate completion rate
                int completedTasks = 0;
                double totalSpent = 0;
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Task task = document.toObject(Task.class);
                    if ("completed".equals(task.getStatus())) {
                        completedTasks++;
                        totalSpent += task.getPayment();
                    }
                }
                
                // Update total spent
                NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("ms", "MY"));
                totalEarningsText.setText(format.format(totalSpent));
                
                // Update completion rate
                if (totalPostedTasks > 0) {
                    int completionRate = (completedTasks * 100) / totalPostedTasks;
                    ratingText.setText(completionRate + "%");
                } else {
                    ratingText.setText("--%");
                }
            });
        
        // Load monthly posted tasks
        loadMonthlyHirerTasks(hirerId);
        
        // Load hirer performance metrics
        loadHirerPerformanceMetrics(hirerId);
    }

    private void loadWorkerDashboardData(String userId) {
        // Load active assigned tasks
        db.collection("tasks")
            .whereEqualTo("assignedTo", userId)
            .whereEqualTo("status", "in_progress")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                activeTasksCount.setText(String.valueOf(queryDocumentSnapshots.size()));
            });

        // Load total earnings
        db.collection("tasks")
            .whereEqualTo("assignedTo", userId)
            .whereEqualTo("status", "completed")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                double totalEarnings = 0;
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Task task = document.toObject(Task.class);
                    totalEarnings += task.getPayment();
                }
                
                NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("ms", "MY"));
                totalEarningsText.setText(format.format(totalEarnings));
            });

        // Load worker rating
        loadWorkerRating(userId);
        
        // Load monthly completed tasks
        loadMonthlyWorkerTasks(userId);
        
        // Load worker performance metrics
        loadWorkerPerformanceMetrics(userId);
    }



    private void loadWorkerRating(String userId) {
        // Calculate average rating from completed tasks
        db.collection("tasks")
            .whereEqualTo("assignedTo", userId)
            .whereEqualTo("status", "completed")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                double totalRating = 0;
                int ratingCount = 0;
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Task task = document.toObject(Task.class);
                    if (task.getWorkerRating() > 0) {
                        totalRating += task.getWorkerRating();
                        ratingCount++;
                    }
                }
                
                if (ratingCount > 0) {
                    double avgRating = totalRating / ratingCount;
                    ratingText.setText(String.format(Locale.getDefault(), "%.1f", avgRating));
                } else {
                    ratingText.setText("N/A");
                }
            });
    }

    private void loadMonthlyHirerTasks(String hirerId) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long monthStart = calendar.getTimeInMillis();
        
        // Count tasks posted this month
        db.collection("tasks")
            .whereEqualTo("hirerId", hirerId)
            .whereGreaterThanOrEqualTo("timestamp", monthStart)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                monthlyTasksText.setText(String.valueOf(queryDocumentSnapshots.size()));
            })
            .addOnFailureListener(e -> {
                monthlyTasksText.setText("0");
            });
    }

    private void loadMonthlyWorkerTasks(String userId) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long monthStart = calendar.getTimeInMillis();
        
        db.collection("tasks")
            .whereEqualTo("assignedTo", userId)
            .whereEqualTo("status", "completed")
            .whereGreaterThanOrEqualTo("timestamp", monthStart)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                monthlyTasksText.setText(String.valueOf(queryDocumentSnapshots.size()));
            });
    }

    private void loadHirerPerformanceMetrics(String hirerId) {
        // Calculate task fulfillment rate (tasks that got completed vs posted)
        db.collection("tasks")
            .whereEqualTo("hirerId", hirerId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int totalTasks = queryDocumentSnapshots.size();
                int completedTasks = 0;
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Task task = document.toObject(Task.class);
                    if ("completed".equals(task.getStatus())) {
                        completedTasks++;
                    }
                }
                
                if (totalTasks > 0) {
                    int fulfillmentRate = (completedTasks * 100) / totalTasks;
                    completionRateText.setText(fulfillmentRate + "%");
                } else {
                    completionRateText.setText("N/A");
                }
            });
    }

    private void loadWorkerPerformanceMetrics(String userId) {
        // Calculate task completion rate
        db.collection("tasks")
            .whereEqualTo("assignedTo", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int totalTasks = queryDocumentSnapshots.size();
                int completedTasks = 0;
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Task task = document.toObject(Task.class);
                    if ("completed".equals(task.getStatus())) {
                        completedTasks++;
                    }
                }
                
                if (totalTasks > 0) {
                    int completionRate = (completedTasks * 100) / totalTasks;
                    completionRateText.setText(completionRate + "%");
                } else {
                    completionRateText.setText("N/A");
                }
            });
    }



    private void navigateToSearch() {
        if (getActivity() instanceof TasksActivity) {
            ((TasksActivity) getActivity()).findViewById(R.id.bottom_navigation)
                .findViewById(R.id.navigation_search).performClick();
        }
    }

    private void navigateToMyTasks() {
        if (getActivity() instanceof TasksActivity) {
            ((TasksActivity) getActivity()).findViewById(R.id.bottom_navigation)
                .findViewById(R.id.navigation_my_tasks).performClick();
        }
    }

    private void navigateToProfile() {
        if (getActivity() instanceof TasksActivity) {
            ((TasksActivity) getActivity()).findViewById(R.id.bottom_navigation)
                .findViewById(R.id.navigation_profile).performClick();
        }
    }

} 