package com.example.sacov3;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;


public class addSchedule extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_schedule);
        TextView backbutton = findViewById(R.id.manageScheduleBack);
        backbutton.setOnClickListener(v -> finish());

        ImageView addbutton = findViewById(R.id.manageScheduleImageView);
        addbutton.setOnClickListener(v -> Toast.makeText(this, R.string.FeatureScheduled, Toast.LENGTH_SHORT).show());
    }
}