package com.example.nollybox;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;

public class HomeMainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_BANNER = 0;
    public static final int VIEW_CATEGORIES = 1;
    public static final int VIEW_TITLE = 2;
    public static final int VIEW_HORIZONTAL_LIST = 3;
    public static final int VIEW_MOVIE = 4;
    public static final int VIEW_RECENT = 5;
    public static final int VIEW_WATCHLIST = 6;

    private List<Movie> featuredMovies;
    private List<Movie> latestMovies;
    private List<Movie> allMovies;
    private List<Movie> recentMovies; // 🚀 Now uses filtered movie objects
    private List<Movie> watchlistMovies; // 🚀 Now uses filtered movie objects
    private CategoryClickListener categoryClickListener;
    private String searchQuery = "";
    private String currentCategory = "All";

    public interface CategoryClickListener {
        void onCategorySelected(String category);
    }

    public HomeMainAdapter(List<Movie> featuredMovies, List<Movie> latestMovies, List<Movie> allMovies, 
                           List<Movie> recentMovies, List<Movie> watchlistMovies, CategoryClickListener listener) {
        this.featuredMovies = featuredMovies;
        this.latestMovies = latestMovies;
        this.allMovies = allMovies;
        this.recentMovies = recentMovies;
        this.watchlistMovies = watchlistMovies;
        this.categoryClickListener = listener;
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.trim() : "";
    }

    public void setCurrentCategory(String category) {
        this.currentCategory = category != null ? category : "All";
    }

    @Override
    public int getItemViewType(int position) {
        // 🚀 SEARCH MODE: If user is searching, hide everything except results
        if (searchQuery != null && !searchQuery.isEmpty()) {
            return VIEW_MOVIE;
        }

        // 🚀 MAIN SECTIONS: Home (Trending/Nollywood) and Movie/FightZone tabs get the FULL professional layout
        String cat = (currentCategory != null) ? currentCategory.toLowerCase().trim() : "all";
        boolean isMainSection = cat.equals("all") || cat.equals("trending") || 
                               cat.equals("movie") || cat.equals("nollywood") || 
                               cat.equals("fightzone") || cat.equals("action");

        if (!isMainSection) {
            // Standard category view (Epic, Drama, etc.): Banner + Grid
            if (position == 0) return VIEW_BANNER;
            if (position == 1) return VIEW_CATEGORIES;
            if (position == 2) return VIEW_TITLE; 
            if (position == 3) return VIEW_HORIZONTAL_LIST; 
            if (position == 4) return VIEW_TITLE; 
            return VIEW_MOVIE;
        }

        // 🚀 SMART VISIBILITY: Use pre-filtered lists
        int recentSize = (recentMovies != null) ? recentMovies.size() : 0;
        int watchlistSize = (watchlistMovies != null) ? watchlistMovies.size() : 0;

        boolean hasRecent = recentSize > 0;
        boolean hasWatchlist = watchlistSize > 0;

        if (position == 0) return VIEW_BANNER;
        if (position == 1) return VIEW_CATEGORIES;
        
        int currentPos = 2;
        if (hasRecent) {
            if (position == currentPos) return VIEW_TITLE; 
            if (position == currentPos + 1) return VIEW_RECENT;
            currentPos += 2;
        }
        
        if (hasWatchlist) {
            if (position == currentPos) return VIEW_TITLE; 
            if (position == currentPos + 1) return VIEW_WATCHLIST;
            currentPos += 2;
        }

        if (position == currentPos) return VIEW_TITLE; 
        if (position == currentPos + 1) return VIEW_HORIZONTAL_LIST;
        if (position == currentPos + 2) return VIEW_TITLE; 
        return VIEW_MOVIE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEW_BANNER:
                return new BannerViewHolder(inflater.inflate(R.layout.layout_home_banner, parent, false));
            case VIEW_CATEGORIES:
                return new CategoriesViewHolder(inflater.inflate(R.layout.layout_home_categories, parent, false));
            case VIEW_TITLE:
                return new TitleViewHolder(inflater.inflate(R.layout.layout_home_section_title, parent, false));
            case VIEW_RECENT:
                return new HorizontalListViewHolder(inflater.inflate(R.layout.layout_home_horizontal_list, parent, false), VIEW_RECENT);
            case VIEW_HORIZONTAL_LIST:
            case VIEW_WATCHLIST:
                return new HorizontalListViewHolder(inflater.inflate(R.layout.layout_home_horizontal_list, parent, false), VIEW_HORIZONTAL_LIST);
            default:
                return new MovieAdapter.MovieViewHolder(inflater.inflate(R.layout.item_movie_card, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // 🚀 SEARCH MODE BINDING
        if (searchQuery != null && !searchQuery.isEmpty()) {
            if (allMovies != null && position >= 0 && position < allMovies.size()) {
                Movie movie = allMovies.get(position);
                bindMovieToViewHolder((MovieAdapter.MovieViewHolder) holder, movie);
            }
            return;
        }

        // 🚀 CATEGORY MODE BINDING (Excluding main sections)
        String cat = (currentCategory != null) ? currentCategory.toLowerCase().trim() : "all";
        boolean isMainSection = cat.equals("all") || cat.equals("trending") || 
                               cat.equals("movie") || cat.equals("nollywood") || 
                               cat.equals("fightzone") || cat.equals("action");
        
        if (!isMainSection) {
            if (holder instanceof BannerViewHolder) {
                ((BannerViewHolder) holder).bind(featuredMovies);
            } else if (holder instanceof CategoriesViewHolder) {
                ((CategoriesViewHolder) holder).bind(currentCategory, categoryClickListener);
            } else if (holder instanceof TitleViewHolder) {
                if (position == 2) ((TitleViewHolder) holder).tvTitle.setText("Popular in " + currentCategory);
                else if (position == 4) ((TitleViewHolder) holder).tvTitle.setText("Explore All " + currentCategory);
            } else if (holder instanceof HorizontalListViewHolder) {
                ((HorizontalListViewHolder) holder).bind(latestMovies);
            } else if (holder instanceof MovieAdapter.MovieViewHolder) {
                int moviePos = position - 5;
                if (allMovies != null && moviePos >= 0 && moviePos < allMovies.size()) {
                    bindMovieToViewHolder((MovieAdapter.MovieViewHolder) holder, allMovies.get(moviePos));
                }
            }
            return;
        }

        // 🚀 STABLE SECTION BINDING
        int recentSize = (recentMovies != null) ? recentMovies.size() : 0;
        int watchlistSize = (watchlistMovies != null) ? watchlistMovies.size() : 0;
        
        int offset = (recentSize > 0 ? 2 : 0) + (watchlistSize > 0 ? 2 : 0);

        if (holder instanceof BannerViewHolder) {
            ((BannerViewHolder) holder).bind(featuredMovies);
        } else if (holder instanceof CategoriesViewHolder) {
            ((CategoriesViewHolder) holder).bind(currentCategory, categoryClickListener);
        } else if (holder instanceof TitleViewHolder) {
            String title = "Full Catalog";
            int currentPos = 2;
            if (recentSize > 0) {
                if (position == currentPos) title = "Continue Watching";
                currentPos += 2;
            }
            if (watchlistSize > 0) {
                if (position == currentPos) title = "My Watchlist";
                currentPos += 2;
            }
            
            // 🚀 DYNAMIC HEADERS based on Category
            boolean isActionMode = cat.contains("movie") || cat.contains("fight") || cat.contains("action");
            
            if (position == currentPos) {
                title = isActionMode ? "Trending Movie" : holder.itemView.getContext().getString(R.string.latest_releases);
            } else if (position == currentPos + 2) {
                title = isActionMode ? "All Action Movies" : "All " + currentCategory;
            }
            ((TitleViewHolder) holder).tvTitle.setText(title);
        } else if (holder instanceof HorizontalListViewHolder) {
            int currentPos = 3; 
            if (recentSize > 0) {
                if (position == currentPos) {
                    ((HorizontalListViewHolder) holder).bind(recentMovies);
                    return;
                }
                currentPos += 2;
            }
            if (watchlistSize > 0) {
                if (position == currentPos) {
                    ((HorizontalListViewHolder) holder).bind(watchlistMovies);
                    return;
                }
                currentPos += 2;
            }
            if (position == currentPos) {
                ((HorizontalListViewHolder) holder).bind(latestMovies);
            }
        } else if (holder instanceof MovieAdapter.MovieViewHolder) {
            int moviePos = position - (5 + offset);
            if (allMovies != null && moviePos >= 0 && moviePos < allMovies.size()) {
                bindMovieToViewHolder((MovieAdapter.MovieViewHolder) holder, allMovies.get(moviePos));
            }
        }
    }

    private void bindMovieToViewHolder(MovieAdapter.MovieViewHolder movieVH, Movie movie) {
        if (movie == null || movieVH == null) return;
        
        if (movieVH.textViewTitle != null) {
            movieVH.textViewTitle.setText(movie.getTitle());
        }
        
        String posterUrl = movie.getPosterUrl() != null ? movie.getPosterUrl().replace("\"", "").trim() : "";
        if (movieVH.imageViewPoster != null && !posterUrl.isEmpty()) {
            Glide.with(movieVH.itemView.getContext())
                    .load(posterUrl)
                    .placeholder(R.color.input_bg)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(movieVH.imageViewPoster);
        }
        
        // 🚀 TV FOCUS EFFECT for catalog items
        movieVH.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                movieVH.cardPoster.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
                movieVH.cardPoster.setElevation(20f);
            } else {
                movieVH.cardPoster.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
                movieVH.cardPoster.setElevation(8f);
            }
        });
        
        movieVH.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            if (context == null) return;
            
            Intent intent = new Intent(context, MainActivity5.class);
            
            // Safe string data for intent
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
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e("Adapter", "Navigation failed", e);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (searchQuery != null && !searchQuery.isEmpty()) {
            return allMovies != null ? allMovies.size() : 0;
        }

        String cat = (currentCategory != null) ? currentCategory.toLowerCase().trim() : "all";
        boolean isMainSection = cat.equals("all") || cat.equals("trending") || 
                               cat.equals("movie") || cat.equals("nollywood") || 
                               cat.equals("fightzone") || cat.equals("action");
        if (!isMainSection) {
            return allMovies != null ? 5 + allMovies.size() : 5;
        }

        int recentSize = (recentMovies != null) ? recentMovies.size() : 0;
        int watchlistSize = (watchlistMovies != null) ? watchlistMovies.size() : 0;
        int offset = (recentSize > 0 ? 2 : 0) + (watchlistSize > 0 ? 2 : 0);
        return allMovies != null ? 5 + offset + allMovies.size() : 5 + offset;
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ViewPager2 viewPager;
        private final Handler autoScrollHandler = new Handler(Looper.getMainLooper());

        BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            viewPager = itemView.findViewById(R.id.bannerViewPager);
            
            // 🚀 Professional Transformer: Items scale down when moving to sides (MovieBox Style)
            CompositePageTransformer transformer = new CompositePageTransformer();
            transformer.addTransformer(new MarginPageTransformer(10));
            transformer.addTransformer((page, position) -> {
                float r = 1 - Math.abs(position);
                page.setScaleY(0.9f + r * 0.1f);
            });
            viewPager.setPageTransformer(transformer);
        }

        void bind(List<Movie> movies) {
            if (movies == null || movies.isEmpty() || viewPager == null) return;
            
            BannerPagerAdapter adapter = new BannerPagerAdapter(movies);
            // 🚀 Interaction for TV: Jumping between hero slides
            adapter.setOnBannerInteractionListener(position -> {
                viewPager.setCurrentItem(position, true);
            });
            viewPager.setAdapter(adapter);
            
            // Start Auto-slide
            startAutoSlide(movies.size());
        }

        private void startAutoSlide(int size) {
            autoScrollHandler.removeCallbacksAndMessages(null);
            autoScrollHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (viewPager != null && size > 0) {
                        int nextItem = (viewPager.getCurrentItem() + 1) % size;
                        viewPager.setCurrentItem(nextItem, true);
                        autoScrollHandler.postDelayed(this, 5000);
                    }
                }
            }, 5000);
        }
    }

    static class CategoriesViewHolder extends RecyclerView.ViewHolder {
        RecyclerView rvCategories;
        CategoriesViewHolder(@NonNull View itemView) {
            super(itemView);
            rvCategories = itemView.findViewById(R.id.rvCategories);
        }
        void bind(String currentCategory, CategoryClickListener listener) {
            List<String> categories = new ArrayList<>();
            String cat = currentCategory.toLowerCase();
            
            // 🚀 SMART DYNAMIC CATEGORIES based on the active tab
            if (cat.contains("movie") || cat.contains("romance") || cat.contains("fight")) {
                categories.add("All");
                categories.add("Hollywood");
                categories.add("Nollywood");
                categories.add("K-Drama");
                categories.add("Western");
            } else {
                categories.add("All");
                categories.add("Epic");
                categories.add("Drama");
                categories.add("Comedy");
                categories.add("Traditional");
                categories.add("TV");
            }
            
            rvCategories.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            rvCategories.setAdapter(new CategoryAdapter(categories, category -> {
                if (listener != null) listener.onCategorySelected(category);
            }));
        }
    }

    static class TitleViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TitleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvSectionTitle);
        }
    }

    static class HorizontalListViewHolder extends RecyclerView.ViewHolder {
        RecyclerView rv;
        int viewType;
        HorizontalListViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;
            rv = itemView.findViewById(R.id.rvHorizontal);
        }
        void bind(List<Movie> movies) {
            rv.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            // 🚀 Use specialized continue watching layout if needed
            int itemLayout = (viewType == VIEW_RECENT) ? R.layout.item_movie_continue : R.layout.item_movie_card_horizontal;
            rv.setAdapter(new MovieAdapter(movies, itemLayout));
        }
    }
}