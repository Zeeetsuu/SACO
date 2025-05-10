package com.example.sacov3;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class contactUsScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_us_screen);
        TextView back = findViewById(R.id.contactUsBack);
        back.setOnClickListener(v -> finish());
    }
}