package com.example.trackback;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final List<LostItem> lostItemList;
    private final Context context;

    public PostAdapter(Context context, List<LostItem> lostItemList) {
        this.context = context;
        this.lostItemList = lostItemList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.postmanagement, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        LostItem item = lostItemList.get(position);

        holder.itemLostText.setText("Item: " + item.getItemLost());
        holder.categoryText.setText("Category: " + item.getCategory());
        holder.dateText.setText("Date Lost: " + item.getDate());
        holder.timeText.setText("Time Lost: " + item.getTime());
        holder.locationText.setText("Last Seen: " + item.getLastSeen());

        Timestamp ts = item.getTimestamp();
        if (ts != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            holder.datePostText.setText("Posted on: " + sdf.format(ts.toDate()));
        } else {
            holder.datePostText.setText("Posted on: N/A");
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, LostItemDetailActivity.class);

            // Pass documentId too
            intent.putExtra("documentId", item.getDocumentId());

            // Pass other fields as extras
            intent.putExtra("itemLost", item.getItemLost());
            intent.putExtra("category", item.getCategory());
            intent.putExtra("brand", item.getBrand());
            intent.putExtra("date", item.getDate());
            intent.putExtra("time", item.getTime());
            intent.putExtra("additionalInfo", item.getAdditionalInfo());
            intent.putExtra("lastSeen", item.getLastSeen());
            intent.putExtra("moreInfo", item.getMoreInfo());
            intent.putExtra("firstName", item.getFirstName());
            intent.putExtra("lastName", item.getLastName());
            intent.putExtra("phone", item.getPhone());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return lostItemList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView itemLostText, categoryText, dateText, timeText, locationText, datePostText;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            itemLostText = itemView.findViewById(R.id.itemLostText);
            categoryText = itemView.findViewById(R.id.categoryText);
            dateText = itemView.findViewById(R.id.dateText);
            timeText = itemView.findViewById(R.id.timeText);
            locationText = itemView.findViewById(R.id.locationText);
            datePostText = itemView.findViewById(R.id.datePostText);
        }
    }
}
