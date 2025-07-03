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
        // Find the root layout
        ViewGroup rootLayout = activity.findViewById(android.R.id.content);
        
        // Create floating action button
        FloatingActionButton fabChatbot = new FloatingActionButton(context);
        fabChatbot.setImageResource(android.R.drawable.ic_dialog_email);
        fabChatbot.setContentDescription("AI Assistant");
        
        // Set position to bottom right
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 32, 100); // right and bottom margins
        
        // Add to root layout
        if (rootLayout instanceof ViewGroup) {
            ((ViewGroup) rootLayout).addView(fabChatbot, params);
            
            // Position at bottom right
            fabChatbot.post(() -> {
                int[] location = new int[2];
                rootLayout.getLocationOnScreen(location);
                
                fabChatbot.setX(rootLayout.getWidth() - fabChatbot.getWidth() - 32);
                fabChatbot.setY(rootLayout.getHeight() - fabChatbot.getHeight() - 100);
            });
        }
        
        // Set click listener
        fabChatbot.setOnClickListener(v -> showChatDialog());
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