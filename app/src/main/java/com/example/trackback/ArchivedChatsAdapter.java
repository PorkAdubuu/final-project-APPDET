package com.example.trackback;

import android.content.Context;
import android.content.Intent;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter for archived chats with unarchive and delete options
 */
public class ArchivedChatsAdapter extends RecyclerView.Adapter<ArchivedChatsAdapter.ViewHolder> {

    private List<ChatListItem> archivedChats;
    private Context context;
    private FirebaseFirestore firestore;
    private String currentUserId;

    public ArchivedChatsAdapter(List<ChatListItem> archivedChats, Context context) {
        this.archivedChats = archivedChats;
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
        ChatListItem chat = archivedChats.get(position);

        // Set name
        holder.nameText.setText(chat.getName() != null ? chat.getName() : "User");

        // Add "You: " prefix if current user sent the last message
        String displayMessage = chat.getLastMessage();
        if (displayMessage != null && !displayMessage.isEmpty()) {
            if (chat.getLastSenderId() != null && chat.getLastSenderId().equals(currentUserId)) {
                displayMessage = "You: " + displayMessage;
            }
        }
        holder.lastMessageText.setText(displayMessage);

        // Format timestamp
        Timestamp ts = chat.getTimestamp();
        if (ts != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            holder.timeText.setText(sdf.format(ts.toDate()));
        } else {
            holder.timeText.setText("");
        }

        // Load profile image
        if (chat.getProfileImageUrl() != null && !chat.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(chat.getProfileImageUrl())
                    .circleCrop()
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.default_avatar);
        }

        // Regular click - open chat
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("receiverId", chat.getReceiverId());
            intent.putExtra("receiverName", chat.getName());
            intent.putExtra("profileImageUrl", chat.getProfileImageUrl());
            context.startActivity(intent);
        });

        // Long press - show options
        holder.itemView.setOnLongClickListener(v -> {
            showArchivedOptionsBottomSheet(chat, position);
            return true;
        });
    }

    /**
     * Show bottom sheet with Unarchive and Delete options
     */
    private void showArchivedOptionsBottomSheet(ChatListItem chat, int position) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_archived_chat_options, null);
        bottomSheet.setContentView(view);

        // Unarchive option
        LinearLayout unarchiveOption = view.findViewById(R.id.menu_unarchive);
        unarchiveOption.setOnClickListener(v -> {
            bottomSheet.dismiss();
            unarchiveChat(chat, position);
        });

        // Delete option
        LinearLayout deleteOption = view.findViewById(R.id.menu_delete);
        deleteOption.setOnClickListener(v -> {
            bottomSheet.dismiss();
            showDeleteConfirmation(chat, position);
        });

        bottomSheet.show();
    }

    /**
     * Unarchive a chat - removes archived flag
     */
    private void unarchiveChat(ChatListItem chat, int position) {
        String chatId = getChatId(currentUserId, chat.getReceiverId());

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("archivedFor_" + currentUserId, false);

        firestore.collection("chatList")
                .document(chatId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Chat unarchived", Toast.LENGTH_SHORT).show();

                    // ✅ Safely remove from list
                    if (position >= 0 && position < archivedChats.size()) {
                        archivedChats.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, archivedChats.size());
                    }

                    // ✅ Refresh only if activity is still alive
                    if (context instanceof ArchiveActivity) {
                        ArchiveActivity activity = (ArchiveActivity) context;

                        // Post a small delay to ensure UI is still valid
                        activity.runOnUiThread(() -> {
                            if (!activity.isFinishing() && !activity.isDestroyed()) {
                                activity.refreshList();
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to unarchive chat", Toast.LENGTH_SHORT).show();
                });
    }


    /**
     * Show confirmation dialog before deleting
     */
    private void showDeleteConfirmation(ChatListItem chat, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Chat?");
        builder.setMessage("Are you sure you want to delete this conversation? This action cannot be undone.");
        builder.setPositiveButton("Delete", (dialog, which) -> deleteChat(chat, position));
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    /**
     * Delete a chat permanently
     */
    private void deleteChat(ChatListItem chat, int position) {
        String chatId = getChatId(currentUserId, chat.getReceiverId());

        // Delete all messages
        firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }

                    // Delete chat document
                    firestore.collection("chatList")
                            .document(chatId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "Chat deleted", Toast.LENGTH_SHORT).show();

                                // Remove from list
                                archivedChats.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, archivedChats.size());

                                // Refresh activity
                                if (context instanceof ArchiveActivity) {
                                    ((ArchiveActivity) context).refreshList();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to delete chat", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to delete chat", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Generate consistent chat ID
     */
    private String getChatId(String userId1, String userId2) {
        return userId1.compareTo(userId2) < 0
                ? userId1 + "_" + userId2
                : userId2 + "_" + userId1;
    }

    @Override
    public int getItemCount() {
        return archivedChats.size();
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