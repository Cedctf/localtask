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
     * Using the FAB already defined in the layout - no more layout interference!
     */
    public void addChatbotButton(Activity activity) {
        // Check user type first
        String userType = sessionManager.getUserType();
        
        // Setup chatbot button based on user type
        setupChatbotButton(activity, userType);
    }
    
    /**
     * Setup chatbot button with conditional visibility and behavior
     * Now uses the existing FAB in layout - truly floating with no layout impact
     */
    private void setupChatbotButton(Activity activity, String userType) {
        // Find the existing FAB in the layout
        final FloatingActionButton fabChatbot = activity.findViewById(R.id.fab_chatbot);
        
        if (fabChatbot == null) {
            return; // FAB not found in layout
        }
        
        // Configure chatbot button based on user type
        if ("Hirer".equals(userType)) {
            // For Hirers - AI Assistant for task management
            fabChatbot.setImageResource(android.R.drawable.ic_dialog_email);
            fabChatbot.setContentDescription("Task Management Assistant");
            
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
            
            // Set click listener with worker-specific context
            fabChatbot.setOnClickListener(v -> {
                Intent intent = new Intent(context, ChatbotActivity.class);
                intent.putExtra("USER_TYPE", "Worker");
                intent.putExtra("CHATBOT_MODE", "TASK_FINDING");
                activity.startActivity(intent);
            });
        }
        
        // Add long click for additional functionality
        fabChatbot.setOnLongClickListener(v -> {
            showChatbotOptionsDialog(activity, userType);
            return true;
        });
        
        // Make the FAB visible (it's initially hidden in layout)
        fabChatbot.setVisibility(View.VISIBLE);
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