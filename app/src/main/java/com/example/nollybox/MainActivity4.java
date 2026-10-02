package com.example.nollybox;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity4 extends AppCompatActivity {

    private HomeFragment homeFragment;
    private TrendingFragment trendingFragment;
    private DownloadsFragment downloadsFragment;
    private ProfileFragment profileFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main4);

        // 🚀 Set the Toolbar FIRST to avoid crashes
        setSupportActionBar(findViewById(R.id.toolbar));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinatorLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0); // 🚀 Immersive: No top padding
            return insets;
        });

        // 🚀 Custom Search Bar Action
        findViewById(R.id.ll_search_bar_top).setOnClickListener(v -> {
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                MenuItem searchItem = toolbar.getMenu().findItem(R.id.action_search);
                if (searchItem != null) {
                    searchItem.expandActionView();
                }
            }
        });

        initFragments();
        setupBottomNavigation();
        setupCategoryTabs();

        // 🚀 Professional Refresh Action
        View btnRefresh = findViewById(R.id.iv_action_top);
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> refreshApp());
        }
    }

    private void refreshApp() {
        View btnRefresh = findViewById(R.id.iv_action_top);
        if (btnRefresh != null) {
            // 🚀 Advanced INFINITE Rotation Animation
            RotateAnimation rotate = new RotateAnimation(
                    0, 360,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );
            rotate.setDuration(800);
            rotate.setRepeatCount(Animation.INFINITE);
            rotate.setInterpolator(new LinearInterpolator());
            btnRefresh.startAnimation(rotate);
            btnRefresh.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            btnRefresh.setEnabled(false); // Prevent multiple clicks
        }

        Runnable onComplete = () -> {
            if (btnRefresh != null) {
                btnRefresh.clearAnimation();
                btnRefresh.setEnabled(true);
            }
            Toast.makeText(this, R.string.experience_refreshed, Toast.LENGTH_SHORT).show();
        };

        // Advanced Refresh: Re-fetch data for active fragments
        if (activeFragment instanceof HomeFragment) {
            ((HomeFragment) activeFragment).refreshData(onComplete);
        } else if (activeFragment instanceof TrendingFragment) {
            ((TrendingFragment) activeFragment).refreshData(onComplete);
        } else if (activeFragment instanceof DownloadsFragment) {
            ((DownloadsFragment) activeFragment).refreshData();
            onComplete.run();
        } else {
            onComplete.run();
        }
    }

    private void setupCategoryTabs() {
        int[] tabIds = {R.id.tab_fightzone, R.id.tab_trending, R.id.tab_movie, R.id.tab_tv, R.id.tab_midnight, R.id.tab_anime};
        
        View.OnClickListener listener = v -> {
            if (v instanceof TextView) {
                String category = ((TextView) v).getText().toString();
                
                // 🚀 TV COMPATIBILITY: Let selectors handle highlighting
                for (int id : tabIds) {
                    TextView tv = findViewById(id);
                    if (tv != null) {
                        tv.setSelected(false);
                    }
                }
                v.setSelected(true);

                if (activeFragment instanceof HomeFragment) {
                    ((HomeFragment) activeFragment).filterByCategory(category);
                } else if (activeFragment instanceof TrendingFragment) {
                    ((TrendingFragment) activeFragment).filterByCategory(category);
                }
            }
        };

        for (int id : tabIds) {
            View v = findViewById(id);
            if (v != null) v.setOnClickListener(listener);
        }
    }

    private void initFragments() {
        // 🚀 Robust Fragment Initialization: Check for existing instances after recreation
        homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag("home");
        trendingFragment = (TrendingFragment) getSupportFragmentManager().findFragmentByTag("trending");
        downloadsFragment = (DownloadsFragment) getSupportFragmentManager().findFragmentByTag("downloads");
        profileFragment = (ProfileFragment) getSupportFragmentManager().findFragmentByTag("profile");

        if (homeFragment == null) homeFragment = new HomeFragment();
        if (trendingFragment == null) trendingFragment = new TrendingFragment();
        if (downloadsFragment == null) downloadsFragment = new DownloadsFragment();
        if (profileFragment == null) profileFragment = new ProfileFragment();

        activeFragment = homeFragment;
        
        // 🚀 Robust Reconstruction: Manage Fragment visibility safely
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        
        if (!homeFragment.isAdded()) {
            transaction.add(R.id.fragment_container, homeFragment, "home");
        } else {
            transaction.show(homeFragment);
        }

        Fragment[] others = {trendingFragment, downloadsFragment, profileFragment};
        for (Fragment f : others) {
            if (f != null && f.isAdded()) {
                transaction.hide(f);
            }
        }

        transaction.commit();
        getSupportFragmentManager().executePendingTransactions();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                switchFragment(homeFragment, "home");
                return true;
            } else if (id == R.id.nav_trending) {
                switchFragment(trendingFragment, "trending");
                return true;
            } else if (id == R.id.nav_downloads) {
                switchFragment(downloadsFragment, "downloads");
                return true;
            } else if (id == R.id.nav_profile) {
                switchFragment(profileFragment, "profile");
                return true;
            }
            return false;
        });
    }

    private void switchFragment(Fragment fragment, String tag) {
        if (fragment == null || activeFragment == fragment) return;

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        
        // 🚀 CRITICAL FIX: Only hide activeFragment if it is actually added and attached
        if (activeFragment != null && activeFragment.isAdded()) {
            transaction.hide(activeFragment);
        }

        if (!fragment.isAdded()) {
            transaction.add(R.id.fragment_container, fragment, tag);
        } else {
            transaction.show(fragment);
        }
        
        transaction.commit();
        activeFragment = fragment;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        
        // 🚀 Toggle Toolbar visibility when searching
        final Toolbar toolbar = findViewById(R.id.toolbar);
        final View headerContainer = findViewById(R.id.ll_header_container);
        if (searchItem != null && toolbar != null) {
            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                    toolbar.setVisibility(View.VISIBLE);
                    if (headerContainer != null) headerContainer.setVisibility(View.GONE);
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                    toolbar.setVisibility(View.GONE);
                    if (headerContainer != null) headerContainer.setVisibility(View.VISIBLE);
                    return true;
                }
            });
        }

        if (searchItem != null) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setQueryHint("Search Nollywood...");
                
                // 🚀 Fix text visibility: Set text and hint colors for SearchAutoComplete
                EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
                if (searchEditText != null) {
                    searchEditText.setTextColor(Color.WHITE);
                    searchEditText.setHintTextColor(Color.GRAY);
                }

                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        // 🚀 CLOUD SENSITIVITY: Focused strictly on searching your Firestore database
                        // Removing YouTube background sync to prioritize your own catalog speed
                        if (activeFragment instanceof HomeFragment) {
                            ((HomeFragment) activeFragment).searchMovies(query);
                        } else if (activeFragment instanceof TrendingFragment) {
                            ((TrendingFragment) activeFragment).searchMovies(query);
                        }
                        searchView.clearFocus();
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        // 🚀 LIVE SENSITIVITY: Filtering the database in real-time as you type
                        if (activeFragment instanceof HomeFragment) {
                            ((HomeFragment) activeFragment).searchMovies(newText);
                        } else if (activeFragment instanceof TrendingFragment) {
                            ((TrendingFragment) activeFragment).searchMovies(newText);
                        }
                        return true;
                    }
                });
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_filter) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}