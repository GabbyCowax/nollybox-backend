package com.example.nollybox;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "cached_movies")
public class Movie {
    @PrimaryKey
    @NonNull
    private String id = "";

    private String tittle;    // Matches your Firestore "tittle"
    private String PosterUrl; // Matches your Firestore "PosterUrl"
    private String VideoUrl;  // Matches your Firestore "VideoUrl"
    private String BannerUrl; // Matches your Firestore "BannerUrl"
    private String createdAt; // Matches your Firestore "createdAt"
    private boolean featured; // Matches your Firestore "featured"
    private String downloadUrl; // New field for real MP4 links
    private String genreString = "Nollywood"; // 🚀 DEFAULT: Ensure never null

    @Ignore
    private List<String> genre = new ArrayList<>(); // Firestore Array

    // Required for Firestore
    public Movie() {}

    public Movie(@NonNull String id, String tittle, String PosterUrl, String VideoUrl, String createdAt) {
        this.id = id;
        this.tittle = tittle;
        this.PosterUrl = PosterUrl;
        this.VideoUrl = VideoUrl;
        this.createdAt = createdAt;
        this.BannerUrl = PosterUrl;
        this.genreString = "Nollywood";
    }

    // 🚀 Compatibility constructor for manual movie creation (Downloads/API Sync)
    @Ignore
    public Movie(String id, String tittle, String posterUrl, String videoId, String genreStr, String createdAt) {
        this.id = id;
        this.tittle = tittle;
        this.PosterUrl = posterUrl;
        this.VideoUrl = videoId;
        this.createdAt = createdAt;
        this.BannerUrl = posterUrl;
        this.genreString = (genreStr != null && !genreStr.isEmpty()) ? genreStr : "Nollywood";
        this.genre = new ArrayList<>();
        this.genre.add(this.genreString);
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getTittle() { return tittle; }
    public void setTittle(String tittle) { this.tittle = tittle; }

    public String getPosterUrl() { return PosterUrl; }
    public void setPosterUrl(String PosterUrl) { this.PosterUrl = PosterUrl; }

    public String getVideoUrl() { return VideoUrl; }
    public void setVideoUrl(String VideoUrl) { this.VideoUrl = VideoUrl; }

    public String getBannerUrl() { return BannerUrl; }
    public void setBannerUrl(String BannerUrl) { this.BannerUrl = BannerUrl; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getGenreString() { return genreString; }
    public void setGenreString(String genreString) { this.genreString = genreString; }

    public List<String> getGenre() { return genre; }
    public void setGenre(List<String> genre) { 
        this.genre = genre; 
        if (genre != null && !genre.isEmpty()) {
            this.genreString = genre.get(0);
        }
    }

    public String getGenreStringField() { return genreString; }
    public void setGenreStringField(String genreString) { this.genreString = genreString; }

    // Compatibility methods for UI
    @Ignore public String getTitle() { 
        if (tittle == null) return "NollyBox Movie";
        return tittle.replace("\"", ""); 
    }
    @Ignore public String getVideoId() { 
        if (VideoUrl == null) return id != null ? id : "";
        String clean = VideoUrl.replace("\"", "");
        if (clean.contains("v=")) {
            try {
                return clean.split("v=")[1].split("&")[0];
            } catch (Exception e) {
                return clean;
            }
        }
        return clean;
    }
    @Ignore public String getGenreStringForUI() { 
        if (genreString != null) return genreString.replace("\"", "");
        if (genre == null || genre.isEmpty() || genre.get(0) == null) return "Nollywood";
        return genre.get(0).replace("\"", ""); 
    }

    @Ignore public boolean isActionMovie() {
        // 🚀 HIGH-PRECISION ACTION FILTER (Mutually Exclusive)
        String title = getTitle().toLowerCase();
        String gs = getGenreStringForUI().toLowerCase();

        // 1. Explicit Action Tags (Zero Tolerance)
        if (gs.contains("action")) return true;
        if (genre != null) {
            for (String g : genre) {
                if (g != null && g.toLowerCase().contains("action")) return true;
            }
        }

        // 2. Hollywood/Modern Action Keywords (No overlap with Village Dramas)
        if (title.contains("full action") || title.contains("action movie") || title.contains("commando")) return true;
        if (title.contains("mercenary") || title.contains("assassin") || title.contains("ninja") || title.contains("karate")) return true;
        if (title.contains("shooter") || title.contains("gangster") || title.contains("underworld")) return true;

        // 3. Conditional: Only move "War" or "Fight" to Action if they ARE NOT explicitly tagged as Nollywood Drama/Epic
        boolean isNollywoodHQ = gs.contains("nollywood") || gs.contains("drama") || gs.contains("epic") || 
                               gs.contains("traditional") || gs.contains("village");
        
        if (!isNollywoodHQ) {
            if (title.contains("fight") || title.contains("war") || title.contains("battle") || title.contains("attack")) return true;
            if (title.contains("soldier") || title.contains("combat") || title.contains("detective")) return true;
        }
        
        return false;
    }

    @Ignore public boolean isRomanceMovie() {
        // 🚀 ROMANCE SEPARATOR
        String gs = getGenreStringForUI().toLowerCase();
        String title = getTitle().toLowerCase();
        return gs.contains("romance") || gs.contains("love") || 
               title.contains("romance movie") || title.contains("love story");
    }

    @Ignore public boolean isNollywood() {
        String gs = getGenreStringForUI().toLowerCase();
        String title = getTitle().toLowerCase();
        // If it's explicitly tagged Nollywood or has village markers, it's Nollywood
        if (gs.contains("nollywood") || gs.contains("traditional") || gs.contains("village")) return true;
        if (title.contains("village") || title.contains("traditional") || title.contains("chief")) return true;
        return false;
    }

    @Ignore public String getPublishedAt() { return createdAt; }
}
