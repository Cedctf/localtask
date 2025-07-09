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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.DocumentSnapshot;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import android.view.View;

public class TaskDetailsActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private TextView titleText, descriptionText, paymentText, statusText, hirerText, dateText, locationText, routeInfoText;
    private Button btnShowRoute, btnApplyForTask, btnCompleteTask, btnMarkAsPaid;
    private SessionManager sessionManager;
    private LocationHelper locationHelper;
    private RouteHelper routeHelper;
    private MapView mapView;
    private GoogleMap googleMap;
    private String taskLocation;
    private ChatbotManager chatbotManager;
    private LatLng currentLocation;
    private LatLng taskLocationCoords;
    private String currentTaskId;

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
        btnApplyForTask = findViewById(R.id.btnApplyForTask);
        btnCompleteTask = findViewById(R.id.btnCompleteTask);
        btnMarkAsPaid = findViewById(R.id.btnMarkAsPaid);
        mapView = findViewById(R.id.taskLocationMapView);

        // Setup Show Route button
        btnShowRoute.setOnClickListener(v -> {
            if (taskLocationCoords != null) {
                getCurrentLocationAndShowRoute();
            } else {
                Toast.makeText(this, "Task location not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup Apply button
        btnApplyForTask.setOnClickListener(v -> applyForTask());

        // Setup Complete Task button
        btnCompleteTask.setOnClickListener(v -> completeTask());

        // Setup Mark as Paid button
        btnMarkAsPaid.setOnClickListener(v -> markAsPaid());

        // Initialize map
        mapView.onCreate(savedInstanceState);
        mapView.onResume();

        // Setup bottom navigation
        setupBottomNavigation();

        // Get task ID from intent
        currentTaskId = getIntent().getStringExtra("task_id");
        if (currentTaskId != null) {
            loadTaskDetails(currentTaskId);
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
                // Go back to TasksActivity (dashboard)
                Intent intent = new Intent(TaskDetailsActivity.this, TasksActivity.class);
                intent.putExtra("USER_NAME", sessionManager.getUserName());
                intent.putExtra("USER_TYPE", sessionManager.getUserType());
                intent.putExtra("USER_EMAIL", sessionManager.getUserEmail());
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.navigation_my_tasks) {
                // Navigate to TasksActivity and show my tasks
                navigateToTasksActivity("my_tasks");
                return true;
            } else if (itemId == R.id.navigation_search) {
                // Navigate to TasksActivity and show search
                navigateToTasksActivity("search");
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
                        
                        // Show/hide buttons based on task status and user type
                        updateButtonVisibility(task);
                        
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

    private void updateButtonVisibility(Task task) {
        String userType = sessionManager.getUserType();
        String taskStatus = task.getStatus();
        String currentUserId = sessionManager.getUserId();
        String assignedTo = task.getAssignedTo();
        String hirerId = task.getHirerId();

        // Default: hide all buttons
        btnApplyForTask.setVisibility(View.GONE);
        btnCompleteTask.setVisibility(View.GONE);
        btnMarkAsPaid.setVisibility(View.GONE);

        if ("Hirer".equals(userType)) {
            // Show Mark as Paid button for hirers on pending payment tasks they created
            if ("pending_payment".equals(taskStatus) && currentUserId != null && currentUserId.equals(hirerId)) {
                btnMarkAsPaid.setVisibility(View.VISIBLE);
            }
        } else {
            // Regular user buttons
            if ("open".equals(taskStatus)) {
                // Show Apply button for regular users on open tasks
                btnApplyForTask.setVisibility(View.VISIBLE);
            } else if ("in_progress".equals(taskStatus) && currentUserId != null && currentUserId.equals(assignedTo)) {
                // Show Complete Task button for users assigned to this in-progress task
                btnCompleteTask.setVisibility(View.VISIBLE);
            }
        }
    }

    private void applyForTask() {
        if (currentTaskId == null) {
            Toast.makeText(this, "Task ID not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = sessionManager.getUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create update data
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "in_progress");
        updates.put("assignedTo", currentUserId);
        updates.put("startTime", FieldValue.serverTimestamp());

        // Update task in Firestore
        db.collection("tasks").document(currentTaskId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(TaskDetailsActivity.this, "Apply Successfully.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(TaskDetailsActivity.this, "Failed to apply for task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void completeTask() {
        if (currentTaskId == null) {
            Toast.makeText(this, "Task ID not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = sessionManager.getUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create update data for task completion
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "pending_payment");
        updates.put("completionTime", FieldValue.serverTimestamp());

        // Update task in Firestore
        db.collection("tasks").document(currentTaskId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Task updated successfully, now fetch hirer's FCM token
                    fetchHirerFCMToken();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(TaskDetailsActivity.this, "Failed to complete task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchHirerFCMToken() {
        // First, get the task document to retrieve hirerId
        db.collection("tasks").document(currentTaskId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Task task = documentSnapshot.toObject(Task.class);
                    if (task != null && task.getHirerId() != null) {
                        String hirerId = task.getHirerId();
                        
                        // Now fetch the hirer's user document to get FCM token
                        db.collection("users").document(hirerId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        String fcmToken = userDoc.getString("fcmToken");
                                        if (fcmToken != null && !fcmToken.isEmpty()) {
                                            android.util.Log.d("TaskDetailsActivity", "Hirer FCM Token: " + fcmToken);
                                            // TODO: Send notification using FCM token
                                            showCompletionSuccess();
                                        } else {
                                            android.util.Log.w("TaskDetailsActivity", "Hirer FCM token not found or empty");
                                            showCompletionSuccess();
                                        }
                                    } else {
                                        android.util.Log.w("TaskDetailsActivity", "Hirer user document not found");
                                        showCompletionSuccess();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    android.util.Log.e("TaskDetailsActivity", "Failed to fetch hirer user document: " + e.getMessage());
                                    showCompletionSuccess();
                                });
                    } else {
                        android.util.Log.w("TaskDetailsActivity", "Task document or hirerId is null");
                        showCompletionSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("TaskDetailsActivity", "Failed to fetch task document: " + e.getMessage());
                    showCompletionSuccess();
                });
    }

    private void showCompletionSuccess() {
        Toast.makeText(TaskDetailsActivity.this, "Task marked as completed. Waiting for hirer to review and pay.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void markAsPaid() {
        if (currentTaskId == null) {
            Toast.makeText(this, "Task ID not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = sessionManager.getUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Immediately disable the button to prevent double clicks
        btnMarkAsPaid.setEnabled(false);
        android.util.Log.d("TaskDetailsActivity", "Mark as Paid button disabled to prevent double payment");

        // First, read the task document to get price, assignedTo, and hirerId
        db.collection("tasks").document(currentTaskId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Task task = documentSnapshot.toObject(Task.class);
                    if (task != null) {
                        // Check that the task status is still 'pending_payment' to prevent double payment
                        if (!"pending_payment".equals(task.getStatus())) {
                            Toast.makeText(TaskDetailsActivity.this, "Payment already processed or task status changed", Toast.LENGTH_SHORT).show();
                            btnMarkAsPaid.setEnabled(true); // Re-enable button for retry if needed
                            android.util.Log.w("TaskDetailsActivity", "Payment blocked - Task status is: " + task.getStatus());
                            return;
                        }
                        
                        // Read the required fields from the task
                        double price = task.getPayment();
                        String assignedTo = task.getAssignedTo();
                        String hirerId = task.getHirerId();
                        
                        android.util.Log.d("TaskDetailsActivity", "Task payment confirmation - Price: " + price + 
                                ", AssignedTo: " + assignedTo + ", HirerId: " + hirerId);

                        // Get DocumentReference for both users from the users collection
                        DocumentReference assignedUserRef = db.collection("users").document(assignedTo);
                        DocumentReference hirerRef = db.collection("users").document(hirerId);
                        
                        android.util.Log.d("TaskDetailsActivity", "Got DocumentReferences - AssignedUser: " + assignedUserRef.getPath() + 
                                ", Hirer: " + hirerRef.getPath());

                        // Run a Firestore transaction to update earnings and spending
                        db.runTransaction((Transaction.Function<Void>) transaction -> {
                            // Read both user documents within the transaction
                            DocumentSnapshot assignedUserSnapshot = transaction.get(assignedUserRef);
                            DocumentSnapshot hirerSnapshot = transaction.get(hirerRef);
                            
                            // Get current financial data (default to 0 if not exists)
                            double currentEarnings = 0;
                            if (assignedUserSnapshot.exists() && assignedUserSnapshot.contains("totalEarnings")) {
                                Number earningsNumber = assignedUserSnapshot.getDouble("totalEarnings");
                                currentEarnings = earningsNumber != null ? earningsNumber.doubleValue() : 0;
                            }
                            
                            double currentSpent = 0;
                            if (hirerSnapshot.exists() && hirerSnapshot.contains("totalSpent")) {
                                Number spentNumber = hirerSnapshot.getDouble("totalSpent");
                                currentSpent = spentNumber != null ? spentNumber.doubleValue() : 0;
                            }
                            
                            // Calculate new values
                            double newEarnings = currentEarnings + price;
                            double newSpent = currentSpent + price;
                            
                            android.util.Log.d("TaskDetailsActivity", "Financial Update - AssignedUser: " + currentEarnings + 
                                    " -> " + newEarnings + ", Hirer: " + currentSpent + " -> " + newSpent);
                            
                            // Update both user documents
                            transaction.update(assignedUserRef, "totalEarnings", newEarnings);
                            transaction.update(hirerRef, "totalSpent", newSpent);
                            
                            return null;
                        }).addOnSuccessListener(aVoid -> {
                            android.util.Log.d("TaskDetailsActivity", "Financial transaction completed successfully");
                            
                            // Fetch and show updated financial totals
                            showUpdatedFinancialTotals(assignedUserRef, hirerRef, price);
                            
                            // Now update the task status to completed
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("status", "completed");
                            updates.put("paymentTime", FieldValue.serverTimestamp());

                            db.collection("tasks").document(currentTaskId)
                                    .update(updates)
                                    .addOnSuccessListener(taskUpdateVoid -> {
                                        // Show payment successful confirmation message
                                        Toast.makeText(TaskDetailsActivity.this, "Payment Successful", Toast.LENGTH_LONG).show();
                                        
                                        // Update the task status in UI to 'completed'
                                        statusText.setText("Status: completed");
                                        
                                        // Disable the Mark as Paid button
                                        btnMarkAsPaid.setVisibility(View.GONE);
                                        btnMarkAsPaid.setEnabled(false);
                                        
                                        android.util.Log.d("TaskDetailsActivity", "UI updated - Status: completed, Button disabled");
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(TaskDetailsActivity.this, "Failed to mark task as paid: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        // Re-enable button on task update failure
                                        btnMarkAsPaid.setEnabled(true);
                                    });
                        }).addOnFailureListener(e -> {
                            android.util.Log.e("TaskDetailsActivity", "Financial transaction failed: " + e.getMessage());
                            Toast.makeText(TaskDetailsActivity.this, "Failed to update user finances: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            // Re-enable button on transaction failure
                            btnMarkAsPaid.setEnabled(true);
                        });
                    } else {
                        Toast.makeText(TaskDetailsActivity.this, "Task data not found", Toast.LENGTH_SHORT).show();
                        // Re-enable button on task data error
                        btnMarkAsPaid.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(TaskDetailsActivity.this, "Failed to read task data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Re-enable button on read failure
                    btnMarkAsPaid.setEnabled(true);
                });
    }

    private void showUpdatedFinancialTotals(DocumentReference assignedUserRef, DocumentReference hirerRef, double taskPrice) {
        // Fetch updated financial data for both users
        assignedUserRef.get().addOnSuccessListener(assignedUserDoc -> {
            hirerRef.get().addOnSuccessListener(hirerDoc -> {
                // Get updated totals
                double updatedEarnings = 0;
                if (assignedUserDoc.exists() && assignedUserDoc.contains("totalEarnings")) {
                    Number earningsNumber = assignedUserDoc.getDouble("totalEarnings");
                    updatedEarnings = earningsNumber != null ? earningsNumber.doubleValue() : 0;
                }
                
                double updatedSpent = 0;
                if (hirerDoc.exists() && hirerDoc.contains("totalSpent")) {
                    Number spentNumber = hirerDoc.getDouble("totalSpent");
                    updatedSpent = spentNumber != null ? spentNumber.doubleValue() : 0;
                }
                
                // Format currency for display
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
                String earningsText = currencyFormat.format(updatedEarnings);
                String spentText = currencyFormat.format(updatedSpent);
                String taskPriceText = currencyFormat.format(taskPrice);
                
                // Create detailed update message
                String updateMessage = "Financial Update:\n" +
                        "• Task payment: " + taskPriceText + "\n" +
                        "• Worker's total earnings: " + earningsText + "\n" +
                        "• Your total spent: " + spentText;
                
                // Show in a toast with longer duration
                Toast.makeText(TaskDetailsActivity.this, updateMessage, Toast.LENGTH_LONG).show();
                
                // Log the updated totals
                android.util.Log.d("TaskDetailsActivity", "Updated Financial Totals - Worker earnings: " + 
                        earningsText + ", Hirer spent: " + spentText + ", Task payment: " + taskPriceText);
                
            }).addOnFailureListener(e -> {
                android.util.Log.w("TaskDetailsActivity", "Failed to fetch hirer's updated totals: " + e.getMessage());
            });
        }).addOnFailureListener(e -> {
            android.util.Log.w("TaskDetailsActivity", "Failed to fetch worker's updated totals: " + e.getMessage());
        });
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