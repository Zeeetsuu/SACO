package com.example.sacov3;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


public class addSchedule extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_schedule);
        TextView backbutton = findViewById(R.id.manageScheduleBack);
        backbutton.setOnClickListener(v -> finish());

    }
}