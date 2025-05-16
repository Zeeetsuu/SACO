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

// import com.example.sacov3.Device;

public class FragmentHome extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    private static final String TAG = "HomeFragment";
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private Query devicesQuery;
    private ChildEventListener deviceEventListener;
    private LinearLayout dynamicContentLayout;
    private Map<String, View> deviceViews = new HashMap<>();
    private String deviceKeyBeingDragged = null;

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
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        TextView helloTextView = view.findViewById(R.id.fraghomeHelloText);
        TextView welcomeTextView = view.findViewById(R.id.fraghomeWelcomeText);
        ImageView manageButton = view.findViewById(R.id.HomeImageView);
        dynamicContentLayout = view.findViewById(R.id.fraghomeDynamicContentLayout);
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
        helloTextView.setText(getString(R.string.hello_format, userNameToDisplay));
        welcomeTextView.setText(getString(R.string.welcome_format, userNameToDisplay));
        return view;
    }

    private void attachDatabaseReadListener() {
        if (devicesQuery == null || deviceEventListener != null) {
            Log.d(TAG, "Skipping attach listener.");
            return;
        }
        Log.d(TAG, "Attaching database read listener.");
        deviceEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                String deviceKey = dataSnapshot.getKey();
                Device device = dataSnapshot.getValue(Device.class);
                if (deviceKey != null && device != null && !deviceViews.containsKey(deviceKey)) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    View deviceItemView = inflater.inflate(R.layout.list_item_home_device, dynamicContentLayout, false);

                    TextView deviceNameTextView = deviceItemView.findViewById(R.id.text_device_name_home);
                    Switch powerSwitch = deviceItemView.findViewById(R.id.switch_device_power);
                    SeekBar tempSeekBar = deviceItemView.findViewById(R.id.seekbar_device_temp);
                    TextView tempDisplayTextView = deviceItemView.findViewById(R.id.text_temp_display);

                    deviceNameTextView.setText(device.getDeviceName() != null ? device.getDeviceName() : "Unnamed Device");

                    powerSwitch.setChecked(device.isPowerOn());
                    powerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        mDatabase.child("devices").child(deviceKey).child("powerOn").setValue(isChecked)
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update power.", Toast.LENGTH_SHORT).show());
                    });

                    int progress = device.getDesiredTemp() - 16;
                    progress = Math.max(0, Math.min(progress, tempSeekBar.getMax()));
                    tempSeekBar.setProgress(progress);

                    if (tempDisplayTextView != null) { tempDisplayTextView.setVisibility(View.INVISIBLE); }

                    tempSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int currentProgress, boolean fromUser) {
                            if (fromUser && tempDisplayTextView != null) {
                                int displayedTemp = currentProgress + 16;
                                tempDisplayTextView.setText(displayedTemp + "°C");

                                int seekBarWidth = seekBar.getWidth();
                                int paddingLeft = seekBar.getPaddingLeft();
                                int paddingRight = seekBar.getPaddingRight();
                                int trackableWidth = seekBarWidth - paddingLeft - paddingRight;
                                float thumbCenter = paddingLeft;
                                if (seekBar.getMax() > 0) { thumbCenter += (trackableWidth * (float)currentProgress / seekBar.getMax()); }
                                int tempTextViewWidth = tempDisplayTextView.getWidth();
                                if (tempTextViewWidth > 0) {
                                    float translationX = thumbCenter - (seekBarWidth / 2f);
                                    tempDisplayTextView.setTranslationX(translationX);
                                }
                            }
                        }
                        @Override public void onStartTrackingTouch(SeekBar seekBar) {
                            deviceKeyBeingDragged = deviceKey;
                            TextView tempDisplay = deviceItemView.findViewById(R.id.text_temp_display);
                            if (tempDisplay != null) {
                                int displayedTemp = seekBar.getProgress() + 16;
                                tempDisplay.setText(displayedTemp + "°C");
                                tempDisplay.setVisibility(View.VISIBLE);
                                tempDisplay.post(() -> {
                                    if (tempDisplay.getWidth() > 0 && seekBar.getWidth() > 0) {
                                        int seekBarWidth = seekBar.getWidth();
                                        int paddingLeft = seekBar.getPaddingLeft();
                                        int paddingRight = seekBar.getPaddingRight();
                                        int trackableWidth = seekBarWidth - paddingLeft - paddingRight;
                                        int currentProgressNow = seekBar.getProgress();
                                        float thumbCenter = paddingLeft;
                                        if (seekBar.getMax() > 0) { thumbCenter += (trackableWidth * (float)currentProgressNow / seekBar.getMax()); }
                                        float translationX = thumbCenter - (seekBarWidth / 2f);
                                        tempDisplay.setTranslationX(translationX);
                                    }
                                });
                            }
                        }
                        @Override public void onStopTrackingTouch(SeekBar seekBar) {
                            if (deviceKey != null) {
                                int newDesiredTemp = seekBar.getProgress() + 16;
                                mDatabase.child("devices").child(deviceKey).child("desiredTemp").runTransaction(new Transaction.Handler() {
                                    @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData currentData) { currentData.setValue(newDesiredTemp); return Transaction.success(currentData); }
                                    @Override public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                                        if (error != null) Log.e(TAG, "Transaction failed for " + deviceKey, error.toException());
                                        Toast.makeText(getContext(), "Temperature updated!", Toast.LENGTH_SHORT).show();
                                        TextView tempDisplay = deviceItemView.findViewById(R.id.text_temp_display);
                                        if (tempDisplay != null) { tempDisplay.setVisibility(View.INVISIBLE); }
                                        deviceKeyBeingDragged = null;
                                    }
                                });
                            } else {
                                TextView tempDisplay = deviceItemView.findViewById(R.id.text_temp_display);
                                if (tempDisplay != null) { tempDisplay.setVisibility(View.INVISIBLE); }
                                deviceKeyBeingDragged = null;
                            }
                        }
                    });
                    dynamicContentLayout.addView(deviceItemView);
                    deviceViews.put(deviceKey, deviceItemView);
                } else {
                    Log.w(TAG, "onChildAdded received null data or duplicate view for key: " + deviceKey);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                String deviceKey = dataSnapshot.getKey();
                Device device = dataSnapshot.getValue(Device.class);
                if (deviceKey != null && device != null && deviceViews.containsKey(deviceKey)) {
                    View deviceItemView = deviceViews.get(deviceKey);
                    if (deviceItemView != null) {
                        TextView deviceNameTextView = deviceItemView.findViewById(R.id.text_device_name_home);
                        Switch powerSwitch = deviceItemView.findViewById(R.id.switch_device_power);
                        SeekBar tempSeekBar = deviceItemView.findViewById(R.id.seekbar_device_temp);
                        TextView tempDisplayTextView = deviceItemView.findViewById(R.id.text_temp_display);

                        deviceNameTextView.setText(device.getDeviceName() != null ? device.getDeviceName() : "Unnamed Device");


                        powerSwitch.setChecked(device.isPowerOn());

                        if (!deviceKey.equals(deviceKeyBeingDragged)) {
                            int progress = device.getDesiredTemp() - 16;
                            progress = Math.max(0, Math.min(progress, tempSeekBar.getMax()));
                            tempSeekBar.setProgress(progress);
                            if (tempDisplayTextView != null && tempDisplayTextView.getVisibility() == View.VISIBLE) {
                                tempDisplayTextView.setVisibility(View.INVISIBLE);
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
                        dynamicContentLayout.removeView(deviceItemView);
                        deviceViews.remove(deviceKey);
                    }
                } else {
                    Log.w(TAG, "onChildRemoved received null key or missing view for key: " + deviceKey);
                }
            }
            @Override public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError databaseError) { Toast.makeText(getContext(), "Failed to load devices.", Toast.LENGTH_SHORT).show(); }
        };
        devicesQuery.addChildEventListener(deviceEventListener);
    }

    private void detachDatabaseReadListener() {
        if (deviceEventListener != null && devicesQuery != null) {
            devicesQuery.removeEventListener(deviceEventListener);
            deviceEventListener = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        attachDatabaseReadListener();
        deviceKeyBeingDragged = null;
        updateHeader();
    }

    @Override
    public void onStop() {
        super.onStop();
        detachDatabaseReadListener();
        deviceKeyBeingDragged = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        detachDatabaseReadListener();
        if (dynamicContentLayout != null) { dynamicContentLayout.removeAllViews(); }
        deviceViews.clear();
        dynamicContentLayout = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        detachDatabaseReadListener();
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
