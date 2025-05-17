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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

public class addDevices extends AppCompatActivity {

    private static final String TAG = "AddDevicesActivity";
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseRecyclerAdapter<Device, IoTDeviceViewHolder> firebaseAdapter;
    private RecyclerView IoTDeviceRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_devices);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://saco-7273a-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        TextView backbutton = findViewById(R.id.manageRoomBack);
        backbutton.setOnClickListener(v -> finish());

        ImageView addCircle = findViewById(R.id.manageRoomImageView);
        addCircle.setOnClickListener(v -> AddIoTDeviceDialog());

        IoTDeviceRecyclerView = findViewById(R.id.devicesRecyclerView);
        IoTDeviceRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadUserDevices();
    }

    private void AddIoTDeviceDialog() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.Logintoadd, Toast.LENGTH_SHORT).show();
            // non logged in user can't reach this part, but in case of a bug
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.addnewdevice);

        final EditText input = new EditText(this);
        input.setHint(R.string.deviceID);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton(R.string.add, (dialog, which) -> {
            String deviceId = input.getText().toString().trim();
            if (!deviceId.isEmpty()) {
                // If else to check whether ID contain illegal character that can't be processed by firebase
                if (deviceId.matches(".*[.#$\\[\\]/\\x00-\\x1F\\x7F].*")) {
                    Toast.makeText(addDevices.this, R.string.mustbenumber, Toast.LENGTH_LONG).show();
                    return;
                }
                addIoTDeviceToFirebase(deviceId, currentUser.getUid());
            } else {
                Toast.makeText(addDevices.this, R.string.IDcantbeempty, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.Cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void addIoTDeviceToFirebase(String deviceId, String userId) {
        DatabaseReference deviceRef = mDatabase.child("devices").child(deviceId);

        deviceRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    String existingOwnerId = task.getResult().child("ownerId").getValue(String.class);
                    if (existingOwnerId != null && !existingOwnerId.isEmpty()) {
                        if (existingOwnerId.equals(userId)) {
                            Toast.makeText(addDevices.this, R.string.devicealreadylinked, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(addDevices.this, R.string.deviceusedbyother, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "Device ID exists but has no ownerId: " + deviceId); // normally impossible but in case of a bug
                        AddDeviceData(deviceId, userId);
                    }
                } else {
                    AddDeviceData(deviceId, userId);
                }
            } else {
                Log.e(TAG, "Failed to check device existence for ID: " + deviceId, task.getException());
                Toast.makeText(addDevices.this, R.string.unexpectederror, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void EditIoTDeviceDialog(String deviceKey, Device currentDevice) {
        if (deviceKey == null || currentDevice == null) {
            Log.w(TAG, "Cannot show edit dialog, deviceKey or currentDevice is null.");
            Toast.makeText(this, R.string.ErrorDevice, Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.editdevice);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText deviceNameInput = new EditText(this);
        deviceNameInput.setHint(R.string.devicename);
        deviceNameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        deviceNameInput.setText(currentDevice.getDeviceName());
        layout.addView(deviceNameInput);

        builder.setView(layout);

        builder.setPositiveButton(R.string.savebutton, (dialog, which) -> {
            String newDeviceName = deviceNameInput.getText().toString().trim();
            if (newDeviceName.isEmpty()) {
                Toast.makeText(this, R.string.DeviceNameEmpty, Toast.LENGTH_SHORT).show();
                return;
            }

            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("deviceName", newDeviceName);

            mDatabase.child("devices").child(deviceKey) // Use the deviceKey (ID) to reference the correct node
                    .updateChildren(updates, (databaseError, databaseReference) -> {
                        if (databaseError != null) {
                            Log.e(TAG, "Failed to update device " + deviceKey + ": " + databaseError.getMessage());
                            Toast.makeText(addDevices.this, R.string.updatefail + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                        } else {
                            Log.d(TAG, "Device updated successfully: " + deviceKey);
                            Toast.makeText(addDevices.this, R.string.updatesuccess, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        builder.setNegativeButton(R.string.Cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }


    private void AddDeviceData(String deviceId, String userId) {
        DatabaseReference deviceRef = mDatabase.child("devices").child(deviceId);
        Device newDevice = new Device(userId, 0, 0, 22, "My AC", false);

        deviceRef.setValue(newDevice, (databaseError, databaseReference) -> {
            if (databaseError != null) {
                Log.e(TAG, "Failed to add device " + deviceId + ": " + databaseError.getMessage(), databaseError.toException());
                Toast.makeText(addDevices.this, R.string.addfail + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "Device added successfully: " + deviceId);
                Toast.makeText(addDevices.this, R.string.addsuccess, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserDevices() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not logged in, can't load devices.");
            return;
        }

        Query devicesQuery = mDatabase.child("devices").orderByChild("ownerId").equalTo(currentUser.getUid());

        FirebaseRecyclerOptions<Device> options =
                new FirebaseRecyclerOptions.Builder<Device>()
                        .setQuery(devicesQuery, Device.class)
                        .build();

        firebaseAdapter = new FirebaseRecyclerAdapter<Device, IoTDeviceViewHolder>(options) {
            @NonNull
            @Override
            public IoTDeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.add_device_template, parent, false);
                return new IoTDeviceViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull IoTDeviceViewHolder holder, int position, @NonNull Device model) {

                String deviceKey = getRef(position).getKey();

                holder.roomName.setText(model.getDeviceName() != null ? model.getDeviceName() : "Unnamed Device");

                if (deviceKey != null) {
                    String deviceID = holder.itemView.getContext().getString(R.string.ID, deviceKey);
                    holder.deviceID.setText(deviceID);
                } else {
                    holder.deviceID.setText(R.string.IDnotavailable);
                }

                holder.deleteIcon.setOnClickListener(v -> {
                    if (deviceKey != null) {
                        new AlertDialog.Builder(addDevices.this)
                                .setTitle(R.string.deletedevice)
                                .setMessage(R.string.deleteconfirmation + model.getDeviceName() + "?")
                                .setPositiveButton(android.R.string.yes, (dialog, which) -> mDatabase.child("devices").child(deviceKey).removeValue((error, ref) -> {
                                    if (error != null) {
                                        Log.e(TAG, "Failed to delete device " + deviceKey + ": " + error.getMessage());
                                        Toast.makeText(addDevices.this, R.string.deletefail, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Log.d(TAG, "Device deleted successfully: " + deviceKey);
                                        Toast.makeText(addDevices.this, model.getDeviceName() + R.string.deleted, Toast.LENGTH_SHORT).show();
                                    }
                                }))
                                .setNegativeButton(android.R.string.no, null)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    } else {
                        Log.w(TAG, "Attempted to delete a device with a null key at position: " + position);
                        Toast.makeText(addDevices.this, R.string.deleteIoTerror, Toast.LENGTH_SHORT).show();
                    }
                });

                holder.editIcon.setOnClickListener(v -> {
                    Device currentDevice = getItem(position);
                    if (deviceKey != null && currentDevice != null) {
                        EditIoTDeviceDialog(deviceKey, currentDevice);
                    } else {
                        Log.w(TAG, "Attempted to edit a device with a null key or model at position: " + position);
                        Toast.makeText(addDevices.this, R.string.errorediting, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
        IoTDeviceRecyclerView.setAdapter(firebaseAdapter);
    }

    public static class IoTDeviceViewHolder extends RecyclerView.ViewHolder {
        TextView roomName;
        TextView deviceID;
        ImageView editIcon;
        ImageView deleteIcon;

        public IoTDeviceViewHolder(View itemView) {
            super(itemView);
            roomName = itemView.findViewById(R.id.room_name);
            deviceID = itemView.findViewById(R.id.device_id);
            editIcon = itemView.findViewById(R.id.edit_icon);
            deleteIcon = itemView.findViewById(R.id.delete_icon);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (firebaseAdapter != null) {
            firebaseAdapter.startListening();
            Log.d(TAG, "Adapter started listening");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (firebaseAdapter != null) {
            firebaseAdapter.stopListening();
            Log.d(TAG, "Adapter stopped listening");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (firebaseAdapter != null) {
            firebaseAdapter.stopListening();
        }
        firebaseAdapter = null;
        Log.d(TAG, "Activity destroyed");
    }
}
