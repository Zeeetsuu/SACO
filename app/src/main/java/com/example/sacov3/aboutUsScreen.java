package com.example.sacov3;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


public class aboutUsScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us_screen);

        TextView backButton = findViewById(R.id.aboutUsBack);
        backButton.setOnClickListener(v -> finish());
    }
}