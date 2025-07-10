package com.example.assignment2;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RouteHelper {
    private static final String TAG = "RouteHelper";
    private final Context context;
    private final OkHttpClient client;
    private final Gson gson;

    // Constants for validation
    private static final double MIN_LATITUDE = -90.0;
    private static final double MAX_LATITUDE = 90.0;
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;
    private static final double MAX_ROUTE_DISTANCE_KM = 1000.0; // Max 1000km for reasonable routing

    public interface RouteCallback {
        void onRouteCalculated(List<LatLng> routePoints, String distance, String duration);
        void onRouteError(String error);
    }

    public RouteHelper(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    public void calculateRoute(LatLng origin, LatLng destination, RouteCallback callback) {
        Log.d(TAG, "calculateRoute called with origin: " + origin + ", destination: " + destination);
        
        // Validate coordinates first
        String validationError = validateCoordinates(origin, destination);
        if (validationError != null) {
            Log.e(TAG, "Coordinate validation failed: " + validationError);
            callback.onRouteError(validationError);
            return;
        }
        
        // Check if locations are too far apart
        double distance = calculateDistance(origin, destination);
        Log.d(TAG, "Distance between points: " + distance + " km");
        
        if (distance > MAX_ROUTE_DISTANCE_KM) {
            String error = "Locations are too far apart for routing (" + Math.round(distance) + " km)";
            Log.e(TAG, error);
            callback.onRouteError(error);
            return;
        }
        
        if (distance < 0.01) { // Less than 10 meters
            String error = "Origin and destination are too close (same location)";
            Log.e(TAG, error);
            callback.onRouteError(error);
            return;
        }
        
        String apiKey = context.getString(R.string.google_maps_key);
        Log.d(TAG, "API Key length: " + (apiKey != null ? apiKey.length() : 0));
        
        if (apiKey == null || apiKey.isEmpty()) {
            Log.e(TAG, "Google Maps API key is null or empty");
            callback.onRouteError("Google Maps API key not configured");
            return;
        }

        // Add travel mode and other parameters for better routing
        String url = String.format(
            "https://maps.googleapis.com/maps/api/directions/json?origin=%f,%f&destination=%f,%f&mode=driving&alternatives=false&avoid=tolls&key=%s",
            origin.latitude, origin.longitude,
            destination.latitude, destination.longitude,
            apiKey
        );
        
        Log.d(TAG, "Request URL: " + url.replace(apiKey, "***API_KEY***"));

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Route calculation failed", e);
                callback.onRouteError("Failed to calculate route: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "Response code: " + response.code());
                
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    Log.e(TAG, "Route API error: " + response.code() + ", Body: " + errorBody);
                    callback.onRouteError("Route API error: " + response.code());
                    return;
                }

                String responseBody = response.body().string();
                Log.d(TAG, "Response body length: " + responseBody.length());
                Log.d(TAG, "Response body (first 500 chars): " + 
                      (responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody));
                
                try {
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    
                    String status = jsonResponse.get("status").getAsString();
                    Log.d(TAG, "API response status: " + status);
                    
                    if (!status.equals("OK")) {
                        String errorMessage = getDetailedErrorMessage(status, jsonResponse);
                        Log.e(TAG, errorMessage);
                        callback.onRouteError(errorMessage);
                        return;
                    }

                    JsonArray routes = jsonResponse.getAsJsonArray("routes");
                    if (routes.size() == 0) {
                        Log.e(TAG, "No routes found in response");
                        callback.onRouteError("No routes found between these locations");
                        return;
                    }

                    JsonObject route = routes.get(0).getAsJsonObject();
                    JsonArray legs = route.getAsJsonArray("legs");
                    JsonObject leg = legs.get(0).getAsJsonObject();

                    String distance = leg.getAsJsonObject("distance").get("text").getAsString();
                    String duration = leg.getAsJsonObject("duration").get("text").getAsString();
                    
                    Log.d(TAG, "Route found - Distance: " + distance + ", Duration: " + duration);

                    // Decode polyline
                    String encodedPolyline = route.getAsJsonObject("overview_polyline").get("points").getAsString();
                    List<LatLng> routePoints = decodePolyline(encodedPolyline);
                    
                    Log.d(TAG, "Route points decoded: " + routePoints.size() + " points");

                    callback.onRouteCalculated(routePoints, distance, duration);

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing route response", e);
                    callback.onRouteError("Error parsing route data: " + e.getMessage());
                }
            }
        });
    }

    public void drawRouteOnMap(GoogleMap googleMap, List<LatLng> routePoints) {
        if (googleMap == null || routePoints == null || routePoints.isEmpty()) {
            Log.w(TAG, "Cannot draw route - invalid parameters");
            return;
        }

        // Clear any existing polylines first
        googleMap.clear();
        
        // Re-add markers (they were cleared above)
        // Note: The calling activity should re-add markers after this method

        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(routePoints)
                .width(8f)
                .color(Color.BLUE)
                .geodesic(true);

        googleMap.addPolyline(polylineOptions);
        Log.d(TAG, "Route drawn on map with " + routePoints.size() + " points");
    }

    public void clearRouteFromMap(GoogleMap googleMap) {
        if (googleMap != null) {
            googleMap.clear();
            Log.d(TAG, "Route cleared from map");
        }
    }

    public boolean isApiKeyConfigured() {
        String apiKey = context.getString(R.string.google_maps_key);
        boolean configured = apiKey != null && !apiKey.isEmpty() && !apiKey.equals("YOUR_API_KEY_HERE");
        Log.d(TAG, "API key configured: " + configured + " (length: " + (apiKey != null ? apiKey.length() : 0) + ")");
        return configured;
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
    
    private String validateCoordinates(LatLng origin, LatLng destination) {
        if (origin == null) {
            return "Origin location is not available";
        }
        if (destination == null) {
            return "Destination location is not available";
        }
        
        // Check latitude bounds
        if (origin.latitude < MIN_LATITUDE || origin.latitude > MAX_LATITUDE) {
            return "Origin latitude is invalid: " + origin.latitude;
        }
        if (destination.latitude < MIN_LATITUDE || destination.latitude > MAX_LATITUDE) {
            return "Destination latitude is invalid: " + destination.latitude;
        }
        
        // Check longitude bounds
        if (origin.longitude < MIN_LONGITUDE || origin.longitude > MAX_LONGITUDE) {
            return "Origin longitude is invalid: " + origin.longitude;
        }
        if (destination.longitude < MIN_LONGITUDE || destination.longitude > MAX_LONGITUDE) {
            return "Destination longitude is invalid: " + destination.longitude;
        }
        
        // Check for NaN or infinite values
        if (Double.isNaN(origin.latitude) || Double.isNaN(origin.longitude) ||
            Double.isInfinite(origin.latitude) || Double.isInfinite(origin.longitude)) {
            return "Origin coordinates contain invalid values";
        }
        if (Double.isNaN(destination.latitude) || Double.isNaN(destination.longitude) ||
            Double.isInfinite(destination.latitude) || Double.isInfinite(destination.longitude)) {
            return "Destination coordinates contain invalid values";
        }
        
        return null; // All validations passed
    }
    
    private double calculateDistance(LatLng origin, LatLng destination) {
        // Haversine formula to calculate distance between two points
        double lat1Rad = Math.toRadians(origin.latitude);
        double lat2Rad = Math.toRadians(destination.latitude);
        double deltaLatRad = Math.toRadians(destination.latitude - origin.latitude);
        double deltaLngRad = Math.toRadians(destination.longitude - origin.longitude);
        
        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLngRad / 2) * Math.sin(deltaLngRad / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return 6371.0 * c; // Earth's radius in kilometers
    }
    
    private String getDetailedErrorMessage(String status, JsonObject jsonResponse) {
        String baseMessage = "Route calculation failed";
        String details = "";
        
        switch (status) {
            case "ZERO_RESULTS":
                details = "No route found between these locations. This may happen if:\n" +
                         "• Locations are on different continents\n" +
                         "• One location is inaccessible by road\n" +
                         "• Locations are in areas without road networks";
                break;
            case "OVER_QUERY_LIMIT":
                details = "API quota exceeded. Please try again later.";
                break;
            case "REQUEST_DENIED":
                details = "API access denied. Please check API key permissions.";
                break;
            case "INVALID_REQUEST":
                details = "Invalid request parameters.";
                break;
            case "UNKNOWN_ERROR":
                details = "Server error occurred. Please try again.";
                break;
            default:
                details = status;
                break;
        }
        
        if (jsonResponse.has("error_message")) {
            details += "\nAPI Message: " + jsonResponse.get("error_message").getAsString();
        }
        
        return baseMessage + ": " + details;
    }
} 