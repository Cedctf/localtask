package com.example.assignment2;

import android.util.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class PasswordUtils {
    
    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    // Generates a random salt
    private static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.encodeToString(salt, Base64.DEFAULT);
    }

    // Hashes the password with the given salt
    private static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(Base64.decode(salt, Base64.DEFAULT));
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.encodeToString(hashedPassword, Base64.DEFAULT);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    // Hashes a password with a new random salt
    // Returns a string in format: salt:hashedPassword
    public static String hashPassword(String password) {
        String salt = generateSalt();
        String hashedPassword = hashPassword(password, salt);
        return salt + ":" + hashedPassword;
    }

    //Verifies a password against a stored hash
    public static boolean verifyPassword(String password, String storedHash) {
        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 2) {
                return false;
            }
            
            String salt = parts[0];
            String storedPasswordHash = parts[1];
            String inputPasswordHash = hashPassword(password, salt);
            
            return storedPasswordHash.equals(inputPasswordHash);
        } catch (Exception e) {
            return false;
        }
    }
} 