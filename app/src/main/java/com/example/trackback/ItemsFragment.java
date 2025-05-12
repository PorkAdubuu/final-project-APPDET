package com.example.trackback;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ItemsFragment extends Fragment {

    private RecyclerView recyclerView;
    private List<ListLostItem> lostItemList = new ArrayList<>();
    private ListLostItemsAdapter adapter;
    private FirebaseFirestore db;

    public ItemsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_items, container, false);

        // Set up RecyclerView
        recyclerView = view.findViewById(R.id.lostItemsRecyclerView);  // Reference the RecyclerView from XML
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));  // Use LinearLayoutManager

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize adapter and set it to RecyclerView
        adapter = new ListLostItemsAdapter(lostItemList, getContext());
        recyclerView.setAdapter(adapter);

        // Check if the user is authenticated
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            // User is authenticated, fetch the lost items
            fetchLostItemsFromFirestore();
        } else {
            // Handle the case when the user is not authenticated (optional)
            Log.w("Firestore", "User not authenticated");
            // You can show a message or redirect to login screen
        }

        return view;
    }

    private void fetchLostItemsFromFirestore() {
        CollectionReference lostItemsRef = db.collection("lostItems");
        lostItemsRef.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            lostItemList.clear();
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                ListLostItem lostItem = document.toObject(ListLostItem.class);
                                lostItemList.add(lostItem);
                            }
                            Log.d("Firestore", "Loaded items: " + lostItemList.size());
                            adapter.notifyDataSetChanged();
                        } else {
                            Log.w("Firestore", "No documents found");
                        }
                    } else {
                        Log.e("Firestore", "Error getting documents: ", task.getException());
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle Firestore fetch failure
                    Log.e("Firestore", "Fetch failed: ", e);
                });
    }
}

