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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView welcomeTextView;
    private ImageView profileImageView;

    private RecyclerView recyclerView;
    private List<ListLostItem> lostItemList = new ArrayList<>();
    private ListLostItemsAdapter adapter;
    private FirebaseFirestore db;

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Set up RecyclerView
        recyclerView = view.findViewById(R.id.recentItemsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize adapter and set it to RecyclerView
        adapter = new ListLostItemsAdapter(lostItemList, getContext());
        recyclerView.setAdapter(adapter);

        // Check if the user is authenticated
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            fetchLostItemsFromFirestore();
        } else {
            Log.w("Firestore", "User not authenticated");
        }

        return view;
    }

    private void fetchLostItemsFromFirestore() {
        CollectionReference lostItemsRef = db.collection("lostItems");
        lostItemsRef.orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            lostItemList.clear();
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                ListLostItem lostItem = document.toObject(ListLostItem.class);
                                lostItemList.add(lostItem);
                            }
                            Log.d("Firestore", "Loaded items: " + lostItemList.size());
                            adapter.notifyDataSetChanged();
                        } else {
                            Log.w("Firestore", "No documents found");
                        }
                    } else {
                        Log.e("Firestore", "Error getting documents: ", task.getException());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Fetch failed: ", e);
                });
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        welcomeTextView = view.findViewById(R.id.welcomeTextView);
        profileImageView = view.findViewById(R.id.profileImageView);
        TextView dayTextView = view.findViewById(R.id.dayTextView);
        TextView monthTextView = view.findViewById(R.id.monthTextView);

        if (user != null) {
            String fullName = user.getDisplayName();
            String firstName = fullName != null ? fullName.split(" ")[0] : "User";

            if (!firstName.isEmpty()) {
                firstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1).toLowerCase();
            }

            welcomeTextView.setText("Hi, " + firstName + "!");
        }

        if (user != null && user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .into(profileImageView);
        } else {
            Glide.with(this)
                    .load(R.drawable.default_avatar)
                    .circleCrop()
                    .into(profileImageView);
        }

        SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());

        Date currentDate = new Date();

        dayTextView.setText(dayFormat.format(currentDate));
        monthTextView.setText(monthFormat.format(currentDate).toUpperCase());

        TextView seeAllBtn = view.findViewById(R.id.seeAllBtn);
        seeAllBtn.setOnClickListener(v -> {
            // Get activity and cast it to HomeActivity
            HomeActivity activity = (HomeActivity) requireActivity();

            // Find BottomNavigationView from activity
            BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNavigationView);

            // Programmatically select the nav_search item (triggers listener in HomeActivity)
            bottomNav.setSelectedItemId(R.id.nav_search);
        });



    }
}
