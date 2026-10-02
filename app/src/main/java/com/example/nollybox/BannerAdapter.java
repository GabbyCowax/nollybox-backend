package com.example.nollybox;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private List<Movie> bannerList;

    public BannerAdapter(List<Movie> bannerList) {
        this.bannerList = bannerList;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        Movie movie = bannerList.get(position);
        
        String bannerUrl = movie.getBannerUrl() != null ? movie.getBannerUrl().replace("\"", "").trim() : "";

        Glide.with(holder.itemView.getContext())
                .load(bannerUrl)
                .placeholder(R.color.input_bg)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.imageViewBanner);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), MainActivity5.class);
            intent.putExtra("title", movie.getTitle());
            intent.putExtra("genre", movie.getGenreString());
            intent.putExtra("posterUrl", bannerUrl);
            intent.putExtra("videoId", movie.getVideoId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return bannerList.size();
    }

    public static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewBanner;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewBanner = itemView.findViewById(R.id.imageViewBanner);
        }
    }
}