package com.example.nollybox;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class TmdbResponse {
    @SerializedName("results")
    public List<MovieResult> results;

    public static class MovieResult {
        @SerializedName("id")
        public int id;

        @SerializedName("title")
        public String title;

        @SerializedName("name")
        public String name; // 🚀 TV Shows use "name" instead of "title"

        @SerializedName("overview")
        public String overview;

        @SerializedName("poster_path")
        public String posterPath;

        @SerializedName("backdrop_path")
        public String backdropPath;

        @SerializedName("release_date")
        public String releaseDate;

        @SerializedName("first_air_date")
        public String firstAirDate; // 🚀 TV Shows use "first_air_date" instead of "release_date"

        @SerializedName("vote_average")
        public double voteAverage;

        public String getTitleString() {
            return title != null ? title : (name != null ? name : "NollyBox Show");
        }

        public String getDateString() {
            return releaseDate != null ? releaseDate : (firstAirDate != null ? firstAirDate : "2026");
        }
    }
}
