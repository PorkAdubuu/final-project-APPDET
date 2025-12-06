package com.example.trackback;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private RecyclerView notificationRecyclerView;
    private List<NotificationModel> notificationList;
    private NotificationAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        notificationRecyclerView = view.findViewById(R.id.notificationRecyclerView);
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(getContext(), notificationList);
        notificationRecyclerView.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadNotifications();

        LinearLayout backBtn = view.findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_overlay, new HomeFragment())
                    .commit();
        });

        return view;
    }

    private void loadNotifications() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)  // Use timestamp instead
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();  // Clear first to avoid duplicates
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        NotificationModel notif = doc.toObject(NotificationModel.class);
                        if (notif != null) {
                            notif.setNotificationDocId(doc.getId());
                            notificationList.add(notif);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show()
                );
    }
}