package com.example.assignment2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BadgesFragment extends Fragment {

    private SessionManager sessionManager;
    private FirebaseFirestore db;
    private SharedPreferences badgePrefs;
    
    // Wallet components (adapted from HomeFragment)
    private TextView walletAmountText;
    private Button cashOutButton;
    
    // Daily activities components
    private TextView dailyStreakCount, timeSpentCount;
    
    // Badge components - using existing views
    private ProgressBar reflectiveDetectiveProgressBar;
    private Button reflectiveDetectiveRedeemButton;
    
    private ProgressBar checkinChampProgressBar;
    private Button checkinChampRedeemButton;
    
    private ProgressBar ambassadorProgressBar;
    private Button ambassadorRedeemButton;
    
    private ProgressBar activityAchieverProgressBar;
    private Button activityAchieverRedeemButton;

    public static BadgesFragment newInstance() {
        return new BadgesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_badges, container, false);

        sessionManager = new SessionManager(requireContext());
        db = FirebaseFirestore.getInstance();
        badgePrefs = requireActivity().getSharedPreferences("badges_prefs", 0);

        initializeViews(view);
        setupWalletFunctionality();
        loadDailyActivities();
        setupBadges();

        return view;
    }

    private void initializeViews(View view) {
        // Wallet components (adapted from HomeFragment)
        walletAmountText = view.findViewById(R.id.text_wallet_amount);
        cashOutButton = view.findViewById(R.id.button_redeem);
        
        // Daily activities views (these don't exist in current layout)
        dailyStreakCount = null;
        timeSpentCount = null;
        
        // Badge views (using existing ones from the layout)
        reflectiveDetectiveProgressBar = view.findViewById(R.id.progress_beginner_tasks);
        reflectiveDetectiveRedeemButton = view.findViewById(R.id.button_beginner_redeem);
        
        checkinChampProgressBar = view.findViewById(R.id.progress_employer_praise);
        checkinChampRedeemButton = view.findViewById(R.id.button_employer_praise_redeem);
        
        ambassadorProgressBar = view.findViewById(R.id.progress_consistent_performer);
        ambassadorRedeemButton = view.findViewById(R.id.button_consistent_performer_redeem);
        
        // Activity achiever uses existing components since specific ones don't exist
        activityAchieverProgressBar = null;
        activityAchieverRedeemButton = null;
    }

    private void setupWalletFunctionality() {
        // Load and display wallet amount (adapted from HomeFragment)
        loadWalletAmount();
        
        // Make wallet amount clickable to add cash (demo feature)
        if (walletAmountText != null) {
            walletAmountText.setOnClickListener(v -> {
                addCashToWallet(50); // Add RM50 for demo
                Toast.makeText(requireContext(), "RM50 added to wallet! (Tap wallet amount to add more)", Toast.LENGTH_SHORT).show();
            });
        }
        
        // Cash Out button (using existing button_redeem as "Withdraw to Bank")
        if (cashOutButton != null) {
            cashOutButton.setOnClickListener(v -> {
                cashOutWallet();
            });
        }
    }
    
    private void loadWalletAmount() {
        int walletAmount = badgePrefs.getInt("wallet_amount", 0);
        updateWalletDisplay(walletAmount);
    }
    
    private void addCashToWallet(int amount) {
        int currentAmount = badgePrefs.getInt("wallet_amount", 0);
        int newAmount = currentAmount + amount;
        
        badgePrefs.edit().putInt("wallet_amount", newAmount).apply();
        updateWalletDisplay(newAmount);
    }
    
    private void cashOutWallet() {
        int currentAmount = badgePrefs.getInt("wallet_amount", 0);
        
        if (currentAmount > 0) {
            // Reset wallet to 0
            badgePrefs.edit().putInt("wallet_amount", 0).apply();
            updateWalletDisplay(0);
            
            Toast.makeText(requireContext(), 
                "RM" + currentAmount + " transferred to your bank account!", 
                Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(requireContext(), 
                "No funds available to cash out", 
                Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateWalletDisplay(int amount) {
        if (walletAmountText != null) {
            walletAmountText.setText("RM" + amount);
        }
    }

    private void loadDailyActivities() {
        // Load and display daily streak
        updateDailyStreak();
        
        // Load and display time spent today
        updateTimeSpent();
    }

    private void updateDailyStreak() {
        // Check if user logged in today
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastLoginDate = badgePrefs.getString("last_login_date", "");
        
        int streak = badgePrefs.getInt("daily_streak", 0);
        
        if (!today.equals(lastLoginDate)) {
            // New day login
            String yesterday = getYesterday();
            if (yesterday.equals(lastLoginDate)) {
                // Consecutive day login
                streak++;
            } else {
                // Streak broken, reset to 1
                streak = 1;
            }
            
            badgePrefs.edit()
                .putString("last_login_date", today)
                .putInt("daily_streak", streak)
                .apply();
        }
        
        // Update UI if view exists
        if (dailyStreakCount != null) {
            dailyStreakCount.setText(String.valueOf(streak));
        }
    }

    private void updateTimeSpent() {
        // Simple time tracking - increment when fragment is viewed
        int todayMinutes = badgePrefs.getInt("today_minutes", 0);
        
        // Add some time for this session (simplified)
        todayMinutes += 1; // Add 1 minute per view
        badgePrefs.edit().putInt("today_minutes", todayMinutes).apply();
        
        // Update UI if view exists
        if (timeSpentCount != null) {
            timeSpentCount.setText(todayMinutes + "m");
        }
    }

    private String getYesterday() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
    }

    private void setupBadges() {
        setupReflectiveDetectiveBadge();
        setupCheckinChampBadge();
        setupAmbassadorBadge();
        setupActivityAchieverBadge();
    }

    private void setupReflectiveDetectiveBadge() {
        // Beginner Badge - Complete first task (mapped to beginner badge)
        int reflections = badgePrefs.getInt("reflections_completed", 3); // Default to completed
        int maxReflections = 3;
        
        boolean claimed = badgePrefs.getBoolean("reflective_detective_claimed", false);
        
        if (claimed) {
            reflectiveDetectiveProgressBar.setProgress(100);
            reflectiveDetectiveRedeemButton.setEnabled(false);
            reflectiveDetectiveRedeemButton.setText("Claimed");
        } else {
            int progress = (reflections * 100) / maxReflections;
            reflectiveDetectiveProgressBar.setProgress(progress);
            reflectiveDetectiveRedeemButton.setEnabled(reflections >= maxReflections);
            reflectiveDetectiveRedeemButton.setText(reflections >= maxReflections ? "Redeem RM20" : "In Progress");
        }
        
        reflectiveDetectiveRedeemButton.setOnClickListener(v -> {
            if (reflections >= maxReflections && !claimed) {
                redeemBadge("reflective_detective_claimed", "Beginner Badge", 20,
                    reflectiveDetectiveProgressBar, reflectiveDetectiveRedeemButton);
            }
        });
    }

    private void setupCheckinChampBadge() {
        // Employer Praise - Receive excellent feedback (mapped to employer praise badge)
        int checkins = badgePrefs.getInt("checkins_completed", 1); // Default to completed
        int maxCheckins = 1;
        
        boolean claimed = badgePrefs.getBoolean("checkin_champ_claimed", false);
        
        if (claimed) {
            checkinChampProgressBar.setProgress(100);
            checkinChampRedeemButton.setEnabled(false);
            checkinChampRedeemButton.setText("Claimed");
        } else {
            int progress = (checkins * 100) / maxCheckins;
            checkinChampProgressBar.setProgress(progress);
            checkinChampRedeemButton.setEnabled(checkins >= maxCheckins);
            checkinChampRedeemButton.setText(checkins >= maxCheckins ? "Redeem RM20" : "In Progress");
        }
        
        checkinChampRedeemButton.setOnClickListener(v -> {
            if (checkins >= maxCheckins && !claimed) {
                redeemBadge("checkin_champ_claimed", "Employer Praise", 20,
                    checkinChampProgressBar, checkinChampRedeemButton);
            }
        });
    }

    private void setupAmbassadorBadge() {
        // Consistent Performer - Complete 5 tasks consistently (mapped to consistent performer badge)
        int invites = badgePrefs.getInt("friends_invited", 1); // Default to completed
        int maxInvites = 1;
        
        boolean claimed = badgePrefs.getBoolean("ambassador_claimed", false);
        
        if (claimed) {
            ambassadorProgressBar.setProgress(100);
            ambassadorRedeemButton.setEnabled(false);
            ambassadorRedeemButton.setText("Claimed");
        } else {
            int progress = (invites * 100) / maxInvites;
            ambassadorProgressBar.setProgress(progress);
            ambassadorRedeemButton.setEnabled(invites >= maxInvites);
            ambassadorRedeemButton.setText(invites >= maxInvites ? "Redeem RM20" : "In Progress");
        }
        
        ambassadorRedeemButton.setOnClickListener(v -> {
            if (invites >= maxInvites && !claimed) {
                redeemBadge("ambassador_claimed", "Consistent Performer", 20,
                    ambassadorProgressBar, ambassadorRedeemButton);
            }
        });
    }

    private void setupActivityAchieverBadge() {
        // Activity Achiever - Complete activities
        // We'll track this through task completions
        loadTaskCompletions();
    }

    private void loadTaskCompletions() {
        String userName = sessionManager.getUserName();
        if (userName != null) {
            db.collection("tasks")
                .whereEqualTo("assignedTo", userName)
                .whereEqualTo("status", "completed")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int completedTasks = queryDocumentSnapshots.size();
                    badgePrefs.edit().putInt("activities_completed", completedTasks).apply();
                    updateActivityAchieverBadge(completedTasks);
                })
                .addOnFailureListener(e -> {
                    // Use cached value
                    int completedTasks = badgePrefs.getInt("activities_completed", 6);
                    updateActivityAchieverBadge(completedTasks);
                });
        } else {
            // Default to 6/10 as shown in image
            updateActivityAchieverBadge(6);
        }
    }

    private void updateActivityAchieverBadge(int activities) {
        // Activity Achiever badge - track progress but no UI updates since no dedicated components
        int maxActivities = 10;
        
        // Just store the progress for future use when UI components are added
        badgePrefs.edit()
            .putInt("activities_completed", activities)
            .putInt("activities_max", maxActivities)
            .apply();
        
        // Show achievement toast when completed
        boolean claimed = badgePrefs.getBoolean("activity_achiever_claimed", false);
        boolean wasAlreadyEligible = badgePrefs.getBoolean("activity_achiever_eligible_shown", false);
        
        if (!claimed && activities >= maxActivities && !wasAlreadyEligible) {
            Toast.makeText(getContext(), 
                "🎯 Activity Achiever badge ready! Redeem for RM20 (" + activities + "/" + maxActivities + ")",
                Toast.LENGTH_LONG).show();
            
            badgePrefs.edit()
                .putBoolean("activity_achiever_eligible_shown", true)
                .apply();
        }
    }

    private void redeemBadge(String claimedKey, String badgeName, int reward, ProgressBar progressBar, Button redeemButton) {
        // Add reward to wallet (wallet functionality restored)
        addCashToWallet(reward);
        
        // Update badge state to claimed
        progressBar.setProgress(100);
        redeemButton.setEnabled(false);
        redeemButton.setText("Claimed");
        
        // Save claimed state
        badgePrefs.edit()
            .putBoolean(claimedKey, true)
            .apply();
        
        // Show achievement message with wallet reward
        Toast.makeText(getContext(), 
            "🎉 " + badgeName + " badge earned! RM" + reward + " added to wallet!", 
            Toast.LENGTH_LONG).show();
    }

    private int calculateLevel(int current, int max) {
        if (current == 0) return 0;
        if (current >= max) return 2;
        return 1;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh wallet amount and daily activities when returning to this fragment
        loadWalletAmount();
        loadDailyActivities();
    }
} 