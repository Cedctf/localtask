package com.example.assignment2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

public class BadgesFragment extends Fragment {

    public static BadgesFragment newInstance() {
        return new BadgesFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        TextView textView = new TextView(getContext());
        textView.setText("Badges Coming Soon!");
        textView.setTextSize(20);
        textView.setPadding(16, 16, 16, 16);
        return textView;
    }
} 