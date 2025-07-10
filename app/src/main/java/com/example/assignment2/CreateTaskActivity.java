package com.example.assignment2;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateTaskActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "CreateTaskActivity";
    private static final int LOCATION_PICKER_REQUEST = 200;

    // UI Components
    private ImageView btnBack;
    private TextInputEditText editTextTitle, editTextDescription, editTextPayment;
    private TextView editTextDate, editTextLocation;
    private LinearLayout dateClickArea, locationClickArea, mapPreviewContainer;
    private Button btnCancel, btnCreateTask;
    private MapView mapView;

    // Data
    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private LocationHelper locationHelper;
    private GoogleMap googleMap;
    private LatLng selectedLocation;
    private String selectedAddress = "";
    private Calendar selectedCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        // Initialize Firebase and managers
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);
        locationHelper = new LocationHelper(this);

        // Initialize views
        initializeViews();
        setupClickListeners();
        setupMapView(savedInstanceState);
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextPayment = findViewById(R.id.editTextPayment);
        editTextDate = findViewById(R.id.editTextDate);
        editTextLocation = findViewById(R.id.editTextLocation);
        dateClickArea = findViewById(R.id.dateClickArea);
        locationClickArea = findViewById(R.id.locationClickArea);
        mapPreviewContainer = findViewById(R.id.mapPreviewContainer);
        btnCancel = findViewById(R.id.btnCancel);
        btnCreateTask = findViewById(R.id.btnCreateTask);
        mapView = findViewById(R.id.mapView);
    }

    private void setupClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        // Cancel button
        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        // Date selection
        dateClickArea.setOnClickListener(v -> showDatePicker());

        // Location selection
        locationClickArea.setOnClickListener(v -> openLocationPicker());

        // Create task button
        btnCreateTask.setOnClickListener(v -> createTask());
    }

    private void setupMapView(Bundle savedInstanceState) {
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        
        // Set minimum date to today
        long minDate = calendar.getTimeInMillis();
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(year, month, dayOfMonth);
                
                // Format and display the selected date
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String formattedDate = dateFormat.format(selectedCalendar.getTime());
                
                editTextDate.setText(formattedDate);
                editTextDate.setTextColor(getResources().getColor(R.color.text_dark));
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(minDate);
        datePickerDialog.show();
    }

    private void openLocationPicker() {
        Log.d(TAG, "Opening location picker");
        
        // Open the map location picker activity
        Intent mapPickerIntent = new Intent(this, MapLocationPickerActivity.class);
        startActivityForResult(mapPickerIntent, LOCATION_PICKER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == LOCATION_PICKER_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                double latitude = data.getDoubleExtra("latitude", 0);
                double longitude = data.getDoubleExtra("longitude", 0);
                String locationName = data.getStringExtra("locationName");
                String locationAddress = data.getStringExtra("locationAddress");
                
                // Update selected location data
                selectedLocation = new LatLng(latitude, longitude);
                selectedAddress = locationAddress != null ? locationAddress : "";
                
                // Update the location display
                String displayText = locationName != null ? locationName : 
                    (locationAddress != null ? locationAddress : 
                    String.format("%.6f, %.6f", latitude, longitude));
                
                editTextLocation.setText(displayText);
                editTextLocation.setTextColor(getResources().getColor(R.color.text_dark));
                
                // Show map preview if location is selected
                if (selectedLocation != null) {
                    showMapPreview();
                }
                
                Log.d(TAG, "Location selected: " + displayText + 
                    " at " + latitude + ", " + longitude);
                
                Toast.makeText(this, "Location selected: " + 
                    (locationName != null ? locationName : "Selected location"), 
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showMapPreview() {
        if (googleMap != null && selectedLocation != null) {
            mapPreviewContainer.setVisibility(View.VISIBLE);
            
            // Clear existing markers and add new one
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions()
                    .position(selectedLocation)
                    .title("Task Location"));
            
            // Move camera to selected location
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15));
        }
    }

    private void createTask() {
        // Get input values
        String title = editTextTitle.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String paymentStr = editTextPayment.getText().toString().trim();
        String dueDate = editTextDate.getText().toString().trim();
        String location = selectedAddress != null && !selectedAddress.isEmpty() ? 
            selectedAddress : editTextLocation.getText().toString().trim();

        // Validation
        if (title.isEmpty() || description.isEmpty() || paymentStr.isEmpty() || 
            dueDate.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate payment
        double payment;
        try {
            payment = Double.parseDouble(paymentStr);
            if (payment <= 0) {
                Toast.makeText(this, "Payment must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid payment amount", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate location
        if (selectedLocation == null) {
            Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get user information
        String hirerId = sessionManager.getUserId();
        String hirerEmail = sessionManager.getUserEmail();
        
        if (hirerId == null || hirerEmail == null) {
            Toast.makeText(this, "Please login to create tasks", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable create button to prevent double submission
        btnCreateTask.setEnabled(false);
        btnCreateTask.setText("Creating...");

        // Create task object
        Map<String, Object> task = new HashMap<>();
        task.put("title", title);
        task.put("description", description);
        task.put("payment", payment);
        task.put("dueDate", dueDate);
        task.put("location", location);
        task.put("latitude", selectedLocation.latitude);
        task.put("longitude", selectedLocation.longitude);
        task.put("hirerId", hirerId);
        task.put("hirerEmail", hirerEmail);
        task.put("status", "open");
        task.put("timestamp", System.currentTimeMillis());
        task.put("assignedTo", "");
        task.put("applicants", new HashMap<String, Object>());

        // Save to Firestore
        db.collection("tasks")
                .add(task)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Task created with ID: " + documentReference.getId());
                    Toast.makeText(CreateTaskActivity.this, "Task created successfully!", 
                        Toast.LENGTH_SHORT).show();
                    
                    // Return success result
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating task", e);
                    Toast.makeText(CreateTaskActivity.this, "Error creating task: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    
                    // Re-enable create button
                    btnCreateTask.setEnabled(true);
                    btnCreateTask.setText("Create Task");
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        
        // Setup map settings
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setAllGesturesEnabled(false);
        
        // Set default location (Malaysia)
        LatLng defaultLocation = new LatLng(3.065, 101.6036);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));
        
        Log.d(TAG, "Map preview ready");
    }

    // MapView lifecycle methods
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }
} 