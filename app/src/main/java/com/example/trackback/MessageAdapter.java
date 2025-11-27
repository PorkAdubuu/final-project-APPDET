package com.example.trackback;

import android.content.Context;
import android.util.Log;
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
    private static final int VIEW_TYPE_SYSTEM = 3;

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

        // Log for debugging
        Log.d("MessageAdapter", "Checking message at position " + position);
        Log.d("MessageAdapter", "  - messageText: " + message.getMessageText());
        Log.d("MessageAdapter", "  - isSystemMessage(): " + message.isSystemMessage());
        Log.d("MessageAdapter", "  - senderId: " + message.getSenderId());

        // Check if it's a system message
        if (message.isSystemMessage()) {
            Log.d("MessageAdapter", "  → This is a SYSTEM message!");
            return VIEW_TYPE_SYSTEM;
        }

        // Check if sent or received
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            Log.d("MessageAdapter", "  → This is a SENT message");
            return VIEW_TYPE_SENT;
        } else {
            Log.d("MessageAdapter", "  → This is a RECEIVED message");
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        Log.d("MessageAdapter", "onCreateViewHolder called with viewType: " + viewType);

        if (viewType == VIEW_TYPE_SYSTEM) {
            Log.d("MessageAdapter", "Creating SYSTEM view holder");
            view = LayoutInflater.from(context).inflate(R.layout.item_message_system, parent, false);
            return new SystemViewHolder(view);
        } else if (viewType == VIEW_TYPE_SENT) {
            Log.d("MessageAdapter", "Creating SENT view holder");
            view = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            Log.d("MessageAdapter", "Creating RECEIVED view holder");
            view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        if (holder instanceof SystemViewHolder) {
            Log.d("MessageAdapter", "Binding SYSTEM message");
            ((SystemViewHolder) holder).bind(message);
        } else if (holder instanceof SentViewHolder) {
            Log.d("MessageAdapter", "Binding SENT message");
            ((SentViewHolder) holder).bind(message, context);
        } else if (holder instanceof ReceivedViewHolder) {
            Log.d("MessageAdapter", "Binding RECEIVED message");
            ((ReceivedViewHolder) holder).bind(message, context);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ---------------- SYSTEM MESSAGE HOLDER ----------------
    static class SystemViewHolder extends RecyclerView.ViewHolder {
        TextView textSystemMessage, textSystemTime;

        SystemViewHolder(View itemView) {
            super(itemView);
            textSystemMessage = itemView.findViewById(R.id.textSystemMessage);
            textSystemTime = itemView.findViewById(R.id.textSystemTime);

            if (textSystemMessage == null) {
                Log.e("SystemViewHolder", "ERROR: textSystemMessage is NULL! Check item_message_system.xml");
            }
            if (textSystemTime == null) {
                Log.e("SystemViewHolder", "ERROR: textSystemTime is NULL! Check item_message_system.xml");
            }
        }

        void bind(Message message) {
            if (textSystemMessage != null) {
                textSystemMessage.setText(message.getMessageText());
            }

            if (textSystemTime != null) {
                Timestamp timestamp = message.getTimestamp();
                if (timestamp != null) {
                    Date date = timestamp.toDate();
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    textSystemTime.setText(sdf.format(date));
                } else {
                    textSystemTime.setText("");
                }
            }
        }
    }

    // ---------------- SENT MESSAGE HOLDER ----------------
    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textTime;
        ImageView imageMessage;

        SentViewHolder(View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessageSent);
            textTime = itemView.findViewById(R.id.textTimeSent);
            imageMessage = itemView.findViewById(R.id.imageMessageSent);
        }

        void bind(Message message, Context context) {
            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                imageMessage.setVisibility(View.VISIBLE);
                textMessage.setVisibility(View.GONE);
                Glide.with(context)
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.circle_outline)
                        .into(imageMessage);
            } else {
                imageMessage.setVisibility(View.GONE);
                textMessage.setVisibility(View.VISIBLE);
                textMessage.setText(message.getMessageText());
            }

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
            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                imageMessage.setVisibility(View.VISIBLE);
                textMessage.setVisibility(View.GONE);
                Glide.with(context)
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.circle_outline)
                        .into(imageMessage);
            } else {
                imageMessage.setVisibility(View.GONE);
                textMessage.setVisibility(View.VISIBLE);
                textMessage.setText(message.getMessageText());
            }

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