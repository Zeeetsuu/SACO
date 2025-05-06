package com.example.sacov3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class LoginScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_screen);

        TextView textView = findViewById(R.id.loginScreenText5);
        textView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginScreen.this, SignUpScreen.class);
            startActivity(intent);
        });

        Button button = findViewById(R.id.loginScreenButton);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(LoginScreen.this, HomeScreen.class);
            startActivity(intent);
        });

    }
}