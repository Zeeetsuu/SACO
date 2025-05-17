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

        mAuth = FirebaseAuth.getInstance();

        TextView backButton = findViewById(R.id.securityBack);
        backButton.setOnClickListener(v -> finish());

        securityEmailTextView = findViewById(R.id.securityEmail);
        Button changePasswordButton = findViewById(R.id.securityChangePassword);
        Button deleteAccountButton = findViewById(R.id.securityDelete);

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

        changePasswordButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, securityChangePassword.class);
            startActivity(intent);
        });

        deleteAccountButton.setOnClickListener(v -> deleteConfirmationDialog());
    }

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

    private void deleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account)
                .setMessage(R.string.DeleteAccountDialog)

                .setPositiveButton(R.string.DeleteAccountButton, (dialog, which) -> deleteUserAccount())
                .setNegativeButton(R.string.Cancel, (dialog, which) -> {
                })
                .show();
    }

    private void deleteUserAccount() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Log.w(TAG, "Attempted account deletion while not logged in.");
            Toast.makeText(this, R.string.mustlogin, Toast.LENGTH_SHORT).show(); //bug prevention
            Intent intent = new Intent(this, LoginScreen.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        user.delete()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User account deleted successfully.");
                        Toast.makeText(this, R.string.accountdeleted, Toast.LENGTH_SHORT).show();
                        mAuth.signOut();

                        Intent intent = new Intent(this, LoginScreen.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finishAffinity();

                    } else {
                        Log.w(TAG, "Failed to delete user account.", task.getException());
                        if (task.getException() instanceof FirebaseAuthRecentLoginRequiredException) {
                            Toast.makeText(this, R.string.enterpasswordagain, Toast.LENGTH_LONG).show();
                            reauthenticateDialog(user);
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

    private void reauthenticateDialog(FirebaseUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.verifyidentity);
        builder.setMessage(R.string.confirmdeletion);

        final EditText passwordInput = new EditText(this);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint(R.string.password);

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

        builder.setPositiveButton(R.string.verify, (dialog, which) -> {
            String password = passwordInput.getText().toString().trim();

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this, R.string.requiredpassword, Toast.LENGTH_SHORT).show();
                return;
            }

            String email = user.getEmail();
            if (email == null) {
                Log.w(TAG, "Cannot re-authenticate: User email is null.");
                Toast.makeText(this, "Cannot verify identity.", Toast.LENGTH_SHORT).show(); //bug prevention
                return;
            }


            AuthCredential credential = EmailAuthProvider.getCredential(email, password);

            user.reauthenticate(credential)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User re-authenticated successfully.");
                            deleteUserAccount();

                        } else {
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
