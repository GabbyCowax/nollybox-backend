package com.example.nollybox;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MovieSyncManager {
    private static final String TAG = "MovieSyncManager";
    
    // 🚀 MULTI-KEY SYSTEM: Add your new keys here to multiply your daily limit!
    // Each key adds 10,000 units (approx. 1,000 more movies per day).
    private static final String[] API_KEYS = {
            "AIzaSyAAaUuj3K2HqlGihlWU6ruzBuvTXNwJPWw", // Key 2 (Newly Added - Full Quota)
            "AIzaSyAFy6L9oNrurBt4TEROcMdHurIotvrAg2s"  // Key 1 (Backup)
    };
    private static int currentKeyIndex = 0; 
    
    public interface SyncCallback {
        void onProgress(String message);
        void onComplete(int count);
        void onError(String error);
    }

    private YouTubeApiService apiService;
    private TmdbApiService tmdbService;
    private FirebaseFirestore db;
    
    // 🚀 NEW VERIFIED KEY: Updated to ensure authorized connection
    private static final String TMDB_API_KEY = "f5edc635a0ba6e3169e1bc2781ca5905";

    public MovieSyncManager() {
        Retrofit youtubeRetrofit = new Retrofit.Builder()
                .baseUrl("https://www.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = youtubeRetrofit.create(YouTubeApiService.class);

        Retrofit tmdbRetrofit = new Retrofit.Builder()
                .baseUrl("https://api.themoviedb.org/3/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        tmdbService = tmdbRetrofit.create(TmdbApiService.class);

        db = FirebaseFirestore.getInstance();
    }

    public void syncNollywoodMovies(String nextToken, int pagesRemaining, SyncCallback callback) {
        // 🚀 PRECISION BASELINE: Strictly importing movies released FROM March 1st, 2026
        // This fulfills the requirement to skip all older content (2024/2025)
        String publishedAfter = "2026-03-01T00:00:00Z";
        
        // 🚀 CINEMATIC KEYWORDS: Targeted for high-quality Nollywood film releases
        // Broadening the query to ensure the empty Firestore is populated
        String[] keywords = {
                "Latest Nollywood Movies Full Movie Official",
                "Nigerian Movies Full Movie Cinema",
                "Trending Nollywood Films Full Movie",
                "Nollywood Blockbuster Full Movie Nigerian"
        };
        
        deepSync(keywords, 0, nextToken, pagesRemaining, publishedAfter, callback);
    }

    private void deepSync(String[] keywords, int index, String nextToken, int pagesRemaining, String publishedAfter, SyncCallback callback) {
        if (index >= keywords.length) {
            callback.onComplete(0); // All keywords processed
            return;
        }

        String currentQuery = keywords[index];
        callback.onProgress("Syncing Fresh: " + currentQuery);

        syncWithQuery(currentQuery, "date", nextToken, pagesRemaining, publishedAfter, new SyncCallback() {
            @Override
            public void onProgress(String message) {
                callback.onProgress(message);
            }

            @Override
            public void onComplete(int count) {
                deepSync(keywords, index + 1, null, pagesRemaining, publishedAfter, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void syncWithQuery(String query, String order, String nextToken, int pagesRemaining, String publishedAfter, SyncCallback callback) {
        if (pagesRemaining <= 0) {
            callback.onComplete(0);
            return;
        }

        // 🚀 SMART SKIP: Automatically move to the next key if current one is full
        if (currentKeyIndex >= API_KEYS.length) {
            callback.onError("All API keys have reached their daily limit.");
            return;
        }

        String apiKey = API_KEYS[currentKeyIndex];
        callback.onProgress("Connecting (Using Key " + (currentKeyIndex + 1) + ")...");

        // 🚀 PROFESSIONAL IMPORT PARAMETERS:
        // order=date (Ensures freshest content is processed first)
        // type=video, videoDuration=long (Only full-length movies, no clips)
        // Removing embeddable=true to ensure we find all Nollywood movies
        apiService.searchVideos("snippet", null, "date", 50, apiKey, "video", null, "long", publishedAfter, query, nextToken)
                .enqueue(new Callback<YouTubeResponse>() {
                    @Override
                    public void onResponse(Call<YouTubeResponse> call, Response<YouTubeResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            int totalFound = response.body().items != null ? response.body().items.size() : 0;
                            Log.d(TAG, "Search successful: " + totalFound + " items found for " + query);
                            processResults(query, "date", response.body(), pagesRemaining, publishedAfter, callback);
                        } else if (response.code() == 403 || response.code() == 429) {
                            // 🚀 INSTANT ROTATION: If this key is full, permanently switch to the next one!
                            Log.w(TAG, "Key " + (currentKeyIndex + 1) + " limit reached. Rotating...");
                            currentKeyIndex++;
                            syncWithQuery(query, order, nextToken, pagesRemaining, publishedAfter, callback);
                        } else {
                            callback.onError("YouTube API Error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<YouTubeResponse> call, Throwable t) {
                        callback.onError("Network Error: " + t.getMessage());
                    }
                });
    }

    private void processResults(String query, String order, YouTubeResponse body, int pagesRemaining, String publishedAfter, SyncCallback callback) {
        if (body.items == null || body.items.isEmpty()) {
            callback.onComplete(0);
            return;
        }

        WriteBatch batch = db.batch();
        int count = 0;

        // Shared Date Format for timestamps
        SimpleDateFormat tsFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        tsFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String currentTime = tsFormat.format(new Date());

        for (YouTubeResponse.Item item : body.items) {
            if (item.id == null || item.id.videoId == null) continue;
            
            String vId = item.id.videoId;
            String titleClean = (item.snippet != null && item.snippet.title != null) 
                    ? item.snippet.title.replace("\"", "").trim() : "NollyBox Movie";
            
            String posterUrl = "";
            if (item.snippet != null && item.snippet.thumbnails != null) {
                YouTubeResponse.Thumbnails thumbs = item.snippet.thumbnails;
                if (thumbs.maxres != null && thumbs.maxres.url != null) {
                    posterUrl = thumbs.maxres.url;
                } else if (thumbs.high != null && thumbs.high.url != null) {
                    posterUrl = thumbs.high.url;
                } else if (thumbs.medium != null && thumbs.medium.url != null) {
                    posterUrl = thumbs.medium.url;
                } else if (thumbs.defaultThumbnail != null && thumbs.defaultThumbnail.url != null) {
                    posterUrl = thumbs.defaultThumbnail.url;
                }
            }
            
            // 🚀 DATA INTEGRITY: Use a high-quality cinematic placeholder if no image is available
            if (posterUrl == null || posterUrl.isEmpty()) {
                posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=1000&auto=format&fit=crop";
            }
            
            // 🚀 REAL DATA SYNC: Use actual YouTube publication date as createdAt
            // This ensures manual imports (using current time) stay at the top of the slideshow
            String publishedAt = (item.snippet != null && item.snippet.publishedAt != null) ? item.snippet.publishedAt : "";
            String videoUrl = "https://www.youtube.com/watch?v=" + vId;

            // 🚀 AGGRESSIVE ACTION DETECTION: Tag as Action if strong indicators exist
            String lowerTitle = titleClean.toLowerCase();
            String detectedGenre = "Nollywood";
            
            if (lowerTitle.contains("full action") || lowerTitle.contains("action movie") || 
                lowerTitle.contains("commando") || lowerTitle.contains("mercenary") || 
                lowerTitle.contains("ninja") || lowerTitle.contains("assassin") ||
                lowerTitle.contains("combat") || lowerTitle.contains("gunfight")) {
                detectedGenre = "Action";
            }

            Movie movie = new Movie(vId, titleClean, posterUrl, videoUrl, detectedGenre, publishedAt);
            movie.setFeatured(false); // 🚀 Standard sync movies are not featured by default
            batch.set(db.collection("movies").document(vId), movie);
            count++;
        }

        int finalCount = count;
        if (finalCount == 0) {
            Log.w(TAG, "No valid cinematic items found in this page.");
            if (pagesRemaining > 1 && body.nextPageToken != null) {
                syncWithQuery(query, order, body.nextPageToken, pagesRemaining - 1, publishedAfter, callback);
            } else {
                callback.onComplete(0);
            }
            return;
        }

        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.i(TAG, "Firestore update successful. Added " + finalCount + " items.");
                callback.onProgress("Added " + finalCount + " items to catalog.");
                if (pagesRemaining > 1 && body.nextPageToken != null) {
                    syncWithQuery(query, order, body.nextPageToken, pagesRemaining - 1, publishedAfter, callback);
                } else {
                    callback.onComplete(finalCount);
                }
            } else {
                Log.e(TAG, "Firestore write failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                callback.onError("Firestore Write Error: Check database rules.");
            }
        });
    }

    public void syncActionMoviesFromTmdb(int pages, SyncCallback callback) {
        callback.onProgress("Connecting to TMDB Movie Catalog...");
        fetchTmdbPage(1, pages, callback);
    }

    private void fetchTmdbPage(int page, int totalPages, SyncCallback callback) {
        if (page > totalPages) {
            callback.onComplete(0);
            return;
        }

        // Genre 28 = Action
        tmdbService.getMoviesByGenre(TMDB_API_KEY, "28", "popularity.desc", page, "en-US")
                .enqueue(new Callback<TmdbResponse>() {
                    @Override
                    public void onResponse(Call<TmdbResponse> call, Response<TmdbResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().results != null) {
                            processTmdbResults(response.body().results, page, totalPages, callback);
                        } else {
                            callback.onError("TMDB Error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<TmdbResponse> call, Throwable t) {
                        callback.onError("TMDB Network Error: " + t.getMessage());
                    }
                });
    }

    private void processTmdbResults(List<TmdbResponse.MovieResult> results, int page, int totalPages, SyncCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            WriteBatch batch = db.batch();
            int count = 0;

            for (TmdbResponse.MovieResult res : results) {
                if (res.title == null) continue;

                String title = res.title.replace("\"", "").trim();
                String posterUrl = "https://image.tmdb.org/t/p/original" + res.posterPath;

                // 🚀 TMDB YOUTUBE TRAILER INTEGRATION: Fetch embedded YouTube trailer key for this action movie
                String youtubeVideoId = "";
                try {
                    Response<TmdbVideoResponse> videoResponse = tmdbService.getMovieVideos(res.id, TMDB_API_KEY).execute();
                    if (videoResponse.isSuccessful() && videoResponse.body() != null && videoResponse.body().results != null) {
                        for (TmdbVideoResponse.VideoResult vr : videoResponse.body().results) {
                            if ("YouTube".equalsIgnoreCase(vr.site) && ("Trailer".equalsIgnoreCase(vr.type) || "Teaser".equalsIgnoreCase(vr.type))) {
                                youtubeVideoId = vr.key;
                                break;
                            }
                        }
                        if (youtubeVideoId.isEmpty() && !videoResponse.body().results.isEmpty()) {
                            for (TmdbVideoResponse.VideoResult vr : videoResponse.body().results) {
                                if ("YouTube".equalsIgnoreCase(vr.site)) {
                                    youtubeVideoId = vr.key;
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching TMDB video for " + title, e);
                }

                // 🚀 STRICT TRAILER FILTER: Skip movies that do not have a YouTube trailer video
                if (youtubeVideoId.isEmpty()) {
                    continue;
                }

                String videoUrl = "https://www.youtube.com/watch?v=" + youtubeVideoId;
                String vId = youtubeVideoId;

                Movie movie = new Movie(vId, title, posterUrl, videoUrl, res.releaseDate);
                // 🚀 INTERNATIONAL TAGGING: Ensure these never show up in Nollywood/Home HQ
                List<String> genres = new ArrayList<>();
                genres.add("Movie");
                genres.add("Action");
                genres.add("Hollywood"); // Marker for International HQ
                movie.setGenre(genres);
                
                // Lock in persistent genre string
                movie.setGenreString("Action");
                
                movie.setFeatured(page == 1 && count < 5); 
                
                batch.set(db.collection("movies").document(vId), movie);
                count++;
            }

            batch.commit().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onProgress("Imported TMDB Page " + page + " with YouTube Trailers");
                    fetchTmdbPage(page + 1, totalPages, callback);
                } else {
                    callback.onError("Firestore write failed during TMDB sync.");
                }
            });
        });
    }

    public void syncTvShowsFromTmdb(int pages, SyncCallback callback) {
        callback.onProgress("Connecting to TMDB TV Series Catalog...");
        fetchTvPage(1, pages, callback);
    }

    private void fetchTvPage(int page, int totalPages, SyncCallback callback) {
        if (page > totalPages) {
            callback.onComplete(0);
            return;
        }

        tmdbService.getTvShows(TMDB_API_KEY, "popularity.desc", page, "en-US")
                .enqueue(new Callback<TmdbResponse>() {
                    @Override
                    public void onResponse(Call<TmdbResponse> call, Response<TmdbResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().results != null) {
                            processTvResults(response.body().results, page, totalPages, callback);
                        } else {
                            callback.onError("TMDB TV Error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<TmdbResponse> call, Throwable t) {
                        callback.onError("TMDB TV Network Error: " + t.getMessage());
                    }
                });
    }

    private void processTvResults(List<TmdbResponse.MovieResult> results, int page, int totalPages, SyncCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            WriteBatch batch = db.batch();
            int count = 0;

            for (TmdbResponse.MovieResult res : results) {
                if (res.getTitleString() == null) continue;

                String title = res.getTitleString().replace("\"", "").trim();
                String posterUrl = "https://image.tmdb.org/t/p/original" + res.posterPath;

                String youtubeVideoId = "";
                try {
                    Response<TmdbVideoResponse> videoResponse = tmdbService.getTvVideos(res.id, TMDB_API_KEY).execute();
                    if (videoResponse.isSuccessful() && videoResponse.body() != null && videoResponse.body().results != null) {
                        for (TmdbVideoResponse.VideoResult vr : videoResponse.body().results) {
                            if ("YouTube".equalsIgnoreCase(vr.site) && ("Trailer".equalsIgnoreCase(vr.type) || "Teaser".equalsIgnoreCase(vr.type))) {
                                youtubeVideoId = vr.key;
                                break;
                            }
                        }
                        if (youtubeVideoId.isEmpty() && !videoResponse.body().results.isEmpty()) {
                            for (TmdbVideoResponse.VideoResult vr : videoResponse.body().results) {
                                if ("YouTube".equalsIgnoreCase(vr.site)) {
                                    youtubeVideoId = vr.key;
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching TMDB TV video for " + title, e);
                }

                if (youtubeVideoId.isEmpty()) continue;

                String videoUrl = "https://www.youtube.com/watch?v=" + youtubeVideoId;
                String vId = youtubeVideoId;

                Movie movie = new Movie(vId, title, posterUrl, videoUrl, res.getDateString());
                List<String> genres = new ArrayList<>();
                genres.add("TV Series");
                movie.setGenre(genres);
                movie.setGenreString("TV Series");
                movie.setFeatured(page == 1 && count < 5);

                batch.set(db.collection("movies").document(vId), movie);
                count++;
            }

            batch.commit().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onProgress("Imported TMDB TV Page " + page);
                    fetchTvPage(page + 1, totalPages, callback);
                } else {
                    callback.onError("Firestore write failed during TV sync.");
                }
            });
        });
    }

    public void syncAnimeFromTmdb(int pages, SyncCallback callback) {
        callback.onProgress("Connecting to TMDB Anime Catalog...");
        fetchAnimePage(1, pages, callback);
    }

    private void fetchAnimePage(int page, int totalPages, SyncCallback callback) {
        if (page > totalPages) {
            callback.onComplete(0);
            return;
        }

        // Genre 16 = Animation, Original Language = ja (Japanese Anime)
        tmdbService.getAnime(TMDB_API_KEY, "16", "ja", "popularity.desc", page)
                .enqueue(new Callback<TmdbResponse>() {
                    @Override
                    public void onResponse(Call<TmdbResponse> call, Response<TmdbResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().results != null) {
                            processAnimeResults(response.body().results, page, totalPages, callback);
                        } else {
                            callback.onError("TMDB Anime Error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<TmdbResponse> call, Throwable t) {
                        callback.onError("TMDB Anime Network Error: " + t.getMessage());
                    }
                });
    }

    private void processAnimeResults(List<TmdbResponse.MovieResult> results, int page, int totalPages, SyncCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            WriteBatch batch = db.batch();
            int count = 0;

            for (TmdbResponse.MovieResult res : results) {
                if (res.title == null) continue;

                String title = res.title.replace("\"", "").trim();
                String posterUrl = "https://image.tmdb.org/t/p/original" + res.posterPath;

                String youtubeVideoId = "";
                try {
                    Response<TmdbVideoResponse> videoResponse = tmdbService.getMovieVideos(res.id, TMDB_API_KEY).execute();
                    if (videoResponse.isSuccessful() && videoResponse.body() != null && videoResponse.body().results != null) {
                        for (TmdbVideoResponse.VideoResult vr : videoResponse.body().results) {
                            if ("YouTube".equalsIgnoreCase(vr.site) && ("Trailer".equalsIgnoreCase(vr.type) || "Teaser".equalsIgnoreCase(vr.type))) {
                                youtubeVideoId = vr.key;
                                break;
                            }
                        }
                        if (youtubeVideoId.isEmpty() && !videoResponse.body().results.isEmpty()) {
                            for (TmdbVideoResponse.VideoResult vr : videoResponse.body().results) {
                                if ("YouTube".equalsIgnoreCase(vr.site)) {
                                    youtubeVideoId = vr.key;
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching TMDB Anime video for " + title, e);
                }

                if (youtubeVideoId.isEmpty()) continue;

                String videoUrl = "https://www.youtube.com/watch?v=" + youtubeVideoId;
                String vId = youtubeVideoId;

                Movie movie = new Movie(vId, title, posterUrl, videoUrl, res.releaseDate);
                List<String> genres = new ArrayList<>();
                genres.add("Anime");
                movie.setGenre(genres);
                movie.setGenreString("Anime");
                movie.setFeatured(page == 1 && count < 5);

                batch.set(db.collection("movies").document(vId), movie);
                count++;
            }

            batch.commit().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onProgress("Imported TMDB Anime Page " + page);
                    fetchAnimePage(page + 1, totalPages, callback);
                } else {
                    callback.onError("Firestore write failed during Anime sync.");
                }
            });
        });
    }

    public void syncFightZoneFromYouTube(SyncCallback callback) {
        callback.onProgress("Connecting to YouTube FightZone Catalog (2026)...");
        String publishedAfter = "2026-01-01T00:00:00Z";
        String[] keywords = {
                "WWE Raw Full Match 2026 Official",
                "WWE SmackDown Full Match 2026",
                "AEW Dynamite Full Match 2026",
                "UFC Full Fight Official 2026"
        };
        deepSyncFightZone(keywords, 0, null, 2, publishedAfter, callback);
    }

    private void deepSyncFightZone(String[] keywords, int index, String nextToken, int pagesRemaining, String publishedAfter, SyncCallback callback) {
        if (index >= keywords.length) {
            callback.onComplete(0);
            return;
        }

        String currentQuery = keywords[index];
        callback.onProgress("Syncing Fights: " + currentQuery);

        syncFightZoneWithQuery(currentQuery, nextToken, pagesRemaining, publishedAfter, new SyncCallback() {
            @Override public void onProgress(String message) { callback.onProgress(message); }
            @Override public void onComplete(int count) {
                deepSyncFightZone(keywords, index + 1, null, pagesRemaining, publishedAfter, callback);
            }
            @Override public void onError(String error) { callback.onError(error); }
        });
    }

    private void syncFightZoneWithQuery(String query, String nextToken, int pagesRemaining, String publishedAfter, SyncCallback callback) {
        if (pagesRemaining <= 0) {
            callback.onComplete(0);
            return;
        }

        if (currentKeyIndex >= API_KEYS.length) {
            callback.onError("All API keys reached daily limit.");
            return;
        }

        String apiKey = API_KEYS[currentKeyIndex];
        apiService.searchVideos("snippet", null, "date", 50, apiKey, "video", null, "long", publishedAfter, query, nextToken)
                .enqueue(new Callback<YouTubeResponse>() {
                    @Override
                    public void onResponse(Call<YouTubeResponse> call, Response<YouTubeResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            processFightZoneResults(response.body(), pagesRemaining, publishedAfter, query, callback);
                        } else if (response.code() == 403 || response.code() == 429) {
                            currentKeyIndex++;
                            syncFightZoneWithQuery(query, nextToken, pagesRemaining, publishedAfter, callback);
                        } else {
                            callback.onError("YouTube API Error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<YouTubeResponse> call, Throwable t) {
                        callback.onError("Network Error: " + t.getMessage());
                    }
                });
    }

    private void processFightZoneResults(YouTubeResponse body, int pagesRemaining, String publishedAfter, String query, SyncCallback callback) {
        if (body.items == null || body.items.isEmpty()) {
            callback.onComplete(0);
            return;
        }

        WriteBatch batch = db.batch();
        int count = 0;

        for (YouTubeResponse.Item item : body.items) {
            if (item.id == null || item.id.videoId == null) continue;

            String vId = item.id.videoId;
            String titleClean = (item.snippet != null && item.snippet.title != null)
                    ? item.snippet.title.replace("\"", "").trim() : "FightZone Match";

            String lowerTitle = titleClean.toLowerCase();
            // 🚀 STRICT EXCLUSION FILTER: Skip anime wrestling, video games, simulations, 2K games, mods, etc.
            if (lowerTitle.contains("anime") || lowerTitle.contains("game") || lowerTitle.contains("gaming") ||
                lowerTitle.contains("gameplay") || lowerTitle.contains("2k") || lowerTitle.contains("simulator") ||
                lowerTitle.contains("ps5") || lowerTitle.contains("xbox") || lowerTitle.contains("cartoon") ||
                lowerTitle.contains("animation") || lowerTitle.contains("mugen") || lowerTitle.contains("mod")) {
                continue;
            }

            String posterUrl = "";
            if (item.snippet != null && item.snippet.thumbnails != null) {
                YouTubeResponse.Thumbnails thumbs = item.snippet.thumbnails;
                if (thumbs.maxres != null && thumbs.maxres.url != null) posterUrl = thumbs.maxres.url;
                else if (thumbs.high != null && thumbs.high.url != null) posterUrl = thumbs.high.url;
                else if (thumbs.medium != null && thumbs.medium.url != null) posterUrl = thumbs.medium.url;
                else if (thumbs.defaultThumbnail != null && thumbs.defaultThumbnail.url != null) posterUrl = thumbs.defaultThumbnail.url;
            }

            if (posterUrl == null || posterUrl.isEmpty()) {
                posterUrl = "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?q=80&w=1000&auto=format&fit=crop";
            }

            String publishedAt = (item.snippet != null && item.snippet.publishedAt != null) ? item.snippet.publishedAt : "";
            String videoUrl = "https://www.youtube.com/watch?v=" + vId;

            Movie movie = new Movie(vId, titleClean, posterUrl, videoUrl, "FightZone", publishedAt);
            movie.setFeatured(false);
            batch.set(db.collection("movies").document(vId), movie);
            count++;
        }

        int finalCount = count;
        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onProgress("Added " + finalCount + " recent fights.");
                if (pagesRemaining > 1 && body.nextPageToken != null) {
                    syncFightZoneWithQuery(query, body.nextPageToken, pagesRemaining - 1, publishedAfter, callback);
                } else {
                    callback.onComplete(finalCount);
                }
            } else {
                callback.onError("Firestore write failed.");
            }
        });
    }

    public String getISO8601Timestamp(int daysAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo);
        Date date = calendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }
}
