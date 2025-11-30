package com.example.trackback;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LostItemDetailsDialog extends DialogFragment {

    private static final String TAG = "LostItemDialog";
    private ListLostItem lostItem;

    private TextView accountFnameLname, useremailadd, phoneText;
    private TextView itemLostText, categoryText, brandText, dateText, timeText;
    private TextView additionalInfoText, lastSeenText, moreInfoText;
    private ImageView profileImageView, itemImageView;
    private Button messageBtn;

    public LostItemDetailsDialog() {
        // Required empty public constructor
    }

    public static LostItemDetailsDialog newInstance(ListLostItem item) {
        LostItemDetailsDialog dialog = new LostItemDetailsDialog();
        Bundle args = new Bundle();
        args.putSerializable("lostItem", item);
        dialog.setArguments(args);
        return dialog;
    }

    public static LostItemDetailsDialog newInstance(String documentId) {
        LostItemDetailsDialog dialog = new LostItemDetailsDialog();
        Bundle args = new Bundle();
        args.putString("documentId", documentId);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_lost_item_details, null);

        initViews(view);

        if (getArguments() != null) {
            if (getArguments().containsKey("lostItem")) {
                lostItem = (ListLostItem) getArguments().getSerializable("lostItem");
            } else if (getArguments().containsKey("documentId")) {
                String documentId = getArguments().getString("documentId");
                fetchLostItemData(documentId, view);
            }
        }

        if (lostItem != null) {
            populateViews();
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        return dialog;
    }

    private void initViews(View view) {
        accountFnameLname = view.findViewById(R.id.accountFnameLname);
        useremailadd = view.findViewById(R.id.useremailadd);
        phoneText = view.findViewById(R.id.phoneText);
        profileImageView = view.findViewById(R.id.profileImageView);

        itemLostText = view.findViewById(R.id.itemLostText);
        categoryText = view.findViewById(R.id.categoryText);
        brandText = view.findViewById(R.id.brandText);
        dateText = view.findViewById(R.id.dateText);
        timeText = view.findViewById(R.id.timeText);
        additionalInfoText = view.findViewById(R.id.additionalInfoText);
        lastSeenText = view.findViewById(R.id.lastSeenText);
        moreInfoText = view.findViewById(R.id.moreInfoText);
        itemImageView = view.findViewById(R.id.itemImageView);

        messageBtn = view.findViewById(R.id.messageBtn);
    }

    private void fetchLostItemData(String documentId, View view) {
        FirebaseFirestore.getInstance()
                .collection("lostItems")
                .document(documentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        lostItem = snapshot.toObject(ListLostItem.class);
                        if (lostItem != null) {
                            populateViews();
                        } else {
                            Log.e(TAG, "LostItem is null after Firestore fetch");
                            Toast.makeText(requireContext(), "Failed to load item details", Toast.LENGTH_SHORT).show();
                            dismiss();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Item not found", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch item from Firestore", e);
                    Toast.makeText(requireContext(), "Failed to load item details", Toast.LENGTH_SHORT).show();
                    dismiss();
                });
    }

    private void populateViews() {
        if (lostItem == null) return;

        boolean isFound = "Found".equalsIgnoreCase(lostItem.getReportType());

        // Item Image
        Glide.with(itemImageView.getContext())
                .load(lostItem.getItemImageUrl())
                .placeholder(R.drawable.item_default)
                .into(itemImageView);

        itemImageView.setOnClickListener(v -> {
            FullImagePreviewDialog previewDialog = new FullImagePreviewDialog(lostItem.getItemImageUrl());
            previewDialog.show(getParentFragmentManager(), "fullImagePreview");
        });

        // Populate TextViews
        itemLostText.setText(lostItem.getItemLost() != null ? lostItem.getItemLost() : "N/A");
        categoryText.setText(lostItem.getCategory() != null ? lostItem.getCategory() : "N/A");
        brandText.setText(lostItem.getBrand() != null ? lostItem.getBrand() : "N/A");
        dateText.setText(lostItem.getDate() != null ? lostItem.getDate() : "N/A");
        timeText.setText(lostItem.getTime() != null ? lostItem.getTime() : "N/A");
        additionalInfoText.setText(lostItem.getAdditionalInfo() != null ? lostItem.getAdditionalInfo() : "N/A");
        lastSeenText.setText(lostItem.getLastSeen() != null ? lostItem.getLastSeen() : "N/A");
        moreInfoText.setText(lostItem.getMoreInfo() != null ? lostItem.getMoreInfo() : "N/A");

        accountFnameLname.setText(
                (lostItem.getFirstName() != null ? lostItem.getFirstName() : "") + " " +
                        (lostItem.getLastName() != null ? lostItem.getLastName() : "")
        );
        useremailadd.setText(lostItem.getEmail() != null ? lostItem.getEmail() : "N/A");
        phoneText.setText(lostItem.getPhone() != null ? lostItem.getPhone() : "N/A");

        // Profile Image
        Glide.with(profileImageView.getContext())
                .load(lostItem.getProfileUrl())
                .placeholder(R.drawable.def_prof)
                .circleCrop()
                .into(profileImageView);

        setupMessageButton();
    }

    private void setupMessageButton() {
        if (messageBtn == null) return;

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            messageBtn.setVisibility(View.GONE);
            return;
        }

        String currentUserId = mAuth.getCurrentUser().getUid();
        String receiverId = lostItem.getUserId();

        if (receiverId == null || receiverId.isEmpty() || currentUserId.equals(receiverId)) {
            messageBtn.setVisibility(View.GONE);
        } else {
            messageBtn.setVisibility(View.VISIBLE);
            messageBtn.setOnClickListener(v -> openChat(receiverId));
        }
    }

    private void openChat(String receiverId) {
        String receiverName = ((lostItem.getFirstName() != null ? lostItem.getFirstName() : "") +
                " " + (lostItem.getLastName() != null ? lostItem.getLastName() : "")).trim();
        if (receiverName.isEmpty()) receiverName = "User";
        String receiverProfileUrl = lostItem.getProfileUrl() != null ? lostItem.getProfileUrl() : "";

        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra("receiverId", receiverId);
        intent.putExtra("receiverName", receiverName);
        intent.putExtra("profileImageUrl", receiverProfileUrl);
        startActivity(intent);
        dismiss();
    }
}
