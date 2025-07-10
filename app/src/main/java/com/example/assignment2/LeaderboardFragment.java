package com.example.assignment2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class LeaderboardFragment extends Fragment {

    private MaterialButton backButton;

    public static LeaderboardFragment newInstance() {
        return new LeaderboardFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);
        
        initializeViews(view);
        setupClickHandlers();
        
        return view;
    }

    private void initializeViews(View view) {
        backButton = view.findViewById(R.id.backButton);
    }

    private void setupClickHandlers() {
        backButton.setOnClickListener(v -> navigateBackToHome());
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