package com.example.trackback;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;

public class LostItemDetailsDialog extends DialogFragment {

    private ListLostItem lostItem;

    public LostItemDetailsDialog(ListLostItem item) {
        this.lostItem = item;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_lost_item_details, null);

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

        // Labels and values
        TextView itemLabel = view.findViewById(R.id.itemLabel);
        TextView itemLostText = view.findViewById(R.id.itemLostText);
        TextView dateLabel = view.findViewById(R.id.dateLabel);
        TextView dateText = view.findViewById(R.id.dateText);
        TextView timeLabel = view.findViewById(R.id.timeLabel);
        TextView timeText = view.findViewById(R.id.timeText);
        TextView locationLabel = view.findViewById(R.id.locationLabel);
        TextView lastSeenText = view.findViewById(R.id.lastSeenText);

        itemLabel.setText("Item " + (isFound ? "Found:" : "Lost:"));
        itemLostText.setText(lostItem.getItemLost());

        dateLabel.setText("Date " + (isFound ? "Found:" : "Lost:"));
        dateText.setText(lostItem.getDate());

        timeLabel.setText("Time " + (isFound ? "Found:" : "Lost:"));
        timeText.setText(lostItem.getTime());

        locationLabel.setText(isFound ? "Found At:" : "Lost At:");
        lastSeenText.setText(lostItem.getLastSeen());

        // Direct display fields
        ((TextView) view.findViewById(R.id.categoryText)).setText(lostItem.getCategory());
        ((TextView) view.findViewById(R.id.brandText)).setText(lostItem.getBrand());
        ((TextView) view.findViewById(R.id.additionalInfoText)).setText(lostItem.getAdditionalInfo());
        ((TextView) view.findViewById(R.id.moreInfoText)).setText(lostItem.getMoreInfo());

        // Account info
        ((TextView) view.findViewById(R.id.accountFnameLname)).setText(lostItem.getFirstName() + " " + lostItem.getLastName());
        ((TextView) view.findViewById(R.id.useremailadd)).setText(lostItem.getEmail());
        ((TextView) view.findViewById(R.id.phoneText)).setText(lostItem.getPhone());

        // Profile Image
        Glide.with(requireContext())
                .load(lostItem.getProfileUrl())
                .placeholder(R.drawable.def_prof)
                .circleCrop()
                .into((ImageView) view.findViewById(R.id.profileImageView));

        // Item Image
        Glide.with(requireContext())
                .load(lostItem.getItemImageUrl())
                .placeholder(R.drawable.item_default)
                .into((ImageView) view.findViewById(R.id.itemImageView));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)

                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        return dialog;
    }
}
