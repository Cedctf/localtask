package com.example.assignment2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.NumberFormat;
import java.util.Locale;

public class TaskDetailsActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private TextView titleText, descriptionText, paymentText, statusText, hirerText, dateText, locationText;
    private SessionManager sessionManager;
    private LocationHelper locationHelper;
    private MapView mapView;
    private GoogleMap googleMap;
    private String taskLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);

        // Initialize Firestore and SessionManager
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);
        locationHelper = new LocationHelper(this);

        // Initialize views
        titleText = findViewById(R.id.taskDetailTitle);
        descriptionText = findViewById(R.id.taskDetailDescription);
        paymentText = findViewById(R.id.taskDetailPayment);
        statusText = findViewById(R.id.taskDetailStatus);
        hirerText = findViewById(R.id.taskDetailHirer);
        dateText = findViewById(R.id.taskDetailDate);
        locationText = findViewById(R.id.taskDetailLocation);
        mapView = findViewById(R.id.taskLocationMapView);

        // Initialize map
        mapView.onCreate(savedInstanceState);
        mapView.onResume();

        // Setup bottom navigation
        setupBottomNavigation();

        // Get task ID from intent
        String taskId = getIntent().getStringExtra("task_id");
        if (taskId != null) {
            loadTaskDetails(taskId);
        } else {
            // Fallback: try to get data from intent extras directly
            String title = getIntent().getStringExtra("task_title");
            String description = getIntent().getStringExtra("task_description");
            String payment = getIntent().getStringExtra("task_payment");
            String hirer = getIntent().getStringExtra("task_hirer");
            String dueDate = getIntent().getStringExtra("task_date");
            String location = getIntent().getStringExtra("task_location");
            
            if (title != null) {
                titleText.setText(title);
                descriptionText.setText(description);
                paymentText.setText(payment);
                hirerText.setText("Posted by: " + hirer);
                dateText.setText(dueDate != null ? dueDate : "No date specified");
                locationText.setText(location != null ? location : "No location specified");
                statusText.setText("Status: open");
                taskLocation = location;
                setupMap();
            }
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
                        dateText.setText(task.getDueDate() != null ? task.getDueDate() : "No date specified");
                        locationText.setText(task.getLocation() != null ? task.getLocation() : "No location specified");
                        taskLocation = task.getLocation();
                        setupMap();
                    }
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Error loading task details: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show());
    }

    private void setupMap() {
        if (taskLocation != null && !taskLocation.isEmpty()) {
            mapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap map) {
                    googleMap = map;
                    googleMap.getUiSettings().setZoomControlsEnabled(true);
                    googleMap.getUiSettings().setMyLocationButtonEnabled(false);
                    
                    // Use geocoding to get coordinates from address
                    locationHelper.getLocationFromAddress(taskLocation, new LocationHelper.GeocodeCallback() {
                        @Override
                        public void onGeocodeResult(LatLng location) {
                            googleMap.addMarker(new MarkerOptions()
                                    .position(location)
                                    .title("Task Location")
                                    .snippet(taskLocation));
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
                        }
                        
                        @Override
                        public void onGeocodeError(String error) {
                            // If geocoding fails, try to show Taylor's University as fallback
                            LatLng fallbackLocation = new LatLng(3.065, 101.6036);
                            googleMap.addMarker(new MarkerOptions()
                                    .position(fallbackLocation)
                                    .title("Task Location")
                                    .snippet(taskLocation + " (Approximate)"));
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fallbackLocation, 15));
                            Toast.makeText(TaskDetailsActivity.this, "Unable to find exact location on map", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }
} 