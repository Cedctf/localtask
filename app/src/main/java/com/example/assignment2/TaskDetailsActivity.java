package com.example.assignment2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.NumberFormat;
import java.util.Locale;

public class TaskDetailsActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private TextView titleText, descriptionText, paymentText, statusText, hirerText;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);

        // Initialize Firestore and SessionManager
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Initialize views
        titleText = findViewById(R.id.taskDetailTitle);
        descriptionText = findViewById(R.id.taskDetailDescription);
        paymentText = findViewById(R.id.taskDetailPayment);
        statusText = findViewById(R.id.taskDetailStatus);
        hirerText = findViewById(R.id.taskDetailHirer);

        // Setup bottom navigation
        setupBottomNavigation();

        // Get task ID from intent
        String taskId = getIntent().getStringExtra("taskId");
        if (taskId != null) {
            loadTaskDetails(taskId);
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        
        // Clear any selected items so none are highlighted
        bottomNav.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomNav.getMenu().size(); i++) {
            bottomNav.getMenu().getItem(i).setChecked(false);
        }
        bottomNav.getMenu().setGroupCheckable(0, true, true);
        
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                // Go back to TasksActivity (home)
                Intent intent = new Intent(TaskDetailsActivity.this, TasksActivity.class);
                intent.putExtra("USER_NAME", sessionManager.getUserName());
                intent.putExtra("USER_TYPE", sessionManager.getUserType());
                intent.putExtra("USER_EMAIL", sessionManager.getUserEmail());
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.navigation_leaderboard) {
                // Navigate to TasksActivity and show leaderboard
                navigateToTasksActivity("leaderboard");
                return true;
            } else if (itemId == R.id.navigation_badges) {
                // Navigate to TasksActivity and show badges
                navigateToTasksActivity("badges");
                return true;
            } else if (itemId == R.id.navigation_profile) {
                // Navigate to TasksActivity and show profile
                navigateToTasksActivity("profile");
                return true;
            }
            return false;
        });
    }

    private void navigateToTasksActivity(String fragment) {
        Intent intent = new Intent(TaskDetailsActivity.this, TasksActivity.class);
        intent.putExtra("USER_NAME", sessionManager.getUserName());
        intent.putExtra("USER_TYPE", sessionManager.getUserType());
        intent.putExtra("USER_EMAIL", sessionManager.getUserEmail());
        intent.putExtra("SHOW_FRAGMENT", fragment);
        startActivity(intent);
        finish();
    }

    private void loadTaskDetails(String taskId) {
        db.collection("tasks").document(taskId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Task task = documentSnapshot.toObject(Task.class);
                    if (task != null) {
                        titleText.setText(task.getTitle());
                        descriptionText.setText(task.getDescription());
                        
                        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
                        paymentText.setText(format.format(task.getPayment()));
                        
                        statusText.setText("Status: " + task.getStatus());
                        hirerText.setText("Posted by: " + task.getHirerName());
                    }
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Error loading task details: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show());
    }
} 