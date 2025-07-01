package com.example.assignment2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {
    private static final String ARG_USER_NAME = "userName";
    private static final String ARG_USER_TYPE = "userType";
    private static final String ARG_USER_EMAIL = "userEmail";

    private String userName;
    private String userType;
    private String userEmail;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

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
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Set user information
        TextView nameText = view.findViewById(R.id.profileName);
        TextView typeText = view.findViewById(R.id.profileType);
        TextView emailText = view.findViewById(R.id.profileEmail);

        nameText.setText(userName);
        typeText.setText(userType);
        emailText.setText(userEmail);

        // Load statistics
        loadUserStatistics(view);

        // Setup buttons
        Button editProfileButton = view.findViewById(R.id.btnEditProfile);
        Button logoutButton = view.findViewById(R.id.btnLogout);

        editProfileButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Edit Profile coming soon!", Toast.LENGTH_SHORT).show();
        });

        logoutButton.setOnClickListener(v -> logout());

        return view;
    }

    private void loadUserStatistics(View view) {
        TextView tasksCompletedText = view.findViewById(R.id.tasksCompleted);
        TextView totalPointsText = view.findViewById(R.id.totalPoints);
        TextView badgesEarnedText = view.findViewById(R.id.badgesEarned);

        // Load tasks completed
        db.collection("tasks")
            .whereEqualTo("assignedTo", userName)
            .whereEqualTo("status", "completed")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                tasksCompletedText.setText(String.valueOf(queryDocumentSnapshots.size()));
            });

        // Load total points
        db.collection("users")
            .whereEqualTo("name", userName)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    int points = queryDocumentSnapshots.getDocuments().get(0).getLong("points") != null ?
                            queryDocumentSnapshots.getDocuments().get(0).getLong("points").intValue() : 0;
                    totalPointsText.setText(String.valueOf(points));
                }
            });

        // Load badges earned
        db.collection("user_badges")
            .whereEqualTo("userName", userName)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                badgesEarnedText.setText(String.valueOf(queryDocumentSnapshots.size()));
            });
    }

    private void logout() {
        // Clear session
        sessionManager.logoutUser();

        // Navigate to login activity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
} 