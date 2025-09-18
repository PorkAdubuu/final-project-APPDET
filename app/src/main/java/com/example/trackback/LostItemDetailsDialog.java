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

import java.io.Serializable;

public class LostItemDetailsDialog extends DialogFragment {

    private ListLostItem lostItem;

    public LostItemDetailsDialog() {
    }

    // ✅ New way: Accept a full LostItem object
    public static LostItemDetailsDialog newInstance(ListLostItem item) {
        LostItemDetailsDialog dialog = new LostItemDetailsDialog();
        Bundle args = new Bundle();
        args.putSerializable("lostItem", item);
        dialog.setArguments(args);
        return dialog;
    }

    // ✅ Existing way: Accept a documentId string
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
                .collection("LostItems")
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
        boolean isFound = lostItem.getReportType().equalsIgnoreCase("Found");

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
        ((TextView) view.findViewById(R.id.itemLostText)).setText(lostItem.getItemLost());

        ((TextView) view.findViewById(R.id.dateLabel)).setText("Date " + (isFound ? "Found:" : "Lost:"));
        ((TextView) view.findViewById(R.id.dateText)).setText(lostItem.getDate());

        ((TextView) view.findViewById(R.id.timeLabel)).setText("Time " + (isFound ? "Found:" : "Lost:"));
        ((TextView) view.findViewById(R.id.timeText)).setText(lostItem.getTime());

        ((TextView) view.findViewById(R.id.locationLabel)).setText(isFound ? "Found At:" : "Lost At:");
        ((TextView) view.findViewById(R.id.lastSeenText)).setText(lostItem.getLastSeen());

        ((TextView) view.findViewById(R.id.categoryText)).setText(lostItem.getCategory());
        ((TextView) view.findViewById(R.id.brandText)).setText(lostItem.getBrand());
        ((TextView) view.findViewById(R.id.additionalInfoText)).setText(lostItem.getAdditionalInfo());
        ((TextView) view.findViewById(R.id.moreInfoText)).setText(lostItem.getMoreInfo());

        ((TextView) view.findViewById(R.id.accountFnameLname)).setText(lostItem.getFirstName() + " " + lostItem.getLastName());
        ((TextView) view.findViewById(R.id.useremailadd)).setText(lostItem.getEmail());
        ((TextView) view.findViewById(R.id.phoneText)).setText(lostItem.getPhone());

        Glide.with(requireContext())
                .load(lostItem.getProfileUrl())
                .placeholder(R.drawable.def_prof)
                .circleCrop()
                .into((ImageView) view.findViewById(R.id.profileImageView));

        Button alertOwnerBtn = view.findViewById(R.id.alertOwnerBtn);
        alertOwnerBtn.setOnClickListener(v -> {
            String ownerEmail = lostItem.getEmail();
            String reportType = lostItem.getReportType();
            String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser() != null ?
                    FirebaseAuth.getInstance().getCurrentUser().getEmail() : "someone";

            String subject = (reportType.equalsIgnoreCase("Lost")) ?
                    "Regarding your lost item" : "Regarding your found item";

            String message = "Hello " + lostItem.getFirstName() + ",\n\n" +
                    "I am " + currentUserEmail + ". I would like to alert you about your " +
                    reportType.toLowerCase() + " item: " + lostItem.getItemLost() + ".\n\n" +
                    "Please get in touch with me.\n\nThank you.";

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{ownerEmail});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, message);

            if (emailIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(emailIntent);
            } else {
                Toast.makeText(requireContext(), "No email app found on this device", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
