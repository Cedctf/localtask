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
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BadgesFragment extends Fragment {

    private int walletAmount = 0; // Store the wallet amount
    private SessionManager sessionManager;
    private FirebaseFirestore db;
    private SharedPreferences badgePrefs;
    
    // Badge components - using existing views
    private ProgressBar reflectiveDetectiveProgressBar;
    private Button reflectiveDetectiveRedeemButton;
    
    private ProgressBar checkinChampProgressBar;
    private Button checkinChampRedeemButton;
    
    private ProgressBar ambassadorProgressBar;
    private Button ambassadorRedeemButton;

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
        setupWalletFunctionality(view);
        loadDailyActivities();
        setupBadges();

        return view;
    }

    private void initializeViews(View view) {
        // Badge views (using existing ones from the layout)
        reflectiveDetectiveProgressBar = view.findViewById(R.id.progress_beginner_tasks);
        reflectiveDetectiveRedeemButton = view.findViewById(R.id.button_beginner_redeem);
        
        checkinChampProgressBar = view.findViewById(R.id.progress_employer_praise);
        checkinChampRedeemButton = view.findViewById(R.id.button_employer_praise_redeem);
        
        ambassadorProgressBar = view.findViewById(R.id.progress_consistent_performer);
        ambassadorRedeemButton = view.findViewById(R.id.button_consistent_performer_redeem);
    }

    private void setupWalletFunctionality(View view) {
        TextView walletAmountText = view.findViewById(R.id.text_wallet_amount);
        Button withdrawButton = view.findViewById(R.id.button_redeem);

        // Load saved wallet amount
        walletAmount = badgePrefs.getInt("wallet_amount", 0);
        walletAmountText.setText("RM" + walletAmount);

        withdrawButton.setOnClickListener(v -> {
            walletAmount = 0;
            walletAmountText.setText("RM" + walletAmount);
            badgePrefs.edit().putInt("wallet_amount", walletAmount).apply();
            Toast.makeText(getContext(), "The money transfer to bank already.", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadDailyActivities() {
        // Load daily streak
        updateDailyStreak();
        
        // Load time spent today
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
    }

    private void updateTimeSpent() {
        // Simple time tracking - increment when fragment is viewed
        int todayMinutes = badgePrefs.getInt("today_minutes", 0);
        
        // Add some time for this session (simplified)
        todayMinutes += 1; // Add 1 minute per view
        badgePrefs.edit().putInt("today_minutes", todayMinutes).apply();
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
        // Reflective Detective - Complete reflections (mapped to beginner badge)
        int reflections = badgePrefs.getInt("reflections_completed", 2); // Default to 2/3 progress
        int maxReflections = 3;
        
        boolean claimed = badgePrefs.getBoolean("reflective_detective_claimed", false);
        
        if (claimed) {
            reflectiveDetectiveProgressBar.setProgress(0);
            reflectiveDetectiveRedeemButton.setEnabled(false);
            reflectiveDetectiveRedeemButton.setText("Claimed");
        } else {
            int progress = (reflections * 100) / maxReflections;
            reflectiveDetectiveProgressBar.setProgress(progress);
            reflectiveDetectiveRedeemButton.setEnabled(reflections >= maxReflections);
            reflectiveDetectiveRedeemButton.setText(reflections >= maxReflections ? "Redeem" : "In Progress");
        }
        
        reflectiveDetectiveRedeemButton.setOnClickListener(v -> {
            if (reflections >= maxReflections && !claimed) {
                redeemBadge("reflective_detective_claimed", 25, reflectiveDetectiveProgressBar, reflectiveDetectiveRedeemButton);
            }
        });
    }

    private void setupCheckinChampBadge() {
        // Check-in Champ - Complete check-ins (mapped to employer praise badge)
        int checkins = badgePrefs.getInt("checkins_completed", 0); // Default to 0/1 progress
        int maxCheckins = 1;
        
        boolean claimed = badgePrefs.getBoolean("checkin_champ_claimed", false);
        
        if (claimed) {
            checkinChampProgressBar.setProgress(0);
            checkinChampRedeemButton.setEnabled(false);
            checkinChampRedeemButton.setText("Claimed");
        } else {
            int progress = (checkins * 100) / maxCheckins;
            checkinChampProgressBar.setProgress(progress);
            checkinChampRedeemButton.setEnabled(checkins >= maxCheckins);
            checkinChampRedeemButton.setText(checkins >= maxCheckins ? "Redeem" : "In Progress");
        }
        
        checkinChampRedeemButton.setOnClickListener(v -> {
            if (checkins >= maxCheckins && !claimed) {
                redeemBadge("checkin_champ_claimed", 15, checkinChampProgressBar, checkinChampRedeemButton);
            }
        });
    }

    private void setupAmbassadorBadge() {
        // Ambassador - Invite friends (mapped to consistent performer badge)
        int invites = badgePrefs.getInt("friends_invited", 0); // Default to 0/1 progress
        int maxInvites = 1;
        
        boolean claimed = badgePrefs.getBoolean("ambassador_claimed", false);
        
        if (claimed) {
            ambassadorProgressBar.setProgress(0);
            ambassadorRedeemButton.setEnabled(false);
            ambassadorRedeemButton.setText("Claimed");
        } else {
            int progress = (invites * 100) / maxInvites;
            ambassadorProgressBar.setProgress(progress);
            ambassadorRedeemButton.setEnabled(invites >= maxInvites);
            ambassadorRedeemButton.setText(invites >= maxInvites ? "Redeem" : "In Progress");
        }
        
        ambassadorRedeemButton.setOnClickListener(v -> {
            if (invites >= maxInvites && !claimed) {
                redeemBadge("ambassador_claimed", 30, ambassadorProgressBar, ambassadorRedeemButton);
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
        int maxActivities = 10;
        
        boolean claimed = badgePrefs.getBoolean("activity_achiever_claimed", false);
        
        if (claimed) {
            // Already claimed - show as completed
            if (reflectiveDetectiveProgressBar != null) {
                reflectiveDetectiveProgressBar.setProgress(100);
            }
        } else {
            int progress = (activities * 100) / maxActivities;
            if (reflectiveDetectiveProgressBar != null) {
                reflectiveDetectiveProgressBar.setProgress(progress);
            }
        }
    }

    private void redeemBadge(String claimedKey, int reward, ProgressBar progressBar, Button redeemButton) {
        walletAmount += reward;
        
        // Update wallet display
        TextView walletAmountText = getView().findViewById(R.id.text_wallet_amount);
        walletAmountText.setText("RM" + walletAmount);
        
        // Update badge state
        progressBar.setProgress(0);
        redeemButton.setEnabled(false);
        redeemButton.setText("Claimed");
        
        // Save state
        badgePrefs.edit()
            .putBoolean(claimedKey, true)
            .putInt("wallet_amount", walletAmount)
            .apply();
        
        Toast.makeText(getContext(), "Badge redeemed! RM" + reward + " added to wallet.", Toast.LENGTH_SHORT).show();
    }

    private int calculateLevel(int current, int max) {
        if (current == 0) return 0;
        if (current >= max) return 2;
        return 1;
    }
} 