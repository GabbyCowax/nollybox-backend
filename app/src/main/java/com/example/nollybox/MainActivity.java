package com.example.nollybox;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // 🚀 Premium Entrance Animations
        View logoCard = findViewById(R.id.logo_card);
        View appName = findViewById(R.id.tv_app_name);
        View tagline = findViewById(R.id.tv_tagline);
        View glow = findViewById(R.id.v_glow);

        // Initial States
        logoCard.setAlpha(0f);
        logoCard.setScaleX(0.8f);
        logoCard.setScaleY(0.8f);
        appName.setAlpha(0f);
        appName.setTranslationY(20f);
        tagline.setAlpha(0f);
        glow.setAlpha(0f);

        // Animation Sequence
        logoCard.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(1200).setStartDelay(200).start();
        glow.animate().alpha(1f).setDuration(1500).setStartDelay(500).start();
        appName.animate().alpha(1f).translationY(0f).setDuration(1000).setStartDelay(800).start();
        tagline.animate().alpha(1f).setDuration(1000).setStartDelay(1200).start();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Splash screen delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            
            // 🚀 SMART TV CHECK: Check for paired session even if Firebase user is null
            boolean isTvLinked = getSharedPreferences("NollyBoxTV", MODE_PRIVATE).getBoolean("is_tv_linked", false);

            Intent intent;
            if (user != null || isTvLinked) {
                // User is signed in or TV is linked, go to Dashboard
                intent = new Intent(MainActivity.this, MainActivity4.class);
            } else {
                // No user is signed in, go to Login
                intent = new Intent(MainActivity.this, MainActivity2.class);
            }
            startActivity(intent);
            finish(); // Remove splash from back stack
        }, 3000); // 3 seconds delay
    }
}