package com.example.bookcom.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.bookcom.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Supabase authentication client for Android
 * Handles sign up, sign in, sign out, and session management
 */
public class SupabaseClient {

    // Supabase credentials from BuildConfig (loaded from local.properties)
    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final String SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY;

    private static final String AUTH_ENDPOINT = "/auth/v1";
    private static final String REST_ENDPOINT = "/rest/v1";

    private static final String PREFS_NAME = "supabase_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static SupabaseClient instance;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final SharedPreferences prefs;

    private SupabaseClient(Context context) {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SupabaseClient getInstance(Context context) {
        if (instance == null) {
            instance = new SupabaseClient(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Sign up a new user with email and password
     */
    public void signUp(String email, String password, String fullName, AuthCallback callback) {
        JsonObject data = new JsonObject();
        JsonObject userMetadata = new JsonObject();
        userMetadata.addProperty("full_name", fullName);
        
        data.addProperty("email", email);
        data.addProperty("password", password);
        data.add("data", userMetadata);

        RequestBody body = RequestBody.create(gson.toJson(data), JSON);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + AUTH_ENDPOINT + "/signup")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (response.isSuccessful()) {
                    try {
                        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                        handleAuthResponse(json, fullName);
                        callback.onSuccess();
                    } catch (Exception e) {
                        callback.onError("Failed to parse response: " + e.getMessage());
                    }
                } else {
                    try {
                        JsonObject error = gson.fromJson(responseBody, JsonObject.class);
                        String message = error.has("error_description") 
                                ? error.get("error_description").getAsString()
                                : error.has("msg") 
                                    ? error.get("msg").getAsString()
                                    : "Sign up failed";
                        callback.onError(message);
                    } catch (Exception e) {
                        callback.onError("Sign up failed: " + response.code());
                    }
                }
            }
        });
    }

    /**
     * Sign in with email and password
     */
    public void signIn(String email, String password, AuthCallback callback) {
        JsonObject data = new JsonObject();
        data.addProperty("email", email);
        data.addProperty("password", password);

        RequestBody body = RequestBody.create(gson.toJson(data), JSON);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + AUTH_ENDPOINT + "/token?grant_type=password")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (response.isSuccessful()) {
                    try {
                        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                        handleAuthResponse(json, null);
                        callback.onSuccess();
                    } catch (Exception e) {
                        callback.onError("Failed to parse response: " + e.getMessage());
                    }
                } else {
                    try {
                        JsonObject error = gson.fromJson(responseBody, JsonObject.class);
                        String message = error.has("error_description") 
                                ? error.get("error_description").getAsString()
                                : error.has("msg") 
                                    ? error.get("msg").getAsString()
                                    : "Sign in failed";
                        callback.onError(message);
                    } catch (Exception e) {
                        callback.onError("Invalid email or password");
                    }
                }
            }
        });
    }

    /**
     * Sign out the current user
     */
    public void signOut(AuthCallback callback) {
        String accessToken = getAccessToken();
        
        if (accessToken == null) {
            clearSession();
            callback.onSuccess();
            return;
        }

        Request request = new Request.Builder()
                .url(SUPABASE_URL + AUTH_ENDPOINT + "/logout")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(RequestBody.create("", JSON))
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Clear session even on network error
                clearSession();
                callback.onSuccess();
            }

            @Override
            public void onResponse(Call call, Response response) {
                clearSession();
                callback.onSuccess();
            }
        });
    }

    /**
     * Refresh the access token using the refresh token
     */
    public void refreshToken(AuthCallback callback) {
        String refreshToken = getRefreshToken();
        
        if (refreshToken == null) {
            callback.onError("No refresh token available");
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("refresh_token", refreshToken);

        RequestBody body = RequestBody.create(gson.toJson(data), JSON);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + AUTH_ENDPOINT + "/token?grant_type=refresh_token")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (response.isSuccessful()) {
                    try {
                        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                        handleAuthResponse(json, null);
                        callback.onSuccess();
                    } catch (Exception e) {
                        callback.onError("Failed to refresh token");
                    }
                } else {
                    clearSession();
                    callback.onError("Session expired. Please sign in again.");
                }
            }
        });
    }

    /**
     * Get the current user from Supabase
     */
    public void getCurrentUser(UserCallback callback) {
        String accessToken = getAccessToken();
        
        if (accessToken == null) {
            callback.onError("Not authenticated");
            return;
        }

        Request request = new Request.Builder()
                .url(SUPABASE_URL + AUTH_ENDPOINT + "/user")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (response.isSuccessful()) {
                    try {
                        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                        User user = parseUser(json);
                        callback.onSuccess(user);
                    } catch (Exception e) {
                        callback.onError("Failed to parse user data");
                    }
                } else {
                    callback.onError("Failed to get user data");
                }
            }
        });
    }

    /**
     * Send password reset email
     */
    public void resetPassword(String email, AuthCallback callback) {
        JsonObject data = new JsonObject();
        data.addProperty("email", email);

        RequestBody body = RequestBody.create(gson.toJson(data), JSON);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + AUTH_ENDPOINT + "/recover")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError("Failed to send reset email");
                }
            }
        });
    }

    // Helper methods

    private void handleAuthResponse(JsonObject json, String fullName) {
        String accessToken = json.has("access_token") ? json.get("access_token").getAsString() : null;
        String refreshToken = json.has("refresh_token") ? json.get("refresh_token").getAsString() : null;
        
        JsonObject userObj = json.has("user") ? json.getAsJsonObject("user") : null;
        String userId = null;
        String email = null;
        String name = fullName;
        
        if (userObj != null) {
            userId = userObj.has("id") ? userObj.get("id").getAsString() : null;
            email = userObj.has("email") ? userObj.get("email").getAsString() : null;
            
            if (name == null && userObj.has("user_metadata")) {
                JsonObject metadata = userObj.getAsJsonObject("user_metadata");
                if (metadata.has("full_name")) {
                    name = metadata.get("full_name").getAsString();
                }
            }
        }

        SharedPreferences.Editor editor = prefs.edit();
        if (accessToken != null) editor.putString(KEY_ACCESS_TOKEN, accessToken);
        if (refreshToken != null) editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        if (userId != null) editor.putString(KEY_USER_ID, userId);
        if (email != null) editor.putString(KEY_USER_EMAIL, email);
        if (name != null) editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }

    private User parseUser(JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : null;
        String email = json.has("email") ? json.get("email").getAsString() : null;
        String name = null;
        
        if (json.has("user_metadata")) {
            JsonObject metadata = json.getAsJsonObject("user_metadata");
            if (metadata.has("full_name")) {
                name = metadata.get("full_name").getAsString();
            }
        }
        
        return new User(id, email, name);
    }

    private void clearSession() {
        prefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_USER_NAME)
                .apply();
    }

    // Public getters for session data

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }

    // Callbacks

    public interface AuthCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface UserCallback {
        void onSuccess(User user);
        void onError(String message);
    }

    // User model
    public static class User {
        private final String id;
        private final String email;
        private final String name;

        public User(String id, String email, String name) {
            this.id = id;
            this.email = email;
            this.name = name;
        }

        public String getId() { return id; }
        public String getEmail() { return email; }
        public String getName() { return name; }
    }
}
