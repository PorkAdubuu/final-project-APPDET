// FullImagePreviewDialog.java
package com.example.trackback;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;

public class FullImagePreviewDialog extends DialogFragment {

    private final String imageUrl;

    public FullImagePreviewDialog(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_image_preview, null);
        ImageView fullImageView = view.findViewById(R.id.fullImageView);

        Glide.with(requireContext())
                .load(imageUrl)
                .placeholder(R.drawable.item_default)
                .into(fullImageView);

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .setCancelable(true)
                .create();
    }
}
