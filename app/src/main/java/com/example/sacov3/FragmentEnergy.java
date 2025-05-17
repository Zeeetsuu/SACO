package com.example.sacov3;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FragmentEnergy extends Fragment {

    private static final String TAG = "FragmentEnergy";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ValueEventListener IoTDeviceNamesListener;
    private TextView devicesCount;
    private TextView yesterdayDate;
    private TextView twoDaysAgo;
    private TextView twoDaysAgoDate;

    private LinearLayout todayContainer;
    private LinearLayout yesterdayContainer;
    private LinearLayout twoDaysAgoContainer;

    private Map<String, String> deviceNames = new HashMap<>();

    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public FragmentEnergy() {
    }

    public static FragmentEnergy newInstance(String param1, String param2) {
        FragmentEnergy fragment = new FragmentEnergy();
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
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_energy, container, false);

        devicesCount = view.findViewById(R.id.fragenergyDevicesCount);
        yesterdayDate = view.findViewById(R.id.fragenergyDate1);
        twoDaysAgo = view.findViewById(R.id.fragenergyDaysAgo1);
        twoDaysAgoDate = view.findViewById(R.id.fragenergyDate2);
        todayContainer = view.findViewById(R.id.fragenergyTodayContainer);
        yesterdayContainer = view.findViewById(R.id.fragenergyYesterdayContainer);
        twoDaysAgoContainer = view.findViewById(R.id.fragenergyTwoDaysAgoContainer);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        yesterdayDate.setText(displayDateFormat.format(cal.getTime()));

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -2);
        twoDaysAgo.setText(getString(R.string.xdaysago, 2));
        twoDaysAgoDate.setText(displayDateFormat.format(cal.getTime()));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fetchAndDisplayDeviceNames();
    }

    private void fetchAndDisplayDeviceNames() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference devicesRef = mDatabase.child("devices");

            IoTDeviceNamesListener = devicesRef.orderByChild("ownerId").equalTo(userId)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            deviceNames.clear();
                            long deviceCount = 0;
                            if (dataSnapshot.exists()) {
                                for (DataSnapshot deviceSnapshot : dataSnapshot.getChildren()) {
                                    String deviceId = deviceSnapshot.getKey();
                                    String deviceName = deviceSnapshot.child("deviceName").getValue(String.class);
                                    if (deviceId != null && deviceName != null) {
                                        deviceNames.put(deviceId, deviceName);
                                        deviceCount++;
                                    } else {
                                        Log.w(TAG, "Device data missing key or name for snapshot: " + deviceSnapshot.getKey());
                                    }
                                }
                            }
                            Log.d(TAG, "Fetched " + deviceNames.size() + " device names.");
                            updateDeviceCount(deviceCount);
                            updateContainers();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.w(TAG, "Failed to load device names.", databaseError.toException());
                            Toast.makeText(getContext(), R.string.failloading, Toast.LENGTH_SHORT).show();
                            clearEnergyContainer();
                            updateDeviceCount(0);
                        }
                    });
        } else {
            Log.d(TAG, "User is not signed in. Can't fetch device names.");
            clearEnergyContainer();
            updateDeviceCount(0);
        }
    }

    private void updateContainers() {
        if (todayContainer == null || yesterdayContainer == null || twoDaysAgoContainer == null) {
            Log.w(TAG, "Energy containers not yet initialized in onCreateView.");
            return;
        }

        todayContainer.removeAllViews();
        yesterdayContainer.removeAllViews();
        twoDaysAgoContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (deviceNames.isEmpty()) {
            TextView noDevicesTextToday = new TextView(getContext());
            noDevicesTextToday.setText(R.string.nodevice);
            noDevicesTextToday.setPadding(18, 8, 18, 8);
            todayContainer.addView(noDevicesTextToday);

            TextView noDevicesTextYesterday = new TextView(getContext());
            noDevicesTextYesterday.setText(R.string.nodevice);
            noDevicesTextYesterday.setPadding(18, 8, 18, 8);
            yesterdayContainer.addView(noDevicesTextYesterday);

            TextView noDevicesTextTwoDaysAgo = new TextView(getContext());
            noDevicesTextTwoDaysAgo.setText(R.string.nodevice);
            noDevicesTextTwoDaysAgo.setPadding(18, 8, 18, 8);
            twoDaysAgoContainer.addView(noDevicesTextTwoDaysAgo);

            Log.d(TAG, "No devices found for user.");
        } else {
            for (Map.Entry<String, String> entry : deviceNames.entrySet()) {
                String deviceId = entry.getKey();
                String deviceName = entry.getValue();

                View todayContainer = inflater.inflate(R.layout.energy_template, this.todayContainer, false);
                TextView todayDeviceName = todayContainer.findViewById(R.id.fragenergyContainerDeviceName);
                TextView todayDeviceStatus = todayContainer.findViewById(R.id.fragenergyContainerDeviceStatus);
                todayDeviceName.setText(deviceName);
                todayDeviceStatus.setText(R.string.DataNotAvailable);
                this.todayContainer.addView(todayContainer);

                View yesterdayContainer = inflater.inflate(R.layout.energy_template, this.yesterdayContainer, false);
                TextView yesterdayDeviceName = yesterdayContainer.findViewById(R.id.fragenergyContainerDeviceName);
                TextView yesterdayDeviceStatus = yesterdayContainer.findViewById(R.id.fragenergyContainerDeviceStatus);
                yesterdayDeviceName.setText(deviceName);
                yesterdayDeviceStatus.setText(R.string.DataNotAvailable);
                this.yesterdayContainer.addView(yesterdayContainer);

                View twoDaysAgoContainer = inflater.inflate(R.layout.energy_template, this.twoDaysAgoContainer, false);
                TextView twoDaysAgoDeviceName = twoDaysAgoContainer.findViewById(R.id.fragenergyContainerDeviceName);
                TextView twoDaysAgoDeviceStatus = twoDaysAgoContainer.findViewById(R.id.fragenergyContainerDeviceStatus);
                twoDaysAgoDeviceName.setText(deviceName);
                twoDaysAgoDeviceStatus.setText(R.string.DataNotAvailable);
                this.twoDaysAgoContainer.addView(twoDaysAgoContainer);

                Log.d(TAG, "Added placeholder for device: " + deviceName);
            }
            Log.d(TAG, "Finished updating all energy sections with device names.");
        }
    }

    public void updateDeviceCount(long count) {
        if (devicesCount != null) {
            devicesCount.setText(getString(R.string.deviceCount, count));
        }
    }

    private void clearEnergyContainer() {
        if (todayContainer != null) todayContainer.removeAllViews();
        if (yesterdayContainer != null) yesterdayContainer.removeAllViews();
        if (twoDaysAgoContainer != null) twoDaysAgoContainer.removeAllViews();
        deviceNames.clear();
        Log.d(TAG, "Cleared energy UI and device names map.");
    }

    private void detachDeviceNamesListener() {
        if (IoTDeviceNamesListener != null) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                mDatabase.child("devices").orderByChild("ownerId").equalTo(currentUser.getUid())
                        .removeEventListener(IoTDeviceNamesListener);
            }
            IoTDeviceNamesListener = null;
            Log.d(TAG, "Detached device names listener.");
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "FragmentEnergy onStart");
        fetchAndDisplayDeviceNames();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "FragmentEnergy onStop");
        detachDeviceNamesListener();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "FragmentEnergy onDestroyView");
        detachDeviceNamesListener();
        clearEnergyContainer();

        devicesCount = null;
        yesterdayDate = null;
        twoDaysAgo = null;
        twoDaysAgoDate = null;
        todayContainer = null;
        yesterdayContainer = null;
        twoDaysAgoContainer = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "FragmentEnergy onDestroy");
        detachDeviceNamesListener();
    }
}
