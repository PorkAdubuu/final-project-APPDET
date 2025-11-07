package com.example.trackback;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BlockedAccountsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private ImageView backBtn;
    private BlockedAccountsAdapter adapter;
    private List<BlockedUser> blockedUsers;
    private FirebaseFirestore firestore;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blocked_accounts);

        firestore = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        recyclerView = findViewById(R.id.blockedAccountsRecyclerView);
        emptyText = findViewById(R.id.emptyBlockedText);
        backBtn = findViewById(R.id.backButton);

        backBtn.setOnClickListener(v -> finish());

        blockedUsers = new ArrayList<>();
        adapter = new BlockedAccountsAdapter(blockedUsers, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadBlockedUsers();
    }

    private void loadBlockedUsers() {
        firestore.collection("users")
                .document(currentUserId)
                .collection("blockedUsers")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    blockedUsers.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        BlockedUser user = new BlockedUser();
                        user.setBlockedUserId(doc.getId());
                        user.setBlockedUserName(doc.getString("blockedUserName"));
                        user.setBlockedUserProfileUrl(doc.getString("blockedUserProfileUrl"));
                        user.setTimestamp(doc.getTimestamp("timestamp")); // if you save timestamp
                        blockedUsers.add(user);
                    }

                    adapter.notifyDataSetChanged();

                    if (blockedUsers.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyText.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                });
    }


    public void refreshList() {
        loadBlockedUsers();
    }

}