package com.example.sacov3;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class metricScreen extends AppCompatActivity {

    private static final String PREF_TEMP_UNIT = "temp_unit";
    private static final String UNIT_CELSIUS = "C";
    private static final String UNIT_FAHRENHEIT = "F";
    private static final String UNIT_KELVIN = "K";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metric_screen);

        TextView backButton = findViewById(R.id.metricBack);
        Button celsiusButton = findViewById(R.id.metricCelcius);
        Button fahrenheitButton = findViewById(R.id.metricFarenheit);
        Button kelvinButton = findViewById(R.id.metricKelvin);


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        backButton.setOnClickListener(v -> finish());

        celsiusButton.setOnClickListener(v -> {
            sharedPreferences.edit().putString(PREF_TEMP_UNIT, UNIT_CELSIUS).apply();
            Toast.makeText(this, R.string.temperaturesetC, Toast.LENGTH_SHORT).show();
        });

        fahrenheitButton.setOnClickListener(v -> {
            sharedPreferences.edit().putString(PREF_TEMP_UNIT, UNIT_FAHRENHEIT).apply();
            Toast.makeText(this, R.string.temperaturesetF, Toast.LENGTH_SHORT).show();
        });

        kelvinButton.setOnClickListener(v -> {
            sharedPreferences.edit().putString(PREF_TEMP_UNIT, UNIT_KELVIN).apply();
            Toast.makeText(this, R.string.temperaturesetK, Toast.LENGTH_SHORT).show();
        });
    }
}
