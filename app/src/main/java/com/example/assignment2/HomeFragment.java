package com.example.assignment2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private static final String ARG_USER_NAME = "userName";
    private static final String ARG_USER_TYPE = "userType";
    
    private String userName;
    private String userType;
    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private List<Task> taskList;

    public static HomeFragment newInstance(String userName, String userType) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_NAME, userName);
        args.putString(ARG_USER_TYPE, userType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userName = getArguments().getString(ARG_USER_NAME);
            userType = getArguments().getString(ARG_USER_TYPE);
        }
        db = FirebaseFirestore.getInstance();
        taskList = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Load tasks from Firestore
        loadTasks();
        
        return view;
    }

    private void loadTasks() {
        db.collection("tasks")
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    taskList.clear();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Task taskItem = document.toObject(Task.class);
                        taskList.add(taskItem);
                    }
                    TaskAdapter adapter = new TaskAdapter(taskList);
                    recyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(getContext(), "Error loading tasks", Toast.LENGTH_SHORT).show();
                }
            });
    }
} 