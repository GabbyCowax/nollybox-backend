package com.example.nollybox;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface MovieDao {
    @Query("SELECT * FROM downloaded_movies")
    List<DownloadedMovie> getAllDownloads();

    @Query("SELECT * FROM downloaded_movies WHERE videoId = :videoId LIMIT 1")
    DownloadedMovie getDownloadById(String videoId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDownload(DownloadedMovie movie);

    @Query("SELECT * FROM recent_movies ORDER BY timestamp DESC LIMIT 10")
    List<RecentMovie> getRecentMovies();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecentMovie(RecentMovie movie);

    @Query("DELETE FROM downloaded_movies WHERE videoId = :videoId")
    void deleteDownload(String videoId);

    // Watchlist (Favorites)
    @Query("SELECT * FROM favorite_movies")
    List<FavoriteMovie> getAllFavorites();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFavorite(FavoriteMovie movie);

    @Query("DELETE FROM favorite_movies WHERE videoId = :videoId")
    void deleteFavorite(String videoId);

    @Query("SELECT EXISTS(SELECT * FROM favorite_movies WHERE videoId = :videoId)")
    boolean isFavorite(String videoId);

    // Caching logic
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCachedMovies(List<Movie> movies);

    @Query("SELECT * FROM cached_movies")
    List<Movie> getAllCachedMovies();

    @Query("DELETE FROM cached_movies")
    void clearCache();
}