package com.example.trackback;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for blocked users with unblock option
 */
public class BlockedAccountsAdapter extends RecyclerView.Adapter<BlockedAccountsAdapter.ViewHolder> {

    private List<BlockedUser> blockedUsers;
    private Context context;
    private FirebaseFirestore firestore;
    private String currentUserId;

    public BlockedAccountsAdapter(List<BlockedUser> blockedUsers, Context context) {
        this.blockedUsers = blockedUsers;
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BlockedUser user = blockedUsers.get(position);

        // Set name
        holder.nameText.setText(user.getBlockedUserName() != null ? user.getBlockedUserName() : "User");

        // Set "Blocked" as the message text
        holder.lastMessageText.setText("Blocked");
        holder.lastMessageText.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));

        // Format timestamp when blocked
        if (user.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            holder.timeText.setText(sdf.format(user.getTimestamp().toDate()));
        } else {
            holder.timeText.setText("");
        }

        // Load profile image
        if (user.getBlockedUserProfileUrl() != null && !user.getBlockedUserProfileUrl().isEmpty()) {
            Glide.with(context)
                    .load(user.getBlockedUserProfileUrl())
                    .circleCrop()
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.default_avatar);
        }

        // Click to show unblock option (DON'T open chat for blocked users)
        holder.itemView.setOnClickListener(v -> {
            showUnblockConfirmation(user, position);
        });

        // Long press also shows unblock option
        holder.itemView.setOnLongClickListener(v -> {
            showUnblockConfirmation(user, position);
            return true;
        });
    }

    /**
     * Show confirmation dialog before unblocking
     */
    private void showUnblockConfirmation(BlockedUser user, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Unblock " + user.getBlockedUserName() + "?");
        builder.setMessage("They will be able to send you messages again.");
        builder.setPositiveButton("Unblock", (dialog, which) -> unblockUser(user, position));
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    /**
     * Unblock a user - removes from blockedUsers collection
     */
    private void unblockUser(BlockedUser user, int position) {
        firestore.collection("users")
                .document(currentUserId)
                .collection("blockedUsers")
                .document(user.getBlockedUserId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, user.getBlockedUserName() + " has been unblocked", Toast.LENGTH_SHORT).show();

                    // ✅ Only remove if index is valid
                    if (position >= 0 && position < blockedUsers.size()) {
                        blockedUsers.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, blockedUsers.size());
                    } else {
                        // Debug log to check unexpected behavior
                        android.util.Log.e("BlockedAccountsAdapter",
                                "Invalid remove position: " + position + ", size: " + blockedUsers.size());
                    }

                    // ✅ Refresh list safely
                    if (context instanceof BlockedAccountsActivity) {
                        ((BlockedAccountsActivity) context).refreshList();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to unblock user", Toast.LENGTH_SHORT).show();
                    android.util.Log.e("BlockedAccountsAdapter", "Error unblocking user", e);
                });
    }


    @Override
    public int getItemCount() {
        return blockedUsers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView nameText, lastMessageText, timeText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImageView);
            nameText = itemView.findViewById(R.id.fullnameText);
            lastMessageText = itemView.findViewById(R.id.lastMessageText);
            timeText = itemView.findViewById(R.id.messageTime);
        }
    }
}