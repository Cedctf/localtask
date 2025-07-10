package com.example.assignment2;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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

public class MyTasksFragment extends Fragment {
    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private MyTasksAdapter adapter;
    private SessionManager sessionManager;
    private List<Task> taskList;
    private List<String> taskIdList;
    private LinearLayout emptyStateLayout;
    private TextView subtitleText;
    private FloatingActionButton fabAddTask;
    private LocationHelper locationHelper;
    private SearchView searchViewMyTasks;
    private ImageButton btnFilterMyTasks;
    private FilterManager filterManager;
    private TextView filterBadgeMyTasks;
    
    // Search and filter state
    private String currentSearchQuery = "";
    private FilterManager.FilterCriteria currentFilterCriteria = null;
    private List<Task> originalTaskList = new ArrayList<>();
    private List<String> originalTaskIdList = new ArrayList<>();

    // Map variables for dialog
    private MapView mapView;
    private GoogleMap googleMap;
    private LatLng selectedLocation;
    private String selectedAddress;

    public static MyTasksFragment newInstance() {
        return new MyTasksFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_tasks, container, false);
        
        // Initialize Firebase and session manager
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(requireContext());
        locationHelper = new LocationHelper(requireContext());
        taskList = new ArrayList<>();
        taskIdList = new ArrayList<>();
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        recyclerView = view.findViewById(R.id.myTasksRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        subtitleText = view.findViewById(R.id.myTasksSubtitle);
        fabAddTask = view.findViewById(R.id.fabAddTask);
        searchViewMyTasks = view.findViewById(R.id.searchViewMyTasks);
        btnFilterMyTasks = view.findViewById(R.id.btnFilterMyTasks);
        filterBadgeMyTasks = view.findViewById(R.id.filterBadgeMyTasks);

        // Setup RecyclerView
        setupRecyclerView();
        
        // Setup search functionality
        setupSearch();
        
        // Setup FAB for hirers
        setupAddTaskButton();
        
        // Load user's assigned tasks
        loadMyAssignedTasks();
    }

    private void setupRecyclerView() {
        if (recyclerView == null) {
            Toast.makeText(requireContext(), "RecyclerView not found!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Initialize adapter with current lists
        adapter = new MyTasksAdapter(taskList, taskIdList);
        
        // Setup LinearLayoutManager
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        
        // Set adapter
        recyclerView.setAdapter(adapter);
        
        // Ensure RecyclerView can scroll properly
        recyclerView.setHasFixedSize(false);
        recyclerView.setNestedScrollingEnabled(true);
        
        // Add some debugging
        Toast.makeText(requireContext(), "RecyclerView setup complete", Toast.LENGTH_SHORT).show();
    }
    
    private void setupSearch() {
        // Initialize FilterManager for My Tasks
        filterManager = new FilterManager(requireContext(), new FilterManager.FilterCallback() {
            @Override
            public void onFiltersApplied(FilterManager.FilterCriteria criteria) {
                currentFilterCriteria = criteria;
                applyMyTasksSearchAndFilters();
            }

            @Override
            public void onFilterCountChanged(int activeFilterCount) {
                updateFilterBadge(activeFilterCount);
            }
        });
        
        // Setup filter button click
        btnFilterMyTasks.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), FilterTasksActivity.class);
            startActivityForResult(intent, 101);
        });
        
        // Enhanced search functionality for My Tasks
        searchViewMyTasks.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query.trim();
                applyMyTasksSearchAndFilters();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText.trim();
                applyMyTasksSearchAndFilters();
                return true;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 101 && resultCode == android.app.Activity.RESULT_OK) {
            if (data != null && data.getBooleanExtra("filters_applied", false)) {
                // Get filter criteria from the activity result
                FilterManager.FilterCriteria criteria = (FilterManager.FilterCriteria) data.getSerializableExtra("filter_criteria");
                if (criteria != null) {
                    currentFilterCriteria = criteria;
                    applyMyTasksSearchAndFilters();
                    
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

    private void loadMyAssignedTasks() {
        String currentUserId = sessionManager.getUserId();
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "Please login to view your tasks", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }

        String userType = sessionManager.getUserType();
        
        if ("Hirer".equals(userType)) {
            // For hirers, show tasks they have created
            loadMyCreatedTasks(currentUserId);
        } else {
            // For users, show tasks they have applied for
            loadMyAppliedTasks(currentUserId);
        }
    }

    private void loadMyAppliedTasks(String currentUserId) {
        db.collection("tasks")
                .whereEqualTo("assignedTo", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    taskList.clear();
                    taskIdList.clear();
                    
                    // Create temporary lists to sort by timestamp
                    List<Task> tempTasks = new ArrayList<>();
                    List<String> tempIds = new ArrayList<>();
                    
                    // Debug: Log the number of tasks found
                    int taskCount = queryDocumentSnapshots.size();
                    Toast.makeText(requireContext(), "Found " + taskCount + " assigned tasks", Toast.LENGTH_SHORT).show();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Task task = document.toObject(Task.class);
                        tempTasks.add(task);
                        tempIds.add(document.getId());
                    }
                    
                    // Sort by timestamp (most recent first) - use timestamp as fallback for startTime
                    for (int i = 0; i < tempTasks.size() - 1; i++) {
                        for (int j = i + 1; j < tempTasks.size(); j++) {
                            long time1 = tempTasks.get(i).getTimestamp();
                            long time2 = tempTasks.get(j).getTimestamp();
                            
                            if (time1 < time2) {
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
                    
                    // Store original data for filtering
                    originalTaskList.clear();
                    originalTaskIdList.clear();
                    originalTaskList.addAll(tempTasks);
                    originalTaskIdList.addAll(tempIds);
                    
                    // Apply current search and filters to new data
                    applyMyTasksSearchAndFilters();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error loading your tasks: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
    }

    private void loadMyCreatedTasks(String currentUserId) {
        db.collection("tasks")
                .whereEqualTo("hirerId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    taskList.clear();
                    taskIdList.clear();
                    
                    // Create temporary lists to sort by timestamp
                    List<Task> tempTasks = new ArrayList<>();
                    List<String> tempIds = new ArrayList<>();
                    
                    // Debug: Log the number of tasks found
                    int taskCount = queryDocumentSnapshots.size();
                    Toast.makeText(requireContext(), "Found " + taskCount + " created tasks", Toast.LENGTH_SHORT).show();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Task task = document.toObject(Task.class);
                        tempTasks.add(task);
                        tempIds.add(document.getId());
                    }
                    
                    // Sort by timestamp (most recent first)
                    for (int i = 0; i < tempTasks.size() - 1; i++) {
                        for (int j = i + 1; j < tempTasks.size(); j++) {
                            long time1 = tempTasks.get(i).getTimestamp();
                            long time2 = tempTasks.get(j).getTimestamp();
                            
                            if (time1 < time2) {
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
                    
                    // Store original data for filtering
                    originalTaskList.clear();
                    originalTaskIdList.clear();
                    originalTaskList.addAll(tempTasks);
                    originalTaskIdList.addAll(tempIds);
                    
                    // Apply current search and filters to new data
                    applyMyTasksSearchAndFilters();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error loading your tasks: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
    }

    /**
     * Enhanced search and filter system for My Tasks
     */
    private void applyMyTasksSearchAndFilters() {
        if (originalTaskList.isEmpty()) {
            // Update UI based on empty results
            taskList.clear();
            taskIdList.clear();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            showEmptyState();
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
        
        // Debug: Log filtered results
        String filterInfo = "";
        if (!currentSearchQuery.isEmpty()) {
            filterInfo += "Search: '" + currentSearchQuery + "' ";
        }
        if (currentFilterCriteria != null) {
            filterInfo += "Filters applied ";
        }
        
        Toast.makeText(requireContext(), 
            "Showing " + taskList.size() + " of " + originalTaskList.size() + " tasks " + filterInfo, 
            Toast.LENGTH_SHORT).show();
        
        // Ensure adapter is notified
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        
        // Update UI based on results
        if (taskList.isEmpty()) {
            showEmptyState();
        } else {
            showTasksList();
            updateSubtitle();
        }
    }
    
    /**
     * Enhanced search algorithm for My Tasks (same as TasksFragment)
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
        String status = task.getStatus().toLowerCase();
        
        // Check if all search terms are found in any combination of fields
        for (String term : searchTerms) {
            boolean termFound = false;
            
            // Direct text matching (including status for My Tasks)
            if (title.contains(term) || 
                description.contains(term) || 
                location.contains(term) || 
                hirerName.contains(term) ||
                paymentStr.contains(term) ||
                status.contains(term)) {
                termFound = true;
            }
            
            // Status keywords
            if (!termFound) {
                if ((term.equals("progress") || term.equals("active")) && status.equals("in_progress")) {
                    termFound = true;
                } else if ((term.equals("complete") || term.equals("done") || term.equals("finished")) && status.equals("completed")) {
                    termFound = true;
                } else if ((term.equals("open") || term.equals("available")) && status.equals("open")) {
                    termFound = true;
                }
            }
            
            if (!termFound) {
                return false; // All terms must be found
            }
        }
        
        return true;
    }
    
    /**
     * Check if a task matches all current filter criteria (same logic as TasksFragment)
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
        
        // Filter by area
        if (!criteria.areas.isEmpty()) {
            if (!taskMatchesArea(task, criteria.areas)) {
                return false;
            }
        }
        
        return true;
    }
    
    // Include the same filter helper methods from TasksFragment
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
    
    private boolean taskMatchesArea(Task task, List<String> areas) {
        String location = task.getLocation().toLowerCase();
        
        for (String area : areas) {
            if (location.contains(area.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Update the filter badge UI based on active filter count
     */
    private void updateFilterBadge(int activeFilterCount) {
        if (filterBadgeMyTasks == null) return;
        
        if (activeFilterCount > 0) {
            filterBadgeMyTasks.setText(String.valueOf(activeFilterCount));
            filterBadgeMyTasks.setVisibility(View.VISIBLE);
            
            // Update button appearance for active state
            btnFilterMyTasks.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.button_primary_background));
            btnFilterMyTasks.setImageResource(R.drawable.ic_filter_list_active);
            
            // Show filter summary in a long press
            btnFilterMyTasks.setOnLongClickListener(v -> {
                String summary = filterManager.getActiveFiltersSummary();
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("🎛️ Active Filters (" + activeFilterCount + ")")
                    .setMessage(summary)
                    .setPositiveButton("Clear All", (dialog, which) -> {
                        filterManager.clearAllFilters();
                        currentFilterCriteria = null;
                        applyMyTasksSearchAndFilters();
                    })
                    .setNegativeButton("Close", null)
                    .show();
                return true;
            });
        } else {
            filterBadgeMyTasks.setVisibility(View.GONE);
            
            // Reset button appearance for inactive state
            btnFilterMyTasks.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.button_primary_background));
            btnFilterMyTasks.setImageResource(R.drawable.ic_filter_list);
            
            // Remove long press listener
            btnFilterMyTasks.setOnLongClickListener(null);
        }
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
    }

    private void showTasksList() {
        emptyStateLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void updateSubtitle() {
        int taskCount = taskList.size();
        String userType = sessionManager.getUserType();
        String subtitleText;
        
        if ("Hirer".equals(userType)) {
            subtitleText = taskCount == 1 ? 
                "You have created 1 task" : 
                "You have created " + taskCount + " tasks";
        } else {
            subtitleText = taskCount == 1 ? 
                "You have 1 task in progress" : 
                "You have " + taskCount + " tasks in progress";
        }
        this.subtitleText.setText(subtitleText);
    }

    private void setupAddTaskButton() {
        String userType = sessionManager.getUserType();
        if ("Hirer".equals(userType)) {
            fabAddTask.setVisibility(View.VISIBLE);
            fabAddTask.setOnClickListener(v -> showAddTaskDialog());
        } else {
            fabAddTask.setVisibility(View.GONE);
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
            // Get current date
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            int year = calendar.get(java.util.Calendar.YEAR);
            int month = calendar.get(java.util.Calendar.MONTH);
            int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);

            // Create and show date picker
            android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                requireContext(),
                (view1, selectedYear, selectedMonth, selectedDay) -> {
                    // Format the selected date
                    String selectedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    dateInput.setText(selectedDate);
                },
                year, month, day
            );
            
            // Set minimum date to today
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
                        loadMyAssignedTasks(); // Refresh the list
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LocationHelper.getLocationPermissionRequestCode()) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Location permission granted! Getting your location...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Location permission denied. You can still enter addresses manually.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh tasks when returning to this fragment
        loadMyAssignedTasks();
    }

    // Custom RecyclerView Adapter for My Tasks
    private class MyTasksAdapter extends RecyclerView.Adapter<MyTasksAdapter.MyTaskViewHolder> {
        private List<Task> tasks;
        private List<String> taskIds;

        MyTasksAdapter(List<Task> tasks, List<String> taskIds) {
            this.tasks = tasks;
            this.taskIds = taskIds;
        }

        @NonNull
        @Override
        public MyTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task, parent, false);
            return new MyTaskViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyTaskViewHolder holder, int position) {
            if (position >= tasks.size() || position >= taskIds.size()) {
                // Safety check
                return;
            }
            
            Task task = tasks.get(position);
            
            holder.taskTitle.setText(task.getTitle());
            holder.taskDescription.setText(task.getDescription());
            
            // Show status with different styling for in_progress tasks
            String status = task.getStatus();
            if ("in_progress".equals(status)) {
                holder.taskStatus.setText("Status: In Progress ✓");
                holder.taskStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                holder.taskStatus.setText("Status: " + status);
            }
            
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
            holder.taskPayment.setText(format.format(task.getPayment()));
            
            holder.taskHirer.setText("By: " + task.getHirerName());
            holder.taskDate.setText(task.getDueDate() != null ? task.getDueDate() : "No deadline");
            holder.taskLocation.setText(task.getLocation() != null ? task.getLocation() : "No location");

            // Handle click to view task details
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), TaskDetailsActivity.class);
                intent.putExtra("task_id", taskIds.get(position));
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
            int count = Math.min(tasks.size(), taskIds.size());
            // Debug: Log item count
            if (count > 0) {
                android.util.Log.d("MyTasksAdapter", "getItemCount: " + count);
            }
            return count;
        }

        class MyTaskViewHolder extends RecyclerView.ViewHolder {
            TextView taskTitle, taskDescription, taskStatus, taskPayment, taskHirer, taskDate, taskLocation;

            MyTaskViewHolder(View itemView) {
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