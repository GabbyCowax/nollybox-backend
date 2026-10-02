package com.example.nollybox;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity6 extends AppCompatActivity {

    private RecyclerView rvLibrary;
    private MovieAdapter adapter;
    private List<Movie> displayList;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main6);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_library);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
            toolbar.setTitle(R.string.tab_watchlist);
        }

        rvLibrary = findViewById(R.id.rv_library);
        tabLayout = findViewById(R.id.tab_layout);
        if (tabLayout != null) tabLayout.setVisibility(View.GONE); // 🚀 Remove tabs as requested

        displayList = new ArrayList<>();
        adapter = new MovieAdapter(displayList, R.layout.item_movie_card, (view, movie, position) -> {
            showPopupMenu(view, movie, position);
        });
        rvLibrary.setLayoutManager(new GridLayoutManager(this, 3));
        rvLibrary.setAdapter(adapter);

        loadWatchlist();
    }

    private void loadWatchlist() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<FavoriteMovie> favorites = AppDatabase.getInstance(this).movieDao().getAllFavorites();
                List<Movie> mappedList = new ArrayList<>();
                for (FavoriteMovie f : favorites) {
                    mappedList.add(new Movie(f.getVideoId(), f.getTitle(), f.getPosterUrl(), f.getVideoId(), f.getGenre(), null));
                }
                
                runOnUiThread(() -> {
                    displayList.clear();
                    displayList.addAll(mappedList);
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                Log.e("Library", "Load failed", e);
            }
        });
    }

    private void showPopupMenu(View view, Movie movie, int position) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add("Delete from List");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Delete from List")) {
                deleteMovie(movie, position);
            }
            return true;
        });
        popup.show();
    }

    private void deleteMovie(Movie movie, int position) {
        if (movie == null) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Only deleting from watchlist now
                AppDatabase.getInstance(this).movieDao().deleteFavorite(movie.getId());
                
                runOnUiThread(() -> {
                    if (position < displayList.size() && displayList.get(position).getId().equals(movie.getId())) {
                        displayList.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, displayList.size());
                    } else {
                        loadWatchlist(); // Fallback if list shifted
                    }
                    Toast.makeText(this, "Removed from Watchlist", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e("Library", "Delete failed", e);
            }
        });
    }
}
