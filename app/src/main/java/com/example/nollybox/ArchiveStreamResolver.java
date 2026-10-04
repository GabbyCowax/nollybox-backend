package com.example.nollybox;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ArchiveStreamResolver {
    private static final String TAG = "ArchiveStreamResolver";
    private static ArchiveApiService apiService;

    public interface ResolveCallback {
        void onSuccess(String mp4DirectUrl);
        void onError(String error);
    }

    private static synchronized ArchiveApiService getApiService() {
        if (apiService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://archive.org/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            apiService = retrofit.create(ArchiveApiService.class);
        }
        return apiService;
    }

    public static void resolveAndCacheStream(final String movieId, final String title, final ResolveCallback callback) {
        if (title == null || title.trim().isEmpty()) {
            if (callback != null) callback.onError("Invalid title");
            return;
        }

        final Handler mainHandler = new Handler(Looper.getMainLooper());
        final String cleanTitle = title.replace("\"", "").trim();
        final String searchQuery = "title:(\"" + cleanTitle + "\") AND mediatype:movies";

        getApiService().searchArchive(searchQuery, 1, "json").enqueue(new Callback<ArchiveSearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<ArchiveSearchResponse> call, @NonNull Response<ArchiveSearchResponse> response) {
                if (response.isSuccessful() && response.body() != null 
                        && response.body().response != null 
                        && response.body().response.docs != null 
                        && !response.body().response.docs.isEmpty()) {
                    
                    String identifier = response.body().response.docs.get(0).identifier;
                    if (identifier != null && !identifier.isEmpty()) {
                        fetchMetadataAndCache(movieId, identifier, cleanTitle, callback, mainHandler);
                        return;
                    }
                }
                
                // Fallback to broader title search
                fetchBroaderSearch(movieId, cleanTitle, callback, mainHandler);
            }

            @Override
            public void onFailure(@NonNull Call<ArchiveSearchResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Search failed for: " + cleanTitle, t);
                fetchBroaderSearch(movieId, cleanTitle, callback, mainHandler);
            }
        });
    }

    private static void fetchBroaderSearch(final String movieId, final String cleanTitle, final ResolveCallback callback, final Handler mainHandler) {
        String query = cleanTitle + " AND mediatype:movies";
        getApiService().searchArchive(query, 1, "json").enqueue(new Callback<ArchiveSearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<ArchiveSearchResponse> call, @NonNull Response<ArchiveSearchResponse> response) {
                if (response.isSuccessful() && response.body() != null 
                        && response.body().response != null 
                        && response.body().response.docs != null 
                        && !response.body().response.docs.isEmpty()) {
                    
                    String identifier = response.body().response.docs.get(0).identifier;
                    if (identifier != null && !identifier.isEmpty()) {
                        fetchMetadataAndCache(movieId, identifier, cleanTitle, callback, mainHandler);
                        return;
                    }
                }
                notifyError("No matching stream found on Archive.org", callback, mainHandler);
            }

            @Override
            public void onFailure(@NonNull Call<ArchiveSearchResponse> call, @NonNull Throwable t) {
                notifyError("Archive.org connection failed", callback, mainHandler);
            }
        });
    }

    private static void fetchMetadataAndCache(final String movieId, final String identifier, final String title, final ResolveCallback callback, final Handler mainHandler) {
        getApiService().getMetadata(identifier).enqueue(new Callback<ArchiveMetadataResponse>() {
            @Override
            public void onResponse(@NonNull Call<ArchiveMetadataResponse> call, @NonNull Response<ArchiveMetadataResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().files != null) {
                    String selectedMp4 = null;
                    
                    for (ArchiveMetadataResponse.ArchiveFile file : response.body().files) {
                        if (file.name != null) {
                            String nameLower = file.name.toLowerCase();
                            if (nameLower.endsWith(".mp4")) {
                                selectedMp4 = file.name;
                                // Prefer 512kb/h264 mp4s if multiple exist
                                if (nameLower.contains("512kb") || nameLower.contains("h264") || nameLower.contains("main")) {
                                    break;
                                }
                            }
                        }
                    }

                    if (selectedMp4 != null) {
                        final String directMp4Url = "https://archive.org/download/" + identifier + "/" + selectedMp4;
                        
                        // 🚀 FIRESTORE CACHING: Save direct MP4 link to Firestore document
                        if (movieId != null && !movieId.isEmpty()) {
                            Map<String, Object> updateMap = new HashMap<>();
                            updateMap.put("downloadUrl", directMp4Url);
                            FirebaseFirestore.getInstance().collection("movies").document(movieId)
                                    .update(updateMap)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Cached direct MP4 link to Firestore for " + title))
                                    .addOnFailureListener(e -> Log.w(TAG, "Firestore caching failed for " + title, e));
                        }

                        notifySuccess(directMp4Url, callback, mainHandler);
                        return;
                    }
                }
                notifyError("No MP4 format available for this movie", callback, mainHandler);
            }

            @Override
            public void onFailure(@NonNull Call<ArchiveMetadataResponse> call, @NonNull Throwable t) {
                notifyError("Failed to fetch Archive.org metadata", callback, mainHandler);
            }
        });
    }

    private static void notifySuccess(final String directUrl, final ResolveCallback callback, final Handler mainHandler) {
        if (callback != null) {
            mainHandler.post(() -> callback.onSuccess(directUrl));
        }
    }

    private static void notifyError(final String error, final ResolveCallback callback, final Handler mainHandler) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(error));
        }
    }
}
