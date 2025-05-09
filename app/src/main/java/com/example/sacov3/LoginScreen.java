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
// import com.google.firebase.database.FirebaseDatabase;

public class LoginScreen extends AppCompatActivity {

    private static final String TAG = "LoginScreen"; // Tag for logging messages
    private FirebaseAuth mAuth; // Firebase Authentication instance
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView signUpLinkTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.loginScreenInputEmail);
        passwordEditText = findViewById(R.id.loginScreenInputPassword);
        loginButton = findViewById(R.id.loginScreenButton);
        signUpLinkTextView = findViewById(R.id.loginScreenText5);

        // On click listener for login button
        loginButton.setOnClickListener(v -> {
            // Get user email and password
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            // Email and password validation
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginScreen.this, "Please enter both email and password.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Call Firebase Authentication to sign in the user
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(LoginScreen.this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");

                            // Get the signed-in user object
                            FirebaseUser user = mAuth.getCurrentUser();

                            // Display a success message
                            Toast.makeText(LoginScreen.this, "Authentication successful.",
                                    Toast.LENGTH_SHORT).show();

                            // Get the UID and potentially fetch user data from Realtime Database
                            if (user != null) {
                                String uid = user.getUid();
                                Log.d(TAG, "User logged in with UID: " + uid);
                                Intent intent = new Intent(LoginScreen.this, HomeScreen.class);
                                startActivity(intent);
                                finish();

                                // TODO: (Realtime Database Step) Example of fetching username after login:
                                // FirebaseDatabase.getInstance().getReference("users").child(uid)
                                //     .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                                //         @Override
                                //         public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot dataSnapshot) {
                                //             String username = dataSnapshot.child("username").getValue(String.class);
                                //             Log.d(TAG, "Fetched username: " + username);
                                //             // You can now use this username in your UI or pass it to the next activity
                                //         }
                                //         @Override
                                //         public void onCancelled(@NonNull com.google.firebase.database.DatabaseError databaseError) {
                                //             Log.w(TAG, "Failed to load username", databaseError.toException());
                                //         }
                                //     });

                            } else {
                                Log.w(TAG, "signInWithEmail:success but user is null?");
                            }


                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            String errorMessage = "Authentication failed.";
                            if (task.getException() != null) {
                                errorMessage += " " + task.getException().getMessage();
                            }
                            Toast.makeText(LoginScreen.this, errorMessage,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        signUpLinkTextView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginScreen.this, SignUpScreen.class);
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
            Intent intent = new Intent(LoginScreen.this, HomeScreen.class);
            startActivity(intent);
            finish();
        } else {
            Log.d(TAG, "onStart: No user signed in");
        }
    }
}
