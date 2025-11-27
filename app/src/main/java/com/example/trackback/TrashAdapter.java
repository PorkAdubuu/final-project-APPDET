package com.example.trackback;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TrashAdapter extends RecyclerView.Adapter<TrashAdapter.ViewHolder> {

    private Context context;
    private List<LostItem> trashedItems;
    private FirebaseFirestore db;

    public TrashAdapter(Context context, List<LostItem> trashedItems) {
        this.context = context;
        this.trashedItems = trashedItems;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.postmanagement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LostItem item = trashedItems.get(position);

        String reportType = item.getReportType();
        boolean isFound = reportType != null && reportType.equalsIgnoreCase("Found");

        // Update labels
        if (isFound) {
            holder.itemLostText.setText("Item Found: " + item.getItemLost());
            holder.dateText.setText("Date Found: " + item.getDate());
            holder.timeText.setText("Time Found: " + item.getTime());
            holder.locationText.setText("Found At: " + item.getLastSeen());
        } else {
            holder.itemLostText.setText("Item Lost: " + item.getItemLost());
            holder.dateText.setText("Date Lost: " + item.getDate());
            holder.timeText.setText("Time Lost: " + item.getTime());
            holder.locationText.setText("Lost At: " + item.getLastSeen());
        }

        holder.categoryText.setText("Category: " + item.getCategory());

        // Show deleted timestamp
        Timestamp deletedAt = item.getDeletedAt();
        if (deletedAt != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            holder.datePostText.setText("Deleted on: " + sdf.format(deletedAt.toDate()));
        } else {
            holder.datePostText.setText("Deleted on: N/A");
        }

        // Load image
        String imageUrl = item.getItemImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.item_default)
                    .into(holder.itemImageView);
        } else {
            holder.itemImageView.setImageResource(R.drawable.item_default);
        }

        // Long press to show options
        holder.itemView.setOnLongClickListener(v -> {
            showOptionsBottomSheet(item, position);
            return true;
        });
    }

    private void showOptionsBottomSheet(LostItem item, int position) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_trash_options, null);
        bottomSheet.setContentView(view);

        // Restore option
        LinearLayout restoreOption = view.findViewById(R.id.menu_restore);
        restoreOption.setOnClickListener(v -> {
            bottomSheet.dismiss();
            restorePost(item, position);
        });

        // Permanent delete option
        LinearLayout deleteOption = view.findViewById(R.id.menu_delete);
        deleteOption.setOnClickListener(v -> {
            bottomSheet.dismiss();
            showPermanentDeleteConfirmation(item, position);
        });

        bottomSheet.show();
    }

    private void restorePost(LostItem item, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Restore Post")
                .setMessage("Do you want to restore this post?")
                .setPositiveButton("Restore", (dialog, which) -> {
                    db.collection("lostItems")
                            .document(item.getDocumentId())
                            .update("isDeleted", false, "deletedAt", null)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "Post restored successfully", Toast.LENGTH_SHORT).show();
                                trashedItems.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, trashedItems.size());
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to restore post", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showPermanentDeleteConfirmation(LostItem item, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Permanently")
                .setMessage("This will permanently delete the post. This action cannot be undone.")
                .setPositiveButton("Delete Permanently", (dialog, which) -> {
                    db.collection("lostItems")
                            .document(item.getDocumentId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "Post deleted permanently", Toast.LENGTH_SHORT).show();
                                trashedItems.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, trashedItems.size());
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to delete post", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public int getItemCount() {
        return trashedItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImageView;
        TextView itemLostText, categoryText, dateText, timeText, locationText, datePostText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImageView = itemView.findViewById(R.id.itemImageView);
            itemLostText = itemView.findViewById(R.id.itemLostText);
            categoryText = itemView.findViewById(R.id.categoryText);
            dateText = itemView.findViewById(R.id.dateText);
            timeText = itemView.findViewById(R.id.timeText);
            locationText = itemView.findViewById(R.id.locationText);
            datePostText = itemView.findViewById(R.id.datePostText);
        }
    }
}