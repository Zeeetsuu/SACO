package com.example.sacov3;


import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;


public class securityScreen extends AppCompatActivity {

    private static final String TAG = "SecurityScreen";
    private FirebaseAuth mAuth;

    private TextView securityEmailTextView;
    private static final String DEFAULT_EMAIL_PLACEHOLDER = "No email available";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_screen);

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Back Button
        TextView backButton = findViewById(R.id.securityBack);
        backButton.setOnClickListener(v -> finish());

        securityEmailTextView = findViewById(R.id.securityEmail);
        Button changePasswordButton = findViewById(R.id.securityChangePassword);
        Button deleteAccountButton = findViewById(R.id.securityDelete);

        // Display user's email
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            if (!TextUtils.isEmpty(userEmail)) {
                securityEmailTextView.setText(userEmail);
            } else {
                securityEmailTextView.setText(DEFAULT_EMAIL_PLACEHOLDER);
            }
        } else {
            securityEmailTextView.setText(DEFAULT_EMAIL_PLACEHOLDER);
        }

        // Change password button
        changePasswordButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, securityChangePassword.class);
            startActivity(intent);
        });

        // Delete button
        deleteAccountButton.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    // Keep user logged in even if the app is closed
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "onStart: User not logged in, redirecting to LoginScreen");
            Intent intent = new Intent(this, LoginScreen.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Log.d(TAG, "onStart: User is logged in with UID " + currentUser.getUid());
            securityEmailTextView.setText(TextUtils.isEmpty(currentUser.getEmail()) ? DEFAULT_EMAIL_PLACEHOLDER : currentUser.getEmail());
        }
    }

    /**
     * Shows a confirmation dialog before attempting to delete the user account.
     */
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account)
                .setMessage(R.string.DeleteAccountDialog)

                .setPositiveButton(R.string.DeleteAccountButton, (dialog, which) -> deleteUserAccount())
                .setNegativeButton(R.string.Cancel, (dialog, which) -> {
                })
                .show();
    }


    /**
     * Attempts to delete the currently logged-in user's account.
     * Handles re-authentication if required.
     */
    private void deleteUserAccount() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Log.w(TAG, "Attempted account deletion while not logged in.");
            Toast.makeText(this, "No user is currently logged in.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginScreen.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Attempt to delete account
        user.delete()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User account deleted successfully.");
                        Toast.makeText(this, "Your account has been deleted.", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();

                        // Redirect to the login screen
                        Intent intent = new Intent(this, LoginScreen.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finishAffinity();

                    } else {
                        // Handle deletion failure
                        Log.w(TAG, "Failed to delete user account.", task.getException());
                        if (task.getException() instanceof FirebaseAuthRecentLoginRequiredException) {
                            Toast.makeText(this, "Please re-enter your password to delete your account.", Toast.LENGTH_LONG).show();
                            showReauthenticateDialog(user);
                        } else {
                            String errorMessage = "Failed to delete account.";
                            if (task.getException() != null) {
                                errorMessage += " " + task.getException().getMessage();
                            }
                            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Shows a dialog to prompt the user to re-enter their password for re-authentication.
     */
    private void showReauthenticateDialog(FirebaseUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.verifyidentity);
        builder.setMessage(R.string.confirmdeletion);

        // Set up the input field in the dialog
        final EditText passwordInput = new EditText(this);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint(R.string.password);

        // Add padding to the EditText
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int marginPx = (int) (getResources().getDisplayMetrics().density * 20);
        params.setMargins(marginPx, 0, marginPx, 0);
        passwordInput.setLayoutParams(params);
        layout.addView(passwordInput);

        builder.setView(layout);


        // Set up the buttons for the re-authentication dialog
        builder.setPositiveButton(R.string.verify, (dialog, which) -> {
            String password = passwordInput.getText().toString().trim();

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this, R.string.requiredpassword, Toast.LENGTH_SHORT).show();
                return;
            }

            // Perform Re-authentication with the entered password
            String email = user.getEmail();
            if (email == null) {
                Log.w(TAG, "Cannot re-authenticate: User email is null.");
                Toast.makeText(this, "Cannot verify identity. Please contact support.", Toast.LENGTH_SHORT).show();
                return;
            }


            AuthCredential credential = EmailAuthProvider.getCredential(email, password);

            user.reauthenticate(credential)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User re-authenticated successfully.");
                            // Re-authentication successful
                            deleteUserAccount();

                        } else {
                            // Re-authentication failed
                            Log.w(TAG, "Re-authentication failed.", task.getException());
                            String errorMessage = "Verification failed. Check your password.";
                            if (task.getException() != null) {
                                errorMessage += " " + task.getException().getMessage();
                            }
                            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        builder.setNegativeButton(R.string.Cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }
}
