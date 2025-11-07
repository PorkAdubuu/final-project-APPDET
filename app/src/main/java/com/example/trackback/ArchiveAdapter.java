package com.example.trackback;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ArchiveAdapter extends RecyclerView.Adapter<ArchiveAdapter.ViewHolder> {

    private List<ChatListItem> archivedChats;
    private Context context;
    private FirebaseFirestore firestore;
    private String currentUserId;

    public ArchiveAdapter(List<ChatListItem> archivedChats, Context context) {
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

        holder.nameText.setText(chat.getName() != null ? chat.getName() : "User");
        holder.lastMessageText.setText(chat.getLastMessage());

        if (chat.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            holder.timeText.setText(sdf.format(chat.getTimestamp().toDate()));
        }

        if (chat.getProfileImageUrl() != null && !chat.getProfileImageUrl().isEmpty()) {
            Glide.with(context).load(chat.getProfileImageUrl()).circleCrop().into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.default_avatar);
        }

        holder.itemView.setOnClickListener(v -> unarchiveChat(chat, position));
    }

    private void unarchiveChat(ChatListItem chat, int position) {
        String chatId = currentUserId.compareTo(chat.getReceiverId()) < 0
                ? currentUserId + "_" + chat.getReceiverId()
                : chat.getReceiverId() + "_" + currentUserId;

        Map<String, Object> update = new HashMap<>();
        update.put("archivedFor_" + currentUserId, false);

        firestore.collection("chatList")
                .document(chatId)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Chat unarchived", Toast.LENGTH_SHORT).show();
                    archivedChats.remove(position);
                    notifyItemRemoved(position);
                });
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