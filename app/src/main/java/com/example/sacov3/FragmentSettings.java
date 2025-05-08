package com.example.sacov3;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView; // Import TextView
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser; // Import FirebaseUser
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils; // Import TextUtils to check for empty strings


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

    // Define a default name for when the user is not signed in or has no display name
    private static final String DEFAULT_USER_NAME = "Mr. Guest"; // Matches your XML placeholder

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

        TextView usernameTextView = view.findViewById(R.id.settingsText1);
        Button logoutButton = view.findViewById(R.id.settingsLogOut);

        // Change Mr.guest to the actual name of the user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is signed in, get their display name (it isn't possible that a user can access this activity without being logged in, but just in case)
            String displayName = currentUser.getDisplayName();

            // Check if the display name is not null or empty
            if (!TextUtils.isEmpty(displayName)) {
                usernameTextView.setText(displayName);
            } else {
                // Display name is empty or null, use the default placeholder
                usernameTextView.setText(DEFAULT_USER_NAME);
            }
        } else {
            // No user is signed in, use the default placeholder
            usernameTextView.setText(DEFAULT_USER_NAME);
        }
        // Logout system
        logoutButton.setOnClickListener(v -> {
            // Confirmation Dialog Logic
            new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.confirmlogout)) // Title of dialog
                    .setMessage(getString(R.string.areyousure)) // Message for the dialog

                    .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                        mAuth.signOut();
                        // IMPORTANT: Replace LoginScreen.class with your actual Login/Start Activity
                        Intent intent = new Intent(getActivity(), LoginScreen.class); // <-- Change LoginScreen.class if needed
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clears the activity stack
                        startActivity(intent);

                        // Optional: If the hosting activity shouldn't stay open after logout
                        // if (getActivity() != null) {
                        //     getActivity().finish();
                        // }
                    })

                    .setNegativeButton(getString(R.string.no), (dialog, which) -> {
                        // dialog.dismiss(); // This happens automatically when a button is clicked
                    })

                    // .setIcon if needed

                    .show(); // Display the dialog
        });
        return view;
    }
}
