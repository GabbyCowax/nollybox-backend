package com.example.nollybox;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    
    private RecyclerView rvHomeMain;
    private HomeMainAdapter mainAdapter;
    private List<Movie> featuredMovies, latestMovies, allMovies, filteredMovies;
    private List<Movie> filteredRecent, filteredWatchlist; // 🚀 Stable pre-filtered lists
    private List<RecentMovie> recentMovies;
    private List<Movie> watchlistMovies;
    private Set<String> movieIds;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private ListenerRegistration movieListener;
    private String currentCategory = "Trending";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        initViews(view);
        setupAdapters();
        
        if (getContext() != null) {
            loadFromCacheOrApi();
            autoSyncLatest();
        }

        return view;
    }

    private void autoSyncLatest() {
        MovieSyncManager syncManager = new MovieSyncManager();
        // Fetch movies from the last 30 days to ensure "Last Month" and "Just Released" content
        String freshDate = syncManager.getISO8601Timestamp(30);
        
        // 🚀 STRICT DYNAMIC QUERY: Automated high-quality search for fresh Nollywood movies
        String dynamicQuery = "Latest Nollywood \"Full Movie\" Official -clip -trailer -anime -teaser";
        
        syncManager.syncWithQuery(dynamicQuery, "date", null, 1, freshDate, new MovieSyncManager.SyncCallback() {
            @Override public void onProgress(String message) {
                Log.d(TAG, "Auto-sync progress: " + message);
            }
            @Override public void onComplete(int count) {
                if (count > 0 && isAdded()) {
                    Log.d(TAG, "Auto-sync successfully added " + count + " fresh movies.");
                }
            }
            @Override public void onError(String error) {
                Log.e(TAG, "Auto-sync failed: " + error);
            }
        });
    }

    private void initViews(View view) {
        rvHomeMain = view.findViewById(R.id.rvHomeMain);
        progressBar = view.findViewById(R.id.pb_home);

        featuredMovies = new ArrayList<>();
        latestMovies = new ArrayList<>();
        allMovies = new ArrayList<>();
        filteredMovies = new ArrayList<>();
        filteredRecent = new ArrayList<>();
        filteredWatchlist = new ArrayList<>();
        recentMovies = new ArrayList<>();
        watchlistMovies = new ArrayList<>();
        movieIds = new HashSet<>();
    }

    private void setupAdapters() {
        mainAdapter = new HomeMainAdapter(featuredMovies, latestMovies, filteredMovies, filteredRecent, filteredWatchlist, category -> {
            filterByCategory(category);
        });

        // 🚀 TV OPTIMIZATION: Use 6 columns for Smart TV screens, 3 for mobile
        boolean isTv = getContext() != null && getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
        final int spanCount = isTv ? 6 : 3;

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int viewType = mainAdapter.getItemViewType(position);
                // Standard movies take 1 slot. Headers/Banners take the full width.
                if (viewType == HomeMainAdapter.VIEW_MOVIE) return 1;
                return spanCount; 
            }
        });

        rvHomeMain.setLayoutManager(layoutManager);
        rvHomeMain.setAdapter(mainAdapter);
    }

    public void refreshData(Runnable onComplete) {
        if (!isAdded()) {
            if (onComplete != null) onComplete.run();
            return;
        }
        
        loadFromCacheOrApi();
        
        MovieSyncManager syncManager = new MovieSyncManager();
        String freshDate = syncManager.getISO8601Timestamp(30);
        String dynamicQuery = "Latest Nollywood \"Full Movie\" Official -clip -trailer -anime -teaser";
        
        syncManager.syncWithQuery(dynamicQuery, "date", null, 1, freshDate, new MovieSyncManager.SyncCallback() {
            @Override public void onProgress(String message) {}
            @Override public void onComplete(int count) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (onComplete != null) onComplete.run();
                    });
                }
            }
            @Override public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (onComplete != null) onComplete.run();
                    });
                }
            }
        });
    }

    public void filterByCategory(String category) {
        if (category == null) return;
        this.currentCategory = category;
        
        try {
            if (mainAdapter != null) {
                mainAdapter.setCurrentCategory(category);
            }
            
            filteredMovies.clear();
            featuredMovies.clear();
            latestMovies.clear();
            filteredRecent.clear();
            filteredWatchlist.clear();

            if (allMovies == null || allMovies.isEmpty()) return;

            List<Movie> sourceList = new ArrayList<>();
            String catLower = category.toLowerCase().trim();
            
            // 🚀 THE GREAT SEPARATION (Pure HQ Organization)
            // Movie/Romance/FightZone -> International HQ (Hollywood/K-Drama/Action/Romance)
            // Trending/All/Nollywood -> Nollywood HQ (Strictly Pure Nollywood)
            boolean isInternationalHQ = catLower.contains("movie") || catLower.contains("romance") || 
                                       catLower.contains("fight") || catLower.contains("action") ||
                                       catLower.contains("tv") || catLower.contains("series") ||
                                       catLower.contains("anime");
            
            // 1. Filter Catalog
            for (Movie m : allMovies) {
                if (m == null) continue;
                
                boolean isNolly = m.isNollywood();
                boolean isAction = m.isActionMovie();
                boolean isRomance = m.isRomanceMovie();
                String genreUi = m.getGenreStringForUI().toLowerCase();
                boolean isTvSeries = genreUi.contains("tv") || genreUi.contains("series");
                boolean isAnime = genreUi.contains("anime");
                boolean isFightZone = genreUi.contains("fight") || genreUi.contains("wrestling") || genreUi.contains("wwe") || genreUi.contains("aew");

                if (catLower.contains("fight")) {
                    if (isFightZone) {
                        sourceList.add(m);
                    }
                } else if (catLower.contains("tv") || catLower.contains("series")) {
                    if (isTvSeries) {
                        sourceList.add(m);
                    }
                } else if (catLower.contains("anime")) {
                    if (isAnime) {
                        sourceList.add(m);
                    }
                } else if (isInternationalHQ) {
                    // International Headquarters: Show non-Nollywood Action/Romance (excluding FightZone)
                    if (!isNolly && (isAction || isRomance) && !isFightZone) {
                        sourceList.add(m);
                    }
                } else {
                    // Nollywood Headquarters: Show ONLY Nollywood
                    if (isNolly) {
                        if (catLower.equals("all") || catLower.equals("trending") || catLower.equals("nollywood")) {
                            sourceList.add(m);
                        } else if (genreUi.contains(catLower)) {
                            sourceList.add(m);
                        }
                    }
                }
            }

            // 2. Build UI Lists safely
            List<Movie> tempFeatured = new ArrayList<>();
            List<Movie> tempLatest = new ArrayList<>();
            
            int featuredLimit = Math.min(sourceList.size(), 5);
            if (featuredLimit > 0) tempFeatured.addAll(sourceList.subList(0, featuredLimit));

            int latestLimit = Math.min(sourceList.size(), 15);
            if (latestLimit > 0) tempLatest.addAll(sourceList.subList(0, latestLimit));

            // 3. Filter personal history
            List<Movie> tempRecent = new ArrayList<>();
            List<Movie> tempWatchlist = new ArrayList<>();

            for (RecentMovie r : recentMovies) {
                if (r == null) continue;
                String rGenre = r.getGenre() != null ? r.getGenre().toLowerCase() : "";
                String rTitle = r.getTitle() != null ? r.getTitle().toLowerCase() : "";
                boolean isActionHistory = rGenre.contains("action") || rTitle.contains("action") || 
                                         rTitle.contains("fight") || rTitle.contains("war") || 
                                         rTitle.contains("battle");
                                         
                if (isInternationalHQ == isActionHistory) {
                    tempRecent.add(new Movie(r.getVideoId(), r.getTitle(), r.getPosterUrl(), r.getVideoId(), r.getGenre(), null));
                }
            }
            for (Movie m : watchlistMovies) {
                if (m != null && isInternationalHQ == m.isActionMovie()) {
                    tempWatchlist.add(m);
                }
            }

            // Lock-in
            featuredMovies.addAll(tempFeatured);
            latestMovies.addAll(tempLatest);
            filteredMovies.addAll(sourceList);
            filteredRecent.addAll(tempRecent);
            filteredWatchlist.addAll(tempWatchlist);

            if (mainAdapter != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> mainAdapter.notifyDataSetChanged());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Filtering error", e);
        }
    }

    private void loadFromCacheOrApi() {
        Context context = getContext();
        if (context == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 1. Recent
                List<RecentMovie> recent = AppDatabase.getInstance(context).movieDao().getRecentMovies();
                
                // 2. Favorites
                List<FavoriteMovie> favorites = AppDatabase.getInstance(context).movieDao().getAllFavorites();
                List<Movie> favMovies = new ArrayList<>();
                for (FavoriteMovie f : favorites) {
                    favMovies.add(new Movie(f.getVideoId(), f.getTitle(), f.getPosterUrl(), f.getVideoId(), f.getGenre(), null));
                }

                // 3. Cached
                List<Movie> cached = AppDatabase.getInstance(context).movieDao().getAllCachedMovies();
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        recentMovies.clear();
                        recentMovies.addAll(recent);

                        watchlistMovies.clear();
                        watchlistMovies.addAll(favMovies);
                        
                        if (!cached.isEmpty()) {
                            updateLocalData(cached);
                            fetchFromApi(); 
                        } else {
                            fetchFromApi();
                        }
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) getActivity().runOnUiThread(this::fetchFromApi);
            }
        });
    }

    private void fetchFromApi() {
        if (!isAdded()) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        if (movieListener != null) movieListener.remove();
        
        // 🚀 REAL-TIME CLOUD SYNC: Listen for every change in the 'movies' collection
        // Absolute latest movies will always trigger a UI refresh immediately
        movieListener = db.collection("movies")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Index missing or error: " + error.getMessage());
                        fetchMoviesNoSort(); 
                        return;
                    }

                    if (value != null && isAdded()) {
                        processSnapshot(value);
                        // Force refresh sorting to put NEWLY added movies at the top of the slideshow
                        refreshUiData();
                    } else if (value != null && value.isEmpty()) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> 
                                Toast.makeText(getContext(), "Catalog is empty. Please run Auto-Sync in Admin Panel.", Toast.LENGTH_LONG).show());
                        }
                    }
                });
    }

    private void fetchMoviesNoSort() {
        if (movieListener != null) movieListener.remove();
        
        movieListener = db.collection("movies").addSnapshotListener((value, error) -> {
            if (value != null && isAdded()) {
                processSnapshot(value);
            }
        });
    }

    private void processSnapshot(QuerySnapshot value) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        
        if (value == null || value.isEmpty()) {
            Log.d(TAG, "No movies found in Firestore collection 'movies'");
            return;
        }

        List<Movie> newMovies = new ArrayList<>();
        for (QueryDocumentSnapshot document : value) {
            try {
                Movie movie = document.toObject(Movie.class);
                newMovies.add(movie);
            } catch (Exception e) {
                Log.e(TAG, "Parsing error", e);
            }
        }
        
        if (!newMovies.isEmpty()) {
            updateLocalData(newMovies);
        }
    }

    private void updateLocalData(List<Movie> newMovies) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            allMovies.clear();
            movieIds.clear();
            for (Movie m : newMovies) {
                if (m.getId() != null && movieIds.add(m.getId())) {
                    allMovies.add(m);
                }
            }
            refreshUiData();
        });

        Context context = getContext();
        if (context != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    AppDatabase.getInstance(context).movieDao().insertCachedMovies(newMovies);
                } catch (Exception e) {
                    Log.e(TAG, "Room caching failed", e);
                }
            });
        }
    }

    private void refreshUiData() {
        if (allMovies == null || allMovies.isEmpty()) return;
        
        // 🚀 CRITICAL SORT: Newest first (createdAt DESC)
        try {
            Collections.sort(allMovies, (m1, m2) -> {
                if (m1.isFeatured() != m2.isFeatured()) return m1.isFeatured() ? -1 : 1;
                String d1 = m1.getCreatedAt() != null ? m1.getCreatedAt() : "";
                String d2 = m2.getCreatedAt() != null ? m2.getCreatedAt() : "";
                int dateCompare = d2.compareTo(d1);
                return dateCompare != 0 ? dateCompare : (m2.getId().compareTo(m1.getId()));
            });
        } catch (Exception ignored) {}

        // 🚀 SMART RE-FILTER: Maintain user's active tab across cloud updates
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                filterByCategory(currentCategory);
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // 🚀 AUTOMATED RECONNECT: Re-establish the live cloud connection every time we return home
        // This ensures movies added in Admin Panel appear in the slideshow instantly
        if (db != null) {
            fetchFromApi();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (movieListener != null) {
            movieListener.remove();
            movieListener = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshPersonalData();
    }

    private void refreshPersonalData() {
        Context context = getContext();
        if (context == null) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Recent
                List<RecentMovie> recent = AppDatabase.getInstance(context).movieDao().getRecentMovies();
                
                // Favorites
                List<FavoriteMovie> favorites = AppDatabase.getInstance(context).movieDao().getAllFavorites();
                List<Movie> favMovies = new ArrayList<>();
                for (FavoriteMovie f : favorites) {
                    favMovies.add(new Movie(f.getVideoId(), f.getTitle(), f.getPosterUrl(), f.getVideoId(), f.getGenre(), null));
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        recentMovies.clear();
                        recentMovies.addAll(recent);
                        
                        watchlistMovies.clear();
                        watchlistMovies.addAll(favMovies);
                        
                        if (mainAdapter != null) mainAdapter.notifyDataSetChanged();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Refresh failed", e);
            }
        });
    }

    public void searchMovies(String query) {
        if (allMovies == null) return;
        
        if (mainAdapter != null) {
            mainAdapter.setSearchQuery(query);
        }

        if (query == null || query.trim().isEmpty()) {
            // 🚀 SMART RESET: When search is cleared, go back to the current tab's filtered state
            filterByCategory(currentCategory);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            filteredMovies.clear();
            
            // Determine active mode to keep search results relevant to the tab
            String catLower = currentCategory.toLowerCase();
            boolean isMovieTab = catLower.contains("movie") || catLower.contains("romance");
            boolean isFightTab = catLower.contains("fight") || catLower.contains("action");
            boolean isActionTab = isMovieTab || isFightTab;

            for (Movie movie : allMovies) {
                if (movie == null) continue;
                
                // 🚀 SYNCED SEARCH
                boolean isAction = movie.isActionMovie();
                boolean isRomance = movie.isRomanceMovie();
                if (isActionTab != (isAction || isRomance)) continue;

                String titleMatch = movie.getTitle() != null ? movie.getTitle().toLowerCase() : "";
                String genreMatch = movie.getGenreStringForUI() != null ? movie.getGenreStringForUI().toLowerCase() : "";
                
                if (titleMatch.contains(lowerCaseQuery) || genreMatch.contains(lowerCaseQuery)) {
                    filteredMovies.add(movie);
                }
            }
        }
        
        if (getActivity() != null && mainAdapter != null) {
            getActivity().runOnUiThread(() -> mainAdapter.notifyDataSetChanged());
        }
    }
}