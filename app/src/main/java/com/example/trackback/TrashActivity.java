package com.example.trackback;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TrashActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyTextView;
    private ImageView backButton;
    private TrashAdapter adapter;
    private List<LostItem> trashedItems;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        recyclerView = findViewById(R.id.trashRecyclerView);
        emptyTextView = findViewById(R.id.emptyTextView);
        backButton = findViewById(R.id.backButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        trashedItems = new ArrayList<>();
        adapter = new TrashAdapter(this, trashedItems);
        recyclerView.setAdapter(adapter);

        backButton.setOnClickListener(v -> onBackPressed());

        loadTrashedPosts();
    }

    private void loadTrashedPosts() {
        if (currentUserId == null) return;

        db.collection("lostItems")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("isDeleted", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    trashedItems.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        LostItem item = doc.toObject(LostItem.class);
                        item.setDocumentId(doc.getId());
                        trashedItems.add(item);
                    }

                    adapter.notifyDataSetChanged();

                    if (trashedItems.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyTextView.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyTextView.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    emptyTextView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTrashedPosts(); // Refresh when returning to this activity
    }
}