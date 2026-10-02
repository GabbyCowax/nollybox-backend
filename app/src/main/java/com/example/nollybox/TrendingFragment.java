package com.example.nollybox;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class TrendingFragment extends Fragment {

    private static final String TAG = "TrendingFragment";
    private RecyclerView rvTrending;
    private MovieAdapter adapter;
    private List<Movie> trendingMovies, allTrendingMovies;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private ListenerRegistration trendingListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trending, container, false);

        db = FirebaseFirestore.getInstance();
        rvTrending = view.findViewById(R.id.rv_trending_full);
        progressBar = view.findViewById(R.id.pb_trending);
        
        trendingMovies = new ArrayList<>();
        allTrendingMovies = new ArrayList<>();
        adapter = new MovieAdapter(trendingMovies, R.layout.item_movie_card);
        rvTrending.setLayoutManager(new GridLayoutManager(getContext(), 3));
        rvTrending.setAdapter(adapter);

        if (getContext() != null) {
            loadTrending();
        }

        return view;
    }

    public void refreshData(Runnable onComplete) {
        if (!isAdded()) {
            if (onComplete != null) onComplete.run();
            return;
        }
        loadTrending();
        if (onComplete != null) {
            new Handler(Looper.getMainLooper()).postDelayed(onComplete, 1500);
        }
    }

    private void loadTrending() {
        Context context = getContext();
        if (context == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<Movie> cached = AppDatabase.getInstance(context).movieDao().getAllCachedMovies();
                if (!cached.isEmpty()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            trendingMovies.clear();
                            trendingMovies.addAll(cached);
                            adapter.notifyDataSetChanged();
                            fetchTrendingFromApi();
                        });
                    }
                } else {
                    if (getActivity() != null) getActivity().runOnUiThread(this::fetchTrendingFromApi);
                }
            } catch (Exception e) {
                if (getActivity() != null) getActivity().runOnUiThread(this::fetchTrendingFromApi);
            }
        });
    }

    private void fetchTrendingFromApi() {
        if (!isAdded()) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        if (trendingListener != null) trendingListener.remove();

        trendingListener = db.collection("movies")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((value, error) -> {
                    if (isAdded() && progressBar != null) progressBar.setVisibility(View.GONE);
                    
                    if (error != null) {
                        Log.e(TAG, "Trending listen failed", error);
                        return;
                    }

                    if (value != null) {
                        allTrendingMovies.clear();
                        trendingMovies.clear();
                        for (QueryDocumentSnapshot document : value) {
                            try {
                                Movie movie = document.toObject(Movie.class);
                                if (movie != null) {
                                    allTrendingMovies.add(movie);
                                    if (!movie.isActionMovie() && !movie.isRomanceMovie()) {
                                        trendingMovies.add(movie);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Parsing error", e);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    public void filterByCategory(String category) {
        if (category == null) return;
        trendingMovies.clear();
        String catLower = category.toLowerCase().trim();
        
        // 🚀 SYNCED FILTERING: Match HomeFragment Pure HQ rules
        boolean isInternationalHQ = catLower.contains("movie") || catLower.contains("romance") || 
                                   catLower.contains("fight") || catLower.contains("action");
        
        for (Movie m : allTrendingMovies) {
            if (m == null) continue;
            
            boolean isNolly = m.isNollywood();
            boolean isAction = m.isActionMovie();
            boolean isRomance = m.isRomanceMovie();

            if (isInternationalHQ) {
                if (!isNolly && (isAction || isRomance)) trendingMovies.add(m);
            } else {
                if (isNolly) {
                    if (catLower.equals("all") || catLower.equals("trending") || catLower.equals("nollywood")) {
                        trendingMovies.add(m);
                    } else if (m.getGenreStringForUI().toLowerCase().contains(catLower)) {
                        trendingMovies.add(m);
                    }
                }
            }
        }
        if (adapter != null && getActivity() != null) {
            getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
        }
    }

    public void searchMovies(String query) {
        if (allTrendingMovies == null) return;
        
        trendingMovies.clear();
        if (query == null || query.trim().isEmpty()) {
            filterByCategory("Trending");
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (Movie movie : allTrendingMovies) {
                if (movie == null) continue;
                
                boolean isAction = movie.isActionMovie();
                boolean isRomance = movie.isRomanceMovie();
                if (isAction || isRomance) continue;

                String title = movie.getTitle().toLowerCase();
                String genre = movie.getGenreStringForUI().toLowerCase();
                
                if (title.contains(lowerCaseQuery) || genre.contains(lowerCaseQuery)) {
                    trendingMovies.add(movie);
                }
            }
        }
        
        if (adapter != null && getActivity() != null) {
            getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (trendingListener != null) {
            trendingListener.remove();
        }
    }
}