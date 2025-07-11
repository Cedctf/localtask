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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
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
    
    // Cash Balance display (now synced with total earnings)
    private TextView cashBalanceAmount;
    private MaterialButton addCashButton, cashOutButton;
    
    // Feature cards
    private MaterialCardView leaderboardCard, searchCard, myTasksCard, badgesCard;
    
    // Quick stats
    private TextView activeTasksCount, completedTasksCount;
    private TextView pointsCount, levelCount;
    private TextView leaderboardSubtext;
    
    // Real-time listeners
    private ListenerRegistration activeTasksListener;
    private ListenerRegistration completedTasksListener;
    private ListenerRegistration userDataListener;

    private RecyclerView homeTaskRecyclerView;
    private TaskAdapter homeTaskAdapter;
    private List<Task> homeTaskList = new ArrayList<>();
    private TextView taskListHeader;
    
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
        setupRealTimeListeners();
        initializeTaskListViews(view);
        fetchAndDisplayHomeTasks();
    }

    private void initializeViews(View view) {
        // Header
        welcomeText = view.findViewById(R.id.welcomeText);
        
        // Cash Balance section (now synced with total earnings)
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

    private void initializeTaskListViews(View view) {
        homeTaskRecyclerView = view.findViewById(R.id.homeTaskRecyclerView);
        taskListHeader = view.findViewById(R.id.taskListHeader);
        homeTaskAdapter = new TaskAdapter(homeTaskList);
        homeTaskRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        homeTaskRecyclerView.setAdapter(homeTaskAdapter);
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
        // Load wallet amount from both sources and sync them
        syncWalletWithTotalEarnings();
        
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
    
    /**
     * Sync wallet balance with total earnings from Firestore
     * This ensures cash balance reflects actual earnings from completed tasks
     */
    private void syncWalletWithTotalEarnings() {
        String userName = sessionManager.getUserName();
        String userType = sessionManager.getUserType();
        
        if (userName != null && !"Hirer".equals(userType)) {
            // For regular users, sync wallet with total earnings
            db.collection("users")
                .whereEqualTo("name", userName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                        
                        // Get total earnings from Firestore
                        double totalEarnings = 0;
                        if (userDoc.contains("totalEarnings")) {
                            Number earningsNumber = userDoc.getDouble("totalEarnings");
                            totalEarnings = earningsNumber != null ? earningsNumber.doubleValue() : 0;
                        }
                        
                        // Convert to integer for RM currency
                        int totalEarningsInt = (int) Math.round(totalEarnings);
                        
                        // Get current wallet amount from SharedPreferences
                        int currentWalletAmount = walletPrefs.getInt("wallet_amount", 0);
                        
                        // Update wallet to match total earnings if they differ
                        if (currentWalletAmount != totalEarningsInt) {
                            android.util.Log.d("HomeFragment", "Syncing wallet: " + currentWalletAmount + 
                                    " -> " + totalEarningsInt + " (from totalEarnings: " + totalEarnings + ")");
                            
                            walletPrefs.edit().putInt("wallet_amount", totalEarningsInt).apply();
                            updateWalletDisplay(totalEarningsInt);
                        } else {
                            updateWalletDisplay(currentWalletAmount);
                        }
                    } else {
                        // User not found in Firestore, use SharedPreferences value
                        int walletAmount = walletPrefs.getInt("wallet_amount", 0);
                        updateWalletDisplay(walletAmount);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.w("HomeFragment", "Failed to sync wallet with total earnings: " + e.getMessage());
                    // Fall back to SharedPreferences value
                    int walletAmount = walletPrefs.getInt("wallet_amount", 0);
                    updateWalletDisplay(walletAmount);
                });
        } else {
            // For hirers, just show SharedPreferences value (they don't earn from tasks)
            int walletAmount = walletPrefs.getInt("wallet_amount", 0);
            updateWalletDisplay(walletAmount);
        }
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

    /**
     * Setup real-time listeners for task statistics
     * This provides live updates when tasks change status
     */
    private void setupRealTimeListeners() {
        String userType = sessionManager.getUserType();
        String userName = sessionManager.getUserName();
        String userId = sessionManager.getUserId();
        
        if (userName != null) {
            if ("Hirer".equals(userType)) {
                setupHirerRealTimeListeners(userId);
            } else {
                setupWorkerRealTimeListeners(userName);
            }
            
            // Setup user data listener for points and level
            setupUserDataListener(userName, userType);
        } else {
            setDefaultValues();
        }
    }
    
    private void setupHirerRealTimeListeners(String hirerId) {
        if (hirerId == null) return;
        
        // Real-time listener for active tasks (posted by hirer)
        activeTasksListener = db.collection("tasks")
            .whereEqualTo("hirerId", hirerId)
            .whereIn("status", List.of("open", "in_progress", "pending_payment"))
            .addSnapshotListener((queryDocumentSnapshots, e) -> {
                if (e != null) {
                    android.util.Log.w("HomeFragment", "Listen failed for hirer active tasks", e);
                    return;
                }
                
                if (queryDocumentSnapshots != null && activeTasksCount != null) {
                    int count = queryDocumentSnapshots.size();
                    activeTasksCount.setText(String.valueOf(count));
                    android.util.Log.d("HomeFragment", "Hirer active tasks updated: " + count);
                }
            });
        
        // Real-time listener for completed tasks (posted by hirer)
        completedTasksListener = db.collection("tasks")
            .whereEqualTo("hirerId", hirerId)
            .whereEqualTo("status", "completed")
            .addSnapshotListener((queryDocumentSnapshots, e) -> {
                if (e != null) {
                    android.util.Log.w("HomeFragment", "Listen failed for hirer completed tasks", e);
                    return;
                }
                
                if (queryDocumentSnapshots != null && completedTasksCount != null) {
                    int count = queryDocumentSnapshots.size();
                    completedTasksCount.setText(String.valueOf(count));
                    android.util.Log.d("HomeFragment", "Hirer completed tasks updated: " + count);
                }
            });
    }
    
    private void setupWorkerRealTimeListeners(String userName) {
        // Real-time listener for active tasks (assigned to worker)
        activeTasksListener = db.collection("tasks")
            .whereEqualTo("assignedTo", userName)
            .whereIn("status", List.of("in_progress", "pending_payment"))
            .addSnapshotListener((queryDocumentSnapshots, e) -> {
                if (e != null) {
                    android.util.Log.w("HomeFragment", "Listen failed for worker active tasks", e);
                    return;
                }
                
                if (queryDocumentSnapshots != null && activeTasksCount != null) {
                    int count = queryDocumentSnapshots.size();
                    activeTasksCount.setText(String.valueOf(count));
                    android.util.Log.d("HomeFragment", "Worker active tasks updated: " + count);
                }
            });
        
        // Real-time listener for completed tasks (assigned to worker)
        completedTasksListener = db.collection("tasks")
            .whereEqualTo("assignedTo", userName)
            .whereEqualTo("status", "completed")
            .addSnapshotListener((queryDocumentSnapshots, e) -> {
                if (e != null) {
                    android.util.Log.w("HomeFragment", "Listen failed for worker completed tasks", e);
                    return;
                }
                
                if (queryDocumentSnapshots != null && completedTasksCount != null) {
                    int count = queryDocumentSnapshots.size();
                    completedTasksCount.setText(String.valueOf(count));
                    android.util.Log.d("HomeFragment", "Worker completed tasks updated: " + count);
                }
            });
    }
    
    private void setupUserDataListener(String userName, String userType) {
        String collection = "Hirer".equals(userType) ? "hirers" : "users";
        
        // Real-time listener for user points and level
        userDataListener = db.collection(collection)
            .whereEqualTo("name", userName)
            .addSnapshotListener((queryDocumentSnapshots, e) -> {
                if (e != null) {
                    android.util.Log.w("HomeFragment", "Listen failed for user data", e);
                    return;
                }
                
                if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                    DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                    
                    // Update points
                    int points = 0;
                    if (userDoc.contains("points")) {
                        Number pointsNumber = userDoc.getLong("points");
                        points = pointsNumber != null ? pointsNumber.intValue() : 0;
                    }
                    
                    if (pointsCount != null) {
                        pointsCount.setText(String.valueOf(points));
                    }
                    
                    // Update level
                    int level = calculateLevel(points);
                    if (levelCount != null) {
                        levelCount.setText(String.valueOf(level));
                    }
                    
                    // For workers, also sync wallet with total earnings in real-time
                    if (!"Hirer".equals(userType) && userDoc.contains("totalEarnings")) {
                        double totalEarnings = 0;
                        Number earningsNumber = userDoc.getDouble("totalEarnings");
                        totalEarnings = earningsNumber != null ? earningsNumber.doubleValue() : 0;
                        
                        int totalEarningsInt = (int) Math.round(totalEarnings);
                        int currentWalletAmount = walletPrefs.getInt("wallet_amount", 0);
                        
                        if (currentWalletAmount != totalEarningsInt) {
                            android.util.Log.d("HomeFragment", "Real-time wallet sync: " + currentWalletAmount + 
                                    " -> " + totalEarningsInt);
                            walletPrefs.edit().putInt("wallet_amount", totalEarningsInt).apply();
                            updateWalletDisplay(totalEarningsInt);
                        }
                    }
                    
                    android.util.Log.d("HomeFragment", "User data updated - Points: " + points + ", Level: " + level);
                } else {
                    if (pointsCount != null) pointsCount.setText("0");
                    if (levelCount != null) levelCount.setText("1");
                }
            });
    }

    private void loadDashboardData() {
        // Initial load - real-time listeners will handle updates
        // This method is kept for backward compatibility but simplified
        setDefaultValues();
    }

    private void loadHirerDashboardData(String hirerId) {
        // Removed - replaced by real-time listeners
    }

    private void loadWorkerDashboardData(String userId) {
        // Removed - replaced by real-time listeners
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

    private void fetchAndDisplayHomeTasks() {
        String userType = sessionManager.getUserType();
        String userName = sessionManager.getUserName();
        String userId = sessionManager.getUserId();
        if (userType == null || (userName == null && userId == null)) return;

        if ("Hirer".equals(userType)) {
            // Hirer: show all created tasks (grouped by status if needed)
            if (taskListHeader != null) taskListHeader.setText("Created Tasks");
            db.collection("tasks")
                .whereEqualTo("hirerId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    homeTaskList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Task task = doc.toObject(Task.class);
                        if (task != null) homeTaskList.add(task);
                    }
                    homeTaskAdapter.notifyDataSetChanged();
                });
        } else {
            // User: show assigned tasks (in progress, pending payment, completed)
            if (taskListHeader != null) taskListHeader.setText("Your Tasks");
            db.collection("tasks")
                .whereEqualTo("assignedTo", userName)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    homeTaskList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Task task = doc.toObject(Task.class);
                        if (task != null) homeTaskList.add(task);
                    }
                    homeTaskAdapter.notifyDataSetChanged();
                });
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh wallet amount display and sync with total earnings
        syncWalletWithTotalEarnings();
        fetchAndDisplayHomeTasks(); // Refresh task list on resume
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Clean up real-time listeners to prevent memory leaks
        if (activeTasksListener != null) {
            activeTasksListener.remove();
            activeTasksListener = null;
        }
        
        if (completedTasksListener != null) {
            completedTasksListener.remove();
            completedTasksListener = null;
        }
        
        if (userDataListener != null) {
            userDataListener.remove();
            userDataListener = null;
        }
        
        android.util.Log.d("HomeFragment", "Real-time listeners cleaned up");
    }
} 