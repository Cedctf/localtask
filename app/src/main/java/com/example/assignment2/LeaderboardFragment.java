package com.example.assignment2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

public class LeaderboardFragment extends Fragment {

    public static LeaderboardFragment newInstance() {
        return new LeaderboardFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        TextView textView = new TextView(getContext());
        textView.setText("Leaderboard Coming Soon!");
        textView.setTextSize(20);
        textView.setPadding(16, 16, 16, 16);
        return textView;
    }
} 