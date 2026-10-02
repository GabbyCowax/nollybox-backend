package com.example.nollybox;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface YouTubeApiService {
    @GET("youtube/v3/search")
    Call<YouTubeResponse> searchVideos(
            @Query("part") String part,
            @Query("channelId") String channelId,
            @Query("order") String order,
            @Query("maxResults") int maxResults,
            @Query("key") String apiKey,
            @Query("type") String type,
            @Query("videoEmbeddable") String videoEmbeddable,
            @Query("videoDuration") String videoDuration,
            @Query("publishedAfter") String publishedAfter,
            @Query("q") String query,
            @Query("pageToken") String pageToken
    );
    @GET("youtube/v3/videos")
    Call<YouTubeVideoResponse> getVideoDetails(
            @Query("part") String part,
            @Query("id") String videoId,
            @Query("key") String apiKey
    );
}