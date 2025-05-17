package com.example.sacov3;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.LinearLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import java.util.HashMap;
import java.util.Map;
import java.text.DecimalFormat;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class FragmentHome extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    private static final String TAG = "HomeFragment";
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private Query devicesQuery;
    private ChildEventListener deviceListListener;
    private LinearLayout linearLayout;
    private Map<String, View> deviceViews = new HashMap<>();
    private String deviceKeyBeingDragged = null;
    private static final String PREF_TEMP_UNIT = "temp_unit";
    private static final String UNIT_CELSIUS = "C";
    private static final String UNIT_FAHRENHEIT = "F";
    private static final String UNIT_KELVIN = "K";
    private String currentTempUnit = UNIT_CELSIUS;

    private DecimalFormat decimalFormat = new DecimalFormat("#.##");

    public FragmentHome() {}
    public static FragmentHome newInstance(String param1, String param2) {
        FragmentHome fragment = new FragmentHome();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://saco-7273a-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            devicesQuery = mDatabase.child("devices").orderByChild("ownerId").equalTo(currentUser.getUid());
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        currentTempUnit = sharedPreferences.getString(PREF_TEMP_UNIT, UNIT_CELSIUS);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        TextView helloText = view.findViewById(R.id.fraghomeHelloText);
        TextView welcomeText = view.findViewById(R.id.fraghomeWelcomeText);
        ImageView manageButton = view.findViewById(R.id.fraghomeManageIcon);
        linearLayout = view.findViewById(R.id.fraghomeDevicesListContainer);

        manageButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), addDevices.class);
            startActivity(intent);
        });

        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userNameToDisplay = "Mr. Guest";
        if (currentUser != null) {
            String authDisplayName = currentUser.getDisplayName();
            if (!TextUtils.isEmpty(authDisplayName)) {
                userNameToDisplay = authDisplayName;
            }
        }
        helloText.setText(getString(R.string.hello_format, userNameToDisplay));
        welcomeText.setText(getString(R.string.welcome_format, userNameToDisplay));

        return view;
    }

    private void setupDeviceListListener() {
        if (devicesQuery == null || deviceListListener != null) {
            Log.d(TAG, "Skipping device list listener setup.");
            return;
        }
        Log.d(TAG, "Setting up device list listener.");

        deviceListListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                String deviceKey = dataSnapshot.getKey();
                Device device = dataSnapshot.getValue(Device.class);
                if (deviceKey != null && device != null && !deviceViews.containsKey(deviceKey)) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    View template = inflater.inflate(R.layout.list_item_home_device, linearLayout, false);

                    TextView deviceName = template.findViewById(R.id.templateDeviceName);
                    Switch powerSwitch = template.findViewById(R.id.templateSwitch);
                    SeekBar temperatureSeekBar = template.findViewById(R.id.templateSeekbar);
                    TextView temperature = template.findViewById(R.id.templateTemperature);

                    deviceName.setText(device.getDeviceName() != null ? device.getDeviceName() : "Unnamed Device");

                    powerSwitch.setChecked(device.isPowerOn());
                    powerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> mDatabase.child("devices").child(deviceKey).child("powerOn").setValue(isChecked)
                            .addOnFailureListener(e -> Toast.makeText(getContext(), R.string.updatefail, Toast.LENGTH_SHORT).show()));

                    int progress = device.getDesiredTemp() - 16;
                    progress = Math.max(0, Math.min(progress, temperatureSeekBar.getMax()));
                    temperatureSeekBar.setProgress(progress);

                    updateTempDisplay(temperature, device.getDesiredTemp());

                    temperature.post(() -> {
                        positionTempDisplay(temperatureSeekBar, temperature, deviceKey);
                    });

                    temperatureSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int currentProgress, boolean fromUser) {
                            int celsiusTemp = currentProgress + 16;
                            updateTempDisplay(temperature, celsiusTemp);
                            positionTempDisplay(seekBar, temperature, deviceKey);
                        }
                        @Override public void onStartTrackingTouch(SeekBar seekBar) {
                            deviceKeyBeingDragged = deviceKey;
                            TextView tempDisplay = template.findViewById(R.id.templateTemperature);
                            if (tempDisplay != null) {
                                int displayedTempCelsius = seekBar.getProgress() + 16;
                                updateTempDisplay(tempDisplay, displayedTempCelsius);
                                tempDisplay.setVisibility(View.VISIBLE);
                                tempDisplay.post(() -> {
                                    positionTempDisplay(seekBar, tempDisplay, deviceKey);
                                });
                            }
                        }
                        @Override public void onStopTrackingTouch(SeekBar seekBar) {
                            if (deviceKey != null) {
                                int newDesiredTempCelsius = seekBar.getProgress() + 16;
                                mDatabase.child("devices").child(deviceKey).child("desiredTemp").runTransaction(new Transaction.Handler() {
                                    @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                        currentData.setValue(newDesiredTempCelsius);
                                        return Transaction.success(currentData);
                                    }
                                    @Override public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                                        if (error != null) Log.e(TAG, "Transaction failed for " + deviceKey, error.toException());
                                        Toast.makeText(getContext(), R.string.temperatureupdated, Toast.LENGTH_SHORT).show();
                                        deviceKeyBeingDragged = null;
                                    }
                                });
                            } else {
                                deviceKeyBeingDragged = null;
                            }
                        }
                    });
                    linearLayout.addView(template);
                    deviceViews.put(deviceKey, template);
                } else {
                    Log.w(TAG, "onChildAdded received null data or duplicate view for key: " + deviceKey);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                String deviceKey = dataSnapshot.getKey();
                Device device = dataSnapshot.getValue(Device.class);
                if (deviceKey != null && device != null && deviceViews.containsKey(deviceKey)) {
                    View Template = deviceViews.get(deviceKey);
                    if (Template != null) {
                        TextView DeviceName = Template.findViewById(R.id.templateDeviceName);
                        Switch PowerSwitch = Template.findViewById(R.id.templateSwitch);
                        SeekBar TemperatureSeekBar = Template.findViewById(R.id.templateSeekbar);
                        TextView TemperatureDisplay = Template.findViewById(R.id.templateTemperature);

                        DeviceName.setText(device.getDeviceName() != null ? device.getDeviceName() : "Unnamed Device");

                        PowerSwitch.setChecked(device.isPowerOn());

                        if (!deviceKey.equals(deviceKeyBeingDragged)) {
                            int progress = device.getDesiredTemp() - 16;
                            progress = Math.max(0, Math.min(progress, TemperatureSeekBar.getMax()));
                            TemperatureSeekBar.setProgress(progress);

                            updateTempDisplay(TemperatureDisplay, device.getDesiredTemp());

                            if (TemperatureDisplay != null) {
                                TemperatureDisplay.post(() -> {
                                    positionTempDisplay(TemperatureSeekBar, TemperatureDisplay, deviceKey);
                                });
                            }
                        } else {
                            if (TemperatureDisplay != null) {
                                TemperatureDisplay.post(() -> {
                                    positionTempDisplay(TemperatureSeekBar, TemperatureDisplay, deviceKey);
                                });
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "onChildChanged received null data or missing view for key: " + deviceKey);
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                String deviceKey = dataSnapshot.getKey();
                if (deviceKey != null && deviceViews.containsKey(deviceKey)) {
                    View deviceItemView = deviceViews.get(deviceKey);
                    if (deviceItemView != null) {
                        linearLayout.removeView(deviceItemView);
                        deviceViews.remove(deviceKey);
                    }
                } else {
                    Log.w(TAG, "onChildRemoved received null key or missing view for key: " + deviceKey);
                }
            }
            @Override public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError databaseError) { Toast.makeText(getContext(), R.string.failloading, Toast.LENGTH_SHORT).show(); }
        };
        devicesQuery.addChildEventListener(deviceListListener);
    }

    private void removeDeviceListListener() {
        if (deviceListListener != null && devicesQuery != null) {
            devicesQuery.removeEventListener(deviceListListener);
            deviceListListener = null;
        }
    }

    private void positionTempDisplay(SeekBar seekBar, TextView tempDisplayTextView, String deviceKeyForLog) {
        if (seekBar == null || tempDisplayTextView == null) {
            Log.d(TAG, "Positioning skipped for " + deviceKeyForLog + ": SeekBar or TextView is null.");
            return;
        }

        int seekBarWidth = seekBar.getWidth();
        int temperatureTextWidth = tempDisplayTextView.getWidth();

        if (temperatureTextWidth == 0 || seekBarWidth == 0) {
            Log.d(TAG, "Positioning skipped for " + deviceKeyForLog + ": TextView or SeekBar width is zero (not laid out yet). TextView Width: " + temperatureTextWidth + ", SeekBar Width: " + seekBarWidth);
            return;
        }

        int paddingLeft = seekBar.getPaddingLeft();
        int paddingRight = seekBar.getPaddingRight();
        int trackableWidth = seekBarWidth - paddingLeft - paddingRight;
        int currentProgress = seekBar.getProgress();
        int maxProgress = seekBar.getMax();

        if (maxProgress == 0) {
            tempDisplayTextView.setTranslationX(paddingLeft - (temperatureTextWidth / 2f));
            return;
        }

        float thumbCenter = paddingLeft + (trackableWidth * (float)currentProgress / maxProgress);

        float containerCenter = seekBarWidth / 2f;
        float translationX = thumbCenter - containerCenter;

        tempDisplayTextView.setTranslationX(translationX);

        Log.d(TAG, "Positioned " + deviceKeyForLog + ": Progress=" + currentProgress + ", Max=" + maxProgress + ", PaddingLeft=" + paddingLeft + ", TrackableWidth=" + trackableWidth + ", ThumbCenter=" + thumbCenter + ", ContainerCenter=" + containerCenter + ", TextViewWidth=" + temperatureTextWidth + ", TranslationX=" + translationX);
    }


    private void updateTempDisplay(TextView tempDisplayTextView, int celsiusTemp) {
        if (tempDisplayTextView == null) return;

        double displayedTemp;
        String unitSymbol;

        switch (currentTempUnit) {
            case UNIT_FAHRENHEIT:
                displayedTemp = (celsiusTemp * 9.0/5.0) + 32;
                unitSymbol = "°F";
                break;
            case UNIT_KELVIN:
                displayedTemp = celsiusTemp + 273.15;
                unitSymbol = "K";
                break;
            case UNIT_CELSIUS:
            default:
                displayedTemp = celsiusTemp;
                unitSymbol = "°C";
                break;
        }

        tempDisplayTextView.setText(decimalFormat.format(displayedTemp) + unitSymbol);
        tempDisplayTextView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        currentTempUnit = sharedPreferences.getString(PREF_TEMP_UNIT, UNIT_CELSIUS);

        setupDeviceListListener();
        deviceKeyBeingDragged = null;
        updateHeader();

        for (Map.Entry<String, View> entry : deviceViews.entrySet()) {
            String deviceKey = entry.getKey();
            View template = entry.getValue();
            TextView tempDisplay = template.findViewById(R.id.templateTemperature);
            SeekBar tempSeekBar = template.findViewById(R.id.templateSeekbar);
            if (tempDisplay != null && tempSeekBar != null) {
                int celsiusTemp = tempSeekBar.getProgress() + 16;
                updateTempDisplay(tempDisplay, celsiusTemp);
                tempDisplay.post(() -> {
                    positionTempDisplay(tempSeekBar, tempDisplay, deviceKey);
                });
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        removeDeviceListListener();
        deviceKeyBeingDragged = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        removeDeviceListListener();
        if (linearLayout != null) { linearLayout.removeAllViews(); }
        deviceViews.clear();
        linearLayout = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeDeviceListListener();
    }

    private void updateHeader() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userNameToDisplay = "Mr. Guest";
        if (currentUser != null) {
            String authDisplayName = currentUser.getDisplayName();
            if (!TextUtils.isEmpty(authDisplayName)) {
                userNameToDisplay = authDisplayName;
            }
        }
        View view = getView();
        if (view != null) {
            TextView helloTextView = view.findViewById(R.id.fraghomeHelloText);
            TextView welcomeTextView = view.findViewById(R.id.fraghomeWelcomeText);
            if (helloTextView != null) helloTextView.setText(getString(R.string.hello_format, userNameToDisplay));
            if (welcomeTextView != null) welcomeTextView.setText(getString(R.string.welcome_format, userNameToDisplay));
        }
    }
}
