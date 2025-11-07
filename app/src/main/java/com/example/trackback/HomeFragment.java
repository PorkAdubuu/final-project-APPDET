package com.example.trackback;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.Intent;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    private TextView welcomeTextView;
    private ImageView profileImageView;
    private RecyclerView recyclerView;
    private List<ListLostItem> lostItemList = new ArrayList<>();
    private ListLostItemsAdapter adapter;
    private FirebaseFirestore db;
    private PieChart pieChart;
    private TextView tvFound, tvLost;

    // Message badge variables
    private TextView messageBadge;
    private ListenerRegistration messageListener;
    private String currentUserId;

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
            Log.w(TAG, "User not authenticated");
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
                        lostItemList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ListLostItem lostItem = document.toObject(ListLostItem.class);
                            lostItemList.add(lostItem);
                        }
                        Log.d(TAG, "Loaded items: " + lostItemList.size());
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.e(TAG, "Error getting documents: ", task.getException());
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Fetch failed: ", e));
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

        pieChart = view.findViewById(R.id.pieChart);
        tvFound = view.findViewById(R.id.tvFound);
        tvLost = view.findViewById(R.id.tvLost);

        messageBadge = view.findViewById(R.id.messageBadge);

        ImageView messageIcon = view.findViewById(R.id.messageIcon);
        messageIcon.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MessagesActivity.class);
            startActivity(intent);
        });

        // Set user info
        if (user != null) {
            String fullName = user.getDisplayName();
            String firstName = fullName != null ? fullName.split(" ")[0] : "User";
            firstName = !firstName.isEmpty() ? firstName.substring(0,1).toUpperCase() + firstName.substring(1).toLowerCase() : "User";
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
            HomeActivity activity = (HomeActivity) requireActivity();
            BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNavigationView);
            bottomNav.setSelectedItemId(R.id.nav_search);
        });

        // Fetch analytics data for pie chart
        fetchAnalyticsData();

        listenForUnreadMessages();
    }

    private void fetchAnalyticsData() {
        db.collection("lostItems")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int lostCount = 0;
                    int foundCount = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String type = doc.getString("reportType");
                        if ("Lost".equalsIgnoreCase(type)) lostCount++;
                        else if ("Found".equalsIgnoreCase(type)) foundCount++;
                    }

                    int total = lostCount + foundCount;
                    float lostPercent = total > 0 ? (lostCount * 100f / total) : 0;
                    float foundPercent = total > 0 ? (foundCount * 100f / total) : 0;

                    tvLost.setText("Lost: " + lostCount);
                    tvFound.setText("Found: " + foundCount);

                    ArrayList<PieEntry> entries = new ArrayList<>();
                    if (foundCount > 0) entries.add(new PieEntry(foundPercent));
                    if (lostCount > 0) entries.add(new PieEntry(lostPercent));

                    PieDataSet dataSet = new PieDataSet(entries, "");
                    dataSet.setColors(new int[]{
                            Color.parseColor("#4dbdf7"), // Blue (Found)
                            Color.parseColor("#3a68dc")  // Light Blue (Lost)
                    });
                    dataSet.setValueTextColor(Color.WHITE);
                    dataSet.setValueTextSize(14f);

                    PieData data = new PieData(dataSet);
                    pieChart.setData(data);

                    pieChart.getDescription().setEnabled(false);
                    pieChart.getLegend().setEnabled(false);
                    pieChart.setDrawHoleEnabled(false);
                    pieChart.setDrawEntryLabels(true);
                    pieChart.setEntryLabelColor(Color.WHITE);
                    pieChart.setEntryLabelTextSize(14f);
                    pieChart.animateY(1000, com.github.mikephil.charting.animation.Easing.EaseInOutQuad);
                    pieChart.setRotationEnabled(false);
                    pieChart.setTouchEnabled(false);
                    pieChart.invalidate();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching analytics", e));
    }

    // Listen for unread messages in real-time
    private void listenForUnreadMessages() {
        if (currentUserId == null) return;

        messageListener = db.collection("chatList")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for messages", error);
                        return;
                    }

                    if (querySnapshot == null) return;

                    int unreadCount = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Map<String, Object> data = doc.getData();

                        // Check if archived
                        Boolean isArchived = (Boolean) data.get("archivedFor_" + currentUserId);
                        if (isArchived != null && isArchived) {
                            continue;
                        }

                        // Check if unread
                        Boolean unreadValue = (Boolean) data.get("unread");
                        String lastSenderId = (String) data.get("lastSenderId");

                        // Only count as unread if someone else sent the last message
                        boolean isUnread = (unreadValue != null && unreadValue) &&
                                (lastSenderId != null && !currentUserId.equals(lastSenderId));

                        if (isUnread) {
                            unreadCount++;
                        }
                    }

                    updateMessageBadge(unreadCount);
                });
    }
    // Update the badge visibility and count
    private void updateMessageBadge(int count) {
        if (messageBadge == null) return;

        if (count > 0) {
            messageBadge.setVisibility(View.VISIBLE);
            if (count > 99) {
                messageBadge.setText("99+");
            } else {
                messageBadge.setText(String.valueOf(count));
            }
        } else {
            messageBadge.setVisibility(View.GONE);
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        // Restart listener when fragment resumes
        if (messageListener == null && currentUserId != null) {
            listenForUnreadMessages();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up listener to prevent memory leaks
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }
    }
}
