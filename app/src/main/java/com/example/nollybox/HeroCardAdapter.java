package com.example.nollybox;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class HeroCardAdapter extends RecyclerView.Adapter<HeroCardAdapter.HeroViewHolder> {

    private List<Movie> movies;
    private OnHeroClickListener listener;

    public interface OnHeroClickListener {
        void onHeroClick(Movie movie);
    }

    public HeroCardAdapter(List<Movie> movies, OnHeroClickListener listener) {
        this.movies = movies;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HeroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new HeroViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hero_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull HeroViewHolder holder, int position) {
        Movie movie = movies.get(position);
        holder.tvTitle.setText(movie.getTitle());
        
        String year = holder.itemView.getContext().getString(R.string.year_placeholder);
        if (movie.getCreatedAt() != null && movie.getCreatedAt().length() >= 4) {
            year = movie.getCreatedAt().substring(0, 4);
        }
        holder.tvMeta.setText(year + " | " + movie.getGenreString());

        // 🚀 TV FOCUS EFFECT: Scale up slightly when focused
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start();
                v.setElevation(20f);
            } else {
                v.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
                v.setElevation(8f);
            }
        });

        String posterUrl = movie.getPosterUrl() != null ? movie.getPosterUrl().replace("\"", "").trim() : "";
        Glide.with(holder.itemView.getContext())
                .load(posterUrl)
                .placeholder(R.color.input_bg)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.ivPoster);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHeroClick(movie);
            } else {
                try {
                    Context context = v.getContext();
                    Intent intent = new Intent(context, MainActivity5.class);
                    
                    String t = movie.getTitle() != null ? movie.getTitle() : "NollyBox Movie";
                    String g = movie.getGenreString() != null ? movie.getGenreString() : "Nollywood";
                    String vId = movie.getVideoId() != null ? movie.getVideoId() : "";
                    String pUrl = movie.getPosterUrl() != null ? movie.getPosterUrl().replace("\"", "") : "";

                    intent.putExtra("title", t);
                    intent.putExtra("genre", g);
                    intent.putExtra("posterUrl", pUrl);
                    intent.putExtra("videoId", vId);
                    
                    if (!(context instanceof Activity)) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                    context.startActivity(intent);
                } catch (Exception e) {
                    Log.e("HeroAdapter", "Navigation failed", e);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    static class HeroViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPoster;
        TextView tvTitle, tvMeta;

        public HeroViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.ivHeroSmallPoster);
            tvTitle = itemView.findViewById(R.id.tvHeroTitle);
            tvMeta = itemView.findViewById(R.id.tvHeroMeta);
        }
    }
}