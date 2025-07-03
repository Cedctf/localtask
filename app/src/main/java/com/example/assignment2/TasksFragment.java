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

public class TasksFragment extends Fragment {
    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private TasksFragmentAdapter adapter;
    private SessionManager sessionManager;
    private FloatingActionButton fabAddTask;
    private List<Task> taskList;
    private LocationHelper locationHelper;

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
        return inflater.inflate(R.layout.fragment_tasks, container, false);
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
        taskList = new ArrayList<>();

        // Show/hide FAB based on user type
        if ("Hirer".equals(sessionManager.getUserType())) {
            fabAddTask.setVisibility(View.VISIBLE);
            fabAddTask.setOnClickListener(v -> showAddTaskDialog());
        } else {
            fabAddTask.setVisibility(View.GONE);
        }

        // Setup RecyclerView
        setupRecyclerView();
        loadTasks();
    }

    private void setupRecyclerView() {
        adapter = new TasksFragmentAdapter(taskList);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadTasks() {
        db.collection("tasks")
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    taskList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Task task = document.toObject(Task.class);
                        task.setId(document.getId());
                        taskList.add(task);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(requireContext(), "Error loading tasks: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show());
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
                googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
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
                    }
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
                (view, selectedYear, selectedMonth, selectedDay) -> {
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

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add New Task")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
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
                            sessionManager.getUserName(),
                            sessionManager.getUserName(),
                            payment,
                            dueDate,
                            location
                    );

                    db.collection("tasks")
                            .add(newTask)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(requireContext(), "Task added successfully", Toast.LENGTH_SHORT).show();
                                loadTasks();
                            })
                            .addOnFailureListener(e -> 
                                Toast.makeText(requireContext(), "Error adding task: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show());
                    
                    // Clean up map
                    if (mapView != null) {
                        mapView.onDestroy();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Clean up map
                    if (mapView != null) {
                        mapView.onDestroy();
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

        TasksFragmentAdapter(List<Task> tasks) {
            this.tasks = tasks;
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
            holder.taskStatus.setText(task.getStatus());
            holder.taskHirer.setText("By: " + task.getHirerName());
            
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
            holder.taskPayment.setText(format.format(task.getPayment()));

            // Set date and location
            holder.taskDate.setText(task.getDueDate() != null ? task.getDueDate() : "No date");
            holder.taskLocation.setText(task.getLocation() != null ? task.getLocation() : "No location");

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), TaskDetailsActivity.class);
                intent.putExtra("task_id", task.getId());
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