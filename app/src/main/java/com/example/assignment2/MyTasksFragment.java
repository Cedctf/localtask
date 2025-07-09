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
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

        // Setup RecyclerView
        setupRecyclerView();
        
        // Setup FAB for hirers
        setupAddTaskButton();
        
        // Load user's assigned tasks
        loadMyAssignedTasks();
    }

    private void setupRecyclerView() {
        adapter = new MyTasksAdapter(taskList, taskIdList);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
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
                    
                    // Add sorted results to main lists
                    taskList.addAll(tempTasks);
                    taskIdList.addAll(tempIds);
                    
                    adapter.notifyDataSetChanged();
                    
                    // Update UI based on results
                    if (taskList.isEmpty()) {
                        showEmptyState();
                    } else {
                        showTasksList();
                        updateSubtitle();
                    }
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
                    
                    // Add sorted results to main lists
                    taskList.addAll(tempTasks);
                    taskIdList.addAll(tempIds);
                    
                    adapter.notifyDataSetChanged();
                    
                    // Update UI based on results
                    if (taskList.isEmpty()) {
                        showEmptyState();
                    } else {
                        showTasksList();
                        updateSubtitle();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error loading your tasks: " + e.getMessage(), 
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
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task, null);
        
        // Get all input fields with correct IDs
        TextInputEditText titleInput = dialogView.findViewById(R.id.editTextTitle);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.editTextDescription);
        TextInputEditText paymentInput = dialogView.findViewById(R.id.editTextPayment);
        TextInputEditText dateInput = dialogView.findViewById(R.id.editTextDate);
        TextInputEditText locationInput = dialogView.findViewById(R.id.editTextLocation);
        mapView = dialogView.findViewById(R.id.mapView);

        // Initialize map
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
                    // Permission not granted, that's okay - button will still be visible
                }
                
                // Set up My Location button click listener
                googleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                    @Override
                    public boolean onMyLocationButtonClick() {
                        // Get current location and update our fields
                        if (locationHelper.hasLocationPermission()) {
                            locationHelper.getCurrentLocation(new LocationHelper.LocationCallback() {
                                @Override
                                public void onLocationReceived(LatLng location, String address) {
                                    selectedLocation = location;
                                    selectedAddress = address;
                                    locationInput.setText(address);
                                    
                                    // Update marker
                                    googleMap.clear();
                                    googleMap.addMarker(new MarkerOptions()
                                            .position(location)
                                            .title("Your Current Location")
                                            .snippet(address));
                                }
                                
                                @Override
                                public void onLocationError(String error) {
                                    Toast.makeText(requireContext(), "Unable to get current location: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            // Request permission if not granted
                            locationHelper.requestLocationPermission(getActivity());
                        }
                        return false; // Return false to allow default behavior (centering map)
                    }
                });
                
                // Set default location to Taylor's University
                LatLng taylorsLocation = new LatLng(3.065, 101.6036); // Taylor's University coordinates
                selectedLocation = taylorsLocation;
                selectedAddress = "1, Jln Taylors, 47500 Subang Jaya, Selangor";
                
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(taylorsLocation, 15));
                googleMap.addMarker(new MarkerOptions()
                        .position(taylorsLocation)
                        .title("Taylor's University")
                        .snippet("Default Location"));
                locationInput.setText(selectedAddress);

                // Allow user to select location by tapping on map
                googleMap.setOnMapClickListener(latLng -> {
                    selectedLocation = latLng;
                    googleMap.clear();
                    googleMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title("Selected Location"));
                    
                    // Reverse geocode to get address
                    locationHelper.getAddressFromLocation(latLng, new LocationHelper.AddressCallback() {
                        @Override
                        public void onAddressResult(String address) {
                            selectedAddress = address;
                            locationInput.setText(address);
                        }

                        @Override
                        public void onAddressError(String error) {
                            selectedAddress = "Lat: " + String.format("%.4f", latLng.latitude) + 
                                           ", Lng: " + String.format("%.4f", latLng.longitude);
                            locationInput.setText(selectedAddress);
                        }
                    });
                });
            }
        });

        // Setup date picker
        dateInput.setOnClickListener(v -> {
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

        // Handle location input changes
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

        // Create dialog with custom styling
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();
                
        // Find the create task button in the dialog layout and set its click listener
        Button createTaskButton = dialogView.findViewById(R.id.createTaskButton);
        if (createTaskButton != null) {
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
                            dueDate,
                            location
                    );

                    db.collection("tasks")
                            .add(newTask)
                            .addOnSuccessListener(documentReference -> {
                                String newTaskId = documentReference.getId();
                                taskIdList.add(newTaskId); // Add the new ID to the list
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
        }
        
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
            return tasks.size();
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