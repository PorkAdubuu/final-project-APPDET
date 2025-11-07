package com.example.trackback;

import android.os.Bundle;
import android.view.View;
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

/**
 * Activity for displaying archived chats with unarchive option
 */
public class ArchiveActivity extends AppCompatActivity {

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

        // Initialize views (using IDs from your activity_archive.xml)
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

    /**
     * Load all archived chats for current user
     */
    private void loadArchivedChats() {
        firestore.collection("chatList")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        android.util.Log.e("ArchiveActivity", "Error loading archived chats", error);
                        return;
                    }

                    archivedChats.clear();

                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            // Check if archived for current user
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

                                // Load user profile
                                loadUserProfile(item, receiverId);

                                archivedChats.add(item);
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
    }

    /**
     * Load user profile data for chat item
     */
    private void loadUserProfile(ChatListItem item, String userId) {
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Try multiple name fields
                        String name = documentSnapshot.getString("fullname");
                        if (name == null || name.isEmpty()) name = documentSnapshot.getString("fullName");
                        if (name == null || name.isEmpty()) name = documentSnapshot.getString("name");
                        if (name == null || name.isEmpty()) name = documentSnapshot.getString("username");
                        if (name == null || name.isEmpty()) name = "User";

                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        if (profileImageUrl == null) profileImageUrl = "";

                        item.setName(name);
                        item.setProfileImageUrl(profileImageUrl);

                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ArchiveActivity", "Error loading user profile", e);
                });
    }


    /**
     * Update empty state visibility
     */
    private void updateEmptyState() {
        if (archivedChats.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Refresh list (called from adapter after unarchiving)
     */
    public void refreshList() {
        loadArchivedChats();
    }
}