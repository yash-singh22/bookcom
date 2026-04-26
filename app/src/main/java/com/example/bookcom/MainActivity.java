package com.example.bookcom;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.bookcom.auth.LoginActivity;
import com.example.bookcom.auth.SupabaseClient;
import com.example.bookcom.ui.UploadBookDialog;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    
    private ImageButton btnMenu;
    private ImageButton btnUpload;
    private FrameLayout btnProfile;
    private EditText etSearch;

    // Navigation items
    private TextView navHome, navLibrary, navCategories, navDownloads;
    private TextView navHelp, navSettings, navSignout;
    
    // Category items
    private TextView navRomance, navBiography, navScience, navTextbook, navMystery;
    
    private SupabaseClient supabaseClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // Upload dialog
    private UploadBookDialog uploadBookDialog;
    private ActivityResultLauncher<String> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply dark mode preference
        SharedPreferences sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean isDarkModeEnabled = sharedPreferences.getBoolean("dark_mode", false);
        if (isDarkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Setup file picker launcher
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::onFilePicked
        );
        
        // Apply window insets only to the main content, not the entire drawer
        View mainContent = findViewById(R.id.main_content);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (mainContent != null) {
                mainContent.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            }
            return WindowInsetsCompat.CONSUMED;
        });

        supabaseClient = SupabaseClient.getInstance(this);

        initViews();
        setupAdminFeatures();
        setupNavigation();
        setupClickListeners();
        setupBackPressedHandler();
        
        // Load Home fragment by default
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }

    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        btnMenu = findViewById(R.id.btn_menu);
        btnUpload = findViewById(R.id.btn_upload);
        btnProfile = findViewById(R.id.btn_profile);
        etSearch = findViewById(R.id.et_search);

        // Navigation items
        navHome = findViewById(R.id.nav_home);
        navLibrary = findViewById(R.id.nav_library);
        navCategories = findViewById(R.id.nav_categories);
        navDownloads = findViewById(R.id.nav_downloads);
        navHelp = findViewById(R.id.nav_help);
        navSettings = findViewById(R.id.nav_settings);
        navSignout = findViewById(R.id.nav_signout);
        
        // Category items
        navRomance = findViewById(R.id.nav_cat_romance);
        navBiography = findViewById(R.id.nav_cat_biography);
        navScience = findViewById(R.id.nav_cat_science);
        navTextbook = findViewById(R.id.nav_cat_textbook);
        navMystery = findViewById(R.id.nav_cat_mystery);
    }

    private void setupAdminFeatures() {
        String userEmail = supabaseClient.getUserEmail();
        // List of admin emails
        List<String> adminEmails = Arrays.asList("admin@book.com");
        boolean isAdmin = userEmail != null && adminEmails.contains(userEmail);

        if (isAdmin) {
            if (btnUpload != null) btnUpload.setVisibility(View.VISIBLE);
        } else {
            if (btnUpload != null) btnUpload.setVisibility(View.GONE);
        }
    }

    private void setupNavigation() {
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                resetNavigationSelection();
                v.setBackgroundResource(R.drawable.bg_nav_item_selected);
                loadFragment(new HomeFragment());
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        if (navLibrary != null) {
            navLibrary.setOnClickListener(v -> {
                resetNavigationSelection();
                v.setBackgroundResource(R.drawable.bg_nav_item_selected);
                loadFragment(new MyLibrariesFragment());
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        if (navCategories != null) {
            navCategories.setOnClickListener(v -> {
                resetNavigationSelection();
                v.setBackgroundResource(R.drawable.bg_nav_item_selected);
                loadFragment(new FavoritesFragment());
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        if (navDownloads != null) {
            navDownloads.setOnClickListener(v -> {
                resetNavigationSelection();
                v.setBackgroundResource(R.drawable.bg_nav_item_selected);
                loadFragment(new DownloadsFragment());
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        // Setup Category clicks with null checks
        if (navRomance != null) navRomance.setOnClickListener(v -> loadCategoryFragment("Romance"));
        if (navBiography != null) navBiography.setOnClickListener(v -> loadCategoryFragment("Biography"));
        if (navScience != null) navScience.setOnClickListener(v -> loadCategoryFragment("Science"));
        if (navTextbook != null) navTextbook.setOnClickListener(v -> loadCategoryFragment("Textbook"));
        if (navMystery != null) navMystery.setOnClickListener(v -> loadCategoryFragment("Mystery"));

        if (navHelp != null) {
            navHelp.setOnClickListener(v -> {
                resetNavigationSelection();
                v.setBackgroundResource(R.drawable.bg_nav_item_selected);
                loadFragment(new HelpSupportFragment());
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                resetNavigationSelection();
                v.setBackgroundResource(R.drawable.bg_nav_item_selected);
                loadFragment(new ProfileFragment());
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        if (navSignout != null) {
            navSignout.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                performSignOut();
            });
        }
    }
    
    private void loadCategoryFragment(String category) {
        resetNavigationSelection();
        // Highlight the item that was clicked (based on ID)
        int viewId = getResources().getIdentifier("nav_cat_" + category.toLowerCase(), "id", getPackageName());
        View catView = findViewById(viewId);
        if (catView != null) {
            catView.setBackgroundResource(R.drawable.bg_nav_item_selected);
        }
        
        loadFragment(HomeFragment.newInstance(category));
        if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
    }
    
    private void performSignOut() {
        supabaseClient.signOut(new SupabaseClient.AuthCallback() {
            @Override
            public void onSuccess() {
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, R.string.logout_success, Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    // Sign out locally even if server call fails
                    navigateToLogin();
                });
            }
        });
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void resetNavigationSelection() {
        if (navHome != null) navHome.setBackgroundResource(R.drawable.bg_nav_item);
        if (navLibrary != null) navLibrary.setBackgroundResource(R.drawable.bg_nav_item);
        if (navCategories != null) navCategories.setBackgroundResource(R.drawable.bg_nav_item);
        if (navDownloads != null) navDownloads.setBackgroundResource(R.drawable.bg_nav_item);
        if (navHelp != null) navHelp.setBackgroundResource(R.drawable.bg_nav_item);
        if (navSettings != null) navSettings.setBackgroundResource(R.drawable.bg_nav_item);
        if (navSignout != null) navSignout.setBackgroundResource(R.drawable.bg_nav_item);
        
        // Reset categories
        if (navRomance != null) navRomance.setBackgroundResource(R.drawable.bg_nav_item);
        if (navBiography != null) navBiography.setBackgroundResource(R.drawable.bg_nav_item);
        if (navScience != null) navScience.setBackgroundResource(R.drawable.bg_nav_item);
        if (navTextbook != null) navTextbook.setBackgroundResource(R.drawable.bg_nav_item);
        if (navMystery != null) navMystery.setBackgroundResource(R.drawable.bg_nav_item);
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void setupClickListeners() {
        // Menu button - open drawer
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    } else {
                        drawerLayout.openDrawer(GravityCompat.START);
                    }
                }
            });
        }

        // Upload button
        if (btnUpload != null) {
            btnUpload.setOnClickListener(v -> showUploadDialog());
        }

        // Profile button
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                loadFragment(new ProfileFragment());
            });
        }
        
        // Search functionality with debounce
        setupSearchListener();
    }
    
    private void setupSearchListener() {
        if (etSearch == null) return;

        Handler searchHandler = new Handler(Looper.getMainLooper());
        final Runnable[] searchRunnable = new Runnable[1];
        
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Remove any pending search requests
                if (searchRunnable[0] != null) {
                    searchHandler.removeCallbacks(searchRunnable[0]);
                }
                
                // Create new search request with 300ms debounce
                searchRunnable[0] = () -> {
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (currentFragment instanceof HomeFragment) {
                        ((HomeFragment) currentFragment).searchBooks(s.toString());
                    }
                };
                
                searchHandler.postDelayed(searchRunnable[0], 300);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
    
    private void showUploadDialog() {
        if (uploadBookDialog == null) {
            uploadBookDialog = new UploadBookDialog(this);
            uploadBookDialog.setFilePickerLauncher(filePickerLauncher);
            uploadBookDialog.setOnBookUploadedListener(book -> {
                Toast.makeText(this, R.string.book_uploaded, Toast.LENGTH_SHORT).show();
                // Refresh the current fragment to show new book
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (currentFragment instanceof HomeFragment) {
                    ((HomeFragment) currentFragment).refreshBooks();
                }
            });
        }
        uploadBookDialog.show();
    }
    
    private void onFilePicked(Uri uri) {
        if (uri != null && uploadBookDialog != null) {
            uploadBookDialog.onFileSelected(uri);
        }
    }
}