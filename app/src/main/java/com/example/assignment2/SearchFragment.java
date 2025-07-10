package com.example.assignment2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchFragment extends Fragment {
    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private SearchAdapter adapter;
    private SessionManager sessionManager;
    private List<Task> taskList;
    private List<String> taskIdList;
    private List<Task> allTasksList; // Store all tasks for searching
    private List<String> allTasksIdList;
    private LinearLayout emptyStateLayout;
    private SearchView searchView;
    
    // Filter system components
    private ImageButton btnFilter;
    private TextView filterBadge;
    private FilterManager filterManager;
    
    // Search and filter state
    private String currentSearchQuery = "";
    private FilterManager.FilterCriteria currentFilterCriteria = null;
    private List<Task> originalTaskList = new ArrayList<>();
    private List<String> originalTaskIdList = new ArrayList<>();

    public static SearchFragment newInstance() {
        return new SearchFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        
        // Initialize Firebase and session manager
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(requireContext());
        taskList = new ArrayList<>();
        taskIdList = new ArrayList<>();
        allTasksList = new ArrayList<>();
        allTasksIdList = new ArrayList<>();
        originalTaskList = new ArrayList<>();
        originalTaskIdList = new ArrayList<>();
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        recyclerView = view.findViewById(R.id.searchRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        searchView = view.findViewById(R.id.searchView);
        btnFilter = view.findViewById(R.id.btnFilter);
        filterBadge = view.findViewById(R.id.filterBadge);

        // Setup RecyclerView
        setupRecyclerView();
        
        // Setup search and filter functionality
        setupSearchAndFilter();
        
        // Load all tasks
        loadAllTasks();
    }

    private void setupRecyclerView() {
        adapter = new SearchAdapter(taskList, taskIdList);
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
            startActivityForResult(intent, 101);
        });
        
        // Enhanced search functionality
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
        
        if (requestCode == 101 && resultCode == android.app.Activity.RESULT_OK) {
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

    /**
     * Enhanced search and filter system for Search Fragment
     */
    private void applySearchAndFilters() {
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
        }
    }
    
    /**
     * Enhanced search algorithm
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
            
            // Direct text matching
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
        
        // Filter by area
        if (!criteria.areas.isEmpty()) {
            if (!taskMatchesArea(task, criteria.areas)) {
                return false;
            }
        }
        
        return true;
    }
    
    // Filter helper methods
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

    private void loadAllTasks() {
        db.collection("tasks")
                .whereEqualTo("status", "open")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allTasksList.clear();
                    allTasksIdList.clear();
                    
                    // Create temporary lists to sort by timestamp
                    List<Task> tempTasks = new ArrayList<>();
                    List<String> tempIds = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Task task = document.toObject(Task.class);
                        tempTasks.add(task);
                        tempIds.add(document.getId());
                    }
                    
                    // Sort by timestamp (most recent first)
                    for (int i = 0; i < tempTasks.size() - 1; i++) {
                        for (int j = i + 1; j < tempTasks.size(); j++) {
                            if (tempTasks.get(i).getTimestamp() < tempTasks.get(j).getTimestamp()) {
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
                    
                    // Add sorted results to all tasks lists (for backward compatibility)
                    allTasksList.addAll(tempTasks);
                    allTasksIdList.addAll(tempIds);
                    
                    // Apply current search and filters to new data
                    applySearchAndFilters();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error loading tasks: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
    }

    private void showTasksList() {
        emptyStateLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh tasks when returning to this fragment
        loadAllTasks();
    }

    // Custom RecyclerView Adapter for Search
    private class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {
        private List<Task> tasks;
        private List<String> taskIds;

        SearchAdapter(List<Task> tasks, List<String> taskIds) {
            this.tasks = tasks;
            this.taskIds = taskIds;
        }

        @NonNull
        @Override
        public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task, parent, false);
            return new SearchViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
            Task task = tasks.get(position);
            
            holder.taskTitle.setText(task.getTitle());
            holder.taskDescription.setText(task.getDescription());
            holder.taskStatus.setText("Status: " + task.getStatus());
            
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
            return tasks.size();
        }

        class SearchViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView taskTitle, taskDescription, taskStatus, taskPayment, taskHirer, taskDate, taskLocation;

            SearchViewHolder(View itemView) {
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