package com.example.nollybox;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "downloaded_movies")
public class DownloadedMovie {
    @PrimaryKey
    @NonNull
    private String videoId;
    private String title;
    private String posterUrl;
    private String filePath;

    public DownloadedMovie(@NonNull String videoId, String title, String posterUrl, String filePath) {
        this.videoId = videoId;
        this.title = title;
        this.posterUrl = posterUrl;
        this.filePath = filePath;
    }

    @NonNull
    public String getVideoId() { return videoId; }
    public void setVideoId(@NonNull String videoId) { this.videoId = videoId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}