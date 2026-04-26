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

public class SignUpActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etFullName, etEmail, etPassword, etConfirmPassword;
    private ImageButton btnTogglePassword, btnToggleConfirmPassword;
    private Button btnSignUp;
    private TextView tvSignIn, tvError;
    private ProgressBar progressBar;

    private SupabaseClient supabaseClient;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        supabaseClient = SupabaseClient.getInstance(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        btnToggleConfirmPassword = findViewById(R.id.btn_toggle_confirm_password);
        btnSignUp = findViewById(R.id.btn_sign_up);
        tvSignIn = findViewById(R.id.tv_sign_in);
        tvError = findViewById(R.id.tv_error);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            togglePasswordVisibility(etPassword, btnTogglePassword, isPasswordVisible);
        });

        btnToggleConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            togglePasswordVisibility(etConfirmPassword, btnToggleConfirmPassword, isConfirmPasswordVisible);
        });

        btnSignUp.setOnClickListener(v -> attemptSignUp());

        tvSignIn.setOnClickListener(v -> {
            finish(); // Go back to login
        });
    }

    private void togglePasswordVisibility(EditText editText, ImageButton toggleButton, boolean visible) {
        if (visible) {
            editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            toggleButton.setImageResource(R.drawable.ic_visibility);
        } else {
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            toggleButton.setImageResource(R.drawable.ic_visibility_off);
        }
        // Keep cursor at end
        editText.setSelection(editText.getText().length());
    }

    private void attemptSignUp() {
        hideError();

        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        // Validate inputs
        if (fullName.isEmpty()) {
            showError(getString(R.string.error_name_required));
            return;
        }

        if (fullName.length() < 2) {
            showError(getString(R.string.error_name_too_short));
            return;
        }

        // Validate name (only characters and spaces allowed)
        if (!fullName.matches("^[a-zA-Z\\s]*$")) {
            showError(getString(R.string.error_name_invalid));
            return;
        }

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

        if (password.length() < 6) {
            showError(getString(R.string.error_password_too_short));
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError(getString(R.string.error_passwords_dont_match));
            return;
        }

        // Show loading
        setLoading(true);

        // Attempt sign up
        supabaseClient.signUp(email, password, fullName, new SupabaseClient.AuthCallback() {
            @Override
            public void onSuccess() {
                mainHandler.post(() -> {
                    setLoading(false);
                    Toast.makeText(SignUpActivity.this, R.string.signup_success, Toast.LENGTH_LONG).show();
                    navigateToMain();
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
        btnSignUp.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSignUp.setText(loading ? "" : getString(R.string.create_account));
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
    }

    private void navigateToMain() {
        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
