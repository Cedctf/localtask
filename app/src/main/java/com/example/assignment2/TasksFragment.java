package com.example.assignment2;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.app.AlertDialog;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SearchView;
import androidx.core.content.ContextCompat;

public class TasksFragment extends Fragment {
    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private TasksFragmentAdapter adapter;
    private SessionManager sessionManager;
    private FloatingActionButton fabAddTask;
    private List<Task> taskList;
    private List<String> taskIdList; // Store document IDs separately
    private List<Task> originalTaskList; // Store original unfiltered list
    private List<String> originalTaskIdList; // Store original IDs
    private LocationHelper locationHelper;
    private FilterManager filterManager;
    private SearchView searchView;
    private ImageButton btnFilter;
    private TextView filterBadge;
    
    // Search state
    private String currentSearchQuery = "";
    private FilterManager.FilterCriteria currentFilterCriteria = null;

    // Map variables for dialog
    private MapView mapView;
    private GoogleMap googleMap;
    private LatLng selectedLocation;
    private String selectedAddress;

    public static TasksFragment newInstance() {
        return new TasksFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);
        
        // Initialize
        db = FirebaseFirestore.getInstance();
        taskList = new ArrayList<>();
        taskIdList = new ArrayList<>();
        sessionManager = new SessionManager(requireContext());
        locationHelper = new LocationHelper(requireContext());
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(requireContext());
        locationHelper = new LocationHelper(requireContext());
        recyclerView = view.findViewById(R.id.tasksRecyclerView);
        fabAddTask = view.findViewById(R.id.fabAddTask);
        searchView = view.findViewById(R.id.searchView);
        btnFilter = view.findViewById(R.id.btnFilter);
        filterBadge = view.findViewById(R.id.filterBadge);
        taskList = new ArrayList<>();
        taskIdList = new ArrayList<>();
        originalTaskList = new ArrayList<>();
        originalTaskIdList = new ArrayList<>();

        // Show/hide FAB based on user type
        if ("Hirer".equals(sessionManager.getUserType())) {
            fabAddTask.setVisibility(View.VISIBLE);
            fabAddTask.setOnClickListener(v -> showAddTaskDialog());
            
            // Add a long click listener to toggle between all tasks and my tasks
            fabAddTask.setOnLongClickListener(v -> {
                showTaskFilterDialog();
                return true;
            });
        } else {
            fabAddTask.setVisibility(View.GONE);
        }

        // Setup RecyclerView
        setupRecyclerView();
        
        // Setup search and filter
        setupSearchAndFilter();
        
        loadTasks();
    }

    private void setupRecyclerView() {
        adapter = new TasksFragmentAdapter(taskList, taskIdList);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }
    
    private void setupSearchAndFilter() {
        // Initialize FilterManager
        filterManager = new FilterManager(requireContext(), new FilterManager.FilterCallback() {
            @Override
            public void onFiltersApplied(FilterManager.FilterCriteria criteria) {
                currentFilterCriteria = criteria;
                applySearchAndFilters();
            }

            @Override
            public void onFilterCountChanged(int activeFilterCount) {
                updateFilterBadge(activeFilterCount);
            }
        });
        
        // Setup filter button click
        btnFilter.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), FilterTasksActivity.class);
            startActivityForResult(intent, 100);
        });
        
        // Setup enhanced search functionality
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query.trim();
                applySearchAndFilters();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText.trim();
                applySearchAndFilters();
                return true;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 100 && resultCode == android.app.Activity.RESULT_OK) {
            if (data != null && data.getBooleanExtra("filters_applied", false)) {
                // Get filter criteria from the activity result
                FilterManager.FilterCriteria criteria = (FilterManager.FilterCriteria) data.getSerializableExtra("filter_criteria");
                if (criteria != null) {
                    currentFilterCriteria = criteria;
                    applySearchAndFilters();
                    
                    // Update filter badge - count active filters
                    int activeFilterCount = 0;
                    if (criteria.categories != null && !criteria.categories.isEmpty()) activeFilterCount++;
                    if (criteria.sortBy != null && !criteria.sortBy.isEmpty()) activeFilterCount++;
                    if (criteria.distance > 0) activeFilterCount++;
                    if (criteria.paymentRange != null && !criteria.paymentRange.isEmpty()) activeFilterCount++;
                    if (criteria.areas != null && !criteria.areas.isEmpty()) activeFilterCount++;
                    if (criteria.durations != null && !criteria.durations.isEmpty()) activeFilterCount++;
                    if (criteria.complexities != null && !criteria.complexities.isEmpty()) activeFilterCount++;
                    
                    updateFilterBadge(activeFilterCount);
                    
                    Toast.makeText(requireContext(), "Filters applied successfully", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void loadTasks() {
        db.collection("tasks")
                .whereEqualTo("status", "open")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    taskList.clear();
                    taskIdList.clear(); // Clear IDs as well
                    
                    // Create temporary lists to sort by timestamp
                    List<Task> tempTasks = new ArrayList<>();
                    List<String> tempIds = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Task task = document.toObject(Task.class);
                        tempTasks.add(task);
                        tempIds.add(document.getId());
                    }
                    
                    // Sort by timestamp (oldest first)
                    for (int i = 0; i < tempTasks.size() - 1; i++) {
                        for (int j = i + 1; j < tempTasks.size(); j++) {
                            if (tempTasks.get(i).getTimestamp() > tempTasks.get(j).getTimestamp()) {
                                // Swap tasks
                                Task tempTask = tempTasks.get(i);
                                String tempId = tempIds.get(i);
                                tempTasks.set(i, tempTasks.get(j));
                                tempIds.set(i, tempIds.get(j));
                                tempTasks.set(j, tempTask);
                                tempIds.set(j, tempId);
                            }
                        }
                    }
                    
                    // Add sorted results to main lists
                    taskList.addAll(tempTasks);
                    taskIdList.addAll(tempIds);
                    
                    // Store original data for filtering
                    originalTaskList.clear();
                    originalTaskIdList.clear();
                    originalTaskList.addAll(tempTasks);
                    originalTaskIdList.addAll(tempIds);
                    
                    // Apply current search and filters to new data
                    applySearchAndFilters();
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(requireContext(), "Error loading tasks: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show());
    }

    // Method to load only tasks uploaded by the current hirer
    private void loadMyTasks() {
        String currentUserId = sessionManager.getUserId();
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "Unable to load your tasks. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("tasks")
                .whereEqualTo("hirerId", currentUserId)
                .whereEqualTo("status", "open")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    taskList.clear();
                    taskIdList.clear(); // Clear IDs as well
                    
                    // Create temporary lists to sort by timestamp
                    List<Task> tempTasks = new ArrayList<>();
                    List<String> tempIds = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Task task = document.toObject(Task.class);
                        tempTasks.add(task);
                        tempIds.add(document.getId());
                    }
                    
                    // Sort by timestamp (oldest first)
                    for (int i = 0; i < tempTasks.size() - 1; i++) {
                        for (int j = i + 1; j < tempTasks.size(); j++) {
                            if (tempTasks.get(i).getTimestamp() > tempTasks.get(j).getTimestamp()) {
                                // Swap tasks
                                Task tempTask = tempTasks.get(i);
                                String tempId = tempIds.get(i);
                                tempTasks.set(i, tempTasks.get(j));
                                tempIds.set(i, tempIds.get(j));
                                tempTasks.set(j, tempTask);
                                tempIds.set(j, tempId);
                            }
                        }
                    }
                    
                    // Add sorted results to main lists
                    taskList.addAll(tempTasks);
                    taskIdList.addAll(tempIds);
                    
                    // Store original data for filtering
                    originalTaskList.clear();
                    originalTaskIdList.clear();
                    originalTaskList.addAll(tempTasks);
                    originalTaskIdList.addAll(tempIds);
                    
                    // Apply current search and filters to new data
                    applySearchAndFilters();
                    Toast.makeText(requireContext(), "Showing " + taskList.size() + " of your open tasks", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(requireContext(), "Error loading your tasks: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show());
    }

    /**
     * Enhanced search and filter system that combines both search and filters
     */
    private void applySearchAndFilters() {
        if (originalTaskList.isEmpty()) {
            return;
        }
        
        taskList.clear();
        taskIdList.clear();
        
        for (int i = 0; i < originalTaskList.size(); i++) {
            Task task = originalTaskList.get(i);
            boolean matches = true;
            
            // Apply search filter first
            if (!currentSearchQuery.isEmpty()) {
                if (!taskMatchesSearchQuery(task, currentSearchQuery)) {
                    matches = false;
                }
            }
            
            // Apply filters if search passes (or no search)
            if (matches && currentFilterCriteria != null) {
                matches = taskMatchesAllFilters(task, currentFilterCriteria);
            }
            
            if (matches) {
                taskList.add(task);
                taskIdList.add(originalTaskIdList.get(i));
            }
        }
        
        // Apply sorting if filters are active
        if (currentFilterCriteria != null && !currentFilterCriteria.sortBy.isEmpty()) {
            applySorting(currentFilterCriteria.sortBy);
        }
        
        adapter.notifyDataSetChanged();
        
        // Show search/filter results feedback
        showSearchFilterFeedback();
    }
    
    /**
     * Enhanced search algorithm with fuzzy matching and multiple field support
     */
    private boolean taskMatchesSearchQuery(Task task, String query) {
        if (query.isEmpty()) return true;
        
        String[] searchTerms = query.toLowerCase().split("\\s+");
        
        // Searchable fields
        String title = task.getTitle().toLowerCase();
        String description = task.getDescription().toLowerCase();
        String location = task.getLocation().toLowerCase();
        String hirerName = task.getHirerName().toLowerCase();
        String paymentStr = "rm" + task.getPayment();
        
        // Check if all search terms are found in any combination of fields
        for (String term : searchTerms) {
            boolean termFound = false;
            
            // Direct text matching
            if (title.contains(term) || 
                description.contains(term) || 
                location.contains(term) || 
                hirerName.contains(term) ||
                paymentStr.contains(term)) {
                termFound = true;
            }
            
            // Category keyword matching
            if (!termFound) {
                termFound = matchesCategoryKeywords(term, title, description);
            }
            
            // Payment range matching
            if (!termFound) {
                termFound = matchesPaymentKeywords(term, task.getPayment());
            }
            
            // Location area matching
            if (!termFound) {
                termFound = matchesLocationKeywords(term, location);
            }
            
            if (!termFound) {
                return false; // All terms must be found
            }
        }
        
        return true;
    }
    
    /**
     * Enhanced category keyword matching for search
     */
    private boolean matchesCategoryKeywords(String term, String title, String description) {
        // Cleaning keywords
        if (term.equals("clean") || term.equals("cleaning") || term.equals("sweep") || 
            term.equals("mop") || term.equals("vacuum") || term.equals("tidy")) {
            return title.contains("clean") || description.contains("clean") ||
                   title.contains("sweep") || description.contains("sweep") ||
                   title.contains("mop") || description.contains("mop");
        }
        
        // Tutoring keywords
        if (term.equals("tutor") || term.equals("teach") || term.equals("lesson") || 
            term.equals("study") || term.equals("homework") || term.equals("education")) {
            return title.contains("tutor") || description.contains("tutor") ||
                   title.contains("teach") || description.contains("teach") ||
                   title.contains("lesson") || description.contains("lesson");
        }
        
        // Delivery keywords
        if (term.equals("deliver") || term.equals("delivery") || term.equals("pickup") || 
            term.equals("transport") || term.equals("send") || term.equals("courier")) {
            return title.contains("deliver") || description.contains("deliver") ||
                   title.contains("pickup") || description.contains("pickup") ||
                   title.contains("transport") || description.contains("transport");
        }
        
        // Small tasks keywords
        if (term.equals("repair") || term.equals("fix") || term.equals("assembly") || 
            term.equals("help") || term.equals("assistance") || term.equals("quick")) {
            return title.contains("repair") || description.contains("repair") ||
                   title.contains("assembly") || description.contains("assembly") ||
                   title.contains("help") || description.contains("help");
        }
        
        return false;
    }
    
    /**
     * Payment keyword matching for search
     */
    private boolean matchesPaymentKeywords(String term, double payment) {
        try {
            // Check if user is searching for specific payment amounts
            if (term.startsWith("rm")) {
                double searchAmount = Double.parseDouble(term.substring(2));
                return Math.abs(payment - searchAmount) < 10; // Within RM10 range
            }
            
            // Payment range keywords
            if (term.equals("cheap") || term.equals("low")) {
                return payment <= 50;
            }
            if (term.equals("expensive") || term.equals("high")) {
                return payment >= 200;
            }
            if (term.equals("medium") || term.equals("average")) {
                return payment >= 50 && payment <= 200;
            }
        } catch (NumberFormatException e) {
            // Ignore invalid number formats
        }
        
        return false;
    }
    
    /**
     * Location keyword matching for search
     */
    private boolean matchesLocationKeywords(String term, String location) {
        // Area shortcuts
        if (term.equals("pj")) {
            return location.contains("petaling jaya");
        }
        if (term.equals("subang")) {
            return location.contains("subang");
        }
        if (term.equals("shah alam")) {
            return location.contains("shah alam");
        }
        if (term.equals("kl") || term.equals("kuala lumpur")) {
            return location.contains("kuala lumpur") || location.contains("kl");
        }
        
        return false;
    }
    
    /**
     * Show feedback about search and filter results
     */
    private void showSearchFilterFeedback() {
        String message = "";
        
        if (!currentSearchQuery.isEmpty() && currentFilterCriteria != null && 
            (!currentFilterCriteria.categories.isEmpty() || !currentFilterCriteria.paymentRange.isEmpty())) {
            message = "Found " + taskList.size() + " tasks matching \"" + currentSearchQuery + "\" with filters";
        } else if (!currentSearchQuery.isEmpty()) {
            message = "Found " + taskList.size() + " tasks matching \"" + currentSearchQuery + "\"";
        } else if (currentFilterCriteria != null && 
                  (!currentFilterCriteria.categories.isEmpty() || !currentFilterCriteria.paymentRange.isEmpty())) {
            message = "Found " + taskList.size() + " tasks with current filters";
        }
        
        if (!message.isEmpty() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Update the filter badge UI based on active filter count
     */
    private void updateFilterBadge(int activeFilterCount) {
        if (filterBadge == null) return;
        
        if (activeFilterCount > 0) {
            filterBadge.setText(String.valueOf(activeFilterCount));
            filterBadge.setVisibility(View.VISIBLE);
            
            // Update button appearance for active state
            btnFilter.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.button_primary_background));
            btnFilter.setImageResource(R.drawable.ic_filter_list_active);
            
            // Show filter summary in a long press
            btnFilter.setOnLongClickListener(v -> {
                String summary = filterManager.getActiveFiltersSummary();
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("🎛️ Active Filters (" + activeFilterCount + ")")
                    .setMessage(summary)
                    .setPositiveButton("Clear All", (dialog, which) -> {
                        filterManager.clearAllFilters();
                        currentFilterCriteria = null;
                        applySearchAndFilters();
                    })
                    .setNegativeButton("Close", null)
                    .show();
                return true;
            });
        } else {
            filterBadge.setVisibility(View.GONE);
            
            // Reset button appearance for inactive state
            btnFilter.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.button_primary_background));
            btnFilter.setImageResource(R.drawable.ic_filter_list);
            
            // Remove long press listener
            btnFilter.setOnLongClickListener(null);
        }
    }
    
    /**
     * Check if a task matches all current filter criteria
     */
    private boolean taskMatchesAllFilters(Task task, FilterManager.FilterCriteria criteria) {
        // Filter by categories (multiple selection)
        if (!criteria.categories.isEmpty()) {
            if (!taskMatchesAnyCategory(task, criteria.categories)) {
                return false;
            }
        }
        
        // Filter by payment range (single selection)
        if (!criteria.paymentRange.isEmpty()) {
            if (!taskMatchesPaymentRange(task, criteria.paymentRange)) {
                return false;
            }
        }
        
        // Filter by duration
        if (!criteria.durations.isEmpty()) {
            if (!taskMatchesDuration(task, criteria.durations)) {
                return false;
            }
        }
        
        // Filter by complexity
        if (!criteria.complexities.isEmpty()) {
            if (!taskMatchesComplexity(task, criteria.complexities)) {
                return false;
            }
        }
        
        // Filter by distance (in km)
        if (criteria.distance > 0) {
            if (!taskMatchesDistance(task, criteria.distance)) {
                return false;
            }
        }
        
        // Filter by area
        if (!criteria.areas.isEmpty()) {
            if (!taskMatchesArea(task, criteria.areas)) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean taskMatchesAnyCategory(Task task, List<String> categories) {
        String title = task.getTitle().toLowerCase();
        String description = task.getDescription().toLowerCase();
        
        for (String category : categories) {
            switch (category.toLowerCase()) {
                case "cleaning":
                    if (title.contains("clean") || description.contains("clean") ||
                        title.contains("sweep") || description.contains("sweep") ||
                        title.contains("mop") || description.contains("mop")) {
                        return true;
                    }
                    break;
                case "tutoring":
                    if (title.contains("tutor") || description.contains("tutor") ||
                        title.contains("teach") || description.contains("teach") ||
                        title.contains("lesson") || description.contains("lesson")) {
                        return true;
                    }
                    break;
                case "delivery":
                    if (title.contains("deliver") || description.contains("deliver") ||
                        title.contains("pickup") || description.contains("pickup") ||
                        title.contains("transport") || description.contains("transport")) {
                        return true;
                    }
                    break;
                case "small tasks":
                    if (title.contains("repair") || description.contains("repair") ||
                        title.contains("assembly") || description.contains("assembly") ||
                        title.contains("help") || description.contains("help")) {
                        return true;
                    }
                    break;
            }
        }
        return false;
    }
    
    private boolean taskMatchesPaymentRange(Task task, String paymentRange) {
        double payment = task.getPayment();
        
        switch (paymentRange) {
            case "10-50":
                return payment >= 10 && payment <= 50;
            case "50-100":
                return payment >= 50 && payment <= 100;
            case "100-200":
                return payment >= 100 && payment <= 200;
            case "200+":
                return payment >= 200;
            default:
                return true;
        }
    }
    
    private boolean taskMatchesDuration(Task task, List<String> durations) {
        // Since Task doesn't have duration field, we'll match based on title/description keywords
        String title = task.getTitle().toLowerCase();
        String description = task.getDescription().toLowerCase();
        
        for (String duration : durations) {
            switch (duration.toLowerCase()) {
                case "quick":
                    if (title.contains("quick") || description.contains("quick") ||
                        title.contains("short") || description.contains("short") ||
                        title.contains("fast") || description.contains("fast")) {
                        return true;
                    }
                    break;
                case "half day":
                    if (title.contains("half day") || description.contains("half day") ||
                        title.contains("morning") || description.contains("morning") ||
                        title.contains("afternoon") || description.contains("afternoon")) {
                        return true;
                    }
                    break;
                case "full day":
                    if (title.contains("full day") || description.contains("full day") ||
                        title.contains("all day") || description.contains("all day")) {
                        return true;
                    }
                    break;
                case "multi-day":
                    if (title.contains("multi") || description.contains("multi") ||
                        title.contains("several days") || description.contains("several days") ||
                        title.contains("week") || description.contains("week")) {
                        return true;
                    }
                    break;
            }
        }
        return false;
    }
    
    private boolean taskMatchesComplexity(Task task, List<String> complexities) {
        // Match based on title/description keywords
        String title = task.getTitle().toLowerCase();
        String description = task.getDescription().toLowerCase();
        
        for (String complexity : complexities) {
            switch (complexity.toLowerCase()) {
                case "beginner":
                    if (title.contains("easy") || description.contains("easy") ||
                        title.contains("simple") || description.contains("simple") ||
                        title.contains("basic") || description.contains("basic") ||
                        title.contains("beginner") || description.contains("beginner")) {
                        return true;
                    }
                    break;
                case "experience":
                    if (title.contains("experience") || description.contains("experience") ||
                        title.contains("intermediate") || description.contains("intermediate") ||
                        title.contains("skilled") || description.contains("skilled")) {
                        return true;
                    }
                    break;
                case "professional":
                    if (title.contains("professional") || description.contains("professional") ||
                        title.contains("expert") || description.contains("expert") ||
                        title.contains("advanced") || description.contains("advanced") ||
                        title.contains("certified") || description.contains("certified")) {
                        return true;
                    }
                    break;
            }
        }
        return false;
    }
    
    private boolean taskMatchesDistance(Task task, int maxDistance) {
        // Since we don't have actual distance calculation, we'll just return true for now
        // In a real implementation, you would calculate distance from user's location to task location
        return true;
    }
    
    private boolean taskMatchesArea(Task task, List<String> areas) {
        String location = task.getLocation().toLowerCase();
        
        for (String area : areas) {
            if (location.contains(area.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    private void applySorting(String sortBy) {
        switch (sortBy) {
            case "payment":
                // Sort by highest payment first
                for (int i = 0; i < taskList.size() - 1; i++) {
                    for (int j = i + 1; j < taskList.size(); j++) {
                        if (taskList.get(i).getPayment() < taskList.get(j).getPayment()) {
                            // Swap tasks and IDs
                            Task tempTask = taskList.get(i);
                            String tempId = taskIdList.get(i);
                            taskList.set(i, taskList.get(j));
                            taskIdList.set(i, taskIdList.get(j));
                            taskList.set(j, tempTask);
                            taskIdList.set(j, tempId);
                        }
                    }
                }
                break;
            case "newest":
                // Sort by newest first
                for (int i = 0; i < taskList.size() - 1; i++) {
                    for (int j = i + 1; j < taskList.size(); j++) {
                        if (taskList.get(i).getTimestamp() < taskList.get(j).getTimestamp()) {
                            // Swap tasks and IDs
                            Task tempTask = taskList.get(i);
                            String tempId = taskIdList.get(i);
                            taskList.set(i, taskList.get(j));
                            taskIdList.set(i, taskIdList.get(j));
                            taskList.set(j, tempTask);
                            taskIdList.set(j, tempId);
                        }
                    }
                }
                break;
                // Note: Distance and Rating sorting would require additional data/implementation
        }
    }

    private void showAddTaskDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task_new, null);
        
        // Get all input fields with correct IDs (same as before for backend compatibility)
        TextInputEditText titleInput = dialogView.findViewById(R.id.editTextTitle);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.editTextDescription);
        TextInputEditText paymentInput = dialogView.findViewById(R.id.editTextPayment);
        TextView dateInput = dialogView.findViewById(R.id.editTextDate);
        TextInputEditText locationInput = dialogView.findViewById(R.id.editTextLocation);
        mapView = dialogView.findViewById(R.id.mapView);
        
        // UI elements from the design
        RelativeLayout datePickerLayout = dialogView.findViewById(R.id.datePickerLayout);
        Button createTaskButton = dialogView.findViewById(R.id.createTaskButton);
        ImageView closeButton = dialogView.findViewById(R.id.closeButton);

        // Initialize map (kept hidden for new design)
        if (mapView != null) {
            mapView.onCreate(null);
            mapView.onResume();
            mapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap map) {
                    googleMap = map;
                    googleMap.getUiSettings().setZoomControlsEnabled(true);
                    googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                    
                    // Enable my location layer if permission is available
                    try {
                        googleMap.setMyLocationEnabled(true);
                    } catch (SecurityException e) {
                        // Permission not granted, that's okay
                    }
                    
                    // Set default location to Taylor's University
                    LatLng taylorsLocation = new LatLng(3.065, 101.6036);
                    selectedLocation = taylorsLocation;
                    selectedAddress = "1, Jln Taylors, 47500 Subang Jaya, Selangor";
                    
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(taylorsLocation, 15));
                    googleMap.addMarker(new MarkerOptions()
                            .position(taylorsLocation)
                            .title("Taylor's University")
                            .snippet("Default Location"));
                    locationInput.setText(selectedAddress);
                }
            });
        }

        // Setup date picker (enhanced for new design)
        datePickerLayout.setOnClickListener(v -> {
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            int year = calendar.get(java.util.Calendar.YEAR);
            int month = calendar.get(java.util.Calendar.MONTH);
            int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);

            android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Format the selected date
                    String selectedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    dateInput.setText(selectedDate);
                },
                year, month, day
            );
            
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });
        


        // Handle location input changes (simplified for new design)
        locationInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String address = s.toString().trim();
                if (!address.isEmpty() && googleMap != null) {
                    locationHelper.getLocationFromAddress(address, new LocationHelper.GeocodeCallback() {
                        @Override
                        public void onGeocodeResult(LatLng location) {
                            selectedLocation = location;
                            selectedAddress = address;
                            googleMap.clear();
                            googleMap.addMarker(new MarkerOptions()
                                    .position(location)
                                    .title(address));
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
                        }

                        @Override
                        public void onGeocodeError(String error) {
                            // Address not found, keep current location
                        }
                    });
                }
            }
        });

        // Create dialog using the new design
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();
        
        // Close button listener
        closeButton.setOnClickListener(v -> {
            if (mapView != null) {
                mapView.onDestroy();
            }
            dialog.dismiss();
        });
        
        // Create task button listener (preserving existing backend logic)
        createTaskButton.setOnClickListener(v -> {
            String title = titleInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();
            String paymentStr = paymentInput.getText().toString().trim();
            String dueDate = dateInput.getText().toString().trim();
            String location = locationInput.getText().toString().trim();

            if (title.isEmpty() || description.isEmpty() || paymentStr.isEmpty() || 
                dueDate.isEmpty() || location.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double payment;
            try {
                payment = Double.parseDouble(paymentStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid payment amount", Toast.LENGTH_SHORT).show();
                return;
            }

            Task newTask = new Task(
                    title,
                    description,
                    sessionManager.getUserId(),
                    sessionManager.getUserName(),
                    payment,
                    dueDate, // Use only date without time
                    location
            );

            db.collection("tasks")
                    .add(newTask)
                    .addOnSuccessListener(documentReference -> {
                        String newTaskId = documentReference.getId();
                        taskIdList.add(newTaskId);
                        Toast.makeText(requireContext(), "Task added successfully", Toast.LENGTH_SHORT).show();
                        loadTasks();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> 
                        Toast.makeText(requireContext(), "Error adding task: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show());
            
            // Clean up map
            if (mapView != null) {
                mapView.onDestroy();
            }
        });
        
        dialog.show();
    }

    private void showTaskFilterDialog() {
        String[] options = {"Show All Tasks", "Show My Tasks Only"};
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Filter Tasks")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        loadTasks(); // Show all tasks
                    } else {
                        loadMyTasks(); // Show only my tasks
                    }
                })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LocationHelper.getLocationPermissionRequestCode()) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Location permission granted! Getting your location...", Toast.LENGTH_SHORT).show();
                // If we have the dialog open and location input field, get current location
                if (googleMap != null) {
                    // Find the location input field from the current dialog (this would need to be stored as a class variable)
                    // For now, just show a message that they can tap the current location button
                    Toast.makeText(requireContext(), "You can now tap 'Use Current Location' button", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(requireContext(), "Location permission denied. You can still enter addresses manually.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Custom RecyclerView Adapter for Fragment
    private class TasksFragmentAdapter extends RecyclerView.Adapter<TasksFragmentAdapter.TaskViewHolder> {
        private List<Task> tasks;
        private List<String> taskIds;

        TasksFragmentAdapter(List<Task> tasks, List<String> taskIds) {
            this.tasks = tasks;
            this.taskIds = taskIds;
        }

        @NonNull
        @Override
        public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task, parent, false);
            return new TaskViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
            Task task = tasks.get(position);
            
            holder.taskTitle.setText(task.getTitle());
            holder.taskDescription.setText(task.getDescription());
            holder.taskStatus.setText("Status: " + task.getStatus());
            
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
            holder.taskPayment.setText(format.format(task.getPayment()));
            
            holder.taskHirer.setText("By: " + task.getHirerName());
            holder.taskDate.setText(task.getDueDate() != null ? task.getDueDate() : "No date");
            holder.taskLocation.setText(task.getLocation() != null ? task.getLocation() : "No location");

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), TaskDetailsActivity.class);
                intent.putExtra("task_id", taskIds.get(position)); // Pass document ID
                intent.putExtra("task_title", task.getTitle());
                intent.putExtra("task_description", task.getDescription());
                intent.putExtra("task_payment", task.getPayment());
                intent.putExtra("task_hirer", task.getHirerName());
                intent.putExtra("task_date", task.getDueDate());
                intent.putExtra("task_location", task.getLocation());
                requireContext().startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return tasks.size();
        }

        class TaskViewHolder extends RecyclerView.ViewHolder {
            TextView taskTitle, taskDescription, taskStatus, taskPayment, taskHirer, taskDate, taskLocation;

            TaskViewHolder(View itemView) {
                super(itemView);
                taskTitle = itemView.findViewById(R.id.taskTitle);
                taskDescription = itemView.findViewById(R.id.taskDescription);
                taskStatus = itemView.findViewById(R.id.taskStatus);
                taskPayment = itemView.findViewById(R.id.taskPayment);
                taskHirer = itemView.findViewById(R.id.taskHirer);
                taskDate = itemView.findViewById(R.id.taskDate);
                taskLocation = itemView.findViewById(R.id.taskLocation);
            }
        }
    }
} 