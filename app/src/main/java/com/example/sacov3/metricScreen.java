package com.example.sacov3;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


public class metricScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metric_screen);

        TextView backButton = findViewById(R.id.metricBack);
        backButton.setOnClickListener(v -> finish());

    }
}