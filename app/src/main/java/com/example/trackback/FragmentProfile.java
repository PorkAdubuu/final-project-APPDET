package com.example.trackback;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;

public class FragmentProfile extends Fragment {

    private static final String TAG = "FragmentProfile";

    private TextView NameTextview, emailText, phoneNumberText;
    private ImageView profileImageView;
    private RecyclerView postRecyclerView;
    private PostAdapter postAdapter;
    private List<LostItem> userReports;

    public FragmentProfile() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        NameTextview = view.findViewById(R.id.NameTextview);
        emailText = view.findViewById(R.id.emailText);
        phoneNumberText = view.findViewById(R.id.phoneNumberText);
        profileImageView = view.findViewById(R.id.profileImageView);
        postRecyclerView = view.findViewById(R.id.itemsRecyclerView);

        View logoutBtn = view.findViewById(R.id.logoutBtn);

        logoutBtn.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Log out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Configure Google Sign-In client (same as in MainActivity)
                        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(getString(R.string.client_id))  // use your client ID
                                .requestEmail()
                                .build();

                        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(requireContext(), gso);

                        // Sign out from Firebase and Google
                        FirebaseAuth.getInstance().signOut();
                        googleSignInClient.signOut().addOnCompleteListener(task -> {
                            // After sign-out complete, finish activity and go to MainActivity
                            requireActivity().finish();
                            startActivity(new android.content.Intent(requireContext(), MainActivity.class));
                        });
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });


        // Setup RecyclerView
        postRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        userReports = new ArrayList<>();
        postAdapter = new PostAdapter(requireContext(), userReports);
        postRecyclerView.setAdapter(postAdapter);



        
        if (user != null) {
            Log.d(TAG, "User is logged in: " + user.getUid());

            // Format and display user name
            String fullName = user.getDisplayName();
            if (fullName != null && fullName.contains(" ")) {
                String[] nameParts = fullName.trim().split("\\s+");
                String firstName = nameParts[0];
                String lastName = nameParts[nameParts.length - 1];
                firstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1).toLowerCase();
                lastName = lastName.substring(0, 1).toUpperCase() + lastName.substring(1).toLowerCase();
                NameTextview.setText(lastName + ", " + firstName);
            } else {
                NameTextview.setText(fullName != null ? fullName : "User");
            }

            emailText.setText(user.getEmail() != null ? user.getEmail() : "No email available");
            phoneNumberText.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : "No phone number");

            // Load profile image or default
            if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(profileImageView);
            } else {
                Glide.with(this).load(R.drawable.default_avatar).circleCrop().into(profileImageView);
            }

            // Firestore query to load user reports filtered by userId
            String userId = user.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("lostItems")
                    .whereEqualTo("userId", userId)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null) {
                            Log.e(TAG, "Listen failed.", e);
                            return;
                        }

                        userReports.clear();
                        if (snapshots != null) {
                            for (QueryDocumentSnapshot doc : snapshots) {
                                LostItem item = doc.toObject(LostItem.class);
                                userReports.add(item);
                            }
                            Log.d(TAG, "Loaded " + userReports.size() + " user reports.");
                        }
                        postAdapter.notifyDataSetChanged();
                    });

        } else {
            Log.d(TAG, "No user is logged in.");
        }
    }


}

