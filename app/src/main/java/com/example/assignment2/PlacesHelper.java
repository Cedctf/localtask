package com.example.assignment2;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlacesHelper {
    private static final String TAG = "PlacesHelper";
    private Context context;
    private PlacesClient placesClient;
    private AutocompleteSessionToken sessionToken;
    
    // Malaysia bounds for focused search results
    private static final LatLng MALAYSIA_SOUTHWEST = new LatLng(0.855, 99.643);
    private static final LatLng MALAYSIA_NORTHEAST = new LatLng(7.363, 119.267);
    private static final RectangularBounds MALAYSIA_BOUNDS = RectangularBounds.newInstance(
        MALAYSIA_SOUTHWEST, MALAYSIA_NORTHEAST);

    public interface PlaceSearchCallback {
        void onPlacesFound(List<PlaceResult> places);
        void onPlaceSearchError(String error);
    }

    public interface PlaceDetailsCallback {
        void onPlaceDetailsReceived(PlaceResult place);
        void onPlaceDetailsError(String error);
    }

    public static class PlaceResult {
        private String placeId;
        private String name;
        private String address;
        private LatLng location;
        private String types;

        public PlaceResult(String placeId, String name, String address, LatLng location, String types) {
            this.placeId = placeId;
            this.name = name;
            this.address = address;
            this.location = location;
            this.types = types;
        }

        // Getters
        public String getPlaceId() { return placeId; }
        public String getName() { return name; }
        public String getAddress() { return address; }
        public LatLng getLocation() { return location; }
        public String getTypes() { return types; }

        public String getDisplayName() {
            return name != null && !name.isEmpty() ? name : address;
        }

        public String getFullDescription() {
            if (name != null && !name.isEmpty() && address != null && !address.isEmpty()) {
                return name + ", " + address;
            }
            return name != null && !name.isEmpty() ? name : address;
        }
    }

    public PlacesHelper(Context context) {
        this.context = context;
        
        // Initialize Places API with your API key
        if (!Places.isInitialized()) {
            String apiKey = context.getString(R.string.google_maps_key);
            if (apiKey == null || apiKey.isEmpty()) {
                Log.e(TAG, "Google Maps API key not found in resources");
                Toast.makeText(context, "Places API not configured properly", Toast.LENGTH_LONG).show();
                return;
            }
            Places.initialize(context, apiKey);
            Log.d(TAG, "Places API initialized successfully");
        }
        
        placesClient = Places.createClient(context);
        sessionToken = AutocompleteSessionToken.newInstance();
    }

    public void searchPlaces(String query, PlaceSearchCallback callback) {
        if (placesClient == null) {
            callback.onPlaceSearchError("Places API not initialized");
            return;
        }

        if (query == null || query.trim().isEmpty()) {
            callback.onPlacesFound(new ArrayList<>());
            return;
        }

        // Check network connectivity before making API call
        if (!isNetworkAvailable()) {
            callback.onPlaceSearchError("No internet connection. Please check your network settings.");
            return;
        }

        Log.d(TAG, "Searching for places: " + query);

        // Create autocomplete request focused on Malaysia
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
            .setLocationBias(MALAYSIA_BOUNDS)
            .setCountries(Arrays.asList("MY")) // Malaysia only
            .setSessionToken(sessionToken)
            .setQuery(query)
            .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener((response) -> {
            List<PlaceResult> results = new ArrayList<>();
            
            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                PlaceResult place = new PlaceResult(
                    prediction.getPlaceId(),
                    prediction.getPrimaryText(null).toString(),
                    prediction.getSecondaryText(null).toString(),
                    null, // Location will be fetched when needed
                    prediction.getPlaceTypes() != null ? prediction.getPlaceTypes().toString() : ""
                );
                results.add(place);
            }
            
            Log.d(TAG, "Found " + results.size() + " places for query: " + query);
            callback.onPlacesFound(results);
            
        }).addOnFailureListener((exception) -> {
            Log.e(TAG, "Places search failed", exception);
            
            // Provide user-friendly error messages
            String errorMessage;
            if (exception.getMessage() != null && exception.getMessage().contains("UnknownHostException")) {
                errorMessage = "No internet connection. Please check your network settings.";
            } else if (exception.getMessage() != null && exception.getMessage().contains("API_KEY")) {
                errorMessage = "Places API configuration error. Please check API key.";
            } else {
                errorMessage = "Place search temporarily unavailable. You can still enter addresses manually.";
            }
            
            callback.onPlaceSearchError(errorMessage);
        });
    }

    public void getPlaceDetails(String placeId, PlaceDetailsCallback callback) {
        if (placesClient == null) {
            callback.onPlaceDetailsError("Places API not initialized");
            return;
        }

        Log.d(TAG, "Fetching details for place ID: " + placeId);

        // Specify the fields to return
        List<Place.Field> placeFields = Arrays.asList(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG,
            Place.Field.TYPES
        );

        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeFields);

        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            Place place = response.getPlace();
            
            PlaceResult result = new PlaceResult(
                place.getId(),
                place.getName(),
                place.getAddress(),
                place.getLatLng(),
                place.getTypes() != null ? place.getTypes().toString() : ""
            );
            
            Log.d(TAG, "Place details received: " + result.getName() + " at " + 
                result.getLocation().latitude + ", " + result.getLocation().longitude);
            
            callback.onPlaceDetailsReceived(result);
            
        }).addOnFailureListener((exception) -> {
            Log.e(TAG, "Place details fetch failed", exception);
            callback.onPlaceDetailsError("Failed to get place details: " + exception.getMessage());
        });
    }

    public void getPlaceDetails(PlaceResult placeResult, PlaceDetailsCallback callback) {
        getPlaceDetails(placeResult.getPlaceId(), callback);
    }

    // Helper method to check if Places API is properly configured
    public boolean isPlacesApiConfigured() {
        String apiKey = context.getString(R.string.google_maps_key);
        return apiKey != null && !apiKey.isEmpty() && placesClient != null;
    }

    // Helper method to check network connectivity
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    // Refresh session token (call this when user makes a selection)
    public void refreshSessionToken() {
        sessionToken = AutocompleteSessionToken.newInstance();
        Log.d(TAG, "Session token refreshed");
    }

    // Cleanup method
    public void cleanup() {
        // Places client doesn't need explicit cleanup, but we can null the reference
        placesClient = null;
        sessionToken = null;
    }
} 