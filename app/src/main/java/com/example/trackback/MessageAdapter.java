package com.example.trackback;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private Context context;
    private List<Message> messages;
    private String currentUserId;

    public MessageAdapter(Context context, List<Message> messages, String currentUserId) {
        this.context = context;
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).bind(message, context);
        } else {
            ((ReceivedViewHolder) holder).bind(message, context);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ---------------- SENT MESSAGE HOLDER ----------------
    static class SentViewHolder extends RecyclerView.ViewHolder {

        TextView textMessage, textTime, readStatus;
        ImageView imageMessage;

        SentViewHolder(View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessageSent);
            textTime = itemView.findViewById(R.id.textTimeSent);
            imageMessage = itemView.findViewById(R.id.imageMessageSent);
        }

        void bind(Message message, Context context) {
            // Check if message has an image
            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                // Show image, hide text
                imageMessage.setVisibility(View.VISIBLE);
                textMessage.setVisibility(View.GONE);

                // Load image with Glide
                Glide.with(context)
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.circle_outline)
                        .into(imageMessage);
            } else {
                // Show text, hide image
                imageMessage.setVisibility(View.GONE);
                textMessage.setVisibility(View.VISIBLE);
                textMessage.setText(message.getMessageText());
            }

            // Show timestamp
            Timestamp timestamp = message.getTimestamp();
            if (timestamp != null) {
                Date date = timestamp.toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                textTime.setText(sdf.format(date));
            } else {
                textTime.setText("");
            }

            // Show read status
            // Remove read status entirely
            if (readStatus != null) {
                readStatus.setVisibility(View.GONE);
            }

        }
    }

    // ---------------- RECEIVED MESSAGE HOLDER ----------------
    static class ReceivedViewHolder extends RecyclerView.ViewHolder {

        TextView textMessage, textTime;
        ImageView imageMessage;

        ReceivedViewHolder(View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessageReceived);
            textTime = itemView.findViewById(R.id.textTimeReceived);
            imageMessage = itemView.findViewById(R.id.imageMessageReceived);
        }

        void bind(Message message, Context context) {
            // Check if message has an image
            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                // Show image, hide text
                imageMessage.setVisibility(View.VISIBLE);
                textMessage.setVisibility(View.GONE);

                // Load image with Glide
                Glide.with(context)
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.circle_outline)
                        .into(imageMessage);
            } else {
                // Show text, hide image
                imageMessage.setVisibility(View.GONE);
                textMessage.setVisibility(View.VISIBLE);
                textMessage.setText(message.getMessageText());
            }

            // Show timestamp
            Timestamp timestamp = message.getTimestamp();
            if (timestamp != null) {
                Date date = timestamp.toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                textTime.setText(sdf.format(date));
            } else {
                textTime.setText("");
            }
        }
    }
}
