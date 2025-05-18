package com.example.trackback;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemsFragment extends Fragment {

    private RecyclerView recyclerView;
    private List<ListLostItem> lostItemList = new ArrayList<>();
    private List<ListLostItem> allLostItems = new ArrayList<>();
    private ListLostItemsAdapter adapter;
    private FirebaseFirestore db;

    public ItemsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_items, container, false);

        recyclerView = view.findViewById(R.id.lostItemsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();

        adapter = new ListLostItemsAdapter(lostItemList, getContext());
        recyclerView.setAdapter(adapter);

        EditText searchEditText = view.findViewById(R.id.searchEditText);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBySearch(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });


        ImageView sortBtn = view.findViewById(R.id.sortBtn);
        sortBtn.setOnClickListener(v -> showSortBottomSheet());

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            fetchLostItemsFromFirestore();
        } else {
            Log.w("Firestore", "User not authenticated");
        }

        return view;
    }

    private void filterBySearch(String query) {
        List<ListLostItem> filteredList = new ArrayList<>();
        for (ListLostItem item : allLostItems) {
            if (item.getItemLost() != null && item.getItemLost().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(item);
            }
        }
        adapter.updateList(filteredList);
    }

    private void fetchLostItemsFromFirestore() {
        CollectionReference lostItemsRef = db.collection("lostItems");
        lostItemsRef.orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            lostItemList.clear();
                            allLostItems.clear();
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                ListLostItem lostItem = document.toObject(ListLostItem.class);
                                lostItemList.add(lostItem);
                                allLostItems.add(lostItem);
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
                    Log.e("Firestore", "Fetch failed: ", e);
                });
    }

    private void showSortBottomSheet() {
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_sort, null);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(view);

        View parent = (View) view.getParent();
        parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        parent.post(() -> {
            BottomSheetBehavior behavior = BottomSheetBehavior.from(parent);
            behavior.setPeekHeight(getResources().getDisplayMetrics().heightPixels / 2);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        // Get radio buttons and applyBtn
        RadioButton radioAll = view.findViewById(R.id.radioAllcategory);
        RadioButton radioGadget = view.findViewById(R.id.radioGadget);
        RadioButton radioAccessories = view.findViewById(R.id.radioAccessories);
        RadioButton radioPersonal = view.findViewById(R.id.radioPersonal);
        RadioButton radioDrinkware = view.findViewById(R.id.radioDrinkware);
        RadioButton radioSchool = view.findViewById(R.id.radioSchoolsupplies);
        RadioButton radioClothing = view.findViewById(R.id.radioClothing);
        RadioButton radioBag = view.findViewById(R.id.radioBag);
        RadioButton radioOthers = view.findViewById(R.id.radioOthers);
        Button applyBtn = view.findViewById(R.id.applyBtn);

        // Group all buttons manually
        List<RadioButton> radioButtons = Arrays.asList(
                radioAll, radioGadget, radioAccessories, radioPersonal,
                radioDrinkware, radioSchool, radioClothing, radioBag, radioOthers
        );

        // Ensure only one is selected manually
        for (RadioButton rb : radioButtons) {
            rb.setOnClickListener(v -> {
                for (RadioButton other : radioButtons) {
                    other.setChecked(false);
                }
                rb.setChecked(true);
            });
        }

        applyBtn.setOnClickListener(v -> {
            String selectedCategory = null;

            if (radioGadget.isChecked()) selectedCategory = "Gadgets";
            else if (radioAccessories.isChecked()) selectedCategory = "Accessories";
            else if (radioPersonal.isChecked()) selectedCategory = "Personal Belongings";
            else if (radioDrinkware.isChecked()) selectedCategory = "Drinkware";
            else if (radioSchool.isChecked()) selectedCategory = "School Supplies";
            else if (radioClothing.isChecked()) selectedCategory = "Clothing";
            else if (radioBag.isChecked()) selectedCategory = "Bags";
            else if (radioOthers.isChecked()) selectedCategory = "Others";

            if (radioAll.isChecked() || selectedCategory == null) {
                adapter.updateList(allLostItems);
            } else {
                filterLostItemsByCategory(selectedCategory);
            }

            dialog.dismiss();
        });

        dialog.show();
    }


    // Optional: keep if you’ll reuse it in new sort logic
    private void filterLostItemsByCategory(String category) {
        List<ListLostItem> filteredList = new ArrayList<>();
        if (category == null) {
            filteredList.addAll(allLostItems);
        } else {
            for (ListLostItem item : allLostItems) {
                if (item.getCategory() != null && item.getCategory().equals(category)) {
                    filteredList.add(item);
                }
            }
        }
        adapter.updateList(filteredList);
    }
}
