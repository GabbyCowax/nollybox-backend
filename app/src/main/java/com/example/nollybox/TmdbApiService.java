package com.example.nollybox;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TmdbApiService {
    @GET("discover/movie")
    Call<TmdbResponse> getMoviesByGenre(
            @Query("api_key") String apiKey,
            @Query("with_genres") String genreId,
            @Query("sort_by") String sortBy,
            @Query("page") int page,
            @Query("language") String language
    );

    @GET("movie/{movie_id}/videos")
    Call<TmdbVideoResponse> getMovieVideos(
            @Path("movie_id") int movieId,
            @Query("api_key") String apiKey
    );

    @GET("discover/tv")
    Call<TmdbResponse> getTvShows(
            @Query("api_key") String apiKey,
            @Query("sort_by") String sortBy,
            @Query("page") int page,
            @Query("language") String language
    );

    @GET("tv/{tv_id}/videos")
    Call<TmdbVideoResponse> getTvVideos(
            @Path("tv_id") int tvId,
            @Query("api_key") String apiKey
    );

    @GET("discover/movie")
    Call<TmdbResponse> getAnime(
            @Query("api_key") String apiKey,
            @Query("with_genres") String genreId,
            @Query("with_original_language") String language,
            @Query("sort_by") String sortBy,
            @Query("page") int page
    );

    @GET("search/tv")
    Call<TmdbResponse> searchTv(
            @Query("api_key") String apiKey,
            @Query("query") String query,
            @Query("page") int page
    );
}
