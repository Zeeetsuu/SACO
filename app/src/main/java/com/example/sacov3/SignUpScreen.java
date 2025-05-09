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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;


public class SignUpScreen extends AppCompatActivity {
    private static final String TAG = "SignUpScreen"; // Tag for logging messages
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private EditText emailEditText;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button signUpButton;
    private TextView loginLinkTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_screen);

        // Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Firebase Realtime Database
        mDatabase = FirebaseDatabase.getInstance().getReference();

        emailEditText = findViewById(R.id.signUpScreenInputEmail);
        usernameEditText = findViewById(R.id.signUpScreenUsernameInput);
        passwordEditText = findViewById(R.id.signUpScreenInputPassword);
        confirmPasswordEditText = findViewById(R.id.signUpScreenConfirmPassword);
        signUpButton = findViewById(R.id.signUpScreenButton);
        loginLinkTextView = findViewById(R.id.signUpScreenText7);

        // Optional: If you have a TextView to show error/status messages
        // statusTextView = findViewById(R.id.statusTextView); // Example ID - uncomment and replace if you have one


        //  Sign up button click listener
        signUpButton.setOnClickListener(v -> {
            // Get input values from the EditText fields
            String email = emailEditText.getText().toString().trim();
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            // Input validation
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(SignUpScreen.this, "All fields are required.",
                        Toast.LENGTH_SHORT).show();
                // Optional: update status TextView
                // if (statusTextView != null) statusTextView.setText("All fields are required.");
                return;
            }

            // Check if password and confirm password match
            if (!password.equals(confirmPassword)) {
                Toast.makeText(SignUpScreen.this, "Passwords do not match.",
                        Toast.LENGTH_SHORT).show();
                // Optional: update status TextView
                // if (statusTextView != null) statusTextView.setText("Passwords do not match.");
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(SignUpScreen.this, "Password must be at least 6 characters long.",
                        Toast.LENGTH_SHORT).show();
                // Optional: update status TextView
                // if (statusTextView != null) statusTextView.setText("Password must be at least 6 characters long.");
                return;
            }


            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(SignUpScreen.this, task -> {
                        if (task.isSuccessful()) {
                            // Sign up success!
                            Log.d(TAG, "createUserWithEmail:success");

                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(SignUpScreen.this, "Sign up successful!",
                                    Toast.LENGTH_SHORT).show();

                            // Optional: update status TextView
                            // if (statusTextView != null) statusTextView.setText("Sign up successful!");


                            // get UID from firebase
                            if (user != null) {
                                String uid = user.getUid();
                                Log.d(TAG, "New user created with UID: " + uid);
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("username", username); // Use the username from the input field
                                userData.put("email", email); // Save email if needed

                                mDatabase.child("users").child(uid).setValue(userData)
                                        .addOnSuccessListener(aVoid -> Log.d(TAG, "User data saved to RTDB for " + uid))
                                        .addOnFailureListener(e -> Log.w(TAG, "Failed to save user data to RTDB for " + uid, e));

                                // Set display name in firebase auth
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(username)
                                        .build();

                                user.updateProfile(profileUpdates)
                                        .addOnCompleteListener(profileTask -> {
                                            if (profileTask.isSuccessful()) {
                                                Log.d(TAG, "User profile updated (display name set in Auth).");
                                                Intent intent = new Intent(SignUpScreen.this, HomeScreen.class);
                                                startActivity(intent);
                                                finish();

                                            } else {
                                                Log.w(TAG, "Error updating user profile (Auth).", profileTask.getException());
                                                Intent intent = new Intent(SignUpScreen.this, HomeScreen.class);
                                                startActivity(intent);
                                                finish();
                                            }
                                        });


                            } else {
                                Log.w(TAG, "createUserWithEmail:success but user is null?");
                                Intent intent = new Intent(SignUpScreen.this, HomeScreen.class);
                                startActivity(intent);
                                finish();
                            }


                        } else {
                            // sign up failed section
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            String errorMessage = "Sign up failed.";
                            if (task.getException() != null) {
                                errorMessage += " " + task.getException().getMessage();
                            }
                            Toast.makeText(SignUpScreen.this, errorMessage,
                                    Toast.LENGTH_SHORT).show();
                            // Optional: update status TextView
                            // if (statusTextView != null) statusTextView.setText(errorMessage);

                            // You can check task.getException() for specific error types (e.g., FirebaseAuthUserCollisionException for email already in use)
                        }
                    });
        });

        loginLinkTextView.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpScreen.this, HomeScreen.class);
            startActivity(intent);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) on start of the activity
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            Log.d(TAG, "onStart: User already signed in with UID " + currentUser.getUid());
            Intent intent = new Intent(SignUpScreen.this, HomeScreen.class);
            startActivity(intent);
            finish();
        } else {
            Log.d(TAG, "onStart: No user signed in");
        }
    }
}
