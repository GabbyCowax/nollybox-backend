package com.example.nollybox;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ArchiveApiService {
    @GET("advancedsearch.php")
    Call<ArchiveSearchResponse> searchArchive(
            @Query("q") String query,
            @Query("rows") int rows,
            @Query("output") String output
    );

    @GET("metadata/{identifier}")
    Call<ArchiveMetadataResponse> getMetadata(
            @Path("identifier") String identifier
    );
}
