package com.example.bookcom.data;

import android.content.Context;
import android.util.Log;

import com.example.bookcom.auth.SupabaseClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadManager {
    private static final String TAG = "DownloadManager";
    private static final String DOWNLOADED_BOOKS_FILE_PREFIX = "downloaded_books_";
    private static DownloadManager instance;
    private final Context context;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final SupabaseClient supabaseClient;

    private DownloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.supabaseClient = SupabaseClient.getInstance(context);
    }

    public static synchronized DownloadManager getInstance(Context context) {
        if (instance == null) {
            instance = new DownloadManager(context);
        }
        return instance;
    }

    private String getUserId() {
        String userId = supabaseClient.getUserId();
        return userId != null ? userId : "anonymous";
    }

    private String getMetadataFileName() {
        return DOWNLOADED_BOOKS_FILE_PREFIX + getUserId() + ".json";
    }

    public void downloadBook(BookModel book, DownloadCallback callback) {
        if (isBookDownloaded(book.getId())) {
            callback.onSuccess(getLocalFile(book.getId()));
            return;
        }

        Request request = new Request.Builder().url(book.getFileUrl()).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("Failed to download file: " + response.code());
                    return;
                }

                File file = getLocalFile(book.getId());
                try (InputStream is = response.body().byteStream();
                     FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                    saveBookMetadata(book);
                    callback.onSuccess(file);
                } catch (IOException e) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    public boolean isBookDownloaded(String bookId) {
        return getLocalFile(bookId).exists();
    }

    public File getLocalFile(String bookId) {
        // Create user-specific directory for downloads
        File dir = new File(context.getFilesDir(), "downloads/" + getUserId());
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, bookId + ".pdf");
    }

    private void saveBookMetadata(BookModel book) {
        List<BookModel> downloadedBooks = getDownloadedBooks();
        // Avoid duplicates
        for (BookModel b : downloadedBooks) {
            if (b.getId().equals(book.getId())) return;
        }
        downloadedBooks.add(book);
        String json = gson.toJson(downloadedBooks);
        String fileName = getMetadataFileName();
        try (FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            fos.write(json.getBytes());
        } catch (IOException e) {
            Log.e(TAG, "Error saving metadata", e);
        }
    }

    public List<BookModel> getDownloadedBooks() {
        String fileName = getMetadataFileName();
        File file = new File(context.getFilesDir(), fileName);
        if (!file.exists()) return new ArrayList<>();

        try (InputStream is = context.openFileInput(fileName)) {
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            String json = new String(buffer);
            Type listType = new TypeToken<ArrayList<BookModel>>() {}.getType();
            List<BookModel> books = gson.fromJson(json, listType);
            return books != null ? books : new ArrayList<>();
        } catch (IOException e) {
            Log.e(TAG, "Error reading metadata", e);
            return new ArrayList<>();
        }
    }

    public void deleteDownload(String bookId) {
        File file = getLocalFile(bookId);
        if (file.exists()) file.delete();

        List<BookModel> downloadedBooks = getDownloadedBooks();
        downloadedBooks.removeIf(b -> b.getId().equals(bookId));
        String json = gson.toJson(downloadedBooks);
        String fileName = getMetadataFileName();
        try (FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            fos.write(json.getBytes());
        } catch (IOException e) {
            Log.e(TAG, "Error updating metadata", e);
        }
    }

    public interface DownloadCallback {
        void onSuccess(File file);
        void onError(String message);
    }
}
