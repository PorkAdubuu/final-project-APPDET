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

import java.util.List;

public class ListLostItemsAdapter extends RecyclerView.Adapter<ListLostItemsAdapter.LostItemViewHolder> {

    private List<ListLostItem> lostItemList;
    private Context context;

    // Constructor
    public ListLostItemsAdapter(List<ListLostItem> lostItemList, Context context) {
        this.lostItemList = lostItemList;
        this.context = context;
    }

    @NonNull
    @Override
    public LostItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_lost, parent, false);  // Inflate item layout
        return new LostItemViewHolder(view);  // Return ViewHolder
    }

    @Override
    public void onBindViewHolder(@NonNull LostItemViewHolder holder, int position) {
        ListLostItem lostItem = lostItemList.get(position);

        // Set text data to TextViews
        holder.itemLostText.setText("Item Lost: " + lostItem.getItemLost());
        holder.categoryText.setText("Category: " + lostItem.getCategory());
        holder.locationText.setText("Location: " + lostItem.getLastSeen());
        holder.dateText.setText("Date Lost: " + lostItem.getDate());

        // Load profile image using Glide (add circleCrop for circular images, if necessary)
        if (lostItem.getProfileUrl() != null && !lostItem.getProfileUrl().isEmpty()) {
            Glide.with(context)
                    .load(lostItem.getProfileUrl())  // Load image URL
                    .circleCrop()  // Optional: make the image circular
                    .into(holder.profileImageView);  // Set the image in ImageView
        } else {
            // Set a placeholder or fallback image if the URL is empty or null
            Glide.with(context)
                    .load(R.drawable.def_prof)  // Placeholder image resource
                    .circleCrop()
                    .into(holder.profileImageView);  // Set the image
        }

        // Handle item click (optional)
        holder.itemView.setOnClickListener(v -> {
            // Implement the action when an item is clicked (e.g., open detailed view)
        });
    }

    @Override
    public int getItemCount() {
        return lostItemList.size();  // Return the total number of items
    }

    public static class LostItemViewHolder extends RecyclerView.ViewHolder {
        TextView itemLostText, categoryText, locationText, dateText;
        ImageView profileImageView;

        public LostItemViewHolder(View itemView) {
            super(itemView);
            itemLostText = itemView.findViewById(R.id.itemLostText);
            categoryText = itemView.findViewById(R.id.categoryText);
            locationText = itemView.findViewById(R.id.locationText);
            dateText = itemView.findViewById(R.id.dateText);
            profileImageView = itemView.findViewById(R.id.profileImageView);
        }
    }
}
