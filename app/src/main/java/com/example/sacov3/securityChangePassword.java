package com.example.sacov3;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;


public class securityChangePassword extends AppCompatActivity {

    private static final String TAG = "ChangePasswordActivity"; // Tag for logging

    private FirebaseAuth mAuth;
    private EditText currentPasswordEditText;
    private EditText newPasswordEditText;
    private EditText confirmPasswordEditText;
    private Button changePasswordButton;
    private TextView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_change_password);

        // Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        currentPasswordEditText = findViewById(R.id.securityCurrentPassword);
        newPasswordEditText = findViewById(R.id.securityNewPassword);
        confirmPasswordEditText = findViewById(R.id.securityConfirmPassword);
        changePasswordButton = findViewById(R.id.securityChangePasswordButton);
        backButton = findViewById(R.id.securityChangePasswordBack);

        // Back button
        backButton.setOnClickListener(v -> {
            finish();
        });

        // Change Password button
        changePasswordButton.setOnClickListener(v -> {
            String currentPassword = currentPasswordEditText.getText().toString().trim();
            String newPassword = newPasswordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            // Input Validation
            if (TextUtils.isEmpty(currentPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "New passwords do not match.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Basic password length check
            if (newPassword.length() < 6) {
                Toast.makeText(this, "New password must be at least 6 characters long.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get the current user
            FirebaseUser user = mAuth.getCurrentUser();

            // Re-authentication and password update
            if (user != null) {
                String email = user.getEmail();

                if (email == null) {
                    Toast.makeText(this, "Cannot change password without a valid email.", Toast.LENGTH_SHORT).show();
                    return;
                }

                AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);

                // Re-authenticate the user
                user.reauthenticate(credential)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "User re-authenticated.");

                                // Update the password
                                user.updatePassword(newPassword)
                                        .addOnCompleteListener(this, updateTask -> {
                                            if (updateTask.isSuccessful()) {
                                                Log.d(TAG, "User password updated.");
                                                Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                                                // Optionally, finish this activity after success
                                                finish();
                                            } else {
                                                // Password update failure
                                                Log.w(TAG, "Error updating password.", updateTask.getException());
                                                String errorMessage = "Failed to update password.";
                                                if (updateTask.getException() != null) {
                                                    errorMessage += " " + updateTask.getException().getMessage();
                                                }
                                                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                                            }
                                        });

                            } else {
                                // Re-authentication failure
                                Log.w(TAG, "Error re-authenticating user.", task.getException());
                                String errorMessage = "Re-authentication failed. Please check your current password.";
                                if (task.getException() instanceof FirebaseAuthRecentLoginRequiredException) {
                                    errorMessage = "Please log in again before changing your password.";
                                } else if (task.getException() != null) {
                                    errorMessage = "Failed to verify current password. " + task.getException().getMessage();
                                }
                                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();


                                 if (task.getException() instanceof FirebaseAuthRecentLoginRequiredException) {
                                     Intent intent = new Intent(this, LoginScreen.class);
                                     intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                     startActivity(intent);
                                     finishAffinity();
                                 }
                            }
                        });


            } else {
                Log.w(TAG, "Attempted password change while not logged in.");
                Toast.makeText(this, "You must be logged in to change password.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginScreen.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(this, LoginScreen.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}
