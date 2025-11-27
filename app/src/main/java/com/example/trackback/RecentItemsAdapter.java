package com.example.trackback;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RecentItemsAdapter extends RecyclerView.Adapter<RecentItemsAdapter.ViewHolder> {

    private List<Item> items;
    private Context context;

    public RecentItemsAdapter(List<Item> items, Context context) {
        this.items = items;
        this.context = context;
    }

    public void updateItems(List<Item> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_recent_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, typeTextView, locationTextView, dateTextView;
        ImageView itemImageView, typeIcon;
        CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            titleTextView = itemView.findViewById(R.id.itemTitle);
            typeTextView = itemView.findViewById(R.id.itemType);
            locationTextView = itemView.findViewById(R.id.itemLocation);
            dateTextView = itemView.findViewById(R.id.itemDate);
            itemImageView = itemView.findViewById(R.id.itemImage);
            typeIcon = itemView.findViewById(R.id.typeIcon);
            cardView = itemView.findViewById(R.id.cardView);
        }

        public void bind(Item item) {
            titleTextView.setText(item.getTitle());
            typeTextView.setText(item.getType());
            locationTextView.setText(item.getLocation());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String date = sdf.format(item.getTimestamp().toDate());
            dateTextView.setText(date);

            if (item.getType() != null && item.getType().equalsIgnoreCase("LOST")) {
                typeTextView.setTextColor(Color.parseColor("#D32F2F"));
                typeIcon.setImageResource(R.drawable.ic_lost);
            } else if (item.getType() != null && item.getType().equalsIgnoreCase("FOUND")) {
                typeTextView.setTextColor(Color.parseColor("#388E3C"));
                typeIcon.setImageResource(R.drawable.ic_found);
            } else {
                typeTextView.setTextColor(Color.parseColor("#888888"));
                typeIcon.setImageResource(R.drawable.ic_lost);
            }

            if (item.getImageUrl() != null) {
                Glide.with(context)
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.placeholder_image)
                        .into(itemImageView);
            }

            itemView.setOnClickListener(v -> {
                FirebaseFirestore.getInstance()
                        .collection("lostItems")
                        .document(item.getId())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                ListLostItem lostItem = documentSnapshot.toObject(ListLostItem.class);
                                if (lostItem != null) {
                                    lostItem.setDocumentId(documentSnapshot.getId());
                                    LostItemDetailsDialog dialog = LostItemDetailsDialog.newInstance(lostItem);
                                    dialog.show(((FragmentActivity) context).getSupportFragmentManager(), "LostItemDetailsDialog");
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                        });
            });
        }
    }
}