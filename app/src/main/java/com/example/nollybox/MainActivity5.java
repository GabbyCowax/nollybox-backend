package com.example.nollybox;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity5 extends AppCompatActivity {

    private String title = "NollyBox Movie", genre = "Nollywood", posterUrl = "", videoId = "", downloadUrl = "";
    private boolean isFavorite = false;
    private RecyclerView rvRecommendations, rvCast;
    private MovieAdapter recommendationAdapter;
    private ActorAdapter castAdapter;
    private final List<Movie> recommendedMovies = new ArrayList<>();
    private final List<Actor> castList = new ArrayList<>();
    private YouTubeApiService apiService;
    private static final String YOUTUBE_API_KEY = "AIzaSyAAaUuj3K2HqlGihlWU6ruzBuvTXNwJPWw";

    private YouTubePlayerView youTubePlayerView;
    private YouTubePlayer activePlayer;
    private ImageView ivBanner;
    private TextView tvDescription, tvTitle, tvGenre;
    private View playerTouchAnchor;
    private View btnRefreshDetail, btnFullscreen, btnFavorite; // 🚀 Generic View to handle TV vs Phone types
    private MaterialButton btnPlay, btnDownload;
    private ProgressBar pbPlayer;

    private View layoutDownloadProgress;
    private CircularProgressIndicator pbDownload;
    private TextView tvDownloadStatus;
    private ImageView ivDownloadAction;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isPaused = false;
    private boolean isPlayPending = false;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        try {
            setContentView(R.layout.activity_main5);
            initRetrofit();
            bindViews();
            initAdapters();
            setupData();
            setupListeners();
            setupYouTubePlayer();
        } catch (Throwable t) {
            Log.e("Detail", "Startup failed", t);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIfDownloadedOffline();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { 
            if (youTubePlayerView != null) youTubePlayerView.release(); 
        } catch (Exception ignored) {}
        backgroundExecutor.shutdownNow();
    }

    private void bindViews() {
        try {
            ivBanner = findViewById(R.id.iv_detail_banner);
            youTubePlayerView = findViewById(R.id.youtube_player_view);
            playerTouchAnchor = findViewById(R.id.player_touch_anchor);
            tvTitle = findViewById(R.id.tv_detail_title);
            tvGenre = findViewById(R.id.tv_detail_genre);
            tvDescription = findViewById(R.id.tv_detail_description);
            btnPlay = findViewById(R.id.btn_play_now);
            btnDownload = findViewById(R.id.btn_download);
            layoutDownloadProgress = findViewById(R.id.layout_download_progress);
            pbDownload = findViewById(R.id.pb_download);
            tvDownloadStatus = findViewById(R.id.tv_download_status);
            ivDownloadAction = findViewById(R.id.iv_download_action);
            btnFavorite = findViewById(R.id.btn_favorite);
            btnRefreshDetail = findViewById(R.id.btn_refresh_detail);
            btnFullscreen = findViewById(R.id.btn_fullscreen);
            rvRecommendations = findViewById(R.id.rv_recommendations);
            rvCast = findViewById(R.id.rv_cast);
            pbPlayer = findViewById(R.id.pb_detail_player);
        } catch (Exception e) { Log.e("Detail", "Bind failed", e); }
    }

    private void setupData() {
        Intent intent = getIntent();
        if (intent != null) {
            try {
                String it = intent.getStringExtra("title"); if (it != null) title = it.replace("\"", "");
                String ig = intent.getStringExtra("genre"); if (ig != null) genre = ig.replace("\"", "");
                String ip = intent.getStringExtra("posterUrl"); if (ip != null) posterUrl = ip.replace("\"", "");
                String iv = intent.getStringExtra("videoId"); if (iv != null) videoId = extractYoutubeId(iv);
                String id = intent.getStringExtra("downloadUrl"); if (id != null) downloadUrl = id.replace("\"", "");
            } catch (Exception e) { Log.e("Detail", "Intent parse failed", e); }
        }

        if (tvTitle != null) tvTitle.setText(title);
        if (tvGenre != null) tvGenre.setText(genre);
        if (tvDescription != null) tvDescription.setText("Optimizing cinematic details...");
        
        if (ivBanner != null && posterUrl != null && !posterUrl.isEmpty()) {
            try { Glide.with(this).load(posterUrl).into(ivBanner); } catch (Exception ignored) {}
        }
        
        if (videoId != null && !videoId.isEmpty()) {
            if (btnFullscreen != null) btnFullscreen.setVisibility(View.VISIBLE);
            if (playerTouchAnchor != null) playerTouchAnchor.setVisibility(View.VISIBLE);
            checkIfFavorite();
        }
        fetchMovieMetadata(null);
    }

    private void checkIfDownloadedOffline() {
        if (videoId == null || videoId.isEmpty()) return;
        backgroundExecutor.execute(() -> {
            try {
                DownloadedMovie dm = AppDatabase.getInstance(getApplicationContext()).movieDao().getDownloadById(videoId);
                mainHandler.post(() -> {
                    if (isFinishing()) return;
                    if (dm != null) {
                        if (btnDownload != null) {
                            btnDownload.setText("SAVED TO LIBRARY");
                            btnDownload.setEnabled(false);
                            btnDownload.setAlpha(0.6f);
                        }
                    } else if (btnDownload != null) {
                        btnDownload.setText(R.string.btn_download);
                        btnDownload.setEnabled(true);
                        btnDownload.setAlpha(1.0f);
                    }
                });
            } catch (Exception e) { Log.e("DB", "Check offline failed", e); }
        });
    }

    private String extractYoutubeId(String idOrUrl) {
        if (idOrUrl == null) return "";
        String clean = idOrUrl.replace("\"", "").trim();
        if (clean.contains("v=")) {
            String[] parts = clean.split("v=");
            if (parts.length > 1) return parts[1].split("&")[0];
        } else if (clean.contains("be/")) {
            String[] parts = clean.split("be/");
            if (parts.length > 1) return parts[1].split("\\?")[0];
        }
        return clean;
    }

    private void setupListeners() {
        if (btnPlay != null) btnPlay.setOnClickListener(v -> handlePlayNow());
        if (btnFullscreen != null) btnFullscreen.setOnClickListener(v -> triggerFullscreen());
        if (playerTouchAnchor != null) playerTouchAnchor.setOnClickListener(v -> triggerFullscreen());
        if (btnDownload != null) btnDownload.setOnClickListener(v -> showDownloadOptions());
        if (btnFavorite != null) btnFavorite.setOnClickListener(v -> toggleFavorite());
        if (btnRefreshDetail != null) btnRefreshDetail.setOnClickListener(this::handleRefresh);
        
        MaterialToolbar toolbar = findViewById(R.id.toolbar_detail);
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());
        
        if (layoutDownloadProgress != null) {
            layoutDownloadProgress.setOnClickListener(v -> {
                isPaused = !isPaused;
                if (ivDownloadAction != null) ivDownloadAction.setImageResource(isPaused ? R.drawable.ic_resume : R.drawable.ic_pause);
                Toast.makeText(this, isPaused ? "Paused" : "Resuming", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupYouTubePlayer() {
        if (youTubePlayerView == null) return;
        getLifecycle().addObserver(youTubePlayerView);
        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                activePlayer = youTubePlayer;
                if (isPlayPending && !isFinishing()) {
                    isPlayPending = false;
                    startEmbeddedPlayer();
                }
            }
        });
    }

    private void handlePlayNow() {
        if (pbPlayer != null) pbPlayer.setVisibility(View.VISIBLE);
        
        backgroundExecutor.execute(() -> {
            try {
                final DownloadedMovie dm = AppDatabase.getInstance(getApplicationContext()).movieDao().getDownloadById(videoId);
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    if (dm != null && dm.getFilePath() != null && !"BOOKMARK".equals(dm.getFilePath()) && new File(dm.getFilePath()).exists()) {
                        if (pbPlayer != null) pbPlayer.setVisibility(View.GONE);
                        Intent it = new Intent(MainActivity5.this, PlayerActivity.class);
                        it.putExtra("videoId", videoId);
                        it.putExtra("localPath", dm.getFilePath());
                        startActivity(it);
                    } else {
                        if (videoId == null || videoId.isEmpty()) {
                            fetchMovieMetadata(this::startEmbeddedPlayer);
                        } else {
                            startEmbeddedPlayer();
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("Detail", "Check in Play Now failed", e);
                runOnUiThread(() -> {
                    if (videoId == null || videoId.isEmpty()) fetchMovieMetadata(this::startEmbeddedPlayer);
                    else startEmbeddedPlayer();
                });
            }
        });
        saveToRecent();
    }

    private void startEmbeddedPlayer() {
        if (videoId == null || videoId.isEmpty() || isFinishing()) {
            if (pbPlayer != null) pbPlayer.setVisibility(View.GONE);
            return;
        }
        runOnUiThread(() -> {
            if (ivBanner != null) ivBanner.setVisibility(View.GONE);
            if (pbPlayer != null) pbPlayer.setVisibility(View.GONE);
            if (youTubePlayerView != null) {
                youTubePlayerView.setVisibility(View.VISIBLE);
                if (activePlayer != null) {
                    activePlayer.loadVideo(videoId, 0);
                    if (playerTouchAnchor != null) playerTouchAnchor.setVisibility(View.GONE);
                } else { isPlayPending = true; if (pbPlayer != null) pbPlayer.setVisibility(View.VISIBLE); }
            }
            if (btnFullscreen != null) btnFullscreen.setVisibility(View.VISIBLE);
        });
    }

    private void handleRefresh(View v) {
        RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(800); rotate.setRepeatCount(Animation.INFINITE); rotate.setInterpolator(new LinearInterpolator());
        v.startAnimation(rotate); v.setEnabled(false);
        fetchMovieMetadata(() -> {
            if (isFinishing()) return;
            v.clearAnimation(); v.setEnabled(true);
            Toast.makeText(this, R.string.content_refreshed, Toast.LENGTH_SHORT).show();
        });
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    private void initRetrofit() {
        try {
            apiService = new Retrofit.Builder().baseUrl("https://www.googleapis.com/").addConverterFactory(GsonConverterFactory.create()).build().create(YouTubeApiService.class);
        } catch (Exception e) { Log.e("Retrofit", "Init failed", e); }
    }

    private void initAdapters() {
        try {
            castAdapter = new ActorAdapter(castList);
            if (rvCast != null) {
                rvCast.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                rvCast.setAdapter(castAdapter);
                rvCast.setNestedScrollingEnabled(false);
            }
            if (rvRecommendations != null) {
                int columns = getResources().getInteger(R.integer.movie_grid_columns);
                if (columns > 3) {
                    // 🚀 TV Mode: Grid Layout matching screenshot
                    recommendationAdapter = new MovieAdapter(recommendedMovies, R.layout.item_movie_card);
                    rvRecommendations.setLayoutManager(new GridLayoutManager(this, columns));
                } else {
                    // 🚀 Phone Mode: Horizontal List with small professional cards
                    recommendationAdapter = new MovieAdapter(recommendedMovies, R.layout.item_movie_card_horizontal);
                    rvRecommendations.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                }
                rvRecommendations.setAdapter(recommendationAdapter);
                rvRecommendations.setNestedScrollingEnabled(false);
            }
        } catch (Exception e) { Log.e("Detail", "Adapters failed", e); }
    }

    private void fetchMovieMetadata(Runnable onComplete) {
        if (apiService == null) { if (onComplete != null) onComplete.run(); return; }
        if (videoId == null || videoId.isEmpty()) {
            if (title != null && !title.isEmpty()) {
                apiService.searchVideos("snippet", null, "relevance", 1, YOUTUBE_API_KEY, "video", "true", "long", null, title + " Nollywood Full Movie", null)
                        .enqueue(new Callback<YouTubeResponse>() {
                            @Override public void onResponse(@NonNull Call<YouTubeResponse> call, @NonNull Response<YouTubeResponse> response) {
                                if (response.isSuccessful() && response.body() != null && response.body().items != null && !response.body().items.isEmpty()) {
                                    YouTubeResponse.Item item = response.body().items.get(0);
                                    if (item != null && item.id != null) {
                                        videoId = extractYoutubeId(item.id.videoId); fetchMovieMetadata(onComplete);
                                        return;
                                    }
                                }
                                fetchRecommendations(title); if (onComplete != null) onComplete.run();
                            }
                            @Override public void onFailure(@NonNull Call<YouTubeResponse> call, @NonNull Throwable t) { fetchRecommendations(title); if (onComplete != null) onComplete.run(); }
                        });
            } else if (onComplete != null) onComplete.run();
            return;
        }

        apiService.getVideoDetails("snippet", videoId, YOUTUBE_API_KEY).enqueue(new Callback<YouTubeVideoResponse>() {
            @Override public void onResponse(@NonNull Call<YouTubeVideoResponse> call, @NonNull Response<YouTubeVideoResponse> response) {
                if (isFinishing()) return;
                if (onComplete != null) onComplete.run();
                if (response.isSuccessful() && response.body() != null && response.body().items != null && !response.body().items.isEmpty()) {
                    YouTubeResponse.Snippet snippet = response.body().items.get(0).snippet;
                    if (snippet != null) {
                        if (tvDescription != null) {
                            String desc = (snippet.description != null && !snippet.description.trim().isEmpty()) ? snippet.description : "No synopsis available.";
                            tvDescription.setText(desc);
                        }
                        if (snippet.tags != null) {
                            castList.clear();
                            for (String tag : snippet.tags) {
                                if (tag != null && tag.split(" ").length >= 2 && !tag.toLowerCase().contains("movie")) {
                                    castList.add(new Actor(tag, "https://ui-avatars.com/api/?name=" + tag.replace(" ", "+") + "&background=FFD700&color=000&size=128"));
                                }
                            }
                            if (castAdapter != null) castAdapter.notifyDataSetChanged();
                        }
                        fetchRecommendations((snippet.tags != null && !snippet.tags.isEmpty()) ? snippet.tags.get(0) : title);
                    }
                } else { fetchRecommendations(title); }
            }
            @Override public void onFailure(@NonNull Call<YouTubeVideoResponse> call, @NonNull Throwable t) { fetchRecommendations(title); if (onComplete != null) onComplete.run(); }
        });
    }

    private void fetchRecommendations(String query) {
        if (apiService == null || isFinishing()) return;
        String searchTitle = query + " Nollywood \"Full Movie\" -clip -trailer";
        apiService.searchVideos("snippet", null, "relevance", 15, YOUTUBE_API_KEY, "video", "true", "long", null, searchTitle, null)
                .enqueue(new Callback<YouTubeResponse>() {
                    @Override public void onResponse(@NonNull Call<YouTubeResponse> call, @NonNull Response<YouTubeResponse> response) {
                        if (isFinishing()) return;
                        if (response.isSuccessful() && response.body() != null && response.body().items != null) {
                            recommendedMovies.clear();
                            for (YouTubeResponse.Item item : response.body().items) {
                                if (item != null && item.id != null && item.id.videoId != null) {
                                    String vId = extractYoutubeId(item.id.videoId);
                                    if (vId.equalsIgnoreCase(videoId)) continue;
                                    String t = (item.snippet != null) ? item.snippet.title : "NollyBox Movie";
                                    String pUrl = (item.snippet != null && item.snippet.thumbnails != null && item.snippet.thumbnails.high != null) ? item.snippet.thumbnails.high.url : "";
                                    recommendedMovies.add(new Movie(vId, t, pUrl, vId, "Recommended", (item.snippet != null) ? item.snippet.publishedAt : ""));
                                }
                            }
                            runOnUiThread(() -> { if (recommendationAdapter != null) recommendationAdapter.notifyDataSetChanged(); });
                        }
                    }
                    @Override public void onFailure(@NonNull Call<YouTubeResponse> call, @NonNull Throwable t) { Log.e("Detail", "Reco failed", t); }
                });
    }

    private void checkIfFavorite() {
        if (videoId == null || videoId.isEmpty()) return;
        backgroundExecutor.execute(() -> {
            try {
                isFavorite = AppDatabase.getInstance(getApplicationContext()).movieDao().isFavorite(videoId);
                runOnUiThread(() -> { 
                    if (!isFinishing() && btnFavorite != null) {
                        int resId = isFavorite ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off;
                        if (btnFavorite instanceof ImageButton) {
                            ((ImageButton) btnFavorite).setImageResource(resId);
                        } else if (btnFavorite instanceof MaterialButton) {
                            ((MaterialButton) btnFavorite).setIconResource(resId);
                        }
                    }
                });
            } catch (Exception e) { Log.e("DB", "Check favorite failed", e); }
        });
    }

    private void toggleFavorite() {
        if (videoId == null || videoId.isEmpty()) return;
        backgroundExecutor.execute(() -> {
            try {
                if (isFavorite) { AppDatabase.getInstance(getApplicationContext()).movieDao().deleteFavorite(videoId); isFavorite = false; }
                else { AppDatabase.getInstance(getApplicationContext()).movieDao().insertFavorite(new FavoriteMovie(videoId, title, posterUrl, genre)); isFavorite = true; }
                runOnUiThread(() -> { 
                    if (!isFinishing() && btnFavorite != null) {
                        int resId = isFavorite ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off;
                        if (btnFavorite instanceof ImageButton) {
                            ((ImageButton) btnFavorite).setImageResource(resId);
                        } else if (btnFavorite instanceof MaterialButton) {
                            ((MaterialButton) btnFavorite).setIconResource(resId);
                        }
                        Toast.makeText(this, isFavorite ? R.string.added_to_watchlist : R.string.removed_from_watchlist, Toast.LENGTH_SHORT).show(); 
                    } 
                });
            } catch (Exception e) { Log.e("DB", "Toggle favorite failed", e); }
        });
    }

    private void showDownloadOptions() {
        if (videoId == null || videoId.isEmpty()) return;
        Toast.makeText(this, "Saving to Library...", Toast.LENGTH_SHORT).show();
        backgroundExecutor.execute(() -> {
            try {
                AppDatabase.getInstance(getApplicationContext()).movieDao().insertDownload(
                        new DownloadedMovie(videoId, title, posterUrl, "BOOKMARK")
                );
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    if (btnDownload != null) {
                        btnDownload.setText("SAVED TO LIBRARY");
                        btnDownload.setEnabled(false);
                        btnDownload.setAlpha(0.6f);
                    }
                    Toast.makeText(this, "Successfully Saved to Library!", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e("Download", "Bookmark saving failed", e);
            }
        });
    }

    private void triggerFullscreen() {
        if (videoId == null || videoId.isEmpty() || isFinishing()) return;
        Intent it = new Intent(MainActivity5.this, PlayerActivity.class);
        it.putExtra("videoId", videoId);
        startActivity(it);
    }

    private void saveToRecent() {
        if (videoId == null || videoId.isEmpty()) return;
        backgroundExecutor.execute(() -> { try { AppDatabase.getInstance(getApplicationContext()).movieDao().insertRecentMovie(new RecentMovie(videoId, title, posterUrl, genre, System.currentTimeMillis())); } catch (Exception e) { Log.e("DB", "Recent failed", e); } });
    }
}
