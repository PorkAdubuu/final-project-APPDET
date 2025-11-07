package com.example.trackback;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
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

    private ListLostItem lostItem;

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
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_lost_item_details, null);

        if (getArguments() != null) {
            if (getArguments().containsKey("lostItem")) {
                lostItem = (ListLostItem) getArguments().getSerializable("lostItem");
                setupDialogView(view);
            } else if (getArguments().containsKey("documentId")) {
                String documentId = getArguments().getString("documentId");
                fetchLostItemData(documentId, view);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        return dialog;
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
                            setupDialogView(view);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to load item details", Toast.LENGTH_SHORT).show()
                );
    }

    private void setupDialogView(View view) {
        if (lostItem == null) {
            Toast.makeText(requireContext(), "Error: Item data not loaded", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        boolean isFound = lostItem.getReportType() != null && lostItem.getReportType().equalsIgnoreCase("Found");

        ImageView itemImageView = view.findViewById(R.id.itemImageView);
        Glide.with(requireContext())
                .load(lostItem.getItemImageUrl())
                .placeholder(R.drawable.item_default)
                .into(itemImageView);

        itemImageView.setOnClickListener(v -> {
            FullImagePreviewDialog previewDialog = new FullImagePreviewDialog(lostItem.getItemImageUrl());
            previewDialog.show(getParentFragmentManager(), "fullImagePreview");
        });

        ((TextView) view.findViewById(R.id.itemLabel)).setText("Item " + (isFound ? "Found:" : "Lost:"));
        ((TextView) view.findViewById(R.id.itemLostText)).setText(lostItem.getItemLost() != null ? lostItem.getItemLost() : "N/A");
        ((TextView) view.findViewById(R.id.dateLabel)).setText("Date " + (isFound ? "Found:" : "Lost:"));
        ((TextView) view.findViewById(R.id.dateText)).setText(lostItem.getDate() != null ? lostItem.getDate() : "N/A");
        ((TextView) view.findViewById(R.id.timeLabel)).setText("Time " + (isFound ? "Found:" : "Lost:"));
        ((TextView) view.findViewById(R.id.timeText)).setText(lostItem.getTime() != null ? lostItem.getTime() : "N/A");
        ((TextView) view.findViewById(R.id.locationLabel)).setText(isFound ? "Found At:" : "Lost At:");
        ((TextView) view.findViewById(R.id.lastSeenText)).setText(lostItem.getLastSeen() != null ? lostItem.getLastSeen() : "N/A");
        ((TextView) view.findViewById(R.id.categoryText)).setText(lostItem.getCategory() != null ? lostItem.getCategory() : "N/A");
        ((TextView) view.findViewById(R.id.brandText)).setText(lostItem.getBrand() != null ? lostItem.getBrand() : "N/A");
        ((TextView) view.findViewById(R.id.additionalInfoText)).setText(lostItem.getAdditionalInfo() != null ? lostItem.getAdditionalInfo() : "N/A");
        ((TextView) view.findViewById(R.id.moreInfoText)).setText(lostItem.getMoreInfo() != null ? lostItem.getMoreInfo() : "N/A");
        ((TextView) view.findViewById(R.id.accountFnameLname)).setText((lostItem.getFirstName() != null ? lostItem.getFirstName() : "") + " " + (lostItem.getLastName() != null ? lostItem.getLastName() : ""));
        ((TextView) view.findViewById(R.id.useremailadd)).setText(lostItem.getEmail() != null ? lostItem.getEmail() : "N/A");
        ((TextView) view.findViewById(R.id.phoneText)).setText(lostItem.getPhone() != null ? lostItem.getPhone() : "N/A");

        Glide.with(requireContext())
                .load(lostItem.getProfileUrl())
                .placeholder(R.drawable.def_prof)
                .circleCrop()
                .into((ImageView) view.findViewById(R.id.profileImageView));

        // --- Message Button ---
        Button messageButton = view.findViewById(R.id.messageBtn);

        if (messageButton != null) {
            messageButton.setOnClickListener(v -> {
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                if (mAuth.getCurrentUser() == null) {
                    Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show();
                    return;
                }

                String currentUserId = mAuth.getCurrentUser().getUid();
                String receiverId = lostItem.getUserId(); // Could be null
                String receiverName = ((lostItem.getFirstName() != null ? lostItem.getFirstName() : "") + " " + (lostItem.getLastName() != null ? lostItem.getLastName() : "")).trim();
                String receiverProfileUrl = lostItem.getProfileUrl() != null ? lostItem.getProfileUrl() : "";

                // Validate receiverId
                if (receiverId == null || receiverId.isEmpty()) {
                    Toast.makeText(requireContext(), "Chat not available: User ID missing.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Prevent messaging yourself
                if (currentUserId.equals(receiverId)) {
                    Toast.makeText(requireContext(), "You cannot message yourself.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Default name fallback
                if (receiverName.isEmpty()) receiverName = "User";

                // Open ChatActivity safely
                try {
                    Intent intent = new Intent(requireContext(), ChatActivity.class);
                    intent.putExtra("receiverId", receiverId);
                    intent.putExtra("receiverName", receiverName);
                    intent.putExtra("profileImageUrl", receiverProfileUrl);
                    startActivity(intent);
                    dismiss();
                } catch (Exception e) {
                    android.util.Log.e("LostItemDialog", "Failed to open chat", e);
                    Toast.makeText(requireContext(), "Error opening chat.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
