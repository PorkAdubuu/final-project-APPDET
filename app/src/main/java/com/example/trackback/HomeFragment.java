package com.example.trackback;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    private TextView welcomeTextView;
    private ImageView profileImageView;
    private FirebaseFirestore db;
    private PieChart pieChart;
    private TextView tvFound, tvLost;
    private TabLayout contentTabLayout;
    private ViewPager2 contentViewPager;
    private ProgressBar analyticsProgressBar;  // Add loading indicator

    private TextView messageBadge;
    private ListenerRegistration messageListener;
    private String currentUserId;

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        }

        return view;
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
        contentTabLayout = view.findViewById(R.id.contentTabLayout);
        contentViewPager = view.findViewById(R.id.contentViewPager);
        analyticsProgressBar = view.findViewById(R.id.analyticsProgressBar);  // Initialize


        ImageView messageIcon = view.findViewById(R.id.messageIcon);
        if (messageIcon != null) {
            messageIcon.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), MessagesActivity.class);
                startActivity(intent);
            });
        }

        if (user != null) {
            String fullName = user.getDisplayName();
            String firstName = fullName != null ? fullName.split(" ")[0] : "User";
            firstName = !firstName.isEmpty() ? firstName.substring(0,1).toUpperCase() + firstName.substring(1).toLowerCase() : "User";
            if (welcomeTextView != null) {
                welcomeTextView.setText("Hi, " + firstName + "!");
            }
        }

        if (user != null && user.getPhotoUrl() != null && profileImageView != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .into(profileImageView);
        } else if (profileImageView != null) {
            Glide.with(this)
                    .load(R.drawable.default_avatar)
                    .circleCrop()
                    .into(profileImageView);
        }

        SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
        Date currentDate = new Date();

        if (dayTextView != null) {
            dayTextView.setText(dayFormat.format(currentDate));
        }
        if (monthTextView != null) {
            monthTextView.setText(monthFormat.format(currentDate).toUpperCase());
        }

        setupViewPager();
        fetchAnalyticsData();

        if (currentUserId != null) {
            listenForUnreadMessages();
        }
    }

    private void setupViewPager() {
        if (contentViewPager == null || contentTabLayout == null) {
            Log.e(TAG, "ViewPager or TabLayout is null!");
            return;
        }

        ContentPagerAdapter adapter = new ContentPagerAdapter(requireActivity());
        contentViewPager.setAdapter(adapter);

        new TabLayoutMediator(contentTabLayout, contentViewPager,
                (tab, position) -> {
                    if (position == 0) {
                        tab.setText("For You");
                    } else {
                        tab.setText("Recent Reports");
                    }
                }
        ).attach();
    }

    private void fetchAnalyticsData() {
        if (db == null) {
            Log.e(TAG, "Firestore is null!");
            return;
        }

        // Show loading state
        showLoadingState();

        Log.d(TAG, "Fetching analytics data...");

        db.collection("lostItems")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int lostCount = 0;
                    int foundCount = 0;

                    Log.d(TAG, "Total documents in 'lostItems': " + querySnapshot.size());

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Boolean isDeleted = doc.getBoolean("isDeleted");
                        if (isDeleted != null && isDeleted) {
                            continue;
                        }

                        String reportType = doc.getString("reportType");

                        Log.d(TAG, "Document ID: " + doc.getId());
                        Log.d(TAG, "  reportType: " + reportType);

                        if (reportType != null) {
                            if ("LOST".equalsIgnoreCase(reportType) || "Lost".equalsIgnoreCase(reportType)) {
                                lostCount++;
                            } else if ("FOUND".equalsIgnoreCase(reportType) || "Found".equalsIgnoreCase(reportType)) {
                                foundCount++;
                            }
                        }
                    }

                    Log.d(TAG, "Found: " + foundCount + ", Lost: " + lostCount);
                    updateUIWithCounts(foundCount, lostCount);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching from 'lostItems'", e);
                    updateUIWithCounts(0, 0);
                });
    }

    private void showLoadingState() {
        if (analyticsProgressBar != null) {
            analyticsProgressBar.setVisibility(View.VISIBLE);
        }
        if (pieChart != null) {
            pieChart.setVisibility(View.INVISIBLE);
        }
        if (tvLost != null) {
            tvLost.setAlpha(0.3f);
        }
        if (tvFound != null) {
            tvFound.setAlpha(0.3f);
        }
    }

    private void hideLoadingState() {
        if (analyticsProgressBar != null) {
            analyticsProgressBar.setVisibility(View.GONE);
        }
        if (pieChart != null) {
            pieChart.setVisibility(View.VISIBLE);
        }
        if (tvLost != null) {
            tvLost.animate().alpha(1f).setDuration(300).start();
        }
        if (tvFound != null) {
            tvFound.animate().alpha(1f).setDuration(300).start();
        }
    }

    private void updateUIWithCounts(int foundCount, int lostCount) {
        if (tvLost != null) {
            tvLost.setText("Lost: " + lostCount);
        }
        if (tvFound != null) {
            tvFound.setText("Found: " + foundCount);
        }

        if (pieChart != null) {
            setupPieChart(foundCount, lostCount);
        } else {
            Log.e(TAG, "PieChart is null!");
        }

        // Hide loading state after data is loaded
        hideLoadingState();
    }

    private void setupPieChart(int foundCount, int lostCount) {
        Log.d(TAG, "Setting up pie chart with Found: " + foundCount + ", Lost: " + lostCount);

        ArrayList<PieEntry> entries = new ArrayList<>();

        if (foundCount > 0) {
            entries.add(new PieEntry(foundCount, "Found"));
        }
        if (lostCount > 0) {
            entries.add(new PieEntry(lostCount, "Lost"));
        }

        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, "No Data"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");

        ArrayList<Integer> colors = new ArrayList<>();
        if (foundCount > 0) {
            colors.add(Color.parseColor("#3a68dc"));
        }
        if (lostCount > 0) {
            colors.add(Color.parseColor("#4dbdf7"));
        }
        if (entries.isEmpty() || (foundCount == 0 && lostCount == 0)) {
            colors.add(Color.parseColor("#CCCCCC"));
        }

        dataSet.setColors(colors);
        dataSet.setDrawValues(false);
        dataSet.setValueTextSize(0f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(50f);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setTransparentCircleAlpha(0);
        pieChart.setRotationEnabled(false);
        pieChart.setTouchEnabled(false);
        pieChart.setDrawEntryLabels(false);

        // Add smooth animations
        pieChart.animateY(1200, Easing.EaseInOutQuad);
        pieChart.animateX(1200, Easing.EaseInOutQuad);

        pieChart.invalidate();

        Log.d(TAG, "Pie chart setup complete with animation");
    }

    private void listenForUnreadMessages() {
        if (currentUserId == null || db == null) return;

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

                        Boolean isArchived = (Boolean) data.get("archivedFor_" + currentUserId);
                        if (isArchived != null && isArchived) {
                            continue;
                        }

                        Boolean unreadValue = (Boolean) data.get("unread");
                        String lastSenderId = (String) data.get("lastSenderId");

                        boolean isUnread = (unreadValue != null && unreadValue) &&
                                (lastSenderId != null && !currentUserId.equals(lastSenderId));

                        if (isUnread) {
                            unreadCount++;
                        }
                    }

                    updateMessageBadge(unreadCount);
                });
    }

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
        if (messageListener == null && currentUserId != null) {
            listenForUnreadMessages();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }
    }

    public static class ContentPagerAdapter extends FragmentStateAdapter {

        public ContentPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new MatchesFragment();
            } else {
                return new RecentReportsFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    public static class MatchesFragment extends Fragment {

        private RecyclerView recyclerView;
        private MatchesAdapter adapter;
        private ProgressBar progressBar;
        private TextView emptyView;
        private MatchingService matchingService;
        private FirebaseAuth mAuth;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_matches, container, false);

            recyclerView = view.findViewById(R.id.recyclerViewMatches);
            progressBar = view.findViewById(R.id.progressBar);
            emptyView = view.findViewById(R.id.emptyView);

            mAuth = FirebaseAuth.getInstance();
            matchingService = new MatchingService();

            setupRecyclerView();
            loadMatches();

            return view;
        }

        private void setupRecyclerView() {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new MatchesAdapter(new ArrayList<>());
            recyclerView.setAdapter(adapter);
        }

        private void loadMatches() {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);

            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                progressBar.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                emptyView.setText("Please sign in to see matches");
                return;
            }

            String userId = user.getUid();

            matchingService.findMatchesForUser(userId,
                    new MatchingService.OnMatchesFoundListener() {
                        @Override
                        public void onMatchesFound(List<ItemMatch> matches) {
                            progressBar.setVisibility(View.GONE);

                            if (matches.isEmpty()) {
                                emptyView.setVisibility(View.VISIBLE);
                                emptyView.setText("No matches yet\n\nWe'll notify you when we find potential matches for your lost items using our Cosine Similarity algorithm.");
                            } else {
                                recyclerView.setVisibility(View.VISIBLE);
                                adapter.updateMatches(matches);
                            }
                        }

                        @Override
                        public void onError(String error) {
                            progressBar.setVisibility(View.GONE);
                            emptyView.setVisibility(View.VISIBLE);
                            emptyView.setText("Post a lost item to see matches here!");
                        }
                    });
        }

        @Override
        public void onResume() {
            super.onResume();
            loadMatches();
        }
    }

    public static class RecentReportsFragment extends Fragment {

        private RecyclerView recyclerView;
        private RecentItemsAdapter adapter;
        private ProgressBar progressBar;
        private TextView emptyView;
        private FirebaseFirestore db;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_recent_reports, container, false);

            recyclerView = view.findViewById(R.id.recyclerViewRecentReports);
            progressBar = view.findViewById(R.id.progressBar);
            emptyView = view.findViewById(R.id.emptyView);

            db = FirebaseFirestore.getInstance();

            setupRecyclerView();
            loadRecentReports();

            return view;
        }

        private void setupRecyclerView() {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new RecentItemsAdapter(new ArrayList<>(), getContext());
            recyclerView.setAdapter(adapter);
        }

        private void loadRecentReports() {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);

            Log.d("RecentReportsFragment", "Loading recent reports...");

            db.collection("lostItems")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        Log.d("RecentReportsFragment", "Items fetched: " + querySnapshot.size());

                        List<Item> items = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Log.d("RecentReportsFragment", "Document ID: " + doc.getId());
                            Log.d("RecentReportsFragment", "Document data: " + doc.getData());

                            try {
                                Boolean isDeleted = doc.getBoolean("isDeleted");
                                if (isDeleted != null && isDeleted) {
                                    continue;
                                }

                                LostItem lostItem = doc.toObject(LostItem.class);
                                if (lostItem != null) {
                                    Item item = convertLostItemToItem(lostItem);
                                    item.setId(doc.getId());
                                    items.add(item);
                                    Log.d("RecentReportsFragment", "Item added successfully: " + doc.getId());
                                }
                            } catch (Exception e) {
                                Log.e("RecentReportsFragment", "Failed to convert document: " + doc.getId(), e);
                            }
                        }

                        progressBar.setVisibility(View.GONE);

                        if (items.isEmpty()) {
                            emptyView.setVisibility(View.VISIBLE);
                            emptyView.setText("No reports yet");
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                            adapter.updateItems(items);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("RecentReportsFragment", "Error loading from 'lostItems'", e);
                        progressBar.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                        Toast.makeText(getContext(), "Error loading reports: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }

        private Item convertLostItemToItem(LostItem lostItem) {
            Item item = new Item();
            item.setUserId(lostItem.getUserId());
            item.setType(lostItem.getReportType());
            item.setTitle(lostItem.getItemLost());
            item.setDescription(lostItem.getAdditionalInfo());
            item.setCategory(lostItem.getCategory());
            item.setLocation(lostItem.getLastSeen());
            item.setTimestamp(lostItem.getTimestamp());
            item.setImageUrl(lostItem.getItemImageUrl());
            return item;
        }
    }
}