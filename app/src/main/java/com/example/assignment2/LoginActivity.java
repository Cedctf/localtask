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
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText etLoginEmail, etLoginPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        // Login button click
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // Register text click
        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to MainActivity for registration
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loginUser() {
        String emailOrName = etLoginEmail.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        // Validation
        if (emailOrName.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email/name and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable login button during authentication
        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        // Check users collection first
        checkUserCollection(emailOrName, password);
    }

    private void checkUserCollection(String emailOrName, String password) {
        // First try to find by email
        db.collection("users")
                .whereEqualTo("email", emailOrName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // User found by email
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            if (user.verifyPassword(password)) {
                                loginSuccessful(user.getName(), "User", user.getEmail());
                                return;
                            }
                        }
                        loginFailed("Invalid password");
                    } else {
                        // Not found by email, try by name
                        checkUserByName(emailOrName, password);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error checking users by email", e);
                    checkUserByName(emailOrName, password);
                });
    }

    private void checkUserByName(String emailOrName, String password) {
        db.collection("users")
                .whereEqualTo("name", emailOrName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // User found by name
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            if (user.verifyPassword(password)) {
                                loginSuccessful(user.getName(), "User", user.getEmail());
                                return;
                            }
                        }
                        loginFailed("Invalid password");
                    } else {
                        // Not found in users collection, check hirers collection
                        checkHirerCollection(emailOrName, password);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error checking users by name", e);
                    checkHirerCollection(emailOrName, password);
                });
    }

    private void checkHirerCollection(String emailOrName, String password) {
        // First try to find by email
        db.collection("hirers")
                .whereEqualTo("email", emailOrName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Hirer found by email
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Hirer hirer = document.toObject(Hirer.class);
                            if (hirer.verifyPassword(password)) {
                                loginSuccessful(hirer.getName(), "Hirer", hirer.getEmail());
                                return;
                            }
                        }
                        loginFailed("Invalid password");
                    } else {
                        // Not found by email, try by name
                        checkHirerByName(emailOrName, password);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error checking hirers by email", e);
                    checkHirerByName(emailOrName, password);
                });
    }

    private void checkHirerByName(String emailOrName, String password) {
        db.collection("hirers")
                .whereEqualTo("name", emailOrName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Hirer found by name
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Hirer hirer = document.toObject(Hirer.class);
                            if (hirer.verifyPassword(password)) {
                                loginSuccessful(hirer.getName(), "Hirer", hirer.getEmail());
                                return;
                            }
                        }
                        loginFailed("Invalid password");
                    } else {
                        // User not found in either collection by email or name
                        loginFailed("User not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error checking hirers by name", e);
                    loginFailed("Login failed. Please try again.");
                });
    }

    private void loginSuccessful(String name, String userType, String email) {
        Toast.makeText(this, "Welcome back, " + name + " (" + userType + ")!", 
                Toast.LENGTH_LONG).show();
        
        Log.d(TAG, "Login successful - Name: " + name + ", Type: " + userType + ", Email: " + email);
        
        // Here you can navigate to the appropriate dashboard based on userType
        // For now, just finish the login activity
        finish();
        
        // TODO: Navigate to dashboard activity
        // Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        // intent.putExtra("USER_NAME", name);
        // intent.putExtra("USER_TYPE", userType);
        // intent.putExtra("USER_EMAIL", email);
        // startActivity(intent);
    }

    private void loginFailed(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        
        // Re-enable login button
        btnLogin.setEnabled(true);
        btnLogin.setText("Login");
        
        // Clear password field for security
        etLoginPassword.setText("");
    }
} 