package com.example.assignment2;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

public class ChatbotManager {
    private static final String TAG = "ChatbotManager";
    
    private Context context;
    private OpenAIService openAIService;
    private SessionManager sessionManager;
    private Dialog chatDialog;
    private LinearLayout chatMessagesContainer;
    private ScrollView chatScrollView;
    private TextInputEditText chatInput;
    private MaterialButton sendButton;
    private ProgressBar loadingIndicator;
    private Handler uiHandler;
    
    public ChatbotManager(Context context) {
        this.context = context;
        this.openAIService = new OpenAIService(context);
        this.sessionManager = new SessionManager(context);
        this.uiHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Enhanced chatbot button setup with user type checking
     * Following the same pattern as the create task function
     */
    public void addChatbotButton(Activity activity) {
        // Check user type first - similar to setupAddTaskButton() pattern
        String userType = sessionManager.getUserType();
        
        // Setup chatbot button based on user type
        setupChatbotButton(activity, userType);
    }
    
    /**
     * Setup chatbot button with conditional visibility and behavior
     * Adapted from MyTasksFragment.setupAddTaskButton() pattern
     */
    private void setupChatbotButton(Activity activity, String userType) {
        // Find the bottom navigation view
        final View bottomNavigation = activity.findViewById(R.id.bottom_navigation);
        
        // Find the main activity layout
        ViewGroup activityLayout = null;
        ViewGroup fragmentContainer = activity.findViewById(R.id.fragment_container);
        if (fragmentContainer != null && fragmentContainer.getParent() instanceof ViewGroup) {
            activityLayout = (ViewGroup) fragmentContainer.getParent();
        } else {
            activityLayout = activity.findViewById(android.R.id.content);
        }
        
        final ViewGroup finalActivityLayout = activityLayout;
        
        // Create floating action button with conditional styling
        final FloatingActionButton fabChatbot = new FloatingActionButton(context);
        
        // Configure chatbot button based on user type (like create task function)
        if ("Hirer".equals(userType)) {
            // For Hirers - AI Assistant for task management
            fabChatbot.setImageResource(android.R.drawable.ic_dialog_email);
            fabChatbot.setContentDescription("Task Management Assistant");
            
            // Show chatbot button for hirers
            addChatbotWithOverlay(fabChatbot, bottomNavigation, finalActivityLayout);
            
            // Set click listener with hirer-specific context
            fabChatbot.setOnClickListener(v -> {
                Intent intent = new Intent(context, ChatbotActivity.class);
                intent.putExtra("USER_TYPE", "Hirer");
                intent.putExtra("CHATBOT_MODE", "TASK_MANAGEMENT");
                activity.startActivity(intent);
            });
            
        } else {
            // For Workers - AI Assistant for finding and completing tasks
            fabChatbot.setImageResource(android.R.drawable.ic_dialog_email);
            fabChatbot.setContentDescription("Task Assistant");
            
            // Show chatbot button for workers
            addChatbotWithOverlay(fabChatbot, bottomNavigation, finalActivityLayout);
            
            // Set click listener with worker-specific context
            fabChatbot.setOnClickListener(v -> {
                Intent intent = new Intent(context, ChatbotActivity.class);
                intent.putExtra("USER_TYPE", "Worker");
                intent.putExtra("CHATBOT_MODE", "TASK_FINDING");
                activity.startActivity(intent);
            });
        }
        
        // Optional: Add long click for additional functionality (like in create task)
        fabChatbot.setOnLongClickListener(v -> {
            showChatbotOptionsDialog(activity, userType);
            return true;
        });
    }
    
    /**
     * Show chatbot options dialog on long press
     * Similar to task filtering in MyTasksFragment
     */
    private void showChatbotOptionsDialog(Activity activity, String userType) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle("AI Assistant Options");
        
        String[] options;
        if ("Hirer".equals(userType)) {
            options = new String[]{
                "Task Management Tips",
                "Hirer Best Practices", 
                "Platform Support",
                "Analytics Insights"
            };
        } else {
            options = new String[]{
                "Find Suitable Tasks",
                "Task Completion Tips",
                "Worker Guidelines",
                "General Support"
            };
        }
        
        builder.setItems(options, (dialog, which) -> {
            Intent intent = new Intent(context, ChatbotActivity.class);
            intent.putExtra("USER_TYPE", userType);
            intent.putExtra("PRESET_MESSAGE", options[which]);
            activity.startActivity(intent);
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void addChatbotWithOverlay(FloatingActionButton fabChatbot, View bottomNavigation, ViewGroup parentLayout) {
        // Convert dp to pixels
        float density = context.getResources().getDisplayMetrics().density;
        int marginDp = 16;
        int marginPx = (int) (marginDp * density);
        
        // Create a wrapper FrameLayout to hold the chatbot button
        android.widget.FrameLayout wrapper = new android.widget.FrameLayout(context);
        
        // Add the FAB to the wrapper
        android.widget.FrameLayout.LayoutParams fabParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        );
        fabParams.gravity = android.view.Gravity.END | android.view.Gravity.BOTTOM;
        fabParams.setMargins(0, 0, marginPx, marginPx);
        wrapper.addView(fabChatbot, fabParams);
        
        // Add wrapper to parent layout
        ViewGroup.LayoutParams wrapperParams;
        
        if (parentLayout instanceof android.widget.LinearLayout) {
            // For LinearLayout, add as last child but position above bottom nav
            wrapperParams = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    0 // We'll adjust this
            );
            ((android.widget.LinearLayout.LayoutParams) wrapperParams).weight = 0;
            
            // Position the wrapper just before the bottom navigation
            int bottomNavIndex = -1;
            if (bottomNavigation != null) {
                for (int i = 0; i < parentLayout.getChildCount(); i++) {
                    if (parentLayout.getChildAt(i) == bottomNavigation) {
                        bottomNavIndex = i;
                        break;
                    }
                }
            }
            
            // Set wrapper height to accommodate the FAB
            int fabSize = (int) (56 * density); // Standard FAB size
            ((android.widget.LinearLayout.LayoutParams) wrapperParams).height = fabSize + marginPx;
            
            if (bottomNavIndex != -1) {
                parentLayout.addView(wrapper, bottomNavIndex, wrapperParams);
            } else {
                parentLayout.addView(wrapper, wrapperParams);
            }
            
        } else {
            // For other layouts, use standard approach
            wrapperParams = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            parentLayout.addView(wrapper, wrapperParams);
            
            // Adjust position if there's bottom navigation
            if (bottomNavigation != null) {
                bottomNavigation.post(() -> {
                    int bottomNavHeight = bottomNavigation.getHeight();
                    if (bottomNavHeight > 0) {
                        fabParams.setMargins(0, 0, marginPx, marginPx + bottomNavHeight);
                        fabChatbot.setLayoutParams(fabParams);
                    }
                });
            }
        }
    }
    
    private void showChatDialog() {
        if (chatDialog != null && chatDialog.isShowing()) {
            return;
        }
        
        // Create dialog using the same approach as Task dialog
        chatDialog = new Dialog(context);
        chatDialog.setContentView(R.layout.dialog_chatbot);
        
        // Match Task dialog window sizing - fullscreen approach
        WindowManager.LayoutParams layoutParams = chatDialog.getWindow().getAttributes();
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        chatDialog.getWindow().setAttributes(layoutParams);
        
        // Initialize views
        chatMessagesContainer = chatDialog.findViewById(R.id.chatMessagesContainer);
        chatScrollView = chatDialog.findViewById(R.id.chatScrollView);
        chatInput = chatDialog.findViewById(R.id.chatInput);
        sendButton = chatDialog.findViewById(R.id.btnSendMessage);
        loadingIndicator = chatDialog.findViewById(R.id.loadingIndicator);
        ImageView closeButton = chatDialog.findViewById(R.id.btnCloseChat);
        
        // Set up listeners
        sendButton.setOnClickListener(v -> sendMessage());
        closeButton.setOnClickListener(v -> chatDialog.dismiss());
        
        // Handle enter key in input
        chatInput.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
        
        chatDialog.show();
    }
    
    private void sendMessage() {
        String message = chatInput.getText().toString().trim();
        if (message.isEmpty()) {
            return;
        }
        
        // Add user message to chat
        addMessageToChat(message, true);
        
        // Clear input and show loading
        chatInput.setText("");
        showLoading(true);
        sendButton.setEnabled(false);
        
        // Send to OpenAI
        openAIService.sendMessage(message, new OpenAIService.ChatCallback() {
            @Override
            public void onSuccess(String response) {
                uiHandler.post(() -> {
                    addMessageToChat(response, false);
                    showLoading(false);
                    sendButton.setEnabled(true);
                });
            }
            
            @Override
            public void onError(String error) {
                uiHandler.post(() -> {
                    addMessageToChat("Sorry, I encountered an error: " + error, false);
                    showLoading(false);
                    sendButton.setEnabled(true);
                    Toast.makeText(context, "Error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void addMessageToChat(String message, boolean isUser) {
        TextView messageView = new TextView(context);
        messageView.setText(message);
        messageView.setTextSize(14);
        messageView.setPadding(24, 16, 24, 16);
        
        // Apply background color as text color to match chatbot page background
        messageView.setTextColor(context.getResources().getColor(R.color.purple_dark_bg));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(16, 8, 16, 8);
        
        if (isUser) {
            params.gravity = Gravity.END;
            messageView.setBackgroundResource(R.drawable.chat_bubble_user);
        } else {
            params.gravity = Gravity.START;
            messageView.setBackgroundResource(R.drawable.chat_bubble_ai);
        }
        
        chatMessagesContainer.addView(messageView, params);
        
        // Scroll to bottom
        chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
    }
    
    private void showLoading(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
} 