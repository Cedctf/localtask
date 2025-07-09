package com.example.assignment2;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
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
        this.uiHandler = new Handler(Looper.getMainLooper());
    }
    
    public void addChatbotButton(Activity activity) {
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
        
        // Create floating action button
        final FloatingActionButton fabChatbot = new FloatingActionButton(context);
        fabChatbot.setImageResource(android.R.drawable.ic_dialog_email);
        fabChatbot.setContentDescription("AI Assistant");
        
        // Add button using a simple overlay approach
        if (finalActivityLayout != null) {
            addChatbotWithOverlay(fabChatbot, bottomNavigation, finalActivityLayout);
        }
        
        // Set click listener
        fabChatbot.setOnClickListener(v -> showChatDialog());
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
        
        // Create dialog
        chatDialog = new Dialog(context);
        chatDialog.setContentView(R.layout.dialog_chatbot);
        
        // Make dialog fullscreen on smaller screens, or 80% on larger screens
        WindowManager.LayoutParams layoutParams = chatDialog.getWindow().getAttributes();
        layoutParams.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.9);
        layoutParams.height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.8);
        chatDialog.getWindow().setAttributes(layoutParams);
        
        // Initialize views
        chatMessagesContainer = chatDialog.findViewById(R.id.chatMessagesContainer);
        chatScrollView = chatDialog.findViewById(R.id.chatScrollView);
        chatInput = chatDialog.findViewById(R.id.chatInput);
        sendButton = chatDialog.findViewById(R.id.btnSendMessage);
        loadingIndicator = chatDialog.findViewById(R.id.loadingIndicator);
        ImageButton closeButton = chatDialog.findViewById(R.id.btnCloseChat);
        
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
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        
        if (isUser) {
            messageView.setBackgroundResource(R.drawable.chat_bubble_user);
            params.gravity = Gravity.END;
            messageView.setMaxWidth((int) (context.getResources().getDisplayMetrics().widthPixels * 0.7));
        } else {
            messageView.setBackgroundResource(R.drawable.chat_bubble_ai);
            params.gravity = Gravity.START;
            messageView.setMaxWidth((int) (context.getResources().getDisplayMetrics().widthPixels * 0.75));
        }
        
        messageView.setLayoutParams(params);
        chatMessagesContainer.addView(messageView);
        
        // Scroll to bottom
        chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
    }
    
    private void showLoading(boolean show) {
        loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
    }
} 