package com.example.assignment2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private EditText etName, etEmail, etPhone, etPassword, etConfirmPassword;
    private Button btnUser, btnHirer, btnRegister;
    private TextView tvLogin;
    private String userType = "User"; // Default
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnUser = findViewById(R.id.btnUser);
        btnHirer = findViewById(R.id.btnHirer);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        // Set default selection
        btnUser.setEnabled(false); // Shows it's selected

        // Toggle listeners
        btnUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userType = "User";
                btnUser.setEnabled(false);
                btnHirer.setEnabled(true);
            }
        });

        btnHirer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userType = "Hirer";
                btnHirer.setEnabled(false);
                btnUser.setEnabled(true);
            }
        });

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
