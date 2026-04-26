package com.example.bookcom.data;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.example.bookcom.BuildConfig;
import com.example.bookcom.auth.SupabaseClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseRepository {

    private static final String TAG = "SupabaseRepo";

    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final String SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY;
    private static final String REST_ENDPOINT = "/rest/v1";
    private static final String STORAGE_ENDPOINT = "/storage/v1";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static SupabaseRepository instance;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final SupabaseClient authClient;
    private final Context context;

    private SupabaseRepository(Context context) {
        this.context = context.getApplicationContext();
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
        authClient = SupabaseClient.getInstance(context);
    }

    public static synchronized SupabaseRepository getInstance(Context context) {
        if (instance == null) {
            instance = new SupabaseRepository(context);
        }
        return instance;
    }

    private Request.Builder getAuthenticatedRequestBuilder(String url) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json");
        
        String accessToken = authClient.getAccessToken();
        if (accessToken != null) {
            builder.addHeader("Authorization", "Bearer " + accessToken);
        }
        
        return builder;
    }

    private void executeWithRetry(Request request, DataCallback<String> callback) {
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (response.code() == 401) {
                    authClient.refreshToken(new SupabaseClient.AuthCallback() {
                        @Override
                        public void onSuccess() {
                            Request newRequest = getAuthenticatedRequestBuilder(request.url().toString())
                                    .method(request.method(), request.body())
                                    .build();
                            httpClient.newCall(newRequest).enqueue(new Callback() {
                                @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
                                @Override public void onResponse(Call call, Response retryResponse) throws IOException {
                                    if (retryResponse.isSuccessful()) callback.onSuccess(retryResponse.body().string());
                                    else callback.onError("Error: " + retryResponse.code());
                                }
                            });
                        }
                        @Override public void onError(String message) { callback.onError("Session expired"); }
                    });
                } else if (response.isSuccessful()) {
                    callback.onSuccess(responseBody);
                } else {
                    callback.onError("Error: " + response.code() + " - " + responseBody);
                }
            }
        });
    }

    // ==================== BOOKS ====================

    public void getBooks(DataCallback<List<BookModel>> callback) {
        String userId = authClient.getUserId();
        // Fetch all books AND include a join to check if they are favorited by THIS user
        String url = SUPABASE_URL + REST_ENDPOINT + "/books?select=*,favorites(user_id)&order=created_at.desc";
        Request request = getAuthenticatedRequestBuilder(url).get().build();

        executeWithRetry(request, new DataCallback<String>() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JsonArray array = gson.fromJson(responseBody, JsonArray.class);
                    List<BookModel> books = new ArrayList<>();
                    for (JsonElement el : array) {
                        JsonObject obj = el.getAsJsonObject();
                        BookModel book = gson.fromJson(obj, BookModel.class);
                        
                        // Check the "favorites" array from the join
                        boolean isFav = false;
                        if (obj.has("favorites")) {
                            JsonArray favs = obj.getAsJsonArray("favorites");
                            for (JsonElement f : favs) {
                                if (f.getAsJsonObject().get("user_id").getAsString().equals(userId)) {
                                    isFav = true;
                                    break;
                                }
                            }
                        }
                        book.setFavorite(isFav);
                        books.add(book);
                    }
                    callback.onSuccess(books);
                } catch (Exception e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
            @Override public void onError(String message) { callback.onError(message); }
        });
    }

    public void getBooksByCategory(String category, DataCallback<List<BookModel>> callback) {
        String userId = authClient.getUserId();
        String url = SUPABASE_URL + REST_ENDPOINT + "/books?select=*,favorites(user_id)&category=eq." + Uri.encode(category) + "&order=created_at.desc";
        Request request = getAuthenticatedRequestBuilder(url).get().build();

        executeWithRetry(request, new DataCallback<String>() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JsonArray array = gson.fromJson(responseBody, JsonArray.class);
                    List<BookModel> books = new ArrayList<>();
                    for (JsonElement el : array) {
                        JsonObject obj = el.getAsJsonObject();
                        BookModel book = gson.fromJson(obj, BookModel.class);

                        boolean isFav = false;
                        if (obj.has("favorites")) {
                            JsonArray favs = obj.getAsJsonArray("favorites");
                            for (JsonElement f : favs) {
                                if (f.getAsJsonObject().get("user_id").getAsString().equals(userId)) {
                                    isFav = true;
                                    break;
                                }
                            }
                        }
                        book.setFavorite(isFav);
                        books.add(book);
                    }
                    callback.onSuccess(books);
                } catch (Exception e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
            @Override public void onError(String message) { callback.onError(message); }
        });
    }

    public void getRecentlyReadBooks(DataCallback<List<BookModel>> callback) {
        String userId = authClient.getUserId();
        if (userId == null) { callback.onError("Not logged in"); return; }

        // Fetch books where last_read_at is not null, join favorites to show star status correctly
        String url = SUPABASE_URL + REST_ENDPOINT + "/books?select=*,favorites(user_id)&last_read_at=is.not.null&user_id=eq." + userId + "&order=last_read_at.desc";
        Request request = getAuthenticatedRequestBuilder(url).get().build();

        executeWithRetry(request, new DataCallback<String>() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JsonArray array = gson.fromJson(responseBody, JsonArray.class);
                    List<BookModel> books = new ArrayList<>();
                    for (JsonElement el : array) {
                        JsonObject obj = el.getAsJsonObject();
                        BookModel book = gson.fromJson(obj, BookModel.class);
                        
                        boolean isFav = false;
                        if (obj.has("favorites")) {
                            JsonArray favs = obj.getAsJsonArray("favorites");
                            for (JsonElement f : favs) {
                                if (f.getAsJsonObject().get("user_id").getAsString().equals(userId)) {
                                    isFav = true;
                                    break;
                                }
                            }
                        }
                        book.setFavorite(isFav);
                        books.add(book);
                    }
                    callback.onSuccess(books);
                } catch (Exception e) {
                    callback.onError("Parse error");
                }
            }
            @Override public void onError(String message) { callback.onError(message); }
        });
    }

    public void getFavoriteBooks(DataCallback<List<BookModel>> callback) {
        String userId = authClient.getUserId();
        if (userId == null) { callback.onError("Not logged in"); return; }

        // Fetch from the favorites table with book details joined
        String url = SUPABASE_URL + REST_ENDPOINT + "/favorites?select=books(*)&user_id=eq." + userId;
        Request request = getAuthenticatedRequestBuilder(url).get().build();

        executeWithRetry(request, new DataCallback<String>() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JsonArray array = gson.fromJson(responseBody, JsonArray.class);
                    List<BookModel> books = new ArrayList<>();
                    for (JsonElement el : array) {
                        JsonObject bookObj = el.getAsJsonObject().getAsJsonObject("books");
                        BookModel book = gson.fromJson(bookObj, BookModel.class);
                        book.setFavorite(true);
                        books.add(book);
                    }
                    callback.onSuccess(books);
                } catch (Exception e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
            @Override public void onError(String message) { callback.onError(message); }
        });
    }

    public void toggleFavorite(String bookId, boolean isFavorite, DataCallback<Void> callback) {
        String userId = authClient.getUserId();
        if (isFavorite) {
            // Add to favorites table
            JsonObject json = new JsonObject();
            json.addProperty("user_id", userId);
            json.addProperty("book_id", bookId);
            Request request = getAuthenticatedRequestBuilder(SUPABASE_URL + REST_ENDPOINT + "/favorites")
                    .post(RequestBody.create(gson.toJson(json), JSON))
                    .build();
            executeWithRetry(request, new DataCallback<String>() {
                @Override public void onSuccess(String data) { callback.onSuccess(null); }
                @Override public void onError(String message) { callback.onError(message); }
            });
        } else {
            // Remove from favorites table
            String url = SUPABASE_URL + REST_ENDPOINT + "/favorites?user_id=eq." + userId + "&book_id=eq." + bookId;
            Request request = getAuthenticatedRequestBuilder(url).delete().build();
            executeWithRetry(request, new DataCallback<String>() {
                @Override public void onSuccess(String data) { callback.onSuccess(null); }
                @Override public void onError(String message) { callback.onError(message); }
            });
        }
    }

    public void searchBooks(String query, DataCallback<List<BookModel>> callback) {
        String userId = authClient.getUserId();
        String encodedQuery = Uri.encode(query);
        String url = SUPABASE_URL + REST_ENDPOINT + "/books?select=*,favorites(user_id)&" +
                "or=(title.ilike.*" + encodedQuery + "*,author.ilike.*" + encodedQuery + "*)" +
                "&order=created_at.desc";
        
        Request request = getAuthenticatedRequestBuilder(url).get().build();

        executeWithRetry(request, new DataCallback<String>() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JsonArray array = gson.fromJson(responseBody, JsonArray.class);
                    List<BookModel> books = new ArrayList<>();
                    for (JsonElement el : array) {
                        JsonObject obj = el.getAsJsonObject();
                        BookModel book = gson.fromJson(obj, BookModel.class);
                        
                        boolean isFav = false;
                        if (obj.has("favorites")) {
                            JsonArray favs = obj.getAsJsonArray("favorites");
                            for (JsonElement f : favs) {
                                if (f.getAsJsonObject().get("user_id").getAsString().equals(userId)) {
                                    isFav = true;
                                    break;
                                }
                            }
                        }
                        book.setFavorite(isFav);
                        books.add(book);
                    }
                    callback.onSuccess(books);
                } catch (Exception e) {
                    callback.onError("Parse error");
                }
            }
            @Override public void onError(String message) { callback.onError(message); }
        });
    }

    public void updateLastReadTimestamp(String bookId, DataCallback<Void> callback) {
        JsonObject json = new JsonObject();
        json.addProperty("last_read_at", "now()");

        RequestBody body = RequestBody.create(gson.toJson(json), JSON);
        Request request = getAuthenticatedRequestBuilder(
                SUPABASE_URL + REST_ENDPOINT + "/books?id=eq." + bookId)
                .patch(body)
                .build();

        executeWithRetry(request, new DataCallback<String>() {
            @Override public void onSuccess(String data) { callback.onSuccess(null); }
            @Override public void onError(String message) { callback.onError(message); }
        });
    }

    public void updateReadingProgress(String bookId, int currentPage, DataCallback<Void> callback) {
        JsonObject json = new JsonObject();
        json.addProperty("current_page", currentPage);
        json.addProperty("last_read_at", "now()");

        RequestBody body = RequestBody.create(gson.toJson(json), JSON);
        Request request = getAuthenticatedRequestBuilder(
                SUPABASE_URL + REST_ENDPOINT + "/books?id=eq." + bookId)
                .patch(body)
                .build();

        executeWithRetry(request, new DataCallback<String>() {
            @Override public void onSuccess(String data) { callback.onSuccess(null); }
            @Override public void onError(String message) { callback.onError(message); }
        });
    }

    // ==================== LIBRARIES ====================

    public void getBooksByLibrary(String libraryId, DataCallback<List<BookModel>> callback) {
        String userId = authClient.getUserId();
        // Updated to include favorites check for library books
        String url = SUPABASE_URL + REST_ENDPOINT + "/library_books?select=books(*,favorites(user_id))&library_id=eq." + libraryId;
        Log.d(TAG, "getBooksByLibrary URL: " + url);
        
        Request request = getAuthenticatedRequestBuilder(url).get().build();

        executeWithRetry(request, new DataCallback<String>() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    Log.d(TAG, "getBooksByLibrary response: " + responseBody);
                    JsonArray array = gson.fromJson(responseBody, JsonArray.class);
                    List<BookModel> books = new ArrayList<>();
                    for (JsonElement el : array) {
                        JsonObject item = el.getAsJsonObject();
                        if (item.has("books") && !item.get("books").isJsonNull()) {
                            JsonObject bookObj = item.getAsJsonObject("books");
                            BookModel book = gson.fromJson(bookObj, BookModel.class);
                            
                            // Check favorite status via join
                            boolean isFav = false;
                            if (bookObj.has("favorites")) {
                                JsonArray favs = bookObj.getAsJsonArray("favorites");
                                for (JsonElement f : favs) {
                                    if (f.getAsJsonObject().get("user_id").getAsString().equals(userId)) {
                                        isFav = true;
                                        break;
                                    }
                                }
                            }
                            book.setFavorite(isFav);
                            books.add(book);
                        }
                    }
                    Log.d(TAG, "Fetched " + books.size() + " books for library " + libraryId);
                    callback.onSuccess(books);
                } catch (Exception e) {
                    Log.e(TAG, "Parse error in getBooksByLibrary: " + e.getMessage());
                    callback.onError("Parse error");
                }
            }
            @Override public void onError(String message) { 
                Log.e(TAG, "Error in getBooksByLibrary: " + message);
                callback.onError(message); 
            }
        });
    }

    public void addBooksToLibrary(String libraryId, List<String> bookIds, DataCallback<Void> callback) {
        JsonArray array = new JsonArray();
        for (String id : bookIds) {
            JsonObject obj = new JsonObject();
            obj.addProperty("library_id", libraryId);
            obj.addProperty("book_id", id);
            array.add(obj);
        }

        String json = gson.toJson(array);
        Log.d(TAG, "Adding books to library. Body: " + json);

        Request request = getAuthenticatedRequestBuilder(SUPABASE_URL + REST_ENDPOINT + "/library_books")
                .post(RequestBody.create(json, JSON))
                .build();

        executeWithRetry(request, new DataCallback<String>() {
            @Override public void onSuccess(String data) { 
                Log.d(TAG, "Successfully added books to library");
                callback.onSuccess(null); 
            }
            @Override public void onError(String message) { 
                Log.e(TAG, "Error adding books to library: " + message);
                callback.onError(message); 
            }
        });
    }

    public void getLibraries(DataCallback<List<LibraryModel>> callback) {
        String userId = authClient.getUserId();
        // Fetch libraries and join with library_books to get counts
        String url = SUPABASE_URL + REST_ENDPOINT + "/libraries?select=*,library_books(count)&user_id=eq." + userId;
        Request request = getAuthenticatedRequestBuilder(url).get().build();

        executeWithRetry(request, new DataCallback<String>() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JsonArray array = gson.fromJson(responseBody, JsonArray.class);
                    List<LibraryModel> libs = new ArrayList<>();
                    for (JsonElement el : array) {
                        JsonObject obj = el.getAsJsonObject();
                        LibraryModel lib = gson.fromJson(obj, LibraryModel.class);
                        if (obj.has("library_books")) {
                            JsonArray counts = obj.getAsJsonArray("library_books");
                            if (counts.size() > 0) {
                                lib.setBookCount(counts.get(0).getAsJsonObject().get("count").getAsInt());
                            }
                        }
                        libs.add(lib);
                    }
                    callback.onSuccess(libs);
                } catch (Exception e) {
                    callback.onError("Parse error");
                }
            }
            @Override public void onError(String message) { callback.onError(message); }
        });
    }
    
    public void createBook(BookModel book, DataCallback<BookModel> callback) {
        book.setUserId(authClient.getUserId());
        JsonObject json = gson.toJsonTree(book).getAsJsonObject();
        RequestBody body = RequestBody.create(gson.toJson(json), JSON);
        Request request = getAuthenticatedRequestBuilder(SUPABASE_URL + REST_ENDPOINT + "/books")
                .addHeader("Prefer", "return=representation").post(body).build();
        executeWithRetry(request, new DataCallback<String>() {
            @Override public void onSuccess(String b) { 
                JsonArray a = gson.fromJson(b, JsonArray.class);
                callback.onSuccess(gson.fromJson(a.get(0), BookModel.class)); 
            }
            @Override public void onError(String m) { callback.onError(m); }
        });
    }

    public void createLibrary(LibraryModel library, DataCallback<LibraryModel> callback) {
        library.setUserId(authClient.getUserId());
        RequestBody body = RequestBody.create(gson.toJson(library), JSON);
        Request request = getAuthenticatedRequestBuilder(SUPABASE_URL + REST_ENDPOINT + "/libraries")
                .addHeader("Prefer", "return=representation").post(body).build();
        executeWithRetry(request, new DataCallback<String>() {
            @Override public void onSuccess(String b) { 
                JsonArray a = gson.fromJson(b, JsonArray.class);
                callback.onSuccess(gson.fromJson(a.get(0), LibraryModel.class)); 
            }
            @Override public void onError(String m) { callback.onError(m); }
        });
    }

    public void deleteLibrary(String libraryId, DataCallback<Void> callback) {
        Request request = getAuthenticatedRequestBuilder(SUPABASE_URL + REST_ENDPOINT + "/libraries?id=eq." + libraryId).delete().build();
        executeWithRetry(request, new DataCallback<String>() {
            @Override public void onSuccess(String d) { callback.onSuccess(null); }
            @Override public void onError(String m) { callback.onError(m); }
        });
    }
    
    public void uploadPdfFile(Uri fileUri, UploadCallback callback) {
        String userId = authClient.getUserId();
        try {
            ContentResolver contentResolver = context.getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(fileUri);
            byte[] fileBytes = new byte[inputStream.available()];
            inputStream.read(fileBytes);
            inputStream.close();
            String fileName = UUID.randomUUID().toString() + ".pdf";
            String filePath = userId + "/" + fileName;
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + STORAGE_ENDPOINT + "/object/books/" + filePath)
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + authClient.getAccessToken())
                    .post(RequestBody.create(fileBytes, MediaType.get("application/pdf"))).build();
            httpClient.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call c, IOException e) { callback.onError(e.getMessage()); }
                @Override public void onResponse(Call c, Response r) throws IOException {
                    if (r.isSuccessful()) callback.onSuccess(SUPABASE_URL + STORAGE_ENDPOINT + "/object/public/books/" + filePath, fileBytes.length);
                    else callback.onError("Upload failed");
                }
            });
        } catch (Exception e) { callback.onError(e.getMessage()); }
    }

    public interface DataCallback<T> { void onSuccess(T data); void onError(String message); }
    public interface UploadCallback { void onSuccess(String fileUrl, long fileSize); void onError(String message); }
}
