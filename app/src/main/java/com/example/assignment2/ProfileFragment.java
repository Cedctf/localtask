package com.example.assignment2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {
    private static final String ARG_USER_NAME = "userName";
    private static final String ARG_USER_TYPE = "userType";
    private static final String ARG_USER_EMAIL = "userEmail";

    private String userName;
    private String userType;
    private String userEmail;

    public static ProfileFragment newInstance(String userName, String userType, String userEmail) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_NAME, userName);
        args.putString(ARG_USER_TYPE, userType);
        args.putString(ARG_USER_EMAIL, userEmail);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userName = getArguments().getString(ARG_USER_NAME);
            userType = getArguments().getString(ARG_USER_TYPE);
            userEmail = getArguments().getString(ARG_USER_EMAIL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        TextView nameText = view.findViewById(R.id.profileName);
        TextView typeText = view.findViewById(R.id.profileType);
        TextView emailText = view.findViewById(R.id.profileEmail);

        nameText.setText("Name: " + userName);
        typeText.setText("Type: " + userType);
        emailText.setText("Email: " + userEmail);

        return view;
    }
} 