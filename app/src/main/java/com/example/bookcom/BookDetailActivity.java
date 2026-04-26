package com.example.bookcom;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.bookcom.data.BookModel;
import com.example.bookcom.data.DownloadManager;
import com.example.bookcom.data.SupabaseRepository;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class BookDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BOOK_ID = "book_id";
    public static final String EXTRA_BOOK_TITLE = "book_title";
    public static final String EXTRA_BOOK_AUTHOR = "book_author";
    public static final String EXTRA_BOOK_CATEGORY = "book_category";
    public static final String EXTRA_BOOK_DESCRIPTION = "book_description";
    public static final String EXTRA_BOOK_FILE_URL = "book_file_url";
    public static final String EXTRA_BOOK_COVER_URL = "book_cover_url";
    public static final String EXTRA_BOOK_PAGE_COUNT = "book_page_count";
    public static final String EXTRA_BOOK_CURRENT_PAGE = "book_current_page";
    public static final String EXTRA_BOOK_IS_FAVORITE = "book_is_favorite";
    public static final String EXTRA_BOOK_COVER_RES = "book_cover_res";

    private ImageView ivBookCover;
    private TextView tvGenreTag, tvYear, tvBookTitle, tvAuthor;
    private TextView tvDescription;
    private Button btnStartReading, btnDownloadPdf, btnAddFavorite;
    private ImageButton btnBack;

    private String bookId;
    private String fileUrl;
    private boolean isFavorite;
    private SupabaseRepository repository;
    private DownloadManager downloadManager;
    private Handler mainHandler;
    private BookModel currentBookModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        repository = SupabaseRepository.getInstance(this);
        downloadManager = DownloadManager.getInstance(this);
        mainHandler = new Handler(Looper.getMainLooper());

        initViews();
        loadBookData();
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        ivBookCover = findViewById(R.id.iv_book_cover);
        tvGenreTag = findViewById(R.id.tv_genre_tag);
        tvYear = findViewById(R.id.tv_year);
        tvBookTitle = findViewById(R.id.tv_book_title);
        tvAuthor = findViewById(R.id.tv_author);
        tvDescription = findViewById(R.id.tv_description);
        btnStartReading = findViewById(R.id.btn_start_reading);
        btnDownloadPdf = findViewById(R.id.btn_download_pdf);
        btnAddFavorite = findViewById(R.id.btn_add_favorite);
    }

    private void loadBookData() {
        Intent intent = getIntent();
        
        bookId = intent.getStringExtra(EXTRA_BOOK_ID);
        String title = intent.getStringExtra(EXTRA_BOOK_TITLE);
        String author = intent.getStringExtra(EXTRA_BOOK_AUTHOR);
        String category = intent.getStringExtra(EXTRA_BOOK_CATEGORY);
        String description = intent.getStringExtra(EXTRA_BOOK_DESCRIPTION);
        fileUrl = intent.getStringExtra(EXTRA_BOOK_FILE_URL);
        String coverUrl = intent.getStringExtra(EXTRA_BOOK_COVER_URL);
        int pageCount = intent.getIntExtra(EXTRA_BOOK_PAGE_COUNT, 0);
        int currentPage = intent.getIntExtra(EXTRA_BOOK_CURRENT_PAGE, 0);
        isFavorite = intent.getBooleanExtra(EXTRA_BOOK_IS_FAVORITE, false);
        int coverRes = intent.getIntExtra(EXTRA_BOOK_COVER_RES, R.drawable.placeholder_book_1);

        currentBookModel = new BookModel();
        currentBookModel.setId(bookId);
        currentBookModel.setTitle(title);
        currentBookModel.setAuthor(author);
        currentBookModel.setCategory(category);
        currentBookModel.setDescription(description);
        currentBookModel.setFileUrl(fileUrl);
        currentBookModel.setCoverUrl(coverUrl);
        currentBookModel.setPageCount(pageCount);
        currentBookModel.setCurrentPage(currentPage);
        currentBookModel.setFavorite(isFavorite);

        // Set book data
        tvBookTitle.setText(title != null ? title : "Unknown Title");
        tvAuthor.setText("by " + (author != null ? author : "Unknown Author"));
        tvGenreTag.setText(category != null ? category : "General");
        
        // Set description
        if (description != null && !description.isEmpty()) {
            tvDescription.setText(description);
        } else {
            tvDescription.setText("No description available for this book.");
        }

        // Set current year
        String currentYear = new SimpleDateFormat("yyyy", Locale.getDefault())
                .format(new java.util.Date());
        tvYear.setText(currentYear);

        // Set cover image
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(this)
                    .load(coverUrl)
                    .into(ivBookCover);
        } else if (coverRes != 0) {
            ivBookCover.setImageResource(coverRes);
        }

        // Update favorite button state
        updateFavoriteButton();
        updateDownloadButtonState();
        
        if (currentPage > 0) {
            btnStartReading.setText("Continue Reading (Page " + (currentPage + 1) + ")");
        }
    }

    private void updateDownloadButtonState() {
        if (downloadManager.isBookDownloaded(bookId)) {
            btnDownloadPdf.setText("Downloaded");
            btnDownloadPdf.setEnabled(false);
        } else {
            btnDownloadPdf.setText(R.string.download_pdf);
            btnDownloadPdf.setEnabled(true);
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnStartReading.setOnClickListener(v -> {
            if (downloadManager.isBookDownloaded(bookId)) {
                openPdfReader(downloadManager.getLocalFile(bookId).getAbsolutePath());
            } else if (fileUrl != null && !fileUrl.isEmpty()) {
                openPdfReader(fileUrl);
            } else {
                Toast.makeText(this, "No PDF file available", Toast.LENGTH_SHORT).show();
            }
        });

        btnDownloadPdf.setOnClickListener(v -> {
            if (fileUrl != null && !fileUrl.isEmpty()) {
                startDownload();
            } else {
                Toast.makeText(this, "No PDF file available", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddFavorite.setOnClickListener(v -> toggleFavorite());
    }

    private void startDownload() {
        btnDownloadPdf.setEnabled(false);
        btnDownloadPdf.setText("Downloading...");
        
        downloadManager.downloadBook(currentBookModel, new DownloadManager.DownloadCallback() {
            @Override
            public void onSuccess(File file) {
                mainHandler.post(() -> {
                    Toast.makeText(BookDetailActivity.this, "Download complete!", Toast.LENGTH_SHORT).show();
                    updateDownloadButtonState();
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    Toast.makeText(BookDetailActivity.this, "Download failed: " + message, Toast.LENGTH_SHORT).show();
                    updateDownloadButtonState();
                });
            }
        });
    }

    private void openPdfReader(String path) {
        // Track the read event
        if (bookId != null) {
            repository.updateLastReadTimestamp(bookId, new SupabaseRepository.DataCallback<Void>() {
                @Override public void onSuccess(Void data) { /* Fire and forget */ }
                @Override public void onError(String message) { /* Log if needed */ }
            });
        }

        Intent intent = new Intent(this, PdfReaderActivity.class);
        intent.putExtra(PdfReaderActivity.EXTRA_PDF_URL, path);
        intent.putExtra(PdfReaderActivity.EXTRA_BOOK_TITLE, tvBookTitle.getText().toString());
        intent.putExtra(PdfReaderActivity.EXTRA_BOOK_ID, bookId);
        intent.putExtra(PdfReaderActivity.EXTRA_CURRENT_PAGE, currentBookModel.getCurrentPage());
        startActivity(intent);
    }

    private void toggleFavorite() {
        Log.d("FAVORITE", "toggleFavorite called - bookId: " + bookId + ", isFav: " + isFavorite);
        
        if (bookId == null) {
            // For sample books without Supabase ID
            Log.w("FAVORITE", "bookId is null, toggling locally only");
            isFavorite = !isFavorite;
            updateFavoriteButton();
            String message = isFavorite ? "Added to favorites" : "Removed from favorites";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("FAVORITE", "Sending toggle request to Supabase - bookId: " + bookId);
        
        repository.toggleFavorite(bookId, !isFavorite, new SupabaseRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d("FAVORITE", "toggleFavorite SUCCESS");
                mainHandler.post(() -> {
                    isFavorite = !isFavorite;
                    updateFavoriteButton();
                    String message = isFavorite ? "Added to favorites" : "Removed from favorites";
                    Toast.makeText(BookDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    Log.d("FAVORITE", "UI Updated - isFavorite now: " + isFavorite);
                });
            }

            @Override
            public void onError(String message) {
                Log.e("FAVORITE", "toggleFavorite ERROR: " + message);
                mainHandler.post(() -> {
                    Toast.makeText(BookDetailActivity.this, "Failed to update favorite: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void updateFavoriteButton() {
        if (isFavorite) {
            btnAddFavorite.setText(R.string.remove_from_favorites);
            btnAddFavorite.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(this, R.drawable.ic_heart_filled), 
                    null, null, null);
        } else {
            btnAddFavorite.setText(R.string.add_to_favorites);
            btnAddFavorite.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(this, R.drawable.ic_heart_outline), 
                    null, null, null);
        }
    }
}
