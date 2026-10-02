package com.example.nollybox;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class PreSplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pre_splash);

        // Professional 1.2 second delay before transitioning to animated splash
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(PreSplashActivity.this, MainActivity.class);
            startActivity(intent);
            
            // Disable default transition animation for seamless jump
            overridePendingTransition(0, 0);
            
            finish();
        }, 1200);
    }
}