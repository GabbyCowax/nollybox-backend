package com.example.nollybox;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    private List<Movie> movieList;
    private int layoutId;
    private OnMenuClickListener menuClickListener;
    private OnItemClickListener itemClickListener;

    public interface OnMenuClickListener {
        void onMenuClick(View view, Movie movie, int position);
    }

    public interface OnItemClickListener {
        void onItemClick(Movie movie);
    }

    public MovieAdapter(List<Movie> movieList, int layoutId) {
        this.movieList = movieList;
        this.layoutId = layoutId;
    }

    public MovieAdapter(List<Movie> movieList, int layoutId, OnMenuClickListener listener) {
        this.movieList = movieList;
        this.layoutId = layoutId;
        this.menuClickListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movieList.get(position);
        holder.textViewTitle.setText(movie.getTitle());
        
        // Clean URL to handle Firestore quotes
        String posterUrl = movie.getPosterUrl() != null ? movie.getPosterUrl().replace("\"", "").trim() : "";

        Glide.with(holder.itemView.getContext())
                .load(posterUrl)
                .placeholder(R.color.input_bg)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.imageViewPoster);

        // 🚀 TV FOCUS EFFECT: Scale up slightly when focused
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                holder.cardPoster.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
                holder.cardPoster.setElevation(20f);
            } else {
                holder.cardPoster.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
                holder.cardPoster.setElevation(8f);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (movie == null) return;

            if (itemClickListener != null) {
                itemClickListener.onItemClick(movie);
                return;
            }
            
            Context context = v.getContext();
            Intent intent = new Intent(context, MainActivity5.class);
            
            // Robust data passing
            String title = movie.getTitle() != null ? movie.getTitle() : "NollyBox Movie";
            String genre = movie.getGenreString() != null ? movie.getGenreString() : "Nollywood";
            String vId = movie.getVideoId() != null ? movie.getVideoId() : "";
            
            intent.putExtra("title", title);
            intent.putExtra("genre", genre);
            intent.putExtra("posterUrl", posterUrl);
            intent.putExtra("videoId", vId);
            intent.putExtra("downloadUrl", movie.getDownloadUrl());
            
            // Check if context is valid for starting activity
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 🚀 Handle Menu Visibility and Click
        if (menuClickListener != null && holder.btnMenu != null) {
            holder.btnMenu.setVisibility(View.VISIBLE);
            holder.btnMenu.setOnClickListener(v -> menuClickListener.onMenuClick(v, movie, position));
        } else if (holder.btnMenu != null) {
            holder.btnMenu.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return movieList.size();
    }

    public static class MovieViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewPoster;
        TextView textViewTitle;
        ImageButton btnMenu;
        View cardPoster;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            cardPoster = itemView.findViewById(R.id.cardPoster);
            imageViewPoster = itemView.findViewById(R.id.imageViewPoster);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            
            // 🚀 CRITICAL FIX: Optional view binding to prevent NullPointerException 
            // when using layouts without a menu button (like item_movie_card_horizontal)
            btnMenu = itemView.findViewById(R.id.btn_movie_menu);
        }
    }
}