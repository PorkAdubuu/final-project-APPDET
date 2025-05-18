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
        View view = LayoutInflater.from(context).inflate(R.layout.item_lost, parent, false);
        return new LostItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LostItemViewHolder holder, int position) {
        ListLostItem lostItem = lostItemList.get(position);

        String reportType = lostItem.getReportType(); // ← Add this check
        if (reportType != null && reportType.equalsIgnoreCase("Found")) {
            holder.itemLostText.setText("Item Found: " + lostItem.getItemLost());
        } else {
            holder.itemLostText.setText("Item Lost: " + lostItem.getItemLost());
        }

        holder.categoryText.setText("Category: " + lostItem.getCategory());
        holder.locationText.setText("Location: " + lostItem.getLastSeen());
        holder.dateText.setText("Date Lost: " + lostItem.getDate());

        if (lostItem.getProfileUrl() != null && !lostItem.getProfileUrl().isEmpty()) {
            Glide.with(context)
                    .load(lostItem.getProfileUrl())
                    .circleCrop()
                    .into(holder.profileImageView);
        } else {
            Glide.with(context)
                    .load(R.drawable.def_prof)
                    .circleCrop()
                    .into(holder.profileImageView);
        }

        holder.itemView.setOnClickListener(v -> {
            // Optional: handle item click
        });
    }



    @Override
    public int getItemCount() {
        return lostItemList.size();
    }

    // Add this method to update the adapter's list and refresh the RecyclerView
    public void updateList(List<ListLostItem> newList) {
        lostItemList = newList;
        notifyDataSetChanged();
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
