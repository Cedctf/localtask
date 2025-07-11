package com.example.assignment2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.os.Handler;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationHelper {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private Context context;
    private FusedLocationProviderClient fusedLocationClient;
    private Geocoder geocoder;

    public interface LocationCallback {
        void onLocationReceived(LatLng location, String address);
        void onLocationError(String error);
    }

    public interface GeocodeCallback {
        void onGeocodeResult(LatLng location);
        void onGeocodeError(String error);
    }

    public interface AddressCallback {
        void onAddressResult(String address);
        void onAddressError(String error);
    }

    public LocationHelper(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.geocoder = new Geocoder(context, Locale.getDefault());
    }

    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
    }

    public void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    public void getCurrentLocation(LocationCallback callback) {
        android.util.Log.d("LocationHelper", "getCurrentLocation called");
        
        if (!hasLocationPermission()) {
            android.util.Log.e("LocationHelper", "Location permission not granted");
            callback.onLocationError("Location permission not granted");
            return;
        }

        android.util.Log.d("LocationHelper", "Location permission granted, requesting location...");

        try {
            // First try to get the last known location
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            android.util.Log.d("LocationHelper", "Last known location found: " + location.getLatitude() + ", " + location.getLongitude());
                            
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            
                            // Check if location seems reasonable for Malaysia app usage
                            if (isLocationOutsideMalaysiaRegion(latLng)) {
                                android.util.Log.w("LocationHelper", "Current location appears to be outside Malaysia region. Using fallback location.");
                                // Use a reasonable Malaysian location (Kuala Lumpur city center)
                                LatLng fallbackLocation = new LatLng(3.1390, 101.6869);
                                callback.onLocationReceived(fallbackLocation, "Kuala Lumpur (Default Location)");
                                return;
                            }
                            
                            // We have a reasonable location, use it
                            getAddressFromLocation(latLng, new AddressCallback() {
                                @Override
                                public void onAddressResult(String address) {
                                    android.util.Log.d("LocationHelper", "Address for last location: " + address);
                                    callback.onLocationReceived(latLng, address);
                                }

                                @Override
                                public void onAddressError(String error) {
                                    android.util.Log.d("LocationHelper", "Address lookup failed for last location: " + error);
                                    callback.onLocationReceived(latLng, "Current Location");
                                }
                            });
                        } else {
                            android.util.Log.d("LocationHelper", "No last known location, requesting fresh location");
                            // No last known location, request a fresh location
                            requestFreshLocation(callback);
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("LocationHelper", "Failed to get last location: " + e.getMessage());
                        // Failed to get last location, try fresh location
                        requestFreshLocation(callback);
                    });
        } catch (SecurityException e) {
            android.util.Log.e("LocationHelper", "Security exception in getCurrentLocation: " + e.getMessage());
            callback.onLocationError("Location permission denied");
        }
    }

    private void requestFreshLocation(LocationCallback callback) {
        try {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                    .setMinUpdateIntervalMillis(5000)
                    .setMaxUpdates(1)
                    .build();

            fusedLocationClient.requestLocationUpdates(locationRequest, new com.google.android.gms.location.LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult != null && locationResult.getLocations() != null && !locationResult.getLocations().isEmpty()) {
                        Location location = locationResult.getLastLocation();
                        if (location != null) {
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            
                            // Stop location updates after getting the result
                            fusedLocationClient.removeLocationUpdates(this);
                            
                            // Debug: Log the location received
                            android.util.Log.d("LocationHelper", "Fresh location received: " + latLng.latitude + ", " + latLng.longitude);
                            
                            // Check if location seems reasonable for Malaysia app usage
                            if (isLocationOutsideMalaysiaRegion(latLng)) {
                                android.util.Log.w("LocationHelper", "Fresh location appears to be outside Malaysia region. Using fallback location.");
                                // Use a reasonable Malaysian location (Kuala Lumpur city center)
                                LatLng fallbackLocation = new LatLng(3.1390, 101.6869);
                                callback.onLocationReceived(fallbackLocation, "Kuala Lumpur (Default Location)");
                                return;
                            }
                            
                            getAddressFromLocation(latLng, new AddressCallback() {
                                @Override
                                public void onAddressResult(String address) {
                                    android.util.Log.d("LocationHelper", "Address found: " + address);
                                    callback.onLocationReceived(latLng, address);
                                }

                                @Override
                                public void onAddressError(String error) {
                                    android.util.Log.d("LocationHelper", "Address lookup failed: " + error);
                                    callback.onLocationReceived(latLng, "Current Location");
                                }
                            });
                        } else {
                            android.util.Log.e("LocationHelper", "Location is null even though locationResult was not null");
                            callback.onLocationError("Unable to get current location. Please check if location services are enabled.");
                        }
                    } else {
                        android.util.Log.e("LocationHelper", "LocationResult is null or empty");
                        callback.onLocationError("Unable to get current location. Please check if location services are enabled.");
                    }
                }
            }, Looper.getMainLooper());
        } catch (SecurityException e) {
            android.util.Log.e("LocationHelper", "Security exception: " + e.getMessage());
            callback.onLocationError("Location permission denied");
        }
    }

    public void getLocationFromAddress(String address, GeocodeCallback callback) {
        // Perform geocoding in background thread to avoid blocking UI and handle timeouts
        Handler mainHandler = new Handler(Looper.getMainLooper());
        
        new Thread(() -> {
            try {
                android.util.Log.d("LocationHelper", "Starting geocoding for: " + address);
                
                // Check if geocoder is available
                if (!Geocoder.isPresent()) {
                    android.util.Log.e("LocationHelper", "Geocoder not available on this device");
                    // Run callback on main thread
                    mainHandler.post(() -> 
                        callback.onGeocodeError("Geocoding service not available on this device"));
                    return;
                }
                
                List<Address> addresses = geocoder.getFromLocationName(address, 1);
                
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    LatLng location = new LatLng(addr.getLatitude(), addr.getLongitude());
                    android.util.Log.d("LocationHelper", "Geocoding successful: " + 
                        location.latitude + ", " + location.longitude);
                    
                    // Run callback on main thread
                    mainHandler.post(() -> callback.onGeocodeResult(location));
                } else {
                    android.util.Log.w("LocationHelper", "No geocoding results found for: " + address);
                    // Run callback on main thread
                    mainHandler.post(() -> 
                        callback.onGeocodeError("Address not found"));
                }
            } catch (IOException e) {
                android.util.Log.e("LocationHelper", "Geocoding failed for: " + address + 
                    ". Error: " + e.getMessage());
                
                // Run callback on main thread
                mainHandler.post(() -> 
                    callback.onGeocodeError("Geocoding error: " + e.getMessage()));
            } catch (Exception e) {
                android.util.Log.e("LocationHelper", "Unexpected error during geocoding: " + e.getMessage());
                
                // Run callback on main thread
                mainHandler.post(() -> 
                    callback.onGeocodeError("Geocoding failed: " + e.getMessage()));
            }
        }).start();
    }

    public void getAddressFromLocation(LatLng location, AddressCallback callback) {
        // Perform reverse geocoding in background thread to avoid blocking UI and handle timeouts
        Handler mainHandler = new Handler(Looper.getMainLooper());
        
        new Thread(() -> {
            try {
                android.util.Log.d("LocationHelper", "Starting reverse geocoding for: " + 
                    location.latitude + ", " + location.longitude);
                
                // Check if geocoder is available
                if (!Geocoder.isPresent()) {
                    android.util.Log.e("LocationHelper", "Geocoder not available on this device");
                    mainHandler.post(() -> 
                        callback.onAddressError("Geocoding service not available on this device"));
                    return;
                }
                
                List<Address> addresses = geocoder.getFromLocation(
                        location.latitude, location.longitude, 1);
                        
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i <= addr.getMaxAddressLineIndex(); i++) {
                        sb.append(addr.getAddressLine(i));
                        if (i < addr.getMaxAddressLineIndex()) sb.append(", ");
                    }
                    String address = sb.toString();
                    android.util.Log.d("LocationHelper", "Reverse geocoding successful: " + address);
                    
                    mainHandler.post(() -> callback.onAddressResult(address));
                } else {
                    android.util.Log.w("LocationHelper", "No reverse geocoding results found");
                    mainHandler.post(() -> callback.onAddressError("Address not found"));
                }
            } catch (IOException e) {
                android.util.Log.e("LocationHelper", "Reverse geocoding failed: " + e.getMessage());
                mainHandler.post(() -> 
                    callback.onAddressError("Reverse geocoding error: " + e.getMessage()));
            } catch (Exception e) {
                android.util.Log.e("LocationHelper", "Unexpected error during reverse geocoding: " + e.getMessage());
                mainHandler.post(() -> 
                    callback.onAddressError("Reverse geocoding failed: " + e.getMessage()));
            }
        }).start();
    }

    public void getAddressFromLocation(LatLng location, GeocodeCallback callback) {
        // Perform reverse geocoding in background thread to avoid blocking UI and handle timeouts
        Handler mainHandler = new Handler(Looper.getMainLooper());
        
        new Thread(() -> {
            try {
                android.util.Log.d("LocationHelper", "Starting reverse geocoding validation for: " + 
                    location.latitude + ", " + location.longitude);
                
                // Check if geocoder is available
                if (!Geocoder.isPresent()) {
                    android.util.Log.e("LocationHelper", "Geocoder not available on this device");
                    mainHandler.post(() -> 
                        callback.onGeocodeError("Geocoding service not available on this device"));
                    return;
                }
                
                List<Address> addresses = geocoder.getFromLocation(
                        location.latitude, location.longitude, 1);
                        
                if (addresses != null && !addresses.isEmpty()) {
                    android.util.Log.d("LocationHelper", "Reverse geocoding validation successful");
                    mainHandler.post(() -> callback.onGeocodeResult(location));
                } else {
                    android.util.Log.w("LocationHelper", "No reverse geocoding validation results found");
                    mainHandler.post(() -> callback.onGeocodeError("Address not found"));
                }
            } catch (IOException e) {
                android.util.Log.e("LocationHelper", "Reverse geocoding validation failed: " + e.getMessage());
                mainHandler.post(() -> 
                    callback.onGeocodeError("Reverse geocoding error: " + e.getMessage()));
            } catch (Exception e) {
                android.util.Log.e("LocationHelper", "Unexpected error during reverse geocoding validation: " + e.getMessage());
                mainHandler.post(() -> 
                    callback.onGeocodeError("Reverse geocoding failed: " + e.getMessage()));
            }
        }).start();
    }

    public static int getLocationPermissionRequestCode() {
        return LOCATION_PERMISSION_REQUEST_CODE;
    }
    
    /**
     * Check if the location appears to be outside a reasonable region for Malaysia-focused app
     * This helps detect when GPS/emulator is providing unrealistic locations (like California)
     */
    private boolean isLocationOutsideMalaysiaRegion(LatLng location) {
        // Define expanded bounds for Malaysia + neighboring regions (Southeast Asia)
        // This includes Malaysia, Singapore, parts of Thailand, Indonesia, Brunei
        double minLat = -1.0;   // Southern Indonesia
        double maxLat = 8.0;    // Northern Thailand
        double minLng = 95.0;   // Western Thailand
        double maxLng = 120.0;  // Eastern Indonesia/Philippines
        
        boolean outsideRegion = location.latitude < minLat || location.latitude > maxLat ||
                               location.longitude < minLng || location.longitude > maxLng;
                               
        if (outsideRegion) {
            android.util.Log.w("LocationHelper", String.format(
                "Location (%.6f, %.6f) is outside Southeast Asia region (lat: %.1f-%.1f, lng: %.1f-%.1f)",
                location.latitude, location.longitude, minLat, maxLat, minLng, maxLng));
        }
        
        return outsideRegion;
    }
} 