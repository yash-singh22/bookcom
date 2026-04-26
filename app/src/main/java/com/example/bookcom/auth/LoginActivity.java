package com.example.bookcom.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bookcom.MainActivity;
import com.example.bookcom.R;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private ImageButton btnTogglePassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvSignUp, tvError;
    private ProgressBar progressBar;

    private SupabaseClient supabaseClient;
    private boolean isPasswordVisible = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        supabaseClient = SupabaseClient.getInstance(this);
        initViews();

        // Check if we have a stored session (refresh token)
        if (supabaseClient.getRefreshToken() != null) {
            attemptAutoLogin();
        } else {
            setupListeners();
        }
    }

    private void attemptAutoLogin() {
        setLoading(true);
        supabaseClient.refreshToken(new SupabaseClient.AuthCallback() {
            @Override
            public void onSuccess() {
                mainHandler.post(() -> {
                    setLoading(false);
                    navigateToMain(false);
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    setLoading(false);
                    // If it's a network error, we can still allow the user to enter 
                    // the app to access offline downloads if they were already logged in.
                    if (message.contains("Network error")) {
                        if (supabaseClient.getUserId() != null) {
                            Toast.makeText(LoginActivity.this, "Offline mode: No internet connection", Toast.LENGTH_SHORT).show();
                            navigateToMain(false);
                        } else {
                            setupListeners();
                            showError("No internet connection and no saved session.");
                        }
                    } else {
                        // If refresh fails because the session is invalid/expired, stay on login
                        setupListeners();
                        if (message.contains("expired") || message.contains("invalid")) {
                            Toast.makeText(LoginActivity.this, "Session expired, please sign in again", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        btnLogin = findViewById(R.id.btn_login);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvSignUp = findViewById(R.id.tv_sign_up);
        tvError = findViewById(R.id.tv_error);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupListeners() {
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        
        btnLogin.setOnClickListener(v -> attemptLogin());
        
        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
        
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        
        if (isPasswordVisible) {
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            btnTogglePassword.setImageResource(R.drawable.ic_visibility);
        } else {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
        }
        
        // Keep cursor at end
        etPassword.setSelection(etPassword.getText().length());
    }

    private void attemptLogin() {
        hideError();
        
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        // Validate inputs
        if (email.isEmpty()) {
            showError(getString(R.string.error_email_required));
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.error_invalid_email));
            return;
        }

        if (password.isEmpty()) {
            showError(getString(R.string.error_password_required));
            return;
        }

        // Show loading
        setLoading(true);

        // Attempt sign in
        supabaseClient.signIn(email, password, new SupabaseClient.AuthCallback() {
            @Override
            public void onSuccess() {
                mainHandler.post(() -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                    navigateToMain(true); // Force admin check on login
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    setLoading(false);
                    showError(message);
                });
            }
        });
    }

    private void handleForgotPassword() {
        String email = etEmail.getText().toString().trim();
        
        if (email.isEmpty()) {
            showError(getString(R.string.error_enter_email_reset));
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.error_invalid_email));
            return;
        }

        setLoading(true);

        supabaseClient.resetPassword(email, new SupabaseClient.AuthCallback() {
            @Override
            public void onSuccess() {
                mainHandler.post(() -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, R.string.password_reset_sent, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    setLoading(false);
                    showError(message);
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setText(loading ? "" : getString(R.string.sign_in));
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
    }

    private void navigateToMain(boolean forceAdminCheck) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        if (forceAdminCheck) {
            intent.putExtra("force_admin_check", true);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
