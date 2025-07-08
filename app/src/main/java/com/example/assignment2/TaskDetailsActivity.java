package com.example.assignment2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import android.view.View;

public class TaskDetailsActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private TextView titleText, descriptionText, paymentText, statusText, hirerText, dateText, locationText, routeInfoText;
    private Button btnShowRoute;
    private SessionManager sessionManager;
    private LocationHelper locationHelper;
    private RouteHelper routeHelper;
    private MapView mapView;
    private GoogleMap googleMap;
    private String taskLocation;
    private ChatbotManager chatbotManager;
    private LatLng currentLocation;
    private LatLng taskLocationCoords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);

        // Initialize Firestore and SessionManager
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);
        locationHelper = new LocationHelper(this);
        routeHelper = new RouteHelper(this);

        // Initialize chatbot
        chatbotManager = new ChatbotManager(this);
        chatbotManager.addChatbotButton(this);

        // Initialize views
        titleText = findViewById(R.id.taskDetailTitle);
        descriptionText = findViewById(R.id.taskDetailDescription);
        paymentText = findViewById(R.id.taskDetailPayment);
        statusText = findViewById(R.id.taskDetailStatus);
        hirerText = findViewById(R.id.taskDetailHirer);
        dateText = findViewById(R.id.taskDetailDate);
        locationText = findViewById(R.id.taskDetailLocation);
        routeInfoText = findViewById(R.id.routeInfo);
        btnShowRoute = findViewById(R.id.btnShowRoute);
        mapView = findViewById(R.id.taskLocationMapView);

        // Setup Show Route button
        btnShowRoute.setOnClickListener(v -> {
            if (taskLocationCoords != null) {
                getCurrentLocationAndShowRoute();
            } else {
                Toast.makeText(this, "Task location not available", Toast.LENGTH_SHORT).show();
            }
        });

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
                    googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                    
                    try {
                        googleMap.setMyLocationEnabled(true);
                    } catch (SecurityException e) {
                        // Permission not granted
                    }
                    
                    // Use geocoding to get coordinates from address
                    locationHelper.getLocationFromAddress(taskLocation, new LocationHelper.GeocodeCallback() {
                        @Override
                        public void onGeocodeResult(LatLng location) {
                            taskLocationCoords = location;
                            // Add marker for task location
                            googleMap.addMarker(new MarkerOptions()
                                    .position(location)
                                    .title("Task Location")
                                    .snippet(taskLocation)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                            
                            // Get current location and show route
                            getCurrentLocationAndShowRoute();
                        }
                        
                        @Override
                        public void onGeocodeError(String error) {
                            // If geocoding fails, try to show Taylor's University as fallback
                            LatLng fallbackLocation = new LatLng(3.065, 101.6036);
                            taskLocationCoords = fallbackLocation;
                            googleMap.addMarker(new MarkerOptions()
                                    .position(fallbackLocation)
                                    .title("Task Location")
                                    .snippet(taskLocation + " (Approximate)")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fallbackLocation, 15));
                            Toast.makeText(TaskDetailsActivity.this, "Unable to find exact location on map", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
    }

    private void getCurrentLocationAndShowRoute() {
        if (locationHelper.hasLocationPermission()) {
            locationHelper.getCurrentLocation(new LocationHelper.LocationCallback() {
                @Override
                public void onLocationReceived(LatLng location, String address) {
                    currentLocation = location;
                    // Add marker for current location
                    googleMap.addMarker(new MarkerOptions()
                            .position(location)
                            .title("Your Location")
                            .snippet("Current Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                    
                    // Calculate and display route
                    calculateRoute();
                    
                    // Adjust camera to show both locations
                    adjustCameraToShowBothLocations();
                }
                
                @Override
                public void onLocationError(String error) {
                    // If can't get current location, just center on task location
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(taskLocationCoords, 15));
                    Toast.makeText(TaskDetailsActivity.this, "Unable to get current location for route", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Request permission or just show task location
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(taskLocationCoords, 15));
            locationHelper.requestLocationPermission(this);
        }
    }

    private void calculateRoute() {
        if (currentLocation != null && taskLocationCoords != null) {
            // Check if API key is configured
            if (!routeHelper.isApiKeyConfigured()) {
                android.util.Log.e("TaskDetailsActivity", "Google Maps API key not configured properly");
                Toast.makeText(this, "Route feature requires Google Maps API key configuration", Toast.LENGTH_LONG).show();
                if (routeInfoText != null) {
                    routeInfoText.setVisibility(View.VISIBLE);
                    routeInfoText.setText("❌ API key not configured");
                }
                return;
            }
            
            // Show route info TextView and set loading text
            if (routeInfoText != null) {
                routeInfoText.setVisibility(View.VISIBLE);
                routeInfoText.setText("Calculating route...");
            }
            
            android.util.Log.d("TaskDetailsActivity", "Calculating route from " + 
                currentLocation.latitude + "," + currentLocation.longitude + 
                " to " + taskLocationCoords.latitude + "," + taskLocationCoords.longitude);
            
            routeHelper.calculateRoute(currentLocation, taskLocationCoords, new RouteHelper.RouteCallback() {
                @Override
                public void onRouteCalculated(List<LatLng> routePoints, String distance, String duration) {
                    android.util.Log.d("TaskDetailsActivity", "Route calculated successfully. Points: " + 
                        routePoints.size() + ", Distance: " + distance + ", Duration: " + duration);
                        
                    runOnUiThread(() -> {
                        // Draw route on map (this clears existing markers)
                        routeHelper.drawRouteOnMap(googleMap, routePoints);
                        
                        // Re-add markers after route is drawn
                        addMarkersToMap();
                        
                        // Update route info text
                        String routeInfo = "🚗 Distance: " + distance + " • ⏱️ Duration: " + duration;
                        if (routeInfoText != null) {
                            routeInfoText.setVisibility(View.VISIBLE);
                            routeInfoText.setText(routeInfo);
                        } else {
                            Toast.makeText(TaskDetailsActivity.this, routeInfo, Toast.LENGTH_LONG).show();
                        }
                    });
                }
                
                @Override
                public void onRouteError(String error) {
                    android.util.Log.e("TaskDetailsActivity", "Route calculation failed: " + error);
                    
                    runOnUiThread(() -> {
                        String errorMessage = "Route calculation failed";
                        if (error.contains("API key")) {
                            errorMessage += ": API key issue";
                        } else if (error.contains("ZERO_RESULTS")) {
                            errorMessage += ": No route found";
                        } else if (error.contains("OVER_QUERY_LIMIT")) {
                            errorMessage += ": API quota exceeded";
                        } else if (error.contains("REQUEST_DENIED")) {
                            errorMessage += ": API access denied";
                        } else {
                            errorMessage += ": " + error;
                        }
                        
                        Toast.makeText(TaskDetailsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        if (routeInfoText != null) {
                            routeInfoText.setVisibility(View.VISIBLE);
                            routeInfoText.setText("❌ Route unavailable");
                        }
                    });
                }
            });
        } else {
            android.util.Log.e("TaskDetailsActivity", "Cannot calculate route - missing locations. " +
                "Current: " + currentLocation + ", Task: " + taskLocationCoords);
            Toast.makeText(this, "Cannot calculate route - location data missing", Toast.LENGTH_SHORT).show();
        }
    }

    private void addMarkersToMap() {
        if (googleMap == null) return;
        
        // Add task location marker
        if (taskLocationCoords != null) {
            googleMap.addMarker(new MarkerOptions()
                    .position(taskLocationCoords)
                    .title("Task Location")
                    .snippet(taskLocation)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }
        
        // Add current location marker
        if (currentLocation != null) {
            googleMap.addMarker(new MarkerOptions()
                    .position(currentLocation)
                    .title("Your Location")
                    .snippet("Current Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        }
    }

    private void adjustCameraToShowBothLocations() {
        if (currentLocation != null && taskLocationCoords != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(currentLocation);
            builder.include(taskLocationCoords);
            LatLngBounds bounds = builder.build();
            
            int padding = 100; // Padding in pixels
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LocationHelper.getLocationPermissionRequestCode()) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted! Calculating route...", Toast.LENGTH_SHORT).show();
                if (taskLocationCoords != null) {
                    getCurrentLocationAndShowRoute();
                }
            } else {
                Toast.makeText(this, "Location permission denied. Route unavailable.", Toast.LENGTH_SHORT).show();
                if (routeInfoText != null) {
                    routeInfoText.setVisibility(View.GONE);
                }
            }
        }
    }
} 