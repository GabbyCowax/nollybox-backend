package com.example.nollybox;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class BannerPagerAdapter extends RecyclerView.Adapter<BannerPagerAdapter.BannerViewHolder> {

    private List<Movie> bannerList;
    private OnBannerInteractionListener interactionListener;

    public interface OnBannerInteractionListener {
        void onHeroSelected(int position);
    }

    public BannerPagerAdapter(List<Movie> bannerList) {
        this.bannerList = bannerList;
    }

    public void setOnBannerInteractionListener(OnBannerInteractionListener listener) {
        this.interactionListener = listener;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new BannerViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner_hero, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        Movie movie = bannerList.get(position);
        Context context = holder.itemView.getContext();
        
        holder.tvTitle.setText(movie.getTitle());
        String year = "2024";
        if (movie.getCreatedAt() != null && movie.getCreatedAt().length() >= 4) {
            year = movie.getCreatedAt().substring(0, 4);
        }
        holder.tvMeta.setText(year + " | " + movie.getGenreStringForUI());

        String posterUrl = movie.getPosterUrl() != null ? movie.getPosterUrl().replace("\"", "").trim() : "";
        
        Glide.with(context)
                .load(posterUrl)
                .placeholder(R.drawable.bg_premium_splash)
                .into(holder.ivBackdrop);
                
        if (holder.ivSmallPoster != null) {
            Glide.with(context)
                    .load(posterUrl)
                    .placeholder(R.color.input_bg)
                    .into(holder.ivSmallPoster);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MainActivity5.class);
            intent.putExtra("title", movie.getTitle());
            intent.putExtra("genre", movie.getGenreStringForUI());
            intent.putExtra("posterUrl", posterUrl);
            intent.putExtra("videoId", movie.getVideoId());
            if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return bannerList.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBackdrop, ivSmallPoster;
        TextView tvTitle, tvMeta;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBackdrop = itemView.findViewById(R.id.ivBannerBackdrop);
            ivSmallPoster = itemView.findViewById(R.id.ivBannerSmallPoster);
            tvTitle = itemView.findViewById(R.id.tvBannerTitle);
            tvMeta = itemView.findViewById(R.id.tvBannerMeta);
        }
    }
}
