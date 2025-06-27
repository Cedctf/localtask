package com.example.assignment2;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etPassword, etConfirmPassword;
    private Button btnUser, btnHirer, btnRegister;
    private String userType = "User"; // Default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnUser = findViewById(R.id.btnUser);
        btnHirer = findViewById(R.id.btnHirer);
        btnRegister = findViewById(R.id.btnRegister);

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

        // Registration successful
        Toast.makeText(this, "Registered successfully as " + userType,
                Toast.LENGTH_LONG).show();

        // Here you would save the data or send to server
        // For now, just show success message
    }
}
