package com.example.assignment2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class MapLocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapLocationPicker";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private GoogleMap mMap;
    private LocationHelper locationHelper;
    private FusedLocationProviderClient fusedLocationClient;

    // UI Components
    private EditText searchLocation;
    private ImageView btnBack, btnSearch;
    private TextView locationName, locationAddress, locationCoordinates;
    private Button btnCurrentLocation, btnConfirmLocation;

    // Selected location data
    private LatLng selectedLocation;
    private String selectedLocationName = "";
    private String selectedLocationAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_location_picker);

        // Initialize location services
        locationHelper = new LocationHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize UI components
        initializeViews();
        setupClickListeners();
        setupSearchFunctionality();

        // Setup map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void initializeViews() {
        searchLocation = findViewById(R.id.searchLocation);
        btnBack = findViewById(R.id.btnBack);
        btnSearch = findViewById(R.id.btnSearch);
        locationName = findViewById(R.id.locationName);
        locationAddress = findViewById(R.id.locationAddress);
        locationCoordinates = findViewById(R.id.locationCoordinates);
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
    }

    private void setupClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        // Search button
        btnSearch.setOnClickListener(v -> performSearch());

        // Current location button
        btnCurrentLocation.setOnClickListener(v -> getCurrentLocation());

        // Confirm location button
        btnConfirmLocation.setOnClickListener(v -> confirmSelectedLocation());
    }

    private void setupSearchFunctionality() {
        // Search on text change
        searchLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Auto-search after user stops typing for 1 second
                if (s.toString().trim().length() > 2) {
                    searchLocation.removeCallbacks(searchRunnable);
                    searchLocation.postDelayed(searchRunnable, 1000);
                }
            }
        });

        // Search on keyboard action
        searchLocation.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    private final Runnable searchRunnable = this::performSearch;

    private void performSearch() {
        String query = searchLocation.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a location to search", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Searching for: " + query);
        
        // First try offline search for common locations
        List<OfflineLocationSearch.LocationResult> offlineResults = OfflineLocationSearch.searchLocations(query);
        
        if (!offlineResults.isEmpty()) {
            // Use first offline result
            OfflineLocationSearch.LocationResult result = offlineResults.get(0);
            Log.d(TAG, "Found offline result: " + result.getName());
            
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(result.getLocation(), 15));
                updateSelectedLocationWithDetails(result.getLocation(), result.getName(), result.getAddress());
                Toast.makeText(this, "Found: " + result.getName(), Toast.LENGTH_SHORT).show();
            }
            
            searchLocation.clearFocus();
            return;
        }
        
        // If no offline results, try online geocoding
        Toast.makeText(this, "Searching online...", Toast.LENGTH_SHORT).show();

        locationHelper.getLocationFromAddress(query, new LocationHelper.GeocodeCallback() {
            @Override
            public void onGeocodeResult(LatLng location) {
                Log.d(TAG, "Online search result: " + location.latitude + ", " + location.longitude);
                
                // Move map to search result
                if (mMap != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
                    updateSelectedLocation(location);
                    Toast.makeText(MapLocationPickerActivity.this, "Location found online!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onGeocodeError(String error) {
                Log.e(TAG, "Online search error: " + error);
                
                // Provide helpful suggestions based on error type
                String friendlyMessage;
                if (error.contains("DEADLINE_EXCEEDED") || error.contains("UnknownHostException") || 
                    error.contains("NoConnectionError") || error.contains("Name resolution")) {
                    friendlyMessage = "No internet connection. Try: 'KLCC', 'Taylor's', 'Sunway', or tap on the map.";
                } else {
                    friendlyMessage = "Location not found. Try: 'KLCC', 'Pavilion', 'Mid Valley', or tap on the map.";
                }
                
                Toast.makeText(MapLocationPickerActivity.this, friendlyMessage, Toast.LENGTH_LONG).show();
                
                // Don't clear search field in case user wants to modify
            }
        });

        // Hide keyboard
        searchLocation.clearFocus();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        
        Log.d(TAG, "Map is ready");

        // Setup map settings
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false); // We have our own button
        
        // Set default location to Malaysia (Taylor's University area)
        LatLng defaultLocation = new LatLng(3.065, 101.6036);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));
        
        // Update the initial UI to show map is ready
        locationName.setText("Ready to select location");
        locationAddress.setText("Tap anywhere on the map, search for a place, or use current location");
        btnConfirmLocation.setEnabled(false);

        // Handle map camera movements
        mMap.setOnCameraIdleListener(() -> {
            LatLng centerLocation = mMap.getCameraPosition().target;
            updateSelectedLocation(centerLocation);
        });

        // Handle map clicks
        mMap.setOnMapClickListener(latLng -> {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            updateSelectedLocation(latLng);
        });

        // Try to get current location on map ready (optional - won't block map usage)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            try {
                getCurrentLocation();
            } catch (Exception e) {
                Log.w(TAG, "Failed to get initial location: " + e.getMessage());
                // Map is still fully functional without current location
            }
        }
    }

    private void updateSelectedLocation(LatLng location) {
        selectedLocation = location;
        
        // Update coordinates display
        locationCoordinates.setText(String.format("%.6f, %.6f", 
            location.latitude, location.longitude));
        locationCoordinates.setVisibility(View.VISIBLE);

        // Get address for this location with enhanced error handling
        locationHelper.getAddressFromLocation(location, new LocationHelper.AddressCallback() {
            @Override
            public void onAddressResult(String address) {
                selectedLocationAddress = address;
                selectedLocationName = extractLocationName(address);
                
                // Update UI
                locationName.setText(selectedLocationName.isEmpty() ? "Selected Location" : selectedLocationName);
                locationAddress.setText(address);
                
                // Enable confirm button
                btnConfirmLocation.setEnabled(true);
                
                Log.d(TAG, "Location updated: " + selectedLocationName + " at " + address);
            }

            @Override
            public void onAddressError(String error) {
                // Use coordinates as fallback
                selectedLocationAddress = String.format("%.6f, %.6f", 
                    location.latitude, location.longitude);
                selectedLocationName = "Selected Location";
                
                // Update UI with coordinates
                locationName.setText(selectedLocationName);
                
                // Show user-friendly message based on error type
                if (error.contains("DEADLINE_EXCEEDED") || error.contains("UnknownHostException") || 
                    error.contains("NoConnectionError") || error.contains("Name resolution")) {
                    locationAddress.setText("No internet - using coordinates: " + selectedLocationAddress);
                } else {
                    locationAddress.setText("Address lookup failed - using coordinates: " + selectedLocationAddress);
                }
                
                // Enable confirm button - coordinates are still valid
                btnConfirmLocation.setEnabled(true);
                
                Log.w(TAG, "Could not get address for location: " + error);
            }
        });
    }
    
    private void updateSelectedLocationWithDetails(LatLng location, String name, String address) {
        selectedLocation = location;
        selectedLocationName = name;
        selectedLocationAddress = address;
        
        // Update coordinates display
        locationCoordinates.setText(String.format("%.6f, %.6f", 
            location.latitude, location.longitude));
        locationCoordinates.setVisibility(View.VISIBLE);
        
        // Update UI with provided details
        locationName.setText(name);
        locationAddress.setText(address);
        
        // Enable confirm button
        btnConfirmLocation.setEnabled(true);
        
        Log.d(TAG, "Location updated with details: " + name + " at " + address);
    }

    private String extractLocationName(String fullAddress) {
        if (fullAddress == null || fullAddress.isEmpty()) {
            return "";
        }
        
        // Extract the first part of the address as the location name
        String[] parts = fullAddress.split(",");
        if (parts.length > 0) {
            return parts[0].trim();
        }
        
        return fullAddress;
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(this, location -> {
                if (location != null && mMap != null) {
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                    updateSelectedLocation(currentLocation);
                    
                    Log.d(TAG, "Current location: " + currentLocation.latitude + ", " + currentLocation.longitude);
                    Toast.makeText(this, "Moved to your current location", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Unable to get current location. Please select manually on the map.", Toast.LENGTH_LONG).show();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get current location", e);
                Toast.makeText(this, "Location services unavailable. Please select manually on the map.", Toast.LENGTH_LONG).show();
            });
    }

    private void confirmSelectedLocation() {
        if (selectedLocation == null) {
            Toast.makeText(this, "Please select a location first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Return the selected location data
        Intent resultIntent = new Intent();
        resultIntent.putExtra("latitude", selectedLocation.latitude);
        resultIntent.putExtra("longitude", selectedLocation.longitude);
        resultIntent.putExtra("locationName", selectedLocationName);
        resultIntent.putExtra("locationAddress", selectedLocationAddress);
        
        Log.d(TAG, "Confirming location: " + selectedLocationName + " (" + 
            selectedLocation.latitude + ", " + selectedLocation.longitude + ")");
        
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchLocation != null) {
            searchLocation.removeCallbacks(searchRunnable);
        }
    }
} 