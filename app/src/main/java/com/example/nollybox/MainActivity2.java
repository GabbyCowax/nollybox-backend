package com.example.nollybox;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity2 extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoogleLogin;
    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private FirebaseFirestore db;
    private ListenerRegistration pairingListener;
    private String currentPairingCode;
    private boolean isTvMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        // 🚀 Detect if running on a Smart TV (Leanback hardware)
        isTvMode = getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);

        setupStandardLoginUI();
    }

    private void setupTvPairingUI() {
        setContentView(R.layout.activity_tv_pairing);
        
        TextView tvCode = findViewById(R.id.tv_pairing_code);
        MaterialButton btnManual = findViewById(R.id.btn_switch_to_manual);
        
        if (btnManual != null) {
            btnManual.setOnClickListener(v -> {
                if (pairingListener != null) pairingListener.remove();
                currentPairingCode = null; // Reset code on manual exit
                setupStandardLoginUI();
            });
        }

        // Add a refresh code listener if the user clicks the code area
        if (tvCode != null) {
            tvCode.setOnClickListener(v -> startTvPairingFlow(tvCode));
        }

        // 🚀 STABILITY FIX: Only generate a new code if one doesn't already exist
        if (currentPairingCode == null) {
            startTvPairingFlow(tvCode);
        } else {
            String formattedCode = currentPairingCode.substring(0, 3) + " " + currentPairingCode.substring(3);
            if (tvCode != null) tvCode.setText(formattedCode);
            listenForPairingSuccess();
        }
    }

    private void setupStandardLoginUI() {
        setContentView(R.layout.activity_main2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnGoogleLogin = findViewById(R.id.btn_google_login);

        // 🚀 Premium Entrance Animations
        applyEntranceAnimations();

        btnLogin.setOnClickListener(v -> loginUser());
        btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());

        MaterialButton btnPhoneLogin = findViewById(R.id.btn_login_with_phone);
        if (btnPhoneLogin != null) {
            btnPhoneLogin.setOnClickListener(v -> setupTvPairingUI());
        }

        TextView tvSignUp = findViewById(R.id.tv_go_to_signup);
        if (tvSignUp != null) {
            tvSignUp.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity2.this, MainActivity3.class);
                startActivity(intent);
            });
        }
    }

    private void startTvPairingFlow(final TextView tvCodeDisplay) {
        if (tvCodeDisplay != null) tvCodeDisplay.setText("GENERATING...");
        
        // 🚀 AUTH FIX: Ensure authenticated session so Firestore security rules allow writing pairing codes
        if (mAuth.getCurrentUser() == null) {
            mAuth.signInAnonymously()
                    .addOnSuccessListener(authResult -> generateAndSavePairingCode(tvCodeDisplay))
                    .addOnFailureListener(e -> {
                        Log.e("TV_PAIR", "Anonymous auth failed: " + e.getMessage());
                        if (tvCodeDisplay != null) tvCodeDisplay.setText("AUTH ERROR");
                    });
        } else {
            generateAndSavePairingCode(tvCodeDisplay);
        }
    }

    private void generateAndSavePairingCode(final TextView tvCodeDisplay) {
        // 🚀 Generate 6-digit numbers as requested
        final String chars = "0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        final String newCode = sb.toString();

        Map<String, Object> pairingData = new HashMap<>();
        pairingData.put("status", "pending");
        pairingData.put("createdAt", FieldValue.serverTimestamp());

        // 🚀 RELIABILITY FIX: Direct set to ensure document exists in Firestore
        db.collection("pairing_codes").document(newCode)
                .set(pairingData)
                .addOnSuccessListener(aVoid -> {
                    currentPairingCode = newCode;
                    String formattedCode = currentPairingCode.substring(0, 3) + " " + currentPairingCode.substring(3);
                    if (tvCodeDisplay != null) {
                        tvCodeDisplay.setText(formattedCode);
                    }
                    listenForPairingSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("TV_PAIR", "Firestore error: " + e.getMessage());
                    if (tvCodeDisplay != null) tvCodeDisplay.setText("RETRYING...");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> startTvPairingFlow(tvCodeDisplay), 2000);
                });
    }

    private void listenForPairingSuccess() {
        if (pairingListener != null) pairingListener.remove();
        
        final TextView tvStatus = findViewById(R.id.tv_pairing_status);
        final ProgressBar pb = findViewById(R.id.pb_pairing);

        pairingListener = db.collection("pairing_codes").document(currentPairingCode)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e("TV_PAIR", "Snapshot error", e);
                        if (tvStatus != null) tvStatus.setText("Connection lost. Retrying...");
                        return;
                    }

                    if (snapshot == null || !snapshot.exists()) return;

                    String status = snapshot.getString("status");
                    
                    if (tvStatus != null) {
                        if ("pending".equals(status)) {
                            tvStatus.setText("Waiting for your phone...");
                        } else if ("linked".equals(status)) {
                            tvStatus.setText("Pairing successful!");
                            if (pb != null) pb.setVisibility(View.GONE);
                        }
                    }

                    if ("linked".equals(status)) {
                        String uid = snapshot.getString("uid");
                        String name = snapshot.getString("userName");
                        String email = snapshot.getString("email");

                        if (uid != null) {
                            saveTvSession(uid, name, email);
                            
                            // 🚀 Professional Welcome Sequence
                            showTvWelcomeOverlay(name);

                            // Cleanup pairing code
                            db.collection("pairing_codes").document(currentPairingCode).delete();
                            if (pairingListener != null) pairingListener.remove();
                            currentPairingCode = null;
                        }
                    }
                });
    }

    private void showTvWelcomeOverlay(String name) {
        View overlay = findViewById(R.id.layout_welcome_overlay);
        TextView tvWelcome = findViewById(R.id.tv_welcome_name);
        if (overlay != null && tvWelcome != null) {
            String welcomeMsg = getString(R.string.tv_welcome_format, name != null ? name : "Fan");
            tvWelcome.setText(welcomeMsg);
            overlay.setVisibility(View.VISIBLE);
            overlay.setAlpha(0f);
            overlay.animate().alpha(1f).setDuration(800).start();

            // Transition to Home after 3 seconds to let user read the message
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isFinishing()) {
                    startActivity(new Intent(MainActivity2.this, MainActivity4.class));
                    finishAffinity();
                }
            }, 3500);
        } else {
            startActivity(new Intent(MainActivity2.this, MainActivity4.class));
            finishAffinity();
        }
    }

    private void saveTvSession(String uid, String name, String email) {
        getSharedPreferences("NollyBoxTV", MODE_PRIVATE).edit()
                .putString("tv_uid", uid)
                .putString("tv_name", name)
                .putString("tv_email", email)
                .putBoolean("is_tv_linked", true)
                .apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pairingListener != null) pairingListener.remove();
    }

    private void applyEntranceAnimations() {
        View logo = findViewById(R.id.iv_login_logo);
        View title = findViewById(R.id.tv_login_title);
        View subtitle = findViewById(R.id.tv_login_subtitle);
        View tilEmail = findViewById(R.id.til_email);
        View tilPassword = findViewById(R.id.til_password);
        View btnLogin = findViewById(R.id.btn_login);
        View btnGoogle = findViewById(R.id.btn_google_login);
        View footer = findViewById(R.id.ll_login_footer);

        View[] views = {logo, title, subtitle, tilEmail, tilPassword, btnLogin, btnGoogle, footer};

        for (int i = 0; i < views.length; i++) {
            if (views[i] == null) continue;
            views[i].setAlpha(0f);
            views[i].setTranslationY(30f);
            views[i].animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setStartDelay(100L * i)
                    .start();
        }
    }

    private void signInWithGoogle() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(true)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        Executor executor = Executors.newSingleThreadExecutor();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                executor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        Credential credential = result.getCredential();
                        if (credential instanceof GoogleIdTokenCredential) {
                            GoogleIdTokenCredential googleIdTokenCredential = (GoogleIdTokenCredential) credential;
                            String idToken = googleIdTokenCredential.getIdToken();
                            firebaseAuthWithGoogle(idToken);
                        }
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        runOnUiThread(() -> Toast.makeText(MainActivity2.this, "Google Sign-In Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }
        );
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String name = user != null ? user.getDisplayName() : "Fan";
                        if (isTvMode) {
                            showTvWelcomeOverlay(name);
                        } else {
                            Toast.makeText(MainActivity2.this, "Google Sign-In Successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity2.this, MainActivity4.class));
                            finishAffinity();
                        }
                    } else {
                        Toast.makeText(MainActivity2.this, "Firebase Auth Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String name = user != null ? user.getDisplayName() : "Fan";
                        if (isTvMode) {
                            showTvWelcomeOverlay(name);
                        } else {
                            Toast.makeText(MainActivity2.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity2.this, MainActivity4.class));
                            finishAffinity();
                        }
                    } else {
                        btnLogin.setEnabled(true);
                        Toast.makeText(MainActivity2.this, "Authentication Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}