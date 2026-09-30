package com.example.binittowinit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.binittowinit.database.ScoreEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@link RecyclerView.Adapter} that formats and displays the high score leaderboard rows.
 * <p>
 * Visual formatting:
 * <ul>
 *   <li>Renders distinct gold, silver, and bronze medal icons for Ranks 1, 2, and 3.</li>
 *   <li>Formats ranks 4+ with bold "#N" text.</li>
 *   <li>Formats numeric scores with locale digit-grouping commas (e.g., "12,500").</li>
 *   <li>Presents breakdown metrics: "Sorted: X • Shooed: Y" and the match timestamp.</li>
 * </ul>
 * </p>
 */
public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
    private final List<ScoreEntry> items = new ArrayList<>();

    /**
     * Replaces the current data set with new score records and triggers a full view refresh.
     *
     * @param scores New list of {@link ScoreEntry} records.
     */
    public void setScores(List<ScoreEntry> scores) {
        items.clear();
        if (scores != null) {
            items.addAll(scores);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScoreEntry entry = items.get(position);
        int rank = position + 1;

        // Display metallic medal icons and distinct podium gradients for top 3 places
        if (rank == 1) {
            holder.itemView.setBackgroundResource(R.drawable.bg_podium_gold);
            holder.imgMedal.setVisibility(View.VISIBLE);
            holder.imgMedal.setImageResource(R.drawable.ic_medal_gold);
            holder.tvRank.setVisibility(View.GONE);
        } else if (rank == 2) {
            holder.itemView.setBackgroundResource(R.drawable.bg_podium_silver);
            holder.imgMedal.setVisibility(View.VISIBLE);
            holder.imgMedal.setImageResource(R.drawable.ic_medal_silver);
            holder.tvRank.setVisibility(View.GONE);
        } else if (rank == 3) {
            holder.itemView.setBackgroundResource(R.drawable.bg_podium_bronze);
            holder.imgMedal.setVisibility(View.VISIBLE);
            holder.imgMedal.setImageResource(R.drawable.ic_medal_bronze);
            holder.tvRank.setVisibility(View.GONE);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_card);
            holder.imgMedal.setVisibility(View.GONE);
            holder.tvRank.setVisibility(View.VISIBLE);
            holder.tvRank.setText("#" + rank);
        }

        holder.tvPlayerName.setText(entry.getPlayerName());
        holder.tvScore.setText(String.format(Locale.getDefault(), "%,d", entry.getScore()));
        holder.tvDetails.setText("Sorted: " + entry.getItemsSorted() + " • Shooed: " + entry.getCrittersRescued());
        holder.tvDate.setText(entry.getTimestamp());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * ViewHolder caching references to the card row child views.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgMedal;
        final TextView tvRank;
        final TextView tvPlayerName;
        final TextView tvScore;
        final TextView tvDetails;
        final TextView tvDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMedal = itemView.findViewById(R.id.img_rank_medal);
            tvRank = itemView.findViewById(R.id.tv_rank_number);
            tvPlayerName = itemView.findViewById(R.id.tv_item_player_name);
            tvScore = itemView.findViewById(R.id.tv_item_score);
            tvDetails = itemView.findViewById(R.id.tv_item_details);
            tvDate = itemView.findViewById(R.id.tv_item_date);
        }
    }
}
