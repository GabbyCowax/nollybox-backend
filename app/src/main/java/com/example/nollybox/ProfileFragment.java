package com.example.nollybox;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import android.widget.Toast;
import android.text.InputFilter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        TextView tvName = view.findViewById(R.id.tv_profile_name);
        TextView tvEmail = view.findViewById(R.id.tv_profile_email);
        MaterialButton btnWatchlist = view.findViewById(R.id.btn_watchlist);
        MaterialButton btnLinkTv = view.findViewById(R.id.btn_link_tv);
        MaterialButton btnAdmin = view.findViewById(R.id.btn_admin_panel);
        MaterialButton btnLogout = view.findViewById(R.id.btn_logout_profile);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tvName.setText(user.getDisplayName() != null ? user.getDisplayName() : "NollyBox Fan");
            tvEmail.setText(user.getEmail());
        }

        btnWatchlist.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), MainActivity6.class);
            startActivity(intent);
        });

        btnLinkTv.setOnClickListener(v -> showPairingDialog());

        btnAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AdminActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getContext(), MainActivity2.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // 🚀 Premium Entrance Animations
        applyEntranceAnimations(view);

        return view;
    }

    private void showPairingDialog() {
        if (getContext() == null) return;

        // Create a professional layout for the dialog
        TextInputLayout til = new TextInputLayout(getContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox);
        til.setHint("Enter 6-Digit Code");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        til.setPadding(padding, padding, padding, padding);

        TextInputEditText et = new TextInputEditText(til.getContext());
        // 🚀 TV LINK FIX: Allow both Letters and Numbers (Alphanumeric)
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        et.setLetterSpacing(0.2f);
        et.setFilters(new InputFilter[] { new InputFilter.LengthFilter(8), new InputFilter.AllCaps() });
        til.addView(et);

        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Link Smart TV")
                .setMessage("Enter the code shown on your TV screen to sign in instantly.")
                .setView(til)
                .setPositiveButton("Link Device", (dialog, which) -> {
                    // 🚀 ROBUST INPUT: Remove all spaces/dashes and convert to UPPERCASE
                    String rawInput = et.getText() != null ? et.getText().toString().trim() : "";
                    String cleanCode = rawInput.replaceAll("\\s+", "").replaceAll("-", "").toUpperCase(Locale.US);
                    
                    if (cleanCode.length() == 6) {
                        performTvLinking(cleanCode);
                    } else {
                        Toast.makeText(getContext(), "Please enter the 6-character code", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performTvLinking(String code) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Please sign in on your phone first", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> linkData = new HashMap<>();
        linkData.put("uid", user.getUid());
        linkData.put("userName", user.getDisplayName() != null ? user.getDisplayName() : "NollyBox Fan");
        linkData.put("email", user.getEmail());
        linkData.put("status", "linked");

        // 🚀 SMART SYNC: Use update() to ensure we only link to an active TV pairing session
        FirebaseFirestore.getInstance().collection("pairing_codes").document(code)
                .update(linkData)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "TV Linked Successfully!", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TV_LINK", "Linking failed for code: " + code, e);
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Invalid or expired code. Please check the TV screen.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyEntranceAnimations(View root) {
        View cardImg = root.findViewById(R.id.card_profile_img);
        View tvName = root.findViewById(R.id.tv_profile_name);
        View tvEmail = root.findViewById(R.id.tv_profile_email);
        View cardActions = root.findViewById(R.id.card_actions);
        View btnLogout = root.findViewById(R.id.btn_logout_profile);

        View[] views = {cardImg, tvName, tvEmail, cardActions, btnLogout};

        for (int i = 0; i < views.length; i++) {
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
}