package com.example.assignment2;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class TaskManager {
    
    public interface TaskListCallback {
        void onSuccess(List<Task> tasks);
        void onError(String error);
    }
    
    public interface TaskCountCallback {
        void onSuccess(int count);
        void onError(String error);
    }
    
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    
    /**
     * Get all tasks uploaded by a specific hirer
     */
    public static void getTasksByHirer(String hirerId, TaskListCallback callback) {
        db.collection("tasks")
                .whereEqualTo("hirerId", hirerId)
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Task> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Task task = document.toObject(Task.class);
                        tasks.add(task);
                    }
                    callback.onSuccess(tasks);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
    
    /**
     * Get count of tasks uploaded by a specific hirer
     */
    public static void getTaskCountByHirer(String hirerId, TaskCountCallback callback) {
        db.collection("tasks")
                .whereEqualTo("hirerId", hirerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> 
                    callback.onSuccess(queryDocumentSnapshots.size()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
    
    /**
     * Get tasks uploaded by a hirer with a specific status
     */
    public static void getTasksByHirerAndStatus(String hirerId, String status, TaskListCallback callback) {
        db.collection("tasks")
                .whereEqualTo("hirerId", hirerId)
                .whereEqualTo("status", status)
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Task> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Task task = document.toObject(Task.class);
                        tasks.add(task);
                    }
                    callback.onSuccess(tasks);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
    
    /**
     * Get hirer information by task ID
     */
    public static void getHirerByTaskId(String taskId, HirerCallback callback) {
        db.collection("tasks").document(taskId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Task task = documentSnapshot.toObject(Task.class);
                    if (task != null && task.getHirerId() != null) {
                        // Get hirer details from hirers collection
                        db.collection("hirers").document(task.getHirerId())
                                .get()
                                .addOnSuccessListener(hirerDoc -> {
                                    if (hirerDoc.exists()) {
                                        Hirer hirer = hirerDoc.toObject(Hirer.class);
                                        callback.onSuccess(hirer);
                                    } else {
                                        callback.onError("Hirer not found");
                                    }
                                })
                                .addOnFailureListener(e -> callback.onError(e.getMessage()));
                    } else {
                        callback.onError("Task not found or no hirer assigned");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
    
    public interface HirerCallback {
        void onSuccess(Hirer hirer);
        void onError(String error);
    }
} 