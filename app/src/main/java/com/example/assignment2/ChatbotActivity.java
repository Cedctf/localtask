package com.example.assignment2;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ChatbotActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView chatRecyclerView;
    private TextInputEditText chatInput;
    private MaterialButton sendButton;
    private MaterialButton btnFindTasks, btnTaskTips, btnSupport;
    
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    
    // User context variables (adapted from create task function pattern)
    private String userType;
    private String chatbotMode;
    private String presetMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        // Get user context from intent (like create task function)
        getUserContextFromIntent();
        
        initializeViews();
        setupRecyclerView();
        setupListeners();
        initializeUserSpecificChat();
    }
    
    /**
     * Get user context from intent, similar to how create task function handles user types
     */
    private void getUserContextFromIntent() {
        userType = getIntent().getStringExtra("USER_TYPE");
        chatbotMode = getIntent().getStringExtra("CHATBOT_MODE");
        presetMessage = getIntent().getStringExtra("PRESET_MESSAGE");
        
        // Default to Worker if not specified
        if (userType == null) {
            userType = "Worker";
        }
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatInput = findViewById(R.id.chatInput);
        sendButton = findViewById(R.id.sendButton);
        btnFindTasks = findViewById(R.id.btnFindTasks);
        btnTaskTips = findViewById(R.id.btnTaskTips);
        btnSupport = findViewById(R.id.btnSupport);
    }

    private void setupRecyclerView() {
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages, this);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    /**
     * Setup listeners with user-specific context
     * Adapted from create task function's conditional behavior pattern
     */
    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        sendButton.setOnClickListener(v -> sendMessage());
        
        // Configure quick action buttons based on user type (like create task function)
        if ("Hirer".equals(userType)) {
            // Hirer-specific quick actions
            btnFindTasks.setText("Manage Tasks");
            btnFindTasks.setOnClickListener(v -> sendQuickAction("Help me manage my posted tasks effectively"));
            
            btnTaskTips.setText("Hirer Tips");
            btnTaskTips.setOnClickListener(v -> sendQuickAction("Give me tips for writing effective task descriptions and attracting quality workers"));
            
            btnSupport.setText("Analytics");
            btnSupport.setOnClickListener(v -> sendQuickAction("Help me understand my task performance and how to improve completion rates"));
            
        } else {
            // Worker-specific quick actions
            btnFindTasks.setText("Find Tasks");
            btnFindTasks.setOnClickListener(v -> sendQuickAction("Help me find suitable tasks based on my skills and location"));
            
            btnTaskTips.setText("Work Tips");
            btnTaskTips.setOnClickListener(v -> sendQuickAction("Give me tips for completing tasks successfully and building a good reputation"));
            
            btnSupport.setText("Support");
            btnSupport.setOnClickListener(v -> sendQuickAction("I need help with using the app and understanding how LocalTask works"));
        }
        
        // Send message on enter key
        chatInput.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    /**
     * Initialize chat with user-specific welcome message
     * Following the pattern of conditional behavior like in create task function
     */
    private void initializeUserSpecificChat() {
        // Add personalized welcome message based on user type
        if ("Hirer".equals(userType)) {
            addBotMessage("Hello! I'm your LocalTask Assistant for Hirers. I can help you manage your tasks, write better job descriptions, and optimize your hiring process.");
            addBotMessage("As a task poster, I can assist you with:");
            addBotMessage("• Creating effective task descriptions\n• Managing worker applications\n• Setting appropriate pricing\n• Tracking task progress\n• Understanding platform analytics");
        } else {
            addBotMessage("Hello! I'm your LocalTask Assistant for Workers. I can help you find suitable tasks, improve your work quality, and build your reputation.");
            addBotMessage("As a task worker, I can assist you with:");
            addBotMessage("• Finding tasks that match your skills\n• Completing tasks efficiently\n• Building a strong profile\n• Understanding payment processes\n• Communicating with hirers");
        }
        
        // If there's a preset message, send it automatically
        if (presetMessage != null && !presetMessage.isEmpty()) {
            sendQuickAction(presetMessage);
        }
    }

    /**
     * Enhanced sendMessage with user context
     */
    private void sendMessage() {
        String message = chatInput.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            return;
        }

        // Add user context to the message for better AI responses
        String contextualMessage = enhanceMessageWithContext(message);
        
        addUserMessage(message);
        chatInput.setText("");

        // Send contextual message to OpenAI
        OpenAIService openAIService = new OpenAIService(this);
        openAIService.sendMessage(contextualMessage, new OpenAIService.ChatCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> addBotMessage(response));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> addBotMessage("Sorry, I encountered an error. Please try again."));
            }
        });
    }

    /**
     * Enhanced sendQuickAction with user context
     */
    private void sendQuickAction(String action) {
        String contextualAction = enhanceMessageWithContext(action);
        
        addUserMessage(action);

        OpenAIService openAIService = new OpenAIService(this);
        openAIService.sendMessage(contextualAction, new OpenAIService.ChatCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> addBotMessage(response));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> addBotMessage("Sorry, I encountered an error. Please try again."));
            }
        });
    }
    
    /**
     * Enhance user messages with context, similar to how create task function adds context
     */
    private String enhanceMessageWithContext(String message) {
        StringBuilder contextualMessage = new StringBuilder();
        
        // Add user type context
        if ("Hirer".equals(userType)) {
            contextualMessage.append("I am a task poster/hirer on LocalTask. ");
        } else {
            contextualMessage.append("I am a task worker on LocalTask. ");
        }
        
        // Add chatbot mode context if available
        if (chatbotMode != null) {
            if ("TASK_MANAGEMENT".equals(chatbotMode)) {
                contextualMessage.append("I need help with managing my posted tasks. ");
            } else if ("TASK_FINDING".equals(chatbotMode)) {
                contextualMessage.append("I need help with finding and completing tasks. ");
            }
        }
        
        contextualMessage.append("Here's my question: ").append(message);
        
        return contextualMessage.toString();
    }

    private void addUserMessage(String message) {
        ChatMessage chatMessage = new ChatMessage(message, true);
        chatMessages.add(chatMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    private void addBotMessage(String message) {
        ChatMessage chatMessage = new ChatMessage(message, false);
        chatMessages.add(chatMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }
    
    // Inner class for chat message
    public static class ChatMessage {
        private String message;
        private boolean isUser;

        public ChatMessage(String message, boolean isUser) {
            this.message = message;
            this.isUser = isUser;
        }

        public String getMessage() {
            return message;
        }

        public boolean isUser() {
            return isUser;
        }
    }
} 