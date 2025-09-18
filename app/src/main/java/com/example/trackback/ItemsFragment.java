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
import android.widget.LinearLayout;
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

        LinearLayout backBtn = view.findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_overlay, new HomeFragment())
                    .commit();
        });


        recyclerView = view.findViewById(R.id.lostItemsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();

        adapter = new ListLostItemsAdapter(lostItemList, getContext());
        recyclerView.setAdapter(adapter);

        EditText searchEditText = view.findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBySearch(s.toString().trim());
            }
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
        db.collection("lostItems")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        lostItemList.clear();
                        allLostItems.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ListLostItem lostItem = document.toObject(ListLostItem.class);
                            lostItemList.add(lostItem);
                            allLostItems.add(lostItem);
                        }
                        Log.d("Firestore", "Loaded items: " + lostItemList.size());
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.e("Firestore", "Error getting documents: ", task.getException());
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Fetch failed: ", e));
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

        // Category buttons
        List<RadioButton> categoryButtons = Arrays.asList(
                view.findViewById(R.id.radioAllcategory),
                view.findViewById(R.id.radioGadget),
                view.findViewById(R.id.radioAccessories),
                view.findViewById(R.id.radioPersonal),
                view.findViewById(R.id.radioDrinkware),
                view.findViewById(R.id.radioSchoolsupplies),
                view.findViewById(R.id.radioClothing),
                view.findViewById(R.id.radioBag),
                view.findViewById(R.id.radioOthers)
        );

        // Report type buttons
        List<RadioButton> reportTypeButtons = Arrays.asList(
                view.findViewById(R.id.radioAll),
                view.findViewById(R.id.radioLost),
                view.findViewById(R.id.radioFound)
        );

        Button applyBtn = view.findViewById(R.id.applyBtn);
        applyBtn.setOnClickListener(v -> {
            String selectedCategory = null;
            if (((RadioButton) view.findViewById(R.id.radioGadget)).isChecked()) selectedCategory = "Gadgets";
            else if (((RadioButton) view.findViewById(R.id.radioAccessories)).isChecked()) selectedCategory = "Accessories";
            else if (((RadioButton) view.findViewById(R.id.radioPersonal)).isChecked()) selectedCategory = "Personal Belongings";
            else if (((RadioButton) view.findViewById(R.id.radioDrinkware)).isChecked()) selectedCategory = "Drinkware";
            else if (((RadioButton) view.findViewById(R.id.radioSchoolsupplies)).isChecked()) selectedCategory = "School Supplies";
            else if (((RadioButton) view.findViewById(R.id.radioClothing)).isChecked()) selectedCategory = "Clothing";
            else if (((RadioButton) view.findViewById(R.id.radioBag)).isChecked()) selectedCategory = "Bags";
            else if (((RadioButton) view.findViewById(R.id.radioOthers)).isChecked()) selectedCategory = "Others";

            String selectedReportType = null;
            if (((RadioButton) view.findViewById(R.id.radioLost)).isChecked()) selectedReportType = "lost";
            else if (((RadioButton) view.findViewById(R.id.radioFound)).isChecked()) selectedReportType = "found";

            // Filter logic
            List<ListLostItem> filteredList = new ArrayList<>();
            for (ListLostItem item : allLostItems) {
                boolean matchCategory = selectedCategory == null || selectedCategory.equals(item.getCategory());
                boolean matchReportType = selectedReportType == null || (item.getReportType() != null &&
                        item.getReportType().equalsIgnoreCase(selectedReportType));
                if (matchCategory && matchReportType) {
                    filteredList.add(item);
                }
            }

            adapter.updateList(filteredList);
            dialog.dismiss();
        });

        dialog.show();
    }
}
