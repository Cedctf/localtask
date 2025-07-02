package com.example.assignment2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
        TextInputEditText titleInput = dialogView.findViewById(R.id.taskTitleInput);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.taskDescriptionInput);
        TextInputEditText paymentInput = dialogView.findViewById(R.id.taskPaymentInput);
        TextInputEditText dateInput = dialogView.findViewById(R.id.taskDateInput);
        TextInputEditText locationInput = dialogView.findViewById(R.id.taskLocationInput);

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
                })
                .setNegativeButton("Cancel", null)
                .show();
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