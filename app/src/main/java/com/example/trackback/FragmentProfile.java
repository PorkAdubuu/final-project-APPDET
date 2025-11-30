package com.example.trackback;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
    private TabLayout tabLayout;
    private List<LostItem> allReports;

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

        LinearLayout backBtn = view.findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_overlay, new HomeFragment())
                    .commit();
        });

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        NameTextview = view.findViewById(R.id.NameTextview);
        emailText = view.findViewById(R.id.emailText);
        phoneNumberText = view.findViewById(R.id.phoneNumberText);
        profileImageView = view.findViewById(R.id.profileImageView);
        postRecyclerView = view.findViewById(R.id.itemsRecyclerView);
        tabLayout = view.findViewById(R.id.tab_layout);

        // Setup hamburger menu button
        ImageView menuButton = view.findViewById(R.id.menuButton);
        menuButton.setOnClickListener(v -> showBottomSheetMenu());

        // Setup RecyclerView
        postRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        allReports = new ArrayList<>();
        userReports = new ArrayList<>();
        postAdapter = new PostAdapter(requireContext(), userReports);
        postRecyclerView.setAdapter(postAdapter);

        // Set tab listener FIRST before loading data
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                Log.d(TAG, "Tab selected: " + position);
                filterReportsByTab(position);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                filterReportsByTab(tab.getPosition());
            }
        });

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
            phoneNumberText.setVisibility(View.GONE);

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

                        allReports.clear();
                        if (snapshots != null) {
                            for (QueryDocumentSnapshot doc : snapshots) {
                                LostItem item = doc.toObject(LostItem.class);
                                allReports.add(item);
                                Log.d(TAG, "Loaded item - reportType: '" + item.getReportType() + "'");
                            }
                            Log.d(TAG, "Total loaded: " + allReports.size() + " user reports");
                        }

                        // Filter based on currently selected tab
                        filterReportsByTab(tabLayout.getSelectedTabPosition());
                    });

        } else {
            Log.d(TAG, "No user is logged in.");
        }
    }

    private void filterReportsByTab(int tabPosition) {
        Log.d(TAG, "=== FILTERING Tab: " + tabPosition + " ===");
        Log.d(TAG, "Total items available: " + allReports.size());

        userReports.clear();

        switch (tabPosition) {
            case 0: // All
                userReports.addAll(allReports);
                Log.d(TAG, "Showing ALL: " + userReports.size() + " items");
                break;

            case 1: // Lost
                for (LostItem item : allReports) {
                    String reportType = item.getReportType();
                    if (reportType != null && reportType.trim().equalsIgnoreCase("lost")) {
                        userReports.add(item);
                    }
                }
                Log.d(TAG, "Showing LOST: " + userReports.size() + " items");
                break;

            case 2: // Found
                for (LostItem item : allReports) {
                    String reportType = item.getReportType();
                    if (reportType != null && reportType.trim().equalsIgnoreCase("found")) {
                        userReports.add(item);
                    }
                }
                Log.d(TAG, "Showing FOUND: " + userReports.size() + " items");
                break;
        }

        Log.d(TAG, "Notifying adapter with " + userReports.size() + " items");
        postAdapter.notifyDataSetChanged();

        // Scroll to top after filtering
        if (postRecyclerView != null) {
            postRecyclerView.scrollToPosition(0);
        }
    }

    private void showBottomSheetMenu() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View bottomSheetView = LayoutInflater.from(getContext()).inflate(R.layout.account_menu, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        LinearLayout menuBlocked = bottomSheetView.findViewById(R.id.blockedAccountsOption);
        LinearLayout menuLogout = bottomSheetView.findViewById(R.id.LogoutOption);

        menuBlocked.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(getContext(), BlockedAccountsActivity.class);
            startActivity(intent);
        });

        menuLogout.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showLogoutDialog();
        });

        bottomSheetDialog.show();
    }

    private void showLogoutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Log out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.client_id))
                            .requestEmail()
                            .build();
                    GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(requireContext(), gso);

                    FirebaseAuth.getInstance().signOut();
                    googleSignInClient.signOut().addOnCompleteListener(task -> {
                        requireActivity().finish();
                        startActivity(new Intent(requireContext(), MainActivity.class));
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}