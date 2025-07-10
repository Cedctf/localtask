package com.example.assignment2;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder> {
    
    private List<LeaderboardUser> users;
    private Context context;
    private SessionManager sessionManager;
    
    public interface OnContactUserListener {
        void onContactUser(LeaderboardUser user);
    }
    
    private OnContactUserListener contactListener;

    public LeaderboardAdapter(List<LeaderboardUser> users, Context context) {
        this.users = users;
        this.context = context;
        this.sessionManager = new SessionManager(context);
    }
    
    public void setOnContactUserListener(OnContactUserListener listener) {
        this.contactListener = listener;
    }

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard_user, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        LeaderboardUser user = users.get(position);
        
        // Set rank (starting from 4 since top 3 are in podium)
        int actualRank = user.getRank();
        holder.rankText.setText(String.valueOf(actualRank));
        
        // Set user information
        holder.userName.setText(user.getName());
        
        // Set score (tasks completed)
        holder.userScore.setText(String.valueOf(user.getTasksCompleted()));
        
        // Show performance badges for hirers
        boolean isHirer = "Hirer".equals(sessionManager.getUserType());
        if (isHirer && user.getTasksCompleted() > 0) {
            holder.performanceBadges.setVisibility(View.VISIBLE);
            
            // Efficiency badge
            if (user.getEfficiencyScore() >= 2.0) {
                holder.efficiencyBadge.setText("⚡ Fast");
                holder.efficiencyBadge.setVisibility(View.VISIBLE);
            } else if (user.getEfficiencyScore() >= 1.0) {
                holder.efficiencyBadge.setText("🚀 Active");
                holder.efficiencyBadge.setVisibility(View.VISIBLE);
            } else {
                holder.efficiencyBadge.setVisibility(View.GONE);
            }
            
            // Rating badge
            if (user.getAverageRating() >= 4.5) {
                holder.ratingBadge.setText("⭐ " + String.format("%.1f", user.getAverageRating()));
                holder.ratingBadge.setVisibility(View.VISIBLE);
            } else if (user.getAverageRating() >= 4.0) {
                holder.ratingBadge.setText("⭐ " + String.format("%.1f", user.getAverageRating()));
                holder.ratingBadge.setVisibility(View.VISIBLE);
            } else {
                holder.ratingBadge.setVisibility(View.GONE);
            }
        } else {
            holder.performanceBadges.setVisibility(View.GONE);
        }
        
        // Contact button - only show for hirers
        if (isHirer) {
            holder.contactButton.setVisibility(View.VISIBLE);
            holder.contactButton.setOnClickListener(v -> {
                if (contactListener != null) {
                    contactListener.onContactUser(user);
                } else {
                    // Default contact behavior
                    showContactOptions(user);
                }
            });
            
            // Show preferred indicator
            if (user.isPreferred()) {
                holder.contactButton.setText("⭐ Call");
                holder.contactButton.setBackgroundResource(R.drawable.button_primary);
            } else {
                holder.contactButton.setText("Contact");
                holder.contactButton.setBackgroundResource(R.drawable.button_outline);
            }
        } else {
            holder.contactButton.setVisibility(View.GONE);
        }
        
        // Set avatar background based on rank
        if (actualRank <= 10) {
            holder.userAvatar.setBackgroundResource(R.drawable.circle_background_light_green);
        } else if (actualRank <= 20) {
            holder.userAvatar.setBackgroundResource(R.drawable.circle_background_light_blue);
        } else {
            holder.userAvatar.setBackgroundResource(R.drawable.circle_background_light_grey);
        }
        
        // Set rank circle color based on rank
        if (actualRank <= 10) {
            holder.rankText.setBackgroundResource(R.drawable.circle_background_light_green);
        } else if (actualRank <= 20) {
            holder.rankText.setBackgroundResource(R.drawable.circle_background_light_blue);
        } else {
            holder.rankText.setBackgroundResource(R.drawable.circle_background_light_grey);
        }
    }
    
    private void showContactOptions(LeaderboardUser user) {
        // Create intent chooser for contacting user
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:" + user.getEmail()));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Task Opportunity - LocalTask Platform");
        emailIntent.putExtra(Intent.EXTRA_TEXT, 
            "Hi " + user.getName() + ",\n\n" +
            "We noticed your excellent performance on the LocalTask platform (" + 
            user.getTasksCompleted() + " tasks completed). " +
            "We have some exciting opportunities that might interest you.\n\n" +
            "Best regards,\n" + sessionManager.getUserName());
        
        Intent phoneIntent = new Intent(Intent.ACTION_DIAL);
        phoneIntent.setData(Uri.parse("tel:" + user.getPhone()));
        
        // Show options
        try {
            context.startActivity(Intent.createChooser(emailIntent, "Contact " + user.getName()));
        } catch (Exception e) {
            Toast.makeText(context, "Email: " + user.getEmail() + "\nPhone: " + user.getPhone(), 
                         Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }
    
    public void updateData(List<LeaderboardUser> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        TextView rankText;
        ImageView userAvatar;
        TextView userName;
        TextView userScore;
        LinearLayout performanceBadges;
        TextView efficiencyBadge;
        TextView ratingBadge;
        Button contactButton;

        public LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.rankText);
            userAvatar = itemView.findViewById(R.id.userAvatar);
            userName = itemView.findViewById(R.id.userName);
            userScore = itemView.findViewById(R.id.userScore);
            performanceBadges = itemView.findViewById(R.id.performanceBadges);
            efficiencyBadge = itemView.findViewById(R.id.efficiencyBadge);
            ratingBadge = itemView.findViewById(R.id.ratingBadge);
            contactButton = itemView.findViewById(R.id.contactButton);
        }
    }
} 