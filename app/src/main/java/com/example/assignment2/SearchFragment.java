package com.example.assignment2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        recyclerView = view.findViewById(R.id.searchRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        searchView = view.findViewById(R.id.searchView);

        // Setup RecyclerView
        setupRecyclerView();
        
        // Setup search functionality
        setupSearch();
        
        // Load all tasks
        loadAllTasks();
    }

    private void setupRecyclerView() {
        adapter = new SearchAdapter(taskList, taskIdList);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterTasks(query.trim().toLowerCase());
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterTasks(newText.trim().toLowerCase());
                return true;
            }
        });
    }

    private void filterTasks(String query) {
        taskList.clear();
        taskIdList.clear();

        if (query.isEmpty()) {
            // Show all tasks when search is empty
            taskList.addAll(allTasksList);
            taskIdList.addAll(allTasksIdList);
        } else {
            // Filter tasks based on search query
            for (int i = 0; i < allTasksList.size(); i++) {
                Task task = allTasksList.get(i);
                if (task.getTitle().toLowerCase().contains(query) ||
                    task.getDescription().toLowerCase().contains(query) ||
                    task.getLocation().toLowerCase().contains(query) ||
                    task.getHirerName().toLowerCase().contains(query)) {
                    taskList.add(task);
                    taskIdList.add(allTasksIdList.get(i));
                }
            }
        }

        adapter.notifyDataSetChanged();
        
        // Update UI based on results
        if (taskList.isEmpty()) {
            showEmptyState();
        } else {
            showTasksList();
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
                    
                    // Add sorted results to all tasks lists
                    allTasksList.addAll(tempTasks);
                    allTasksIdList.addAll(tempIds);
                    
                    // Initially show all tasks
                    taskList.clear();
                    taskIdList.clear();
                    taskList.addAll(allTasksList);
                    taskIdList.addAll(allTasksIdList);
                    
                    adapter.notifyDataSetChanged();
                    
                    if (taskList.isEmpty()) {
                        showEmptyState();
                    } else {
                        showTasksList();
                    }
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