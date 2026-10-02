package com.example.nollybox;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;

import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Toast;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import android.view.KeyEvent;
import java.io.File;

public class PlayerActivity extends AppCompatActivity {

    private ExoPlayer exoPlayer;
    private PlayerView localPlayerView;
    private YouTubePlayerView youTubePlayerView;
    private YouTubePlayer activeYouTubePlayer;
    private boolean isPlayerInitialized = false;
    private boolean isYouTubePaused = false;

    private static final String TAG = "PlayerActivity";

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_player);
            setupFullScreen();
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            youTubePlayerView = findViewById(R.id.youtube_player_view);
            localPlayerView = findViewById(R.id.local_player_view);
            View btnBack = findViewById(R.id.btn_back_player);
            if (btnBack != null) btnBack.setOnClickListener(v -> finish());

            String videoId = getIntent().getStringExtra("videoId");
            String localPath = getIntent().getStringExtra("localPath");

            Log.d(TAG, "Starting playback - ID: " + videoId + ", Path: " + localPath);

            if (localPath != null && !localPath.isEmpty() && new File(localPath).exists()) {
                playOffline(localPath);
            } else if (videoId != null && !videoId.isEmpty()) {
                playOnline(extractYoutubeId(videoId));
            } else {
                Toast.makeText(this, "Cinematic link broken", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Player startup failed", e);
            finish();
        }
    }

    private String extractYoutubeId(String idOrUrl) {
        if (idOrUrl == null) return "";
        String clean = idOrUrl.replace("\"", "").trim();
        if (clean.contains("v=")) {
            String[] parts = clean.split("v=");
            if (parts.length > 1) return parts[1].split("&")[0];
        } else if (clean.contains("be/")) {
            String[] parts = clean.split("be/");
            if (parts.length > 1) return parts[1].split("\\?")[0];
        }
        return clean;
    }

    @OptIn(markerClass = UnstableApi.class)
    private void playOffline(String path) {
        try {
            if (localPlayerView == null) return;
            localPlayerView.setVisibility(View.VISIBLE);
            if (youTubePlayerView != null) youTubePlayerView.setVisibility(View.GONE);

            if (exoPlayer != null) exoPlayer.release();
            exoPlayer = new ExoPlayer.Builder(this).build();
            localPlayerView.setPlayer(exoPlayer);
            
            // 🚀 Smart TV Optimization: Auto-hide controls after 3 seconds
            setLocalPlayerControls(localPlayerView);

            MediaItem mediaItem = MediaItem.fromUri(Uri.fromFile(new File(path)));
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            exoPlayer.play();
        } catch (Exception e) {
            Log.e(TAG, "Offline failed", e);
            String vId = getIntent().getStringExtra("videoId");
            if (vId != null) playOnline(extractYoutubeId(vId));
        }
    }

    private void playOnline(String vId) {
        if (vId == null || vId.isEmpty() || youTubePlayerView == null || isPlayerInitialized || isFinishing()) return;
        isPlayerInitialized = true;

        Log.d(TAG, "Initializing YouTube Player for ID: " + vId);
        try {
            youTubePlayerView.setVisibility(View.VISIBLE);
            if (localPlayerView != null) localPlayerView.setVisibility(View.GONE);

            getLifecycle().addObserver(youTubePlayerView);
            youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
                @Override
                public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                    Log.d(TAG, "YouTube Player Ready. Loading video...");
                    activeYouTubePlayer = youTubePlayer;
                    youTubePlayer.loadVideo(vId, 0);
                }
                
                @Override
                public void onError(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerError error) {
                    Log.e(TAG, "Online error: " + error.name());
                    if (!isFinishing()) {
                        runOnUiThread(() -> Toast.makeText(PlayerActivity.this, "Network Error: " + error.name(), Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "YouTube initialization failed", e);
            finish();
        }
    }

    private void setupFullScreen() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 🚀 Smart TV Remote Integration: Bind DPAD_CENTER and ENTER to Toggle Pause/Play
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            togglePlayback();
            return true;
        }
        // Handle Back button to close player
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void setLocalPlayerControls(PlayerView view) {
        view.setControllerShowTimeoutMs(3000);
        view.setControllerHideOnTouch(true);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void togglePlayback() {
        // 1. Handle Local ExoPlayer (Offline)
        if (exoPlayer != null && localPlayerView != null && localPlayerView.getVisibility() == View.VISIBLE) {
            if (exoPlayer.isPlaying()) {
                exoPlayer.pause();
                localPlayerView.showController();
            } else {
                exoPlayer.play();
                localPlayerView.hideController();
            }
        }
        // 2. Handle YouTube Player (Online)
        else if (activeYouTubePlayer != null && youTubePlayerView != null && youTubePlayerView.getVisibility() == View.VISIBLE) {
            if (!isYouTubePaused) {
                activeYouTubePlayer.pause();
                isYouTubePaused = true;
            } else {
                activeYouTubePlayer.play();
                isYouTubePaused = false;
            }
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (exoPlayer != null) exoPlayer.release();
            if (youTubePlayerView != null) youTubePlayerView.release();
        } catch (Exception ignored) {}
    }
}
