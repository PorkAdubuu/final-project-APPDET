package com.example.trackback;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class MatchesAdapter extends RecyclerView.Adapter<MatchesAdapter.MatchViewHolder> {

    private List<ItemMatch> matches;
    private Context context;

    public MatchesAdapter(List<ItemMatch> matches) {
        this.matches = matches;
    }

    public void updateMatches(List<ItemMatch> newMatches) {
        this.matches = newMatches;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_match_card, parent, false);
        return new MatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        ItemMatch match = matches.get(position);
        holder.bind(match);
    }

    @Override
    public int getItemCount() {
        return matches.size();
    }

    class MatchViewHolder extends RecyclerView.ViewHolder {
        TextView similarityScore, lostItemTitle, foundItemTitle, matchReason;
        ImageView lostItemImage, foundItemImage;
        Button btnViewDetails;
        CardView cardView;

        public MatchViewHolder(@NonNull View itemView) {
            super(itemView);

            similarityScore = itemView.findViewById(R.id.similarityScore);
            lostItemTitle = itemView.findViewById(R.id.lostItemTitle);
            foundItemTitle = itemView.findViewById(R.id.foundItemTitle);
            matchReason = itemView.findViewById(R.id.matchReason);
            lostItemImage = itemView.findViewById(R.id.lostItemImage);
            foundItemImage = itemView.findViewById(R.id.foundItemImage);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            cardView = itemView.findViewById(R.id.cardView);
        }

        public void bind(ItemMatch match) {
            // Calculate and display similarity percentage
            int percentage = (int) (match.getSimilarityScore() * 100);
            similarityScore.setText(percentage + "%");

            // Set titles
            lostItemTitle.setText(match.getLostItem().getTitle());
            foundItemTitle.setText(match.getFoundItem().getTitle());

            // Set match reason (truncate if too long)
            String reason = match.getMatchReason();
            if (reason != null && reason.length() > 80) {
                reason = reason.substring(0, 77) + "...";
            }
            matchReason.setText(reason);

            // Set card background color based on match quality
            if (percentage >= 70) {
                // High match - light green
                cardView.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
                similarityScore.setTextColor(Color.parseColor("#2E7D32"));
            } else if (percentage >= 50) {
                // Medium match - light yellow
                cardView.setCardBackgroundColor(Color.parseColor("#FFF9C4"));
                similarityScore.setTextColor(Color.parseColor("#F57F17"));
            } else {
                // Lower match - white
                cardView.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
                similarityScore.setTextColor(Color.parseColor("#2196F3"));
            }

            // Load images using Glide
            if (match.getLostItem().getImageUrl() != null && !match.getLostItem().getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(match.getLostItem().getImageUrl())
                        .placeholder(R.drawable.placeholder_image)
                        .into(lostItemImage);
            } else {
                lostItemImage.setImageResource(R.drawable.placeholder_image);
            }

            if (match.getFoundItem().getImageUrl() != null && !match.getFoundItem().getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(match.getFoundItem().getImageUrl())
                        .placeholder(R.drawable.placeholder_image)
                        .into(foundItemImage);
            } else {
                foundItemImage.setImageResource(R.drawable.placeholder_image);
            }

            // Click listener to view full match details
            btnViewDetails.setOnClickListener(v -> openMatchDetail(match));

            // Optional: Click on card itself
            itemView.setOnClickListener(v -> openMatchDetail(match));
        }

        private void openMatchDetail(ItemMatch match) {
            Intent intent = new Intent(context, MatchDetailActivity.class);
            intent.putExtra("lostItemId", match.getLostItem().getId());
            intent.putExtra("foundItemId", match.getFoundItem().getId());
            intent.putExtra("similarityScore", match.getSimilarityScore());
            intent.putExtra("matchReason", match.getMatchReason());
            context.startActivity(intent);
        }
    }
}