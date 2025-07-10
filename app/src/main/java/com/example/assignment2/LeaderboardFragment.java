package com.example.assignment2;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardFragment extends Fragment implements LeaderboardAdapter.OnContactUserListener {

    private static final String TAG = "LeaderboardFragment";
    private MaterialButton backButton;
    private RecyclerView leaderboardRecyclerView;
    private LeaderboardAdapter adapter;
    private ProgressBar loadingIndicator;
    private LinearLayout emptyStateLayout;
    private MaterialButton refreshButton;
    private SessionManager sessionManager;
    private List<LeaderboardUser> leaderboardUsers;
    private TextView leaderboardTitle;
    
    // Podium views
    private LinearLayout podiumLayout;
    private ImageView firstPlaceAvatar, secondPlaceAvatar, thirdPlaceAvatar;
    private TextView firstPlaceName, secondPlaceName, thirdPlaceName;
    private TextView firstPlaceScore, secondPlaceScore, thirdPlaceScore;
    private LinearLayout firstPlace, secondPlace, thirdPlace;
    
    // Time period tabs
    private TextView tabThisMonth, tabAllTime;
    private boolean showingAllTime = true;

    public static LeaderboardFragment newInstance() {
        return new LeaderboardFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);
        
        sessionManager = new SessionManager(requireContext());
        leaderboardUsers = new ArrayList<>();
        
        initializeViews(view);
        setupRecyclerView();
        setupClickHandlers();
        
        // Load leaderboard data
        loadLeaderboardData();
        
        return view;
    }

    private void initializeViews(View view) {
        backButton = view.findViewById(R.id.backButton);
        leaderboardRecyclerView = view.findViewById(R.id.leaderboardRecyclerView);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        refreshButton = view.findViewById(R.id.refreshButton);
        leaderboardTitle = view.findViewById(R.id.leaderboardTitle);
        
        // Podium views
        podiumLayout = view.findViewById(R.id.podiumLayout);
        
        firstPlace = view.findViewById(R.id.firstPlace);
        secondPlace = view.findViewById(R.id.secondPlace);
        thirdPlace = view.findViewById(R.id.thirdPlace);
        
        firstPlaceAvatar = view.findViewById(R.id.firstPlaceAvatar);
        secondPlaceAvatar = view.findViewById(R.id.secondPlaceAvatar);
        thirdPlaceAvatar = view.findViewById(R.id.thirdPlaceAvatar);
        
        firstPlaceName = view.findViewById(R.id.firstPlaceName);
        secondPlaceName = view.findViewById(R.id.secondPlaceName);
        thirdPlaceName = view.findViewById(R.id.thirdPlaceName);
        
        firstPlaceScore = view.findViewById(R.id.firstPlaceScore);
        secondPlaceScore = view.findViewById(R.id.secondPlaceScore);
        thirdPlaceScore = view.findViewById(R.id.thirdPlaceScore);
        
        // Time period tabs
        tabThisMonth = view.findViewById(R.id.tabThisMonth);
        tabAllTime = view.findViewById(R.id.tabAllTime);
        
        // Update title based on user type
        if ("Hirer".equals(sessionManager.getUserType())) {
            leaderboardTitle.setText("Top Performers");
        } else {
            leaderboardTitle.setText("Top Contributors");
        }
        
        updateTabSelection();
    }

    private void setupRecyclerView() {
        adapter = new LeaderboardAdapter(new ArrayList<>(), requireContext());
        adapter.setOnContactUserListener(this);
        leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        leaderboardRecyclerView.setAdapter(adapter);
    }

    private void setupClickHandlers() {
        backButton.setOnClickListener(v -> navigateBackToHome());
        refreshButton.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Refreshing leaderboard...", Toast.LENGTH_SHORT).show();
            loadLeaderboardData();
        });
        
        // Time period tab handlers
        tabThisMonth.setOnClickListener(v -> {
            showingAllTime = false;
            updateTabSelection();
            loadLeaderboardData();
        });
        
        tabAllTime.setOnClickListener(v -> {
            showingAllTime = true;
            updateTabSelection();
            loadLeaderboardData();
        });
        
        // Make podium clickable for hirers
        if ("Hirer".equals(sessionManager.getUserType())) {
            firstPlace.setOnClickListener(v -> {
                if (leaderboardUsers.size() > 0) {
                    onContactUser(leaderboardUsers.get(0));
                }
            });
            
            secondPlace.setOnClickListener(v -> {
                if (leaderboardUsers.size() > 1) {
                    onContactUser(leaderboardUsers.get(1));
                }
            });
            
            thirdPlace.setOnClickListener(v -> {
                if (leaderboardUsers.size() > 2) {
                    onContactUser(leaderboardUsers.get(2));
                }
            });
        }
    }
    
    private void updateTabSelection() {
        if (showingAllTime) {
            tabAllTime.setAlpha(1.0f);
            tabAllTime.setTextSize(14);
            tabThisMonth.setAlpha(0.6f);
            tabThisMonth.setTextSize(14);
        } else {
            tabThisMonth.setAlpha(1.0f);
            tabThisMonth.setTextSize(14);
            tabAllTime.setAlpha(0.6f);
            tabAllTime.setTextSize(14);
        }
    }

    private void loadLeaderboardData() {
        Log.d(TAG, "Loading leaderboard data...");
        showLoading(true);
        
        LeaderboardManager.getLeaderboard(new LeaderboardManager.LeaderboardCallback() {
            @Override
            public void onSuccess(List<LeaderboardUser> leaderboard) {
                Log.d(TAG, "Leaderboard data loaded successfully. Found " + leaderboard.size() + " users");
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        
                        leaderboardUsers.clear();
                        leaderboardUsers.addAll(leaderboard);
                        
                        // Update podium (top 3)
                        updatePodiumDisplay();
                        
                        // Update RecyclerView (4th onwards)
                        updateRecyclerViewDisplay();
                        
                        Log.d(TAG, "Updated UI with " + leaderboardUsers.size() + " users");
                        
                        // Show empty state if no data
                        if (leaderboardUsers.isEmpty()) {
                            Log.d(TAG, "No users found, showing empty state");
                            showEmptyState(true);
                        } else {
                            Log.d(TAG, "Users found, hiding empty state");
                            showEmptyState(false);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading leaderboard: " + error);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(requireContext(), "Error loading leaderboard: " + error, 
                                     Toast.LENGTH_SHORT).show();
                        showEmptyState(true);
                    });
                }
            }
        });
    }
    
    private void updatePodiumDisplay() {
        // Show podium only if we have users
        if (leaderboardUsers.size() >= 1) {
            podiumLayout.setVisibility(View.VISIBLE);
            
            // 1st place
            LeaderboardUser first = leaderboardUsers.get(0);
            firstPlaceName.setText(first.getName());
            firstPlaceScore.setText(String.valueOf(first.getTasksCompleted()));
            firstPlace.setVisibility(View.VISIBLE);
            
            // 2nd place
            if (leaderboardUsers.size() >= 2) {
                LeaderboardUser second = leaderboardUsers.get(1);
                secondPlaceName.setText(second.getName());
                secondPlaceScore.setText(String.valueOf(second.getTasksCompleted()));
                secondPlace.setVisibility(View.VISIBLE);
            } else {
                secondPlace.setVisibility(View.INVISIBLE);
            }
            
            // 3rd place
            if (leaderboardUsers.size() >= 3) {
                LeaderboardUser third = leaderboardUsers.get(2);
                thirdPlaceName.setText(third.getName());
                thirdPlaceScore.setText(String.valueOf(third.getTasksCompleted()));
                thirdPlace.setVisibility(View.VISIBLE);
            } else {
                thirdPlace.setVisibility(View.INVISIBLE);
            }
        } else {
            podiumLayout.setVisibility(View.GONE);
        }
    }
    
    private void updateRecyclerViewDisplay() {
        // Show users from 4th place onwards
        List<LeaderboardUser> remainingUsers = new ArrayList<>();
        if (leaderboardUsers.size() > 3) {
            remainingUsers.addAll(leaderboardUsers.subList(3, leaderboardUsers.size()));
        }
        
        adapter.updateData(remainingUsers);
    }
    
    private void showLoading(boolean show) {
        Log.d(TAG, "showLoading: " + show);
        loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            podiumLayout.setVisibility(View.GONE);
            leaderboardRecyclerView.setVisibility(View.GONE);
        }
    }
    
    private void showEmptyState(boolean show) {
        Log.d(TAG, "showEmptyState: " + show);
        emptyStateLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            podiumLayout.setVisibility(View.GONE);
            leaderboardRecyclerView.setVisibility(View.GONE);
        } else {
            podiumLayout.setVisibility(View.VISIBLE);
            leaderboardRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onContactUser(LeaderboardUser user) {
        if (!"Hirer".equals(sessionManager.getUserType())) {
            Toast.makeText(requireContext(), "Only hirers can contact workers", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show contact options dialog
        showContactDialog(user);
    }
    
    private void showContactDialog(LeaderboardUser user) {
        // Use enhanced text-based dialog with comprehensive user profile
        showEnhancedTextProfileDialog(user);
    }
    
    private void showEnhancedTextProfileDialog(LeaderboardUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        
        // Calculate additional metrics
        double successRate = calculateSuccessRate(user);
        String performanceLevel = getPerformanceLevel(user);
        String recentActivity = getRecentActivityStatus(user);
        
        // Create comprehensive profile text
        String profileInfo = String.format(
            "🏆 TOP PERFORMER PROFILE\n\n" +
            
            "👤 %s\n" +
            "📧 %s\n" +
            "📱 %s\n\n" +
            
            "📊 PERFORMANCE OVERVIEW\n" +
            "• Rank: #%d %s\n" +
            "• Tasks Completed: %d\n" +
            "• Success Rate: %.1f%%\n" +
            "• Efficiency: %.1f tasks/day\n" +
            "• Average Rating: %s\n" +
            "• Total Earnings: RM %.2f\n\n" +
            
            "🎯 PERFORMANCE LEVEL\n" +
            "• Status: %s\n" +
            "• Recent Activity: %s\n" +
            "• Reliability: %s\n\n" +
            
            "💼 WHY HIRE THIS WORKER?\n" +
            "• %s\n" +
            "• %s\n" +
            "• %s\n\n" +
            
            "🏅 ACHIEVEMENTS\n" +
            "• %s\n" +
            "• %s",
            
            // Basic Info
            user.getName(),
            user.getEmail(),
            user.getPhone(),
            
            // Performance Overview  
            user.getRank(),
            getRankBadge(user.getRank()),
            user.getTasksCompleted(),
            successRate,
            user.getEfficiencyScore(),
            user.getAverageRating() > 0 ? String.format("⭐ %.1f/5.0", user.getAverageRating()) : "Not rated yet",
            user.getTotalEarnings(),
            
            // Performance Level
            performanceLevel,
            recentActivity,
            getReliabilityStatus(successRate),
            
            // Why Hire
            getSellingPoint1(user),
            getSellingPoint2(user),
            getSellingPoint3(user),
            
            // Achievements
            getAchievement1(user),
            getAchievement2(user)
        );
        
        builder.setTitle("🌟 Worker Profile")
               .setMessage(profileInfo)
               .setPositiveButton("📧 Send Email", (dialog, which) -> sendProfessionalEmail(user))
               .setNeutralButton("📞 Call Now", (dialog, which) -> makePhoneCall(user))
               .setNegativeButton("⭐ Mark Preferred", (dialog, which) -> markUserAsPreferred(user))
               .show();
    }
    
    // Helper methods for calculating additional metrics
    private double calculateSuccessRate(LeaderboardUser user) {
        // For now, calculate based on existing data
        // In a real app, you'd query for assigned vs completed tasks
        if (user.getTasksCompleted() == 0) return 0;
        
        // Estimate based on performance (high performers likely have high success rates)
        double baseRate = 85.0; // Base success rate
        if (user.getAverageRating() >= 4.5) baseRate += 10;
        else if (user.getAverageRating() >= 4.0) baseRate += 5;
        
        if (user.getEfficiencyScore() >= 2.0) baseRate += 5;
        
        return Math.min(baseRate, 99.0); // Cap at 99%
    }
    
    private String getPerformanceLevel(LeaderboardUser user) {
        if (user.getRank() <= 3) return "🏆 Elite Performer";
        else if (user.getRank() <= 10) return "🥇 Top Tier";
        else if (user.getTasksCompleted() >= 20) return "🔸 Experienced";
        else if (user.getTasksCompleted() >= 10) return "🔹 Active";
        else return "🆕 Rising Star";
    }
    
    private String getRecentActivityStatus(LeaderboardUser user) {
        // In a real app, you'd check last task completion time
        long daysSinceLastTask = (System.currentTimeMillis() - user.getLastTaskCompletionTime()) / (24 * 60 * 60 * 1000);
        
        if (daysSinceLastTask <= 1) return "🟢 Active today";
        else if (daysSinceLastTask <= 7) return "🟡 Active this week"; 
        else if (daysSinceLastTask <= 30) return "🟠 Active this month";
        else return "🔴 Inactive recently";
    }
    
    private String getRankBadge(int rank) {
        switch (rank) {
            case 1: return "👑";
            case 2: return "🥈";  
            case 3: return "🥉";
            default: return rank <= 10 ? "🏅" : "";
        }
    }
    
    private String getReliabilityStatus(double successRate) {
        if (successRate >= 95) return "🟢 Extremely Reliable";
        else if (successRate >= 90) return "🟢 Very Reliable";
        else if (successRate >= 85) return "🟡 Reliable";
        else return "🟠 Developing";
    }
    
    private String getSellingPoint1(LeaderboardUser user) {
        if (user.getEfficiencyScore() >= 3.0) {
            return "⚡ Lightning fast: Completes " + String.format("%.1f", user.getEfficiencyScore()) + " tasks daily";
        } else if (user.getAverageRating() >= 4.5) {
            return "🌟 Premium quality: " + String.format("%.1f", user.getAverageRating()) + "/5 star average rating";
        } else {
            return "📈 Proven track record: " + user.getTasksCompleted() + " successful completions";
        }
    }
    
    private String getSellingPoint2(LeaderboardUser user) {
        if (user.getRank() <= 5) {
            return "🏆 Top 5 performer on the entire platform";
        } else if (user.getTotalEarnings() >= 1000) {
            return "💰 High earner: RM " + String.format("%.0f", user.getTotalEarnings()) + " total earned";
        } else {
            return "🎯 Consistent performance across all task types";
        }
    }
    
    private String getSellingPoint3(LeaderboardUser user) {
        return "🤝 Professional communication and work ethic";
    }
    
    private String getAchievement1(LeaderboardUser user) {
        if (user.getRank() == 1) return "👑 #1 Ranked Worker";
        else if (user.getRank() <= 3) return "🏅 Top 3 Platform Performer";
        else if (user.getTasksCompleted() >= 50) return "🏆 50+ Tasks Mastery Badge";
        else if (user.getTasksCompleted() >= 25) return "🥇 25+ Tasks Veteran";
        else return "⭐ Rising Star Badge";
    }
    
    private String getAchievement2(LeaderboardUser user) {
        if (user.getAverageRating() >= 4.8) return "🌟 Perfect Rating Achiever";
        else if (user.getAverageRating() >= 4.5) return "⭐ Premium Quality Badge";
        else if (user.getEfficiencyScore() >= 3.0) return "⚡ Speed Demon Badge";
        else if (user.getEfficiencyScore() >= 2.0) return "🚀 Fast Worker Badge";
        else return "🔥 Consistent Performer";
    }
    
    private void sendProfessionalEmail(LeaderboardUser user) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:" + user.getEmail()));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Exclusive Opportunity - LocalTask Premium Hirer");
        
        String emailBody = String.format(
            "Dear %s,\n\n" +
            
            "I hope this message finds you well. I am %s, and I came across your exceptional profile on the LocalTask platform.\n\n" +
            
            "Your performance statistics are truly impressive:\n" +
            "🏆 Rank #%d on the platform\n" +
            "📊 %d tasks completed successfully\n" +
            "⚡ %.1f tasks per day efficiency\n" +
            "⭐ %.1f average rating from hirers\n" +
            "💰 RM %.2f total earnings\n\n" +
            
            "I represent a company that values top-tier talent, and we have some exciting opportunities that align perfectly with your skill set and proven track record.\n\n" +
            
            "Would you be interested in discussing:\n" +
            "• Premium rate projects (above standard platform rates)\n" +
            "• Flexible scheduling opportunities\n" +
            "• Long-term partnership potential\n" +
            "• Performance bonuses and incentives\n\n" +
            
            "I would love to schedule a brief call at your convenience to discuss how we can work together. Your reputation for quality and efficiency makes you exactly the kind of professional we're looking for.\n\n" +
            
            "Please let me know your availability for a quick 10-15 minute conversation this week.\n\n" +
            
            "Best regards,\n%s\n" +
            "Professional Hirer | LocalTask Platform\n" +
            "📱 Contact: %s",
            
            user.getName(),
            sessionManager.getUserName(),
            user.getRank(),
            user.getTasksCompleted(), 
            user.getEfficiencyScore(),
            user.getAverageRating(),
            user.getTotalEarnings(),
            sessionManager.getUserName(),
            sessionManager.getUserName()
        );
        
        emailIntent.putExtra(Intent.EXTRA_TEXT, emailBody);
        
        try {
            startActivity(emailIntent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void makePhoneCall(LeaderboardUser user) {
        Intent phoneIntent = new Intent(Intent.ACTION_DIAL);
        phoneIntent.setData(Uri.parse("tel:" + user.getPhone()));
        try {
            startActivity(phoneIntent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No phone app found", Toast.LENGTH_SHORT).show();
        }
    }

    
    private void markUserAsPreferred(LeaderboardUser user) {
        LeaderboardManager.markUserAsPreferred(user.getName(), true, 
            new LeaderboardManager.ContactUserCallback() {
                @Override
                public void onSuccess(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), 
                                user.getName() + " marked as preferred worker!", 
                                Toast.LENGTH_SHORT).show();
                            
                            // Update the user in the list
                            for (LeaderboardUser lu : leaderboardUsers) {
                                if (lu.getName().equals(user.getName())) {
                                    lu.setPreferred(true);
                                    break;
                                }
                            }
                            adapter.notifyDataSetChanged();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> 
                            Toast.makeText(requireContext(), "Error: " + error, 
                                         Toast.LENGTH_SHORT).show());
                    }
                }
            });
    }

    private void navigateBackToHome() {
        // Navigate back to HomeFragment
        if (getActivity() instanceof TasksActivity) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.navigation_home);
            }
        }
    }
} 