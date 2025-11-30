package com.example.trackback;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

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

    // Store current filters
    private String currentSearchQuery = "";
    private String currentCategory = null;
    private String currentReportType = null;
    private String currentLocation = null;

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
                currentSearchQuery = s.toString().trim();
                applyAllFilters();
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

    // Combined filter logic
    private void applyAllFilters() {
        List<ListLostItem> filteredList = new ArrayList<>();

        for (ListLostItem item : allLostItems) {
            // Search filter
            boolean matchSearch = currentSearchQuery.isEmpty() ||
                    (item.getItemLost() != null &&
                            item.getItemLost().toLowerCase().contains(currentSearchQuery.toLowerCase()));

            // Category filter
            boolean matchCategory = currentCategory == null ||
                    currentCategory.equals(item.getCategory());

            // Report type filter
            boolean matchReportType = currentReportType == null ||
                    (item.getReportType() != null &&
                            item.getReportType().equalsIgnoreCase(currentReportType));

            // Location filter
            boolean matchLocation = currentLocation == null ||
                    (item.getLastSeen() != null &&
                            item.getLastSeen().equalsIgnoreCase(currentLocation));

            // Item must match ALL active filters
            if (matchSearch && matchCategory && matchReportType && matchLocation) {
                filteredList.add(item);
            }
        }

        lostItemList.clear();
        lostItemList.addAll(filteredList);
        adapter.notifyDataSetChanged();

        Log.d("ItemsFragment", "Filtered items: " + filteredList.size() +
                " (Search: '" + currentSearchQuery + "', Category: " + currentCategory +
                ", Type: " + currentReportType + ", Location: " + currentLocation + ")");
    }

    private void fetchLostItemsFromFirestore() {
        db.collection("lostItems")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        allLostItems.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ListLostItem lostItem = document.toObject(ListLostItem.class);
                            allLostItems.add(lostItem);
                        }
                        Log.d("Firestore", "Loaded items: " + allLostItems.size());
                        applyAllFilters(); // Apply filters after loading
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

        // Make background transparent
        View parent = (View) view.getParent();
        parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        // Set stable height
        LinearLayout bottomSheetContainer = view.findViewById(R.id.bottomSheetContainer);

        parent.post(() -> {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);

            // Force bottom sheet to respect XML height
            int measuredHeight = bottomSheetContainer.getMeasuredHeight();

            // If height is 0 on first pass, fallback to 60% screen
            if (measuredHeight == 0) {
                DisplayMetrics dm = getResources().getDisplayMetrics();
                measuredHeight = (int) (dm.heightPixels * 0.60);
            }

            behavior.setFitToContents(false);
            behavior.setPeekHeight(measuredHeight, true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

            Log.d("BottomSheet", "Final height: " + measuredHeight);
        });

        // Get all radio buttons
        RadioButton radioAllCategory = view.findViewById(R.id.radioAllcategory);
        RadioButton radioGadget = view.findViewById(R.id.radioGadget);
        RadioButton radioAccessories = view.findViewById(R.id.radioAccessories);
        RadioButton radioPersonal = view.findViewById(R.id.radioPersonal);
        RadioButton radioDrinkware = view.findViewById(R.id.radioDrinkware);
        RadioButton radioSchoolSupplies = view.findViewById(R.id.radioSchoolsupplies);
        RadioButton radioClothing = view.findViewById(R.id.radioClothing);
        RadioButton radioBag = view.findViewById(R.id.radioBag);
        RadioButton radioOthers = view.findViewById(R.id.radioOthers);

        RadioButton radioAll = view.findViewById(R.id.radioAll);
        RadioButton radioLost = view.findViewById(R.id.radioLost);
        RadioButton radioFound = view.findViewById(R.id.radioFound);

        RadioButton radioAllLocation = view.findViewById(R.id.radioAlllocation);
        RadioButton radioOval = view.findViewById(R.id.radioOval);
        RadioButton radioHPSB = view.findViewById(R.id.radioHPSB);
        RadioButton radioAdmin = view.findViewById(R.id.radioAdmin);
        RadioButton radioAcad1 = view.findViewById(R.id.radioAcad1);
        RadioButton radioAcad2 = view.findViewById(R.id.radioAcad2);
        RadioButton radioLibrary = view.findViewById(R.id.radioLibrary);
        RadioButton radioCafeteria = view.findViewById(R.id.radioCafeteria);

        // Set current selections when opening
        if (currentCategory == null) radioAllCategory.setChecked(true);
        else if (currentCategory.equals("Gadgets")) radioGadget.setChecked(true);
        else if (currentCategory.equals("Accessories")) radioAccessories.setChecked(true);
        else if (currentCategory.equals("Personal Belongings")) radioPersonal.setChecked(true);
        else if (currentCategory.equals("Drinkware")) radioDrinkware.setChecked(true);
        else if (currentCategory.equals("School Supplies")) radioSchoolSupplies.setChecked(true);
        else if (currentCategory.equals("Clothing")) radioClothing.setChecked(true);
        else if (currentCategory.equals("Bags")) radioBag.setChecked(true);
        else if (currentCategory.equals("Others")) radioOthers.setChecked(true);

        if (currentReportType == null) radioAll.setChecked(true);
        else if (currentReportType.equals("Lost")) radioLost.setChecked(true);
        else if (currentReportType.equals("Found")) radioFound.setChecked(true);

        if (currentLocation == null) radioAllLocation.setChecked(true);
        else if (currentLocation.equals("Umak Oval")) radioOval.setChecked(true);
        else if (currentLocation.equals("HPSB")) radioHPSB.setChecked(true);
        else if (currentLocation.equals("Admin Building")) radioAdmin.setChecked(true);
        else if (currentLocation.equals("Academic Building 1")) radioAcad1.setChecked(true);
        else if (currentLocation.equals("Academic Building 2")) radioAcad2.setChecked(true);
        else if (currentLocation.equals("Library")) radioLibrary.setChecked(true);
        else if (currentLocation.equals("Cafeteria")) radioCafeteria.setChecked(true);

        // Proper RadioGroup behavior - only one selected at a time per group
        setupRadioGroup(radioAllCategory, radioGadget, radioAccessories, radioPersonal,
                radioDrinkware, radioSchoolSupplies, radioClothing, radioBag, radioOthers);
        setupRadioGroup(radioAll, radioLost, radioFound);
        setupRadioGroup(radioAllLocation, radioOval, radioHPSB, radioAdmin,
                radioAcad1, radioAcad2, radioLibrary, radioCafeteria);

        // Clear All button
        Button clearAllBtn = view.findViewById(R.id.clearAllBtn);
        clearAllBtn.setOnClickListener(v -> {
            // Reset all filters to default (All)
            radioAllCategory.setChecked(true);
            radioAll.setChecked(true);
            radioAllLocation.setChecked(true);

            currentCategory = null;
            currentReportType = null;
            currentLocation = null;

            applyAllFilters();
            dialog.dismiss();

            Log.d("ItemsFragment", "All filters cleared");
        });

        // Apply button
        Button applyBtn = view.findViewById(R.id.applyBtn);
        applyBtn.setOnClickListener(v -> {
            // Get selected category
            if (radioAllCategory.isChecked()) currentCategory = null;
            else if (radioGadget.isChecked()) currentCategory = "Gadgets";
            else if (radioAccessories.isChecked()) currentCategory = "Accessories";
            else if (radioPersonal.isChecked()) currentCategory = "Personal Belongings";
            else if (radioDrinkware.isChecked()) currentCategory = "Drinkware";
            else if (radioSchoolSupplies.isChecked()) currentCategory = "School Supplies";
            else if (radioClothing.isChecked()) currentCategory = "Clothing";
            else if (radioBag.isChecked()) currentCategory = "Bags";
            else if (radioOthers.isChecked()) currentCategory = "Others";

            // Get selected report type
            if (radioAll.isChecked()) currentReportType = null;
            else if (radioLost.isChecked()) currentReportType = "Lost";
            else if (radioFound.isChecked()) currentReportType = "Found";

            // Get selected location
            if (radioAllLocation.isChecked()) currentLocation = null;
            else if (radioOval.isChecked()) currentLocation = "Umak Oval";
            else if (radioHPSB.isChecked()) currentLocation = "HPSB";
            else if (radioAdmin.isChecked()) currentLocation = "Admin Building";
            else if (radioAcad1.isChecked()) currentLocation = "Academic Building 1";
            else if (radioAcad2.isChecked()) currentLocation = "Academic Building 2";
            else if (radioLibrary.isChecked()) currentLocation = "Library";
            else if (radioCafeteria.isChecked()) currentLocation = "Cafeteria";

            applyAllFilters();
            dialog.dismiss();
        });

        dialog.show();
    }

    // Helper to make RadioButtons act like a RadioGroup
    private void setupRadioGroup(RadioButton... buttons) {
        for (RadioButton button : buttons) {
            button.setOnClickListener(v -> {
                for (RadioButton other : buttons) {
                    other.setChecked(other == button);
                }
            });
        }
    }
}