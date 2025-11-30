package com.example.trackback;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class MatchesFragment extends Fragment {

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

        String userId = mAuth.getCurrentUser().getUid();

        matchingService.findMatchesForUser(userId,
                new MatchingService.OnMatchesFoundListener() {
                    @Override
                    public void onMatchesFound(List<ItemMatch> matches) {
                        progressBar.setVisibility(View.GONE);

                        if (matches.isEmpty()) {
                            emptyView.setVisibility(View.VISIBLE);
                            emptyView.setText("No matches yet\n\nWe'll notify you when we find potential matches for your items.");
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                            adapter.updateMatches(matches);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        progressBar.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                        emptyView.setText("Post an item to see matches here!");
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMatches(); // Refresh matches when tab is reopened
    }
}