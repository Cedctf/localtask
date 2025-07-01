package com.example.assignment2;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.NumberFormat;
import java.util.Locale;

public class TaskDetailsActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private TextView titleText, descriptionText, paymentText, statusText, hirerText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        titleText = findViewById(R.id.taskDetailTitle);
        descriptionText = findViewById(R.id.taskDetailDescription);
        paymentText = findViewById(R.id.taskDetailPayment);
        statusText = findViewById(R.id.taskDetailStatus);
        hirerText = findViewById(R.id.taskDetailHirer);

        // Get task ID from intent
        String taskId = getIntent().getStringExtra("taskId");
        if (taskId != null) {
            loadTaskDetails(taskId);
        }
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