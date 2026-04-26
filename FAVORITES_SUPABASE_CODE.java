/**
 * FAVORITES SUPABASE CODE
 * Complete solution for fetching and managing favorite books
 */

// =====================================================
// 1. IMPROVED getFavoriteBooks() METHOD
// =====================================================
public void getFavoriteBooks(DataCallback<List<BookModel>> callback) {
    String userId = authClient.getUserId();
    if (userId == null) {
        callback.onError("User not logged in");
        return;
    }

    // REST API Query for Supabase
    // Filters: user_id matches AND is_favorite is true
    String url = SUPABASE_URL + REST_ENDPOINT + "/books?user_id=eq." + userId + 
                "&is_favorite=eq.true&order=created_at.desc";
    
    Log.d(TAG, "Requesting favorites from: " + url);
    Log.d(TAG, "User ID: " + userId);

    Request request = getAuthenticatedRequestBuilder(url)
            .get()
            .build();

    httpClient.newCall(request).enqueue(new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            Log.e(TAG, "getFavoriteBooks network error", e);
            callback.onError("Network error: " + e.getMessage());
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            String responseBody = response.body() != null ? response.body().string() : "";
            
            Log.d(TAG, "Favorites Response Code: " + response.code());
            Log.d(TAG, "Favorites Response: " + responseBody);
            
            if (response.isSuccessful()) {
                try {
                    Type listType = new TypeToken<ArrayList<BookModel>>(){}.getType();
                    List<BookModel> books = gson.fromJson(responseBody, listType);
                    
                    if (books == null) {
                        books = new ArrayList<>();
                    }
                    
                    Log.d(TAG, "Successfully fetched " + books.size() + " favorite books");
                    callback.onSuccess(books);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse favorites JSON", e);
                    callback.onError("Failed to parse favorites: " + e.getMessage());
                }
            } else {
                Log.e(TAG, "Supabase returned error: " + response.code() + " - " + responseBody);
                callback.onError("Error " + response.code() + ": " + responseBody);
            }
        }
    });
}

// =====================================================
// 2. ALTERNATIVE: Get User's Favorite Books with Details
// =====================================================
public void getFavoriteBooksWithDetails(DataCallback<List<BookModel>> callback) {
    String userId = authClient.getUserId();
    if (userId == null) {
        callback.onError("User not logged in");
        return;
    }

    // Fetch with specific columns for optimization
    String url = SUPABASE_URL + REST_ENDPOINT + "/books?" +
            "select=id,user_id,title,author,description,cover_url,file_url,category," +
            "page_count,current_page,is_favorite,file_size,file_name,created_at,updated_at&" +
            "user_id=eq." + userId + "&" +
            "is_favorite=eq.true&" +
            "order=created_at.desc";

    Request request = getAuthenticatedRequestBuilder(url).get().build();

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
                    Type listType = new TypeToken<ArrayList<BookModel>>(){}.getType();
                    List<BookModel> books = gson.fromJson(responseBody, listType);
                    callback.onSuccess(books != null ? books : new ArrayList<>());
                } catch (Exception e) {
                    callback.onError("Failed to parse favorites: " + e.getMessage());
                }
            } else {
                callback.onError("Failed to fetch favorites: " + response.code());
            }
        }
    });
}

// =====================================================
// 3. IMPROVED toggleFavorite() METHOD
// =====================================================
public void toggleFavorite(String bookId, boolean isFavorite, DataCallback<Void> callback) {
    if (bookId == null || bookId.isEmpty()) {
        callback.onError("Book ID is null or empty");
        return;
    }

    JsonObject json = new JsonObject();
    json.addProperty("is_favorite", isFavorite);

    RequestBody body = RequestBody.create(gson.toJson(json), JSON);

    String url = SUPABASE_URL + REST_ENDPOINT + "/books?id=eq." + bookId;
    
    Log.d(TAG, "Toggling favorite for book: " + bookId + " to " + isFavorite);

    Request request = getAuthenticatedRequestBuilder(url)
            .patch(body)
            .build();

    httpClient.newCall(request).enqueue(new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            Log.e(TAG, "toggleFavorite network error", e);
            callback.onError("Network error: " + e.getMessage());
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            String responseBody = response.body() != null ? response.body().string() : "";
            
            Log.d(TAG, "Toggle response code: " + response.code());
            Log.d(TAG, "Toggle response: " + responseBody);
            
            if (response.isSuccessful()) {
                callback.onSuccess(null);
            } else {
                callback.onError("Failed to update favorite: " + response.code() + " - " + responseBody);
            }
        }
    });
}

// =====================================================
// 4. NEW: Check Favorite Status
// =====================================================
public void checkIfFavorite(String bookId, DataCallback<Boolean> callback) {
    String url = SUPABASE_URL + REST_ENDPOINT + "/books?id=eq." + bookId + 
                "&select=is_favorite";

    Request request = getAuthenticatedRequestBuilder(url).get().build();

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
                    JsonArray array = gson.fromJson(responseBody, JsonArray.class);
                    if (array.size() > 0) {
                        boolean isFavorite = array.get(0).getAsJsonObject()
                                .get("is_favorite").getAsBoolean();
                        callback.onSuccess(isFavorite);
                    } else {
                        callback.onSuccess(false);
                    }
                } catch (Exception e) {
                    callback.onError("Failed to parse response");
                }
            } else {
                callback.onError("Failed to check favorite: " + response.code());
            }
        }
    });
}

// =====================================================
// 5. NEW: Get Favorite Count for User
// =====================================================
public void getFavoriteCount(DataCallback<Integer> callback) {
    String userId = authClient.getUserId();
    if (userId == null) {
        callback.onError("User not logged in");
        return;
    }

    String url = SUPABASE_URL + REST_ENDPOINT + "/books?user_id=eq." + userId + 
                "&is_favorite=eq.true&select=id";

    Request request = getAuthenticatedRequestBuilder(url).get().build();

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
                    JsonArray array = gson.fromJson(responseBody, JsonArray.class);
                    callback.onSuccess(array.size());
                } catch (Exception e) {
                    callback.onError("Failed to parse count");
                }
            } else {
                callback.onError("Failed to get count: " + response.code());
            }
        }
    });
}

// =====================================================
// 6. DEBUGGING METHOD
// =====================================================
public void debugFavorites() {
    String userId = authClient.getUserId();
    String accessToken = authClient.getAccessToken();
    
    Log.d(TAG, "=== FAVORITES DEBUG INFO ===");
    Log.d(TAG, "User ID: " + (userId != null ? userId : "NULL"));
    Log.d(TAG, "Access Token: " + (accessToken != null ? "Present" : "NULL"));
    Log.d(TAG, "Supabase URL: " + SUPABASE_URL);
    Log.d(TAG, "API Key: " + SUPABASE_ANON_KEY.substring(0, 20) + "...");
    Log.d(TAG, "============================");
}

// =====================================================
// 7. SQL SCHEMA FOR BOOKS TABLE (Supabase)
// =====================================================
/*
-- Run this SQL in your Supabase SQL Editor to ensure the table structure is correct

CREATE TABLE IF NOT EXISTS books (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    library_id UUID REFERENCES libraries(id) ON DELETE SET NULL,
    title TEXT NOT NULL,
    author TEXT,
    description TEXT,
    category TEXT,
    cover_url TEXT,
    file_url TEXT,
    file_name TEXT,
    file_size BIGINT DEFAULT 0,
    page_count INTEGER DEFAULT 0,
    current_page INTEGER DEFAULT 0,
    is_favorite BOOLEAN DEFAULT false,
    last_read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Create indexes for better query performance
CREATE INDEX idx_books_user_id ON books(user_id);
CREATE INDEX idx_books_is_favorite ON books(is_favorite);
CREATE INDEX idx_books_user_favorite ON books(user_id, is_favorite);
CREATE INDEX idx_books_library_id ON books(library_id);

-- Enable RLS (Row Level Security)
ALTER TABLE books ENABLE ROW LEVEL SECURITY;

-- Allow users to read their own books
CREATE POLICY "Users can read their own books"
ON books FOR SELECT
USING (auth.uid() = user_id);

-- Allow users to insert their own books
CREATE POLICY "Users can insert their own books"
ON books FOR INSERT
WITH CHECK (auth.uid() = user_id);

-- Allow users to update their own books
CREATE POLICY "Users can update their own books"
ON books FOR UPDATE
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

-- Allow users to delete their own books
CREATE POLICY "Users can delete their own books"
ON books FOR DELETE
USING (auth.uid() = user_id);

-- Optional: Allow public read access if needed
CREATE POLICY "Allow public read on specific books"
ON books FOR SELECT
USING (true);
*/

// =====================================================
// 8. COMMON REST API QUERIES FOR FAVORITES
// =====================================================
/*
QUERY 1: Get all favorite books for current user
GET /rest/v1/books?user_id=eq.{userId}&is_favorite=eq.true&order=created_at.desc

QUERY 2: Get favorite books with specific columns
GET /rest/v1/books?select=id,title,author,cover_url&user_id=eq.{userId}&is_favorite=eq.true

QUERY 3: Toggle favorite status
PATCH /rest/v1/books?id=eq.{bookId}
Body: {"is_favorite": true}

QUERY 4: Get favorite count
GET /rest/v1/books?user_id=eq.{userId}&is_favorite=eq.true&select=count()

QUERY 5: Get favorites sorted by recently added
GET /rest/v1/books?user_id=eq.{userId}&is_favorite=eq.true&order=created_at.desc&limit=20

QUERY 6: Search in favorite books
GET /rest/v1/books?user_id=eq.{userId}&is_favorite=eq.true&or=(title.ilike.*search*,author.ilike.*search*)
*/

// =====================================================
// 9. HEADERS REQUIRED FOR AUTHENTICATED REQUESTS
// =====================================================
/*
Headers needed for all Supabase authenticated requests:
- apikey: {SUPABASE_ANON_KEY}
- Authorization: Bearer {accessToken}
- Content-Type: application/json

These are already handled by getAuthenticatedRequestBuilder() method
*/

