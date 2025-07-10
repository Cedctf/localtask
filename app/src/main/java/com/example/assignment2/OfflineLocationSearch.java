package com.example.assignment2;

import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OfflineLocationSearch {
    
    public static class LocationResult {
        private String name;
        private String address;
        private LatLng location;
        private String category;
        
        public LocationResult(String name, String address, LatLng location, String category) {
            this.name = name;
            this.address = address;
            this.location = location;
            this.category = category;
        }
        
        public String getName() { return name; }
        public String getAddress() { return address; }
        public LatLng getLocation() { return location; }
        public String getCategory() { return category; }
    }
    
    private static final Map<String, LocationResult> COMMON_LOCATIONS = new HashMap<>();
    
    static {
        // Universities
        COMMON_LOCATIONS.put("taylor's university", new LocationResult(
            "Taylor's University", "1, Jln Taylors, 47500 Subang Jaya, Selangor", 
            new LatLng(3.065, 101.6036), "University"));
        
        COMMON_LOCATIONS.put("taylors", new LocationResult(
            "Taylor's University", "1, Jln Taylors, 47500 Subang Jaya, Selangor", 
            new LatLng(3.065, 101.6036), "University"));
            
        COMMON_LOCATIONS.put("sunway university", new LocationResult(
            "Sunway University", "Jalan Universiti, Bandar Sunway, 47500 Petaling Jaya, Selangor", 
            new LatLng(3.0667, 101.6020), "University"));
            
        COMMON_LOCATIONS.put("um", new LocationResult(
            "University of Malaya", "Jalan Universiti, 50603 Kuala Lumpur", 
            new LatLng(3.1215, 101.6536), "University"));
            
        COMMON_LOCATIONS.put("upm", new LocationResult(
            "Universiti Putra Malaysia", "43400 Serdang, Selangor", 
            new LatLng(2.9762, 101.7164), "University"));
        
        // Shopping Centers
        COMMON_LOCATIONS.put("klcc", new LocationResult(
            "KLCC", "Kuala Lumpur City Centre, 50088 Kuala Lumpur", 
            new LatLng(3.1570, 101.7118), "Shopping Mall"));
            
        COMMON_LOCATIONS.put("sunway pyramid", new LocationResult(
            "Sunway Pyramid", "3, Jalan PJS 11/15, Bandar Sunway, 47500 Petaling Jaya, Selangor", 
            new LatLng(3.0733, 101.6067), "Shopping Mall"));
            
        COMMON_LOCATIONS.put("mid valley", new LocationResult(
            "Mid Valley Megamall", "Lingkaran Syed Putra, Mid Valley City, 59200 Kuala Lumpur", 
            new LatLng(3.1181, 101.6776), "Shopping Mall"));
            
        COMMON_LOCATIONS.put("pavilion", new LocationResult(
            "Pavilion KL", "168, Jalan Bukit Bintang, Bukit Bintang, 55100 Kuala Lumpur", 
            new LatLng(3.1495, 101.7134), "Shopping Mall"));
            
        COMMON_LOCATIONS.put("ioi city mall", new LocationResult(
            "IOI City Mall", "Lebuh IRC, IOI Resort City, 62502 Putrajaya, Selangor", 
            new LatLng(2.9634, 101.7195), "Shopping Mall"));
        
        // Transportation Hubs
        COMMON_LOCATIONS.put("kl sentral", new LocationResult(
            "KL Sentral", "Jalan Tun Sambanthan, Brickfields, 50470 Kuala Lumpur", 
            new LatLng(3.1337, 101.6862), "Transport Hub"));
            
        COMMON_LOCATIONS.put("klia", new LocationResult(
            "KLIA", "Kuala Lumpur International Airport, 64000 KLIA, Selangor", 
            new LatLng(2.7456, 101.7072), "Airport"));
            
        COMMON_LOCATIONS.put("klia2", new LocationResult(
            "KLIA2", "Kuala Lumpur International Airport 2, 43900 Sepang, Selangor", 
            new LatLng(2.7344, 101.6998), "Airport"));
        
        // Government/Business Areas
        COMMON_LOCATIONS.put("putrajaya", new LocationResult(
            "Putrajaya", "Federal Territory of Putrajaya", 
            new LatLng(2.9264, 101.6964), "Government Area"));
            
        COMMON_LOCATIONS.put("cyberjaya", new LocationResult(
            "Cyberjaya", "63000 Cyberjaya, Selangor", 
            new LatLng(2.9213, 101.6559), "Technology Hub"));
        
        // Popular Areas
        COMMON_LOCATIONS.put("bukit bintang", new LocationResult(
            "Bukit Bintang", "Bukit Bintang, Kuala Lumpur", 
            new LatLng(3.1477, 101.7108), "Entertainment District"));
            
        COMMON_LOCATIONS.put("petaling jaya", new LocationResult(
            "Petaling Jaya", "Petaling Jaya, Selangor", 
            new LatLng(3.1073, 101.6420), "City"));
            
        COMMON_LOCATIONS.put("subang jaya", new LocationResult(
            "Subang Jaya", "Subang Jaya, Selangor", 
            new LatLng(3.0504, 101.5820), "City"));
            
        COMMON_LOCATIONS.put("shah alam", new LocationResult(
            "Shah Alam", "Shah Alam, Selangor", 
            new LatLng(3.0733, 101.5185), "City"));
        
        // Common Food Places
        COMMON_LOCATIONS.put("starbucks klcc", new LocationResult(
            "Starbucks KLCC", "Level G, Suria KLCC, Kuala Lumpur City Centre", 
            new LatLng(3.1570, 101.7118), "Cafe"));
            
        COMMON_LOCATIONS.put("mamak", new LocationResult(
            "Mamak Restaurant", "Common Malaysian restaurant", 
            new LatLng(3.1390, 101.6869), "Restaurant"));
    }
    
    public static List<LocationResult> searchLocations(String query) {
        List<LocationResult> results = new ArrayList<>();
        
        if (query == null || query.trim().isEmpty()) {
            return results;
        }
        
        String lowerQuery = query.toLowerCase().trim();
        
        // Exact matches first
        if (COMMON_LOCATIONS.containsKey(lowerQuery)) {
            results.add(COMMON_LOCATIONS.get(lowerQuery));
        }
        
        // Partial matches
        for (Map.Entry<String, LocationResult> entry : COMMON_LOCATIONS.entrySet()) {
            String key = entry.getKey();
            LocationResult location = entry.getValue();
            
            // Skip if already added as exact match
            if (key.equals(lowerQuery)) {
                continue;
            }
            
            // Check if query matches key or location name
            if (key.contains(lowerQuery) || 
                location.getName().toLowerCase().contains(lowerQuery) ||
                location.getAddress().toLowerCase().contains(lowerQuery)) {
                results.add(location);
            }
        }
        
        // Limit results to prevent overwhelming UI
        if (results.size() > 5) {
            results = results.subList(0, 5);
        }
        
        return results;
    }
    
    public static boolean hasOfflineResult(String query) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }
        
        String lowerQuery = query.toLowerCase().trim();
        
        // Check exact match
        if (COMMON_LOCATIONS.containsKey(lowerQuery)) {
            return true;
        }
        
        // Check partial matches
        for (String key : COMMON_LOCATIONS.keySet()) {
            if (key.contains(lowerQuery)) {
                return true;
            }
        }
        
        return false;
    }
} 