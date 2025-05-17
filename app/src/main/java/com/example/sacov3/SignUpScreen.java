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
    private static final String TAG = "SignUpScreen";
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private EditText emailEditText;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button signUpButton;
    private TextView loginLinkText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_screen);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        emailEditText = findViewById(R.id.signUpScreenInputEmail);
        usernameEditText = findViewById(R.id.signUpScreenUsernameInput);
        passwordEditText = findViewById(R.id.signUpScreenInputPassword);
        confirmPasswordEditText = findViewById(R.id.signUpScreenConfirmPassword);
        signUpButton = findViewById(R.id.signUpScreenButton);
        loginLinkText = findViewById(R.id.signUpScreenText7);

        signUpButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(SignUpScreen.this, R.string.required,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirmPassword)) {
                Toast.makeText(SignUpScreen.this, R.string.passwordnotmatch,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(SignUpScreen.this, R.string.passwordminimallength,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(SignUpScreen.this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(SignUpScreen.this, "Sign up successful!",
                                    Toast.LENGTH_SHORT).show();

                            if (user != null) {
                                String uid = user.getUid();
                                Log.d(TAG, "New user created with UID: " + uid);
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("username", username);
                                userData.put("email", email);

                                mDatabase.child("users").child(uid).setValue(userData)
                                        .addOnSuccessListener(aVoid -> Log.d(TAG, "User data saved to RTDB for " + uid))
                                        .addOnFailureListener(e -> Log.w(TAG, "Failed to save user data to RTDB for " + uid, e));

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
                                Log.w(TAG, "createUserWithEmail:success but null user");
                                Intent intent = new Intent(SignUpScreen.this, HomeScreen.class);
                                startActivity(intent);
                                finish();
                            }


                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            String errorMessage = "Sign up failed.";
                            if (task.getException() != null) {
                                errorMessage += " " + task.getException().getMessage();
                            }
                            Toast.makeText(SignUpScreen.this, errorMessage,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        loginLinkText.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpScreen.this, LoginScreen.class);
            startActivity(intent);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
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
