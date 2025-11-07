package com.example.trackback;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MessagesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private ImageView backBtn;
    private FirebaseFirestore firestore;
    private String currentUserId;

    private ChatListAdapter chatListAdapter;
    private List<ChatListItem> chatList;
    private ListenerRegistration chatListListener;
    private Set<String> blockedUserIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        firestore = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        recyclerView = findViewById(R.id.messageRecyclerView);
        emptyText = findViewById(R.id.emptyText);
        backBtn = findViewById(R.id.imageView8);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatList = new ArrayList<>();
        blockedUserIds = new HashSet<>();
        chatListAdapter = new ChatListAdapter(chatList, this);
        recyclerView.setAdapter(chatListAdapter);

        backBtn.setOnClickListener(v -> onBackPressed());

        loadBlockedUsers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatListListener != null) {
            chatListListener.remove();
            chatListListener = null;
        }
    }

    private void loadBlockedUsers() {
        firestore.collection("users")
                .document(currentUserId)
                .collection("blockedUsers")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    blockedUserIds.clear();
                    querySnapshot.forEach(doc -> blockedUserIds.add(doc.getId()));
                    loadChatList();
                })
                .addOnFailureListener(e -> {
                    loadChatList();
                });
    }

    private void loadChatList() {
        if (chatListListener != null) {
            return;
        }

        chatListListener = firestore.collection("chatList")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    for (DocumentChange dc : value.getDocumentChanges()) {
                        Map<String, Object> data = dc.getDocument().getData();
                        if (data == null) continue;

                        // Check if archived
                        Boolean isArchived = (Boolean) data.get("archivedFor_" + currentUserId);
                        if (isArchived != null && isArchived) {
                            continue;
                        }

                        String user1Id = (String) data.get("user1Id");
                        String user2Id = (String) data.get("user2Id");
                        String lastSenderId = (String) data.get("lastSenderId");

                        boolean isUser1 = currentUserId.equals(user1Id);
                        String otherUserId = isUser1 ? user2Id : user1Id;

                        // Check if blocked
                        if (blockedUserIds.contains(otherUserId)) {
                            continue;
                        }

                        String otherUserName = isUser1 ? (String) data.get("user2Name") : (String) data.get("user1Name");
                        String otherUserProfile = isUser1 ? (String) data.get("user2ProfileUrl") : (String) data.get("user1ProfileUrl");

                        Boolean unreadValue = (Boolean) data.get("unread");
                        boolean isUnread = (unreadValue != null && unreadValue) &&
                                (lastSenderId != null && !currentUserId.equals(lastSenderId));

                        String lastMessage = (String) data.get("lastMessage");

                        ChatListItem item = new ChatListItem();
                        item.setLastMessage(lastMessage);
                        item.setTimestamp((com.google.firebase.Timestamp) data.get("timestamp"));
                        item.setName(otherUserName != null ? otherUserName : "User");
                        item.setProfileImageUrl(otherUserProfile);
                        item.setReceiverId(otherUserId);
                        item.setSenderId(currentUserId);
                        item.setUnread(isUnread);
                        item.setLastSenderId(lastSenderId);
                        item.setDocumentId(dc.getDocument().getId());

                        switch (dc.getType()) {
                            case ADDED:
                                boolean exists = false;
                                for (ChatListItem existingItem : chatList) {
                                    if (existingItem.getReceiverId().equals(otherUserId)) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    chatList.add(item);
                                }
                                break;

                            case MODIFIED:
                                for (int i = 0; i < chatList.size(); i++) {
                                    if (chatList.get(i).getReceiverId().equals(otherUserId)) {
                                        chatList.set(i, item);
                                        break;
                                    }
                                }
                                break;

                            case REMOVED:
                                chatList.removeIf(chat -> chat.getReceiverId().equals(otherUserId));
                                break;
                        }
                    }

                    // Sort by timestamp
                    chatList.sort((a, b) -> {
                        if (a.getTimestamp() == null) return 1;
                        if (b.getTimestamp() == null) return -1;
                        return b.getTimestamp().compareTo(a.getTimestamp());
                    });

                    chatListAdapter.notifyDataSetChanged();

                    if (chatList.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyText.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyText.setVisibility(View.GONE);
                    }
                });
    }
}