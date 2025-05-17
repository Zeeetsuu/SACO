package com.example.sacov3;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import android.widget.LinearLayout;

import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentSettings#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentSettings extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private FirebaseAuth mAuth;
    private TextView usernameTextView;
    private static final String DEFAULT_USER_NAME = "Mr. Guest";

    public FragmentSettings() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentSettings.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentSettings newInstance(String param1, String param2) {
        FragmentSettings fragment = new FragmentSettings();
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        usernameTextView = view.findViewById(R.id.settingsText1);
        TextView editTextView = view.findViewById(R.id.settingsEdit);
        Button logoutButton = view.findViewById(R.id.settingsLogOut);
        Button securityButton = view.findViewById(R.id.settingsSecurity);
        Button languageButton = view.findViewById(R.id.settingsLanguage);
        Button metricButton = view.findViewById(R.id.settingsMetric);
        Button aboutUsButton = view.findViewById(R.id.settingsAboutUs);
        Button contactUsButton = view.findViewById(R.id.settingsContactUs);

        loadUserInfo();

        editTextView.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                EditUsernameDialog(currentUser);
            } else {
                Toast.makeText(getContext(), R.string.mustlogin, Toast.LENGTH_SHORT).show();
            }
        });

        languageButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), languageScreen.class);
            startActivity(intent);
        });
        securityButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), securityScreen.class);
            startActivity(intent);
        });
        metricButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), metricScreen.class);
            startActivity(intent);
        });
        aboutUsButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), aboutUsScreen.class);
            startActivity(intent);
        });
        contactUsButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), contactUsScreen.class);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.confirmlogout))
                    .setMessage(getString(R.string.areyousure))

                    .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                        mAuth.signOut();
                        Intent intent = new Intent(getActivity(), LoginScreen.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton(getString(R.string.no), (dialog, which) -> {
                    })
                    .show();
        });
        return view;
    }

    private void loadUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            if (!TextUtils.isEmpty(displayName)) {
                usernameTextView.setText(displayName);
            } else {
                usernameTextView.setText(DEFAULT_USER_NAME);
            }
        } else {
            usernameTextView.setText(DEFAULT_USER_NAME);
        }
    }

    private void EditUsernameDialog(@NonNull FirebaseUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.editusername);

        // Input field
        final EditText input = new EditText(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        input.setLayoutParams(lp);
        int paddingDp = 16;
        float density = getResources().getDisplayMetrics().density;
        int paddingPx = (int)(paddingDp * density + 0.5f);
        input.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

        String currentName = user.getDisplayName();
        if (!TextUtils.isEmpty(currentName) && !currentName.equals(DEFAULT_USER_NAME)) {
            input.setText(currentName);
            input.setSelection(currentName.length());
        }

        builder.setView(input);

        builder.setPositiveButton(R.string.Update, (dialog, which) -> {
            String newUsername = input.getText().toString().trim();

            if (TextUtils.isEmpty(newUsername)) {
                Toast.makeText(getContext(), R.string.usernamecantbeempty, Toast.LENGTH_SHORT).show();
                return;
            }

            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newUsername)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            usernameTextView.setText(newUsername);
                            Toast.makeText(getContext(), R.string.usernameupdatesuccess, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), R.string.usernameupdatefail + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        builder.setNegativeButton(R.string.Cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
