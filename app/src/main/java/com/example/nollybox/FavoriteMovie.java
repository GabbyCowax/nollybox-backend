package com.example.nollybox;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorite_movies")
public class FavoriteMovie {
    @PrimaryKey
    @NonNull
    private String videoId;
    private String title;
    private String posterUrl;
    private String genre;

    public FavoriteMovie(@NonNull String videoId, String title, String posterUrl, String genre) {
        this.videoId = videoId;
        this.title = title;
        this.posterUrl = posterUrl;
        this.genre = genre;
    }

    @NonNull
    public String getVideoId() { return videoId; }
    public void setVideoId(@NonNull String videoId) { this.videoId = videoId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
}