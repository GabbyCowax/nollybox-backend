package com.example.nollybox;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface CinematicStreamService {
    @GET("streams/{videoId}")
    Call<PipedResponse> getStreams(@Path("videoId") String videoId);
}
