package com.example.trackback;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ArchivedChatsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private LinearLayout backBtn;
    private ArchivedChatsAdapter adapter;
    private List<ChatListItem> archivedChats;
    private FirebaseFirestore firestore;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        // Initialize views from your existing layout
        recyclerView = findViewById(R.id.messageRecyclerView);
        emptyText = findViewById(R.id.emptyTextView);
        backBtn = findViewById(R.id.backBtn);

        // Setup back button
        backBtn.setOnClickListener(v -> finish());

        // Setup RecyclerView
        archivedChats = new ArrayList<>();
        adapter = new ArchivedChatsAdapter(archivedChats, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadArchivedChats();
    }

    private void loadArchivedChats() {
        firestore.collection("chatList")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        android.util.Log.e("ArchivedChats", "Error loading archived chats", error);
                        return;
                    }

                    archivedChats.clear();

                    if (value != null && !value.isEmpty()) {
                        for (QueryDocumentSnapshot doc : value) {
                            // Check if this chat is archived for the current user
                            Boolean isArchived = doc.getBoolean("archivedFor_" + currentUserId);
                            if (isArchived != null && isArchived) {
                                // Get the other user's ID
                                String[] userIds = doc.getId().split("_");
                                String receiverId = userIds[0].equals(currentUserId) ? userIds[1] : userIds[0];

                                // Create chat item
                                ChatListItem item = new ChatListItem();
                                item.setReceiverId(receiverId);
                                item.setLastMessage(doc.getString("lastMessage"));
                                item.setTimestamp(doc.getTimestamp("timestamp"));
                                item.setLastSenderId(doc.getString("lastSenderId"));
                                item.setSenderId(currentUserId);
                                item.setDocumentId(doc.getId());

                                // Load user profile data and add to list
                                loadUserProfileAndAdd(item, receiverId);
                            }
                        }
                    } else {
                        updateEmptyState();
                    }
                });
    }

    private void loadUserProfileAndAdd(ChatListItem item, String userId) {
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("fullname");
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                        item.setName(name != null ? name : "User");
                        item.setProfileImageUrl(profileImageUrl);

                        android.util.Log.d("ArchivedChats", "Loaded user: " + item.getName() + ", ID: " + userId);
                    } else {
                        item.setName("User");
                        android.util.Log.w("ArchivedChats", "User document not found for ID: " + userId);
                    }

                    // Add to list after profile is loaded
                    archivedChats.add(item);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ArchivedChats", "Error loading user profile for " + userId, e);
                    item.setName("User");

                    // Add to list even if profile loading fails
                    archivedChats.add(item);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
    }

    private void updateEmptyState() {
        if (archivedChats.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // Method to refresh the list (called from adapter when chat is unarchived)
    public void refreshList() {
        loadArchivedChats();
    }
}