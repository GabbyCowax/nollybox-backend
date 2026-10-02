package com.example.nollybox;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class DownloadsFragment extends Fragment {

    private RecyclerView rvDownloads;
    private MovieAdapter adapter;
    private List<Movie> downloadList;
    private TextView tvNoDownloads;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_downloads, container, false);

        rvDownloads = view.findViewById(R.id.rv_downloads);
        tvNoDownloads = view.findViewById(R.id.tv_no_downloads);
        
        downloadList = new ArrayList<>();
        adapter = new MovieAdapter(downloadList, R.layout.item_movie_card, (view1, movie, position) -> {
            showDownloadOptions(view1, movie, position);
        });
        adapter.setOnItemClickListener(this::playOffline);
        rvDownloads.setLayoutManager(new GridLayoutManager(getContext(), 3));
        rvDownloads.setAdapter(adapter);

        loadDownloads();

        return view;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadDownloads();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDownloads();
    }

    public void refreshData() {
        if (isAdded()) {
            loadDownloads();
        }
    }

    private void playOffline(Movie movie) {
        if (movie == null) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            if (getContext() == null) return;
            DownloadedMovie dm = AppDatabase.getInstance(getContext()).movieDao().getDownloadById(movie.getId());
            if (dm != null) {
                // 🚀 $0 ARCHITECTURE: Play saved bookmark directly from YouTube
                if ("BOOKMARK".equals(dm.getFilePath())) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Intent intent = new Intent(getContext(), PlayerActivity.class);
                            intent.putExtra("videoId", movie.getVideoId());
                            startActivity(intent);
                        });
                    }
                } else {
                    // Fallback for real file if path is not a bookmark
                    File file = new File(dm.getFilePath());
                    if (file.exists()) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Intent intent = new Intent(getContext(), OfflinePlayerActivity.class);
                                intent.putExtra("videoId", movie.getVideoId());
                                intent.putExtra("localPath", dm.getFilePath());
                                startActivity(intent);
                            });
                        }
                    }
                }
            }
        });
    }

    private void loadDownloads() {
        if (!isAdded() || getContext() == null) return;
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<DownloadedMovie> downloads = AppDatabase.getInstance(getContext().getApplicationContext()).movieDao().getAllDownloads();
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        downloadList.clear();
                        for (DownloadedMovie d : downloads) {
                            String t = d.getTitle() != null ? d.getTitle() : "NollyBox Movie";
                            String p = d.getPosterUrl() != null ? d.getPosterUrl() : "";
                            
                            // 🚀 $0 ARCHITECTURE: Bookmarks are always "READY" to play
                            String status = "BOOKMARK".equals(d.getFilePath()) ? "Ready" : "Incomplete";
                            
                            downloadList.add(new Movie(d.getVideoId(), t, p, d.getVideoId(), status, null));
                        }
                        adapter.notifyDataSetChanged();
                        if (tvNoDownloads != null) {
                            tvNoDownloads.setVisibility(downloadList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("Downloads", "Sync error", e);
            }
        });
    }

    private void showDownloadOptions(View view, Movie movie, int position) {
        PopupMenu popup = new PopupMenu(getContext(), view);
        popup.getMenu().add("Watch Now");
        popup.getMenu().add("Remove from Library");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Watch Now")) {
                playOffline(movie);
            } else if (item.getTitle().equals("Remove from Library")) {
                deleteDownload(movie, position);
            }
            return true;
        });
        popup.show();
    }

    private void deleteDownload(Movie movie, int position) {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (getContext() == null) return;
            AppDatabase.getInstance(getContext()).movieDao().deleteDownload(movie.getId());
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (position < downloadList.size()) {
                        downloadList.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, downloadList.size());
                    }
                    if (tvNoDownloads != null) tvNoDownloads.setVisibility(downloadList.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }
        });
    }
}
