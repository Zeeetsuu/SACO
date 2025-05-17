package com.example.sacov3;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.HashMap;
import java.util.Map;

public class HomeScreen extends AppCompatActivity {

    public static final int MENU_HOME = R.id.menuHome;
    public static final int MENU_SCHEDULE = R.id.menuSchedule;
    public static final int MENU_ENERGY = R.id.menuEnergy;
    public static final int MENU_SETTINGS = R.id.menuSettings;

    private final Map<Integer, Fragment> fragmentMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_screen);

        BottomNavigationView bottomNavigationView = findViewById(R.id.homeScreenNavBar);
        bottomNavigationView.setOnItemSelectedListener(navListener);

        initializeFragmentMap();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.homeScreenFrame, new FragmentHome()).commit();
        }
    }

    private void initializeFragmentMap() {
        fragmentMap.put(MENU_HOME, new FragmentHome());
        fragmentMap.put(MENU_SCHEDULE, new FragmentSchedule());
        fragmentMap.put(MENU_ENERGY, new FragmentEnergy());
        fragmentMap.put(MENU_SETTINGS, new FragmentSettings());
    }

    private final BottomNavigationView.OnItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = fragmentMap.get(item.getItemId());
                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.homeScreenFrame, selectedFragment).commit();
                }
                return true;
            };
}