package com.example.trackback;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class RecentReportsFragment extends Fragment {

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

        db.collection("items")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressBar.setVisibility(View.GONE);

                    List<Item> items = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Item item = doc.toObject(Item.class);
                        if (item != null) {
                            item.setId(doc.getId());
                            items.add(item);
                        }
                    }

                    if (items.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        adapter.updateItems(items);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), "Error loading reports",
                            Toast.LENGTH_SHORT).show();
                });
    }
}