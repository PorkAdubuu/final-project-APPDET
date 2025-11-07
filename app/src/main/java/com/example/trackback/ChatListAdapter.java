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
import androidx.core.content.ContextCompat;
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
 * Adapter for displaying chat list with Archive, Block, and Delete functionality
 */
public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    private List<ChatListItem> chatList;
    private Context context;
    private FirebaseFirestore firestore;
    private String currentUserId;

    public ChatListAdapter(List<ChatListItem> chatList, Context context) {
        this.chatList = chatList;
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
        ChatListItem chat = chatList.get(position);

        // Set name
        holder.nameText.setText(chat.getName() != null ? chat.getName() : "User");

        // ✅ Add "You: " prefix and "· Seen" if message was read
        String displayMessage = chat.getLastMessage();
        if (displayMessage != null && !displayMessage.isEmpty()) {
            if (chat.getLastSenderId() != null && chat.getLastSenderId().equals(currentUserId)) {
                // You sent the last message
                if (!chat.isUnread()) {
                    displayMessage = "You: " + displayMessage;
                } else {
                    displayMessage = "You: " + displayMessage;
                }
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

        // Set read/unread background
        if (chat.isUnread()) {
            holder.itemView.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_unread));
        } else {
            holder.itemView.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_read));
        }

        // Regular click - open chat
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);

            // Determine who the other person is
            String otherUserId = chat.getReceiverId().equals(currentUserId)
                    ? chat.getSenderId()
                    : chat.getReceiverId();


            intent.putExtra("receiverId", otherUserId);
            intent.putExtra("receiverName", chat.getName());
            intent.putExtra("profileImageUrl", chat.getProfileImageUrl());

            context.startActivity(intent);
        });


        // Long press - show options bottom sheet
        holder.itemView.setOnLongClickListener(v -> {
            showOptionsBottomSheet(chat, position);
            return true;
        });
    }

    /**
     * Show bottom sheet with Block, Archive, Delete options
     */
    private void showOptionsBottomSheet(ChatListItem chat, int position) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_chat_options, null);
        bottomSheet.setContentView(view);

        // Block option
        LinearLayout blockOption = view.findViewById(R.id.blockOption);
        blockOption.setOnClickListener(v -> {
            bottomSheet.dismiss();
            showBlockConfirmation(chat, position);
        });

        // Archive option
        LinearLayout archiveOption = view.findViewById(R.id.archiveOption);
        archiveOption.setOnClickListener(v -> {
            bottomSheet.dismiss();
            archiveChat(chat, position);
        });

        // Delete option
        LinearLayout deleteOption = view.findViewById(R.id.deleteOption);
        deleteOption.setOnClickListener(v -> {
            bottomSheet.dismiss();
            showDeleteConfirmation(chat, position);
        });

        bottomSheet.show();
    }

    /**
     * Show confirmation dialog before blocking user
     */
    private void showBlockConfirmation(ChatListItem chat, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Block " + chat.getName() + "?");
        builder.setMessage("They won't be able to send you messages. You can unblock them later.");
        builder.setPositiveButton("Block", (dialog, which) -> blockUser(chat, position));
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    /**
     * Show confirmation dialog before deleting chat
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
     * Block a user - adds to blockedUsers collection
     */
    private void blockUser(ChatListItem chat, int position) {
        Map<String, Object> blockData = new HashMap<>();
        blockData.put("blockedUserId", chat.getReceiverId());
        blockData.put("blockedUserName", chat.getName());
        blockData.put("blockedUserProfileUrl", chat.getProfileImageUrl());
        blockData.put("timestamp", Timestamp.now());

        firestore.collection("users")
                .document(currentUserId)
                .collection("blockedUsers")
                .document(chat.getReceiverId())
                .set(blockData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, chat.getName() + " has been blocked", Toast.LENGTH_SHORT).show();

                    // Send a system message to notify the other user (optional)
                    sendBlockNotification(chat.getReceiverId());

                    // Remove from chat list
                    chatList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, chatList.size());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to block user", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Optional: Send a notification that user is blocked
     */
    private void sendBlockNotification(String blockedUserId) {
        String chatId = getChatId(currentUserId, blockedUserId);

        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("message", "This user has blocked you. You cannot send messages.");
        systemMessage.put("senderId", "SYSTEM");
        systemMessage.put("timestamp", Timestamp.now());
        systemMessage.put("isSystemMessage", true);

        firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(systemMessage);
    }

    /**
     * Archive a chat - sets archivedFor_[userId] flag
     */
    private void archiveChat(ChatListItem chat, int position) {
        String chatId = getChatId(currentUserId, chat.getReceiverId());

        Map<String, Object> archiveData = new HashMap<>();
        archiveData.put("archivedFor_" + currentUserId, true);

        firestore.collection("chatList")
                .document(chatId)
                .update(archiveData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Chat archived", Toast.LENGTH_SHORT).show();

                    // Remove from chat list
                    chatList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, chatList.size());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to archive chat", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Delete a chat permanently - removes all messages and chat document
     */
    private void deleteChat(ChatListItem chat, int position) {
        String chatId = getChatId(currentUserId, chat.getReceiverId());

        // Delete all messages first
        firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Delete each message
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }

                    // Delete the chat document
                    firestore.collection("chatList")
                            .document(chatId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "Chat deleted", Toast.LENGTH_SHORT).show();

                                // Remove from chat list
                                chatList.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, chatList.size());
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
     * Generate consistent chat ID for two users
     */
    private String getChatId(String userId1, String userId2) {
        return userId1.compareTo(userId2) < 0
                ? userId1 + "_" + userId2
                : userId2 + "_" + userId1;
    }

    @Override
    public int getItemCount() {
        return chatList.size();
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