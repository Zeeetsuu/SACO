package com.example.sacov3;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

public class addDevices extends AppCompatActivity {

    private static final String TAG = "AddDevicesActivity"; // Tag for logging

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseRecyclerAdapter<Device, DeviceViewHolder> firebaseAdapter;
    private RecyclerView devicesRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_devices);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://saco-7273a-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        TextView backbutton = findViewById(R.id.manageRoomBack);
        backbutton.setOnClickListener(v -> finish());

        ImageView addCircle = findViewById(R.id.manageRoomImageView);
        addCircle.setOnClickListener(v -> showAddDeviceDialog());

        // Recycler view
        devicesRecyclerView = findViewById(R.id.devicesRecyclerView);
        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load and display user's devices
        loadUserDevices();
    }

    private void showAddDeviceDialog() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to add devices.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Device");

        // input for device ID
        final EditText input = new EditText(this);
        input.setHint("Enter Unique Device ID"); // Suggest unique ID
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String deviceId = input.getText().toString().trim();
            if (!deviceId.isEmpty()) {
                addDeviceToDatabase(deviceId, currentUser.getUid());
            } else {
                Toast.makeText(addDevices.this, "Device ID cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addDeviceToDatabase(String deviceId, String userId) {
        DatabaseReference deviceRef = mDatabase.child("devices").child(deviceId);
        deviceRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    String existingOwnerId = task.getResult().child("ownerId").getValue(String.class);
                    if (existingOwnerId != null && !existingOwnerId.isEmpty()) {
                        if (existingOwnerId.equals(userId)) {
                            Toast.makeText(addDevices.this, "This device is already linked to your account.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(addDevices.this, "This device ID is already claimed.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "Device ID exists but has no ownerId: " + deviceId);
                        actuallyAddDeviceData(deviceId, userId);
                    }
                } else {
                    actuallyAddDeviceData(deviceId, userId);
                }
            } else {
                Log.e(TAG, "Failed to check device existence: " + deviceId, task.getException());
                Toast.makeText(addDevices.this, "Error checking device ID.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditDeviceDialog(String deviceKey, Device currentDevice) {
        if (deviceKey == null || currentDevice == null) {
            Log.w(TAG, "Cannot show edit dialog, deviceKey or currentDevice is null.");
            Toast.makeText(this, "Error editing device.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Device");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);


        final EditText deviceNameInput = new EditText(this);
        deviceNameInput.setHint("Device Name");
        deviceNameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        deviceNameInput.setText(currentDevice.getDeviceName());
        layout.addView(deviceNameInput);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newDeviceName = deviceNameInput.getText().toString().trim();
            if (newDeviceName.isEmpty()) {
                Toast.makeText(this, "Device name cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }

            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("deviceName", newDeviceName);

            mDatabase.child("devices").child(deviceKey)
                    .updateChildren(updates, (databaseError, databaseReference) -> {
                        if (databaseError != null) {
                            Log.e(TAG, "Failed to update device " + deviceKey + ": " + databaseError.getMessage());
                            Toast.makeText(addDevices.this, "Failed to update device: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                        } else {
                            Log.d(TAG, "Device updated successfully: " + deviceKey);
                            Toast.makeText(addDevices.this, "Device updated!", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }


    private void actuallyAddDeviceData(String deviceId, String userId) {
        DatabaseReference deviceRef = mDatabase.child("devices").child(deviceId);

        Device newDevice = new Device(userId, 0, 0, 22, "My AC"); // Default values

        deviceRef.setValue(newDevice, (databaseError, databaseReference) -> {
            if (databaseError != null) {
                Log.e(TAG, "Failed to add device " + deviceId + ": " + databaseError.getMessage(), databaseError.toException());
                Toast.makeText(addDevices.this, "Failed to add device: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "Device added successfully: " + deviceId);
                Toast.makeText(addDevices.this, "Device added successfully!", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void loadUserDevices() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not logged in, cannot load devices.");
            return;
        }

        Query devicesQuery = mDatabase.child("devices").orderByChild("ownerId").equalTo(currentUser.getUid());

        FirebaseRecyclerOptions<Device> options =
                new FirebaseRecyclerOptions.Builder<Device>()
                        .setQuery(devicesQuery, Device.class)
                        .build();

        firebaseAdapter = new FirebaseRecyclerAdapter<Device, DeviceViewHolder>(options) {
            @NonNull
            @Override
            public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_device, parent, false);
                return new DeviceViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull DeviceViewHolder holder, int position, @NonNull Device model) {

                String deviceKey = getRef(position).getKey();

                holder.roomNameTextView.setText(model.getDeviceName() != null ? model.getDeviceName() : "Unnamed Device");

                // Delete button
                holder.deleteIconImageView.setOnClickListener(v -> {
                    if (deviceKey != null) {
                        new AlertDialog.Builder(addDevices.this)
                                .setTitle("Delete Device")
                                .setMessage("Are you sure you want to delete '" + model.getDeviceName() + "'?")
                                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                    mDatabase.child("devices").child(deviceKey).removeValue((error, ref) -> {
                                        if (error != null) {
                                            Log.e(TAG, "Failed to delete device " + deviceKey + ": " + error.getMessage());
                                            Toast.makeText(addDevices.this, "Failed to delete device.", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Log.d(TAG, "Device deleted successfully: " + deviceKey);
                                            Toast.makeText(addDevices.this, "'" + model.getDeviceName() + "' deleted.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                })
                                .setNegativeButton(android.R.string.no, null)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show(); // Display the dialog
                    } else {
                        Log.w(TAG, "Attempted to delete a device with a null key at position: " + position);
                        Toast.makeText(addDevices.this, "Error deleting device.", Toast.LENGTH_SHORT).show();
                    }
                });


                // Edit button
                holder.editIconImageView.setOnClickListener(v -> {
                    Device currentDevice = getItem(position);

                    if (deviceKey != null && currentDevice != null) {
                        showEditDeviceDialog(deviceKey, currentDevice);
                    } else {
                        Log.w(TAG, "Attempted to edit a device with a null key or model at position: " + position);
                        Toast.makeText(addDevices.this, "Error preparing to edit device.", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        };

        devicesRecyclerView.setAdapter(firebaseAdapter);
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView roomNameTextView;
        ImageView editIconImageView;
        ImageView deleteIconImageView;

        public DeviceViewHolder(View itemView) {
            super(itemView);
            roomNameTextView = itemView.findViewById(R.id.room_name);
            editIconImageView = itemView.findViewById(R.id.edit_icon);
            deleteIconImageView = itemView.findViewById(R.id.delete_icon);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (firebaseAdapter != null) {
            firebaseAdapter.startListening();
            Log.d(TAG, "Adapter started listening.");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (firebaseAdapter != null) {
            firebaseAdapter.stopListening();
            Log.d(TAG, "Adapter stopped listening.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Good practice to release resources
        firebaseAdapter = null; // Help garbage collection
        Log.d(TAG, "Activity destroyed.");
    }
}
