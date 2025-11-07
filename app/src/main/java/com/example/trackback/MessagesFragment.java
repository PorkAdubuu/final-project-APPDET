package com.example.trackback;

import android.annotation.SuppressLint;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MessagesFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private ImageView backBtn;
    private FirebaseFirestore firestore;
    private String currentUserId;

    private ChatListAdapter chatListAdapter;
    private List<ChatListItem> chatList;

    public MessagesFragment() {
        // Required empty public constructor
    }

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        // This XML should be your chat list layout, not the conversation layout
        View view = inflater.inflate(R.layout.activity_chat, container, false);

        recyclerView = view.findViewById(R.id.messageRecyclerView); // Ensure this ID matches your chat list XML
        emptyText = view.findViewById(R.id.emptyText);
        backBtn = view.findViewById(R.id.imageView8);

        firestore = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(chatList, getContext());
        recyclerView.setAdapter(chatListAdapter);

        loadChatList();

        backBtn.setOnClickListener(v -> requireActivity().onBackPressed());

        return view;
    }

    private void loadChatList() {
        firestore.collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    // Keep only one chat per "other user" (latest message first)
                    Map<String, ChatListItem> latestMessages = new LinkedHashMap<>();

                    for (DocumentChange dc : value.getDocumentChanges()) {
                        ChatListItem chat = dc.getDocument().toObject(ChatListItem.class);

                        // Only include messages where the user is sender or receiver
                        if (!chat.getSenderId().equals(currentUserId) &&
                                !chat.getReceiverId().equals(currentUserId)) continue;

                        // Identify the other user in the conversation
                        String otherUserId = chat.getSenderId().equals(currentUserId)
                                ? chat.getReceiverId()
                                : chat.getSenderId();

                        // Only keep the first message for each user (Firestore is ordered DESC)
                        if (!latestMessages.containsKey(otherUserId)) {
                            latestMessages.put(otherUserId, chat);
                        }
                    }

                    chatList.clear();
                    chatList.addAll(latestMessages.values());
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
