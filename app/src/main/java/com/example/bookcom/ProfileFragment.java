package com.example.bookcom;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.bookcom.auth.LoginActivity;
import com.example.bookcom.auth.SupabaseClient;

public class ProfileFragment extends Fragment {

    private TextView tvUserName, tvUserEmail;
    private Button btnLogout;
    private SwitchCompat switchDarkMode;
    private LinearLayout llAbout;
    private SupabaseClient supabaseClient;
    private SharedPreferences sharedPreferences;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final String PREF_DARK_MODE = "dark_mode";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        supabaseClient = SupabaseClient.getInstance(requireContext());
        sharedPreferences = requireContext().getSharedPreferences("app_prefs", requireContext().MODE_PRIVATE);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserEmail = view.findViewById(R.id.tv_user_email);
        btnLogout = view.findViewById(R.id.btn_logout);
        switchDarkMode = view.findViewById(R.id.switch_dark_mode);
        llAbout = view.findViewById(R.id.ll_about);

        // Set user info
        tvUserName.setText(supabaseClient.getUserName());
        tvUserEmail.setText(supabaseClient.getUserEmail());

        // Setup Dark Mode Toggle
        boolean isDarkModeEnabled = sharedPreferences.getBoolean(PREF_DARK_MODE, false);
        switchDarkMode.setChecked(isDarkModeEnabled);
        
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(PREF_DARK_MODE, isChecked).apply();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            Toast.makeText(requireContext(), isChecked ? "Dark mode enabled" : "Light mode enabled", Toast.LENGTH_SHORT).show();
        });

        // About click
        llAbout.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "BookCom - Book Reading App v1.0", Toast.LENGTH_SHORT).show();
        });

        // Handle logout
        btnLogout.setOnClickListener(v -> {
            supabaseClient.signOut(new SupabaseClient.AuthCallback() {
                @Override
                public void onSuccess() {
                    mainHandler.post(() -> {
                        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(requireActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    });
                }

                @Override
                public void onError(String message) {
                    mainHandler.post(() -> {
                        Toast.makeText(requireContext(), "Logout failed: " + message, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
    }
}