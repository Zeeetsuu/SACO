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


public class LoginScreen extends AppCompatActivity {

    private static final String TAG = "LoginScreen";
    private FirebaseAuth mAuth;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView signUpLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);

        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.loginScreenInputEmail);
        passwordEditText = findViewById(R.id.loginScreenInputPassword);
        loginButton = findViewById(R.id.loginScreenButton);
        signUpLink = findViewById(R.id.loginScreenText5);

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginScreen.this, R.string.enterbothemailandpass,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(LoginScreen.this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signIn success");

                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(LoginScreen.this, R.string.authenticationsuccess,
                                    Toast.LENGTH_SHORT).show();

                            if (user != null) {
                                String uid = user.getUid();
                                Log.d(TAG, "User logged in with UID: " + uid);
                                Intent intent = new Intent(LoginScreen.this, HomeScreen.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Log.w(TAG, "signIn error: User is null");
                            }


                        } else {
                            // Sign in failed
                            Log.w(TAG, "signIn failure", task.getException());
                            String errorMessage = "Authentication failed.";
                            if (task.getException() != null) {
                                errorMessage += " " + task.getException().getMessage();
                            }
                            Toast.makeText(LoginScreen.this, errorMessage,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        signUpLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginScreen.this, SignUpScreen.class);
            startActivity(intent);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
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
