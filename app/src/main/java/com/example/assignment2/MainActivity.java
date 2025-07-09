package com.example.assignment2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private EditText etName, etEmail, etPhone, etPassword, etConfirmPassword;
    private Spinner spinnerUserType;
    private Button btnRegister;
    private TextView tvLogin;
    private String userType = "User"; // Default
    private FirebaseFirestore db;
    private ChatbotManager chatbotManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configure status bar
        StatusBarHelper.configureStatusBar(this);

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize chatbot
        chatbotManager = new ChatbotManager(this);
        chatbotManager.addChatbotButton(this);

        // Initialize views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spinnerUserType = findViewById(R.id.spinnerUserType);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        // Setup spinner
        setupUserTypeSpinner();

        // Register button
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        // Login text click
        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to LoginActivity
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Close registration activity
            }
        });
    }

    private void setupUserTypeSpinner() {
        // Create array of options
        String[] userTypes = {"User", "Hirer"};
        
        // Create adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, 
            R.layout.spinner_item, 
            userTypes
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        
        // Set adapter to spinner
        spinnerUserType.setAdapter(adapter);
        
        // Set default selection
        spinnerUserType.setSelection(0); // User is default
        
        // Set listener
        spinnerUserType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                userType = userTypes[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                userType = "User"; // Default
            }
        });
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Simple validation
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save to Firebase based on user type
        if (userType.equals("User")) {
            saveUserToFirestore(name, email, phone, password);
        } else {
            saveHirerToFirestore(name, email, phone, password);
        }
    }

    private void saveUserToFirestore(String name, String email, String phone, String password) {
        User user = new User(name, email, phone, password, userType);
        
        db.collection("users")
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "User added with ID: " + documentReference.getId());
                    Toast.makeText(MainActivity.this, "User registered successfully!", 
                            Toast.LENGTH_LONG).show();
                    clearFields();
                    
                    // Navigate to login after successful registration
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding user", e);
                    Toast.makeText(MainActivity.this, "Registration failed: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                });
    }

    private void saveHirerToFirestore(String name, String email, String phone, String password) {
        Hirer hirer = new Hirer(name, email, phone, password, userType);
        
        db.collection("hirers")
                .add(hirer)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Hirer added with ID: " + documentReference.getId());
                    Toast.makeText(MainActivity.this, "Hirer registered successfully!", 
                            Toast.LENGTH_LONG).show();
                    clearFields();
                    
                    // Navigate to login after successful registration
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding hirer", e);
                    Toast.makeText(MainActivity.this, "Registration failed: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                });
    }

    private void clearFields() {
        etName.setText("");
        etEmail.setText("");
        etPhone.setText("");
        etPassword.setText("");
        etConfirmPassword.setText("");
    }
}
