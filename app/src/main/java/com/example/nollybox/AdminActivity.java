package com.example.nollybox;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AdminActivity extends AppCompatActivity {

    private EditText etTitle;
    private MaterialButton btnSearch, btnUpload;
    private ProgressBar progressBar;
    private MaterialCardView cardPreview;
    private ImageView ivPreview;
    private TextView tvPreviewTitle;

    private YouTubeApiService apiService;
    private static final String[] API_KEYS = {
            "AIzaSyAFy6L9oNrurBt4TEROcMdHurIotvrAg2s", // Key 1
            "AIzaSyAAaUuj3K2HqlGihlWU6ruzBuvTXNwJPWw"  // Key 2
    };
    private static int keyIndex = 1; // Start with Key 2
    private Movie foundMovie;
    private boolean isDeepSearchMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        etTitle = findViewById(R.id.et_admin_movie_title);
        btnSearch = findViewById(R.id.btn_admin_search);
        btnUpload = findViewById(R.id.btn_admin_upload);
        progressBar = findViewById(R.id.pb_admin);
        cardPreview = findViewById(R.id.card_admin_preview);
        ivPreview = findViewById(R.id.iv_admin_preview);
        tvPreviewTitle = findViewById(R.id.tv_admin_preview_title);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(YouTubeApiService.class);

        btnSearch.setOnClickListener(v -> searchMovieOnYouTube());
        btnUpload.setOnClickListener(v -> uploadToFirestore());
        
        SwitchMaterial switchDeepSearch = findViewById(R.id.switch_deep_search);
        if (switchDeepSearch != null) {
            switchDeepSearch.setOnCheckedChangeListener((buttonView, isChecked) -> isDeepSearchMode = isChecked);
        }

        findViewById(R.id.btn_admin_auto_sync).setOnClickListener(v -> startAutoSync());
        findViewById(R.id.btn_admin_tmdb_sync).setOnClickListener(v -> startTmdbSync());
        findViewById(R.id.btn_admin_tmdb_tv_sync).setOnClickListener(v -> startTvSync());
        findViewById(R.id.btn_admin_tmdb_anime_sync).setOnClickListener(v -> startAnimeSync());
        findViewById(R.id.btn_admin_tmdb_fight_sync).setOnClickListener(v -> startWrestlingSync());
        findViewById(R.id.btn_admin_clear_all).setOnClickListener(v -> wipeCloudCatalog());
    }

    private void startTmdbSync() {
        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.btn_admin_tmdb_sync).setEnabled(false);

        MovieSyncManager syncManager = new MovieSyncManager();
        syncManager.syncActionMoviesFromTmdb(3, new MovieSyncManager.SyncCallback() {
            @Override
            public void onProgress(String message) {
                runOnUiThread(() -> Toast.makeText(AdminActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onComplete(int count) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.btn_admin_tmdb_sync).setEnabled(true);
                    Toast.makeText(AdminActivity.this, "TMDB Action Sync Complete!", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.btn_admin_tmdb_sync).setEnabled(true);
                    Toast.makeText(AdminActivity.this, "TMDB Action Sync Failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void startTvSync() {
        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.btn_admin_tmdb_tv_sync).setEnabled(false);

        MovieSyncManager syncManager = new MovieSyncManager();
        syncManager.syncTvShowsFromTmdb(3, new MovieSyncManager.SyncCallback() {
            @Override
            public void onProgress(String message) {
                runOnUiThread(() -> Toast.makeText(AdminActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onComplete(int count) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.btn_admin_tmdb_tv_sync).setEnabled(true);
                    Toast.makeText(AdminActivity.this, "TMDB TV Series Sync Complete!", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.btn_admin_tmdb_tv_sync).setEnabled(true);
                    Toast.makeText(AdminActivity.this, "TMDB TV Sync Failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void startAnimeSync() {
        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.btn_admin_tmdb_anime_sync).setEnabled(false);

        MovieSyncManager syncManager = new MovieSyncManager();
        syncManager.syncAnimeFromTmdb(3, new MovieSyncManager.SyncCallback() {
            @Override
            public void onProgress(String message) {
                runOnUiThread(() -> Toast.makeText(AdminActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onComplete(int count) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.btn_admin_tmdb_anime_sync).setEnabled(true);
                    Toast.makeText(AdminActivity.this, "TMDB Anime Sync Complete!", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.btn_admin_tmdb_anime_sync).setEnabled(true);
                    Toast.makeText(AdminActivity.this, "TMDB Anime Sync Failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void startWrestlingSync() {
        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.btn_admin_tmdb_fight_sync).setEnabled(false);

        MovieSyncManager syncManager = new MovieSyncManager();
        syncManager.syncFightZoneFromYouTube(new MovieSyncManager.SyncCallback() {
            @Override
            public void onProgress(String message) {
                runOnUiThread(() -> Toast.makeText(AdminActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onComplete(int count) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.btn_admin_tmdb_fight_sync).setEnabled(true);
                    Toast.makeText(AdminActivity.this, "FightZone 2026 Sync Complete! (" + count + " items)", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.btn_admin_tmdb_fight_sync).setEnabled(true);
                    Toast.makeText(AdminActivity.this, "FightZone Sync Failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void wipeCloudCatalog() {
        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.btn_admin_clear_all).setEnabled(false);

        FirebaseFirestore.getInstance().collection("movies")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = FirebaseFirestore.getInstance().batch();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit().addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        findViewById(R.id.btn_admin_clear_all).setEnabled(true);
                        if (task.isSuccessful()) {
                            Toast.makeText(AdminActivity.this, "Cloud Catalog Wiped Successfully!", Toast.LENGTH_LONG).show();
                        } else {
                            String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(AdminActivity.this, "Wipe failed: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.btn_admin_clear_all).setEnabled(true);
                    Toast.makeText(AdminActivity.this, "Error fetching catalog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void startAutoSync() {
        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.btn_admin_auto_sync).setEnabled(false);
        
        MovieSyncManager syncManager = new MovieSyncManager();
        syncManager.syncNollywoodMovies(null, 5, new MovieSyncManager.SyncCallback() {
            @Override
            public void onProgress(String message) {
                runOnUiThread(() -> Toast.makeText(AdminActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onComplete(int count) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.btn_admin_auto_sync).setEnabled(true);
                    Toast.makeText(AdminActivity.this, "Sync Complete! Refreshing catalog...", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.btn_admin_auto_sync).setEnabled(true);
                    Toast.makeText(AdminActivity.this, "Sync Failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void searchMovieOnYouTube() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        btnSearch.setEnabled(false);

        String query;
        String duration;
        if (isDeepSearchMode) {
            query = title + " Nollywood \"Full Movie\" Official -clip -AI -trailer -teaser -short";
            duration = "long";
        } else {
            query = title + " Nollywood";
            duration = "any";
        }

        apiService.searchVideos("snippet", null, "date", 5, API_KEYS[keyIndex], "video", "true", duration, null, query, null)
                .enqueue(new Callback<YouTubeResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<YouTubeResponse> call, @NonNull Response<YouTubeResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        btnSearch.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null && !response.body().items.isEmpty()) {
                            YouTubeResponse.Item item = response.body().items.get(0);
                            String vId = item.id.videoId;
                            String t = item.snippet.title.replace("\"", "").trim();
                            
                            String pUrl = "";
                            if (item.snippet.thumbnails != null) {
                                YouTubeResponse.Thumbnails thumbs = item.snippet.thumbnails;
                                if (thumbs.maxres != null && thumbs.maxres.url != null) {
                                    pUrl = thumbs.maxres.url;
                                } else if (thumbs.high != null && thumbs.high.url != null) {
                                    pUrl = thumbs.high.url;
                                } else if (thumbs.medium != null && thumbs.medium.url != null) {
                                    pUrl = thumbs.medium.url;
                                } else if (thumbs.defaultThumbnail != null && thumbs.defaultThumbnail.url != null) {
                                    pUrl = thumbs.defaultThumbnail.url;
                                }
                            }

                            if (pUrl == null || pUrl.isEmpty()) {
                                pUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=1000&auto=format&fit=crop";
                            }
                            
                            // 🚀 SMART TIMESTAMP: Use current time to put manual imports at the top
                            SimpleDateFormat tsFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                            tsFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                            String currentTime = tsFormat.format(new Date());

                            foundMovie = new Movie(vId, t, pUrl, "https://www.youtube.com/watch?v=" + vId, "Nollywood", currentTime);
                            foundMovie.setFeatured(true); // 🚀 PERMANENT SLIDESHOW: Mark manual imports as featured

                            cardPreview.setVisibility(View.VISIBLE);
                            tvPreviewTitle.setVisibility(View.VISIBLE);
                            btnUpload.setVisibility(View.VISIBLE);
                            tvPreviewTitle.setText(t);
                            Glide.with(AdminActivity.this).load(pUrl).into(ivPreview);
                        } else {
                            Toast.makeText(AdminActivity.this, "No movie found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<YouTubeResponse> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnSearch.setEnabled(true);
                        Toast.makeText(AdminActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadToFirestore() {
        if (foundMovie == null) return;

        progressBar.setVisibility(View.VISIBLE);
        btnUpload.setEnabled(false);

        FirebaseFirestore.getInstance().collection("movies")
                .document(foundMovie.getId())
                .set(foundMovie)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                    Toast.makeText(AdminActivity.this, "Successfully added to Cloud!", Toast.LENGTH_LONG).show();
                    etTitle.setText("");
                    cardPreview.setVisibility(View.GONE);
                    tvPreviewTitle.setVisibility(View.GONE);
                    btnUpload.setVisibility(View.GONE);
                    foundMovie = null;
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                    Toast.makeText(AdminActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
