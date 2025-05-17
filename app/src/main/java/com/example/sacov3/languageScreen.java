package com.example.sacov3;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.os.LocaleListCompat;
import java.util.Locale;

public class languageScreen extends AppCompatActivity {

    CardView languageCard1;
    CardView languageCard2;
    TextView languageBack;
    TextView languageText1;
    TextView languageText2;
    private static final String PREFS_NAME = "LanguagePrefs";
    private static final String LANGUAGE_KEY = "selected_language";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadLocale();

        setContentView(R.layout.activity_language_screen);
        languageBack = findViewById(R.id.languageBack);
        languageCard1 = findViewById(R.id.languageCard1);
        languageCard2 = findViewById(R.id.languageCard2);


        languageText1 = findViewById(R.id.languageText1);
        languageText2 = findViewById(R.id.languageText2);

        if (languageBack != null) {
            languageBack.setOnClickListener(v -> finish());
        }

        languageCard1.setOnClickListener(v -> {
            setAndSaveLocale("en");
            updateLanguageTextViews();
            recreate();
        });

        languageCard2.setOnClickListener(v -> {
            setAndSaveLocale("in");
            updateLanguageTextViews();
            recreate();
        });
    }

    private void setAndSaveLocale(String languageCode) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(LANGUAGE_KEY, languageCode);
        editor.apply();
        setLocale(languageCode);
    }

    private void loadLocale() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String language = prefs.getString(LANGUAGE_KEY, "");

        if (!language.isEmpty()) {
            setLocale(language);
        }
    }

    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        LocaleListCompat localeList = LocaleListCompat.create(locale);
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(localeList);
    }

    private void updateLanguageTextViews() {
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        Context context = createConfigurationContext(configuration);
        Resources localizedResources = context.getResources();

        if (languageBack != null) {
            languageBack.setText(localizedResources.getString(R.string.language));
        }
        if (languageText1 != null) {
            languageText1.setText(localizedResources.getString(R.string.english));
        }
        if (languageText2 != null) {
            languageText2.setText(localizedResources.getString(R.string.bahasa_indonesia));
        }
    }
}