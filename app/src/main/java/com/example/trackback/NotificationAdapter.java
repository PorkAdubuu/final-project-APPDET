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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private Context context;
    private List<NotificationModel> notificationList;

    public NotificationAdapter(Context context, List<NotificationModel> notificationList) {
        this.context = context;
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_notif, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationModel notif = notificationList.get(position);

        holder.fullnameText.setText(notif.getFname() + " " + notif.getLastName());
        holder.itemText.setText("Posted a new " + notif.getReportType() + " item.");
        holder.dateAndTimeText.setText(notif.getDate() + " | " + notif.getTime());
        holder.typeText.setText("Type: " + notif.getReportType());

        if (notif.getImageUrl() != null && !notif.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(notif.getImageUrl())
                    .circleCrop()
                    .into(holder.profileImageView);

        } else {
            holder.profileImageView.setImageResource(R.drawable.def_prof); // fallback image
        }

        if (!notif.isRead()) {
            holder.itemView.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_unread));
        } else {
            holder.itemView.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_read));
        }

        holder.viewPostBtn.setOnClickListener(v -> {
            String lostItemDocId = notif.getDocumentId();

            FirebaseFirestore.getInstance()
                    .collection("lostItems")
                    .document(lostItemDocId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            ListLostItem lostItem = documentSnapshot.toObject(ListLostItem.class);
                            if (lostItem != null) {
                                LostItemDetailsDialog dialog = LostItemDetailsDialog.newInstance(lostItem); // ✅ FIXED
                                dialog.show(((FragmentActivity) context).getSupportFragmentManager(), "LostItemDetailsDialog");

                                // Mark notification as read
                                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                FirebaseFirestore.getInstance()
                                        .collection("users")
                                        .document(userId)
                                        .collection("notifications")
                                        .document(notif.getNotificationDocId())  // use the Firestore doc ID stored in model
                                        .update("read", true)
                                        .addOnSuccessListener(aVoid -> {
                                            // Update local model and notify adapter
                                            notif.setRead(true);  // make sure your model has setRead(boolean) method
                                            notifyItemChanged(position);  // update this item in RecyclerView
                                        });

                            }
                        } else {
                            Toast.makeText(context, "Lost item details not found.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to load lost item details.", Toast.LENGTH_SHORT).show();
                    });
        });

    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView fullnameText, itemText, dateAndTimeText, typeText;
        ImageView profileImageView;
        LinearLayout viewPostBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fullnameText = itemView.findViewById(R.id.fullnameText);
            itemText = itemView.findViewById(R.id.itemText);
            dateAndTimeText = itemView.findViewById(R.id.dateAndTimeext);
            typeText = itemView.findViewById(R.id.typeText);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            viewPostBtn = itemView.findViewById(R.id.viewPostBtn);
        }
    }
}
