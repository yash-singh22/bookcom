package com.example.bookcom;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookcom.data.BookModel;
import com.example.bookcom.data.SupabaseRepository;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class LibraryDetailActivity extends AppCompatActivity implements BookAdapter.OnBookClickListener {

    private static final String TAG = "LibraryDetailActivity";
    public static final String EXTRA_LIBRARY_ID = "library_id";
    public static final String EXTRA_LIBRARY_NAME = "library_name";

    private RecyclerView rvLibraryBooks;
    private BookAdapter adapter;
    private List<Book> libraryBooks;
    private SupabaseRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String libraryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_detail);

        repository = SupabaseRepository.getInstance(this);
        libraryId = getIntent().getStringExtra(EXTRA_LIBRARY_ID);
        String libraryName = getIntent().getStringExtra(EXTRA_LIBRARY_NAME);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(libraryName != null ? libraryName : "Library");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvLibraryBooks = findViewById(R.id.rv_library_books);
        rvLibraryBooks.setLayoutManager(new GridLayoutManager(this, 2));

        libraryBooks = new ArrayList<>();
        adapter = new BookAdapter(libraryBooks, this);
        rvLibraryBooks.setAdapter(adapter);

        if (libraryId != null) {
            loadLibraryBooks();
        } else {
            Toast.makeText(this, "Error: Library ID missing", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadLibraryBooks() {
        Log.d(TAG, "Loading books for library: " + libraryId);
        repository.getBooksByLibrary(libraryId, new SupabaseRepository.DataCallback<List<BookModel>>() {
            @Override
            public void onSuccess(List<BookModel> bookModels) {
                mainHandler.post(() -> {
                    libraryBooks.clear();
                    if (bookModels != null) {
                        for (int i = 0; i < bookModels.size(); i++) {
                            BookModel model = bookModels.get(i);
                            Book book = new Book(
                                    i, 
                                    model.getTitle(), 
                                    model.getAuthor(), 
                                    model.getCategory(), 
                                    getPlaceholderDrawable(model.getCategory())
                            );
                            book.setSupabaseId(model.getId());
                            book.setCoverUrl(model.getCoverUrl());
                            book.setDescription(model.getDescription());
                            book.setFileUrl(model.getFileUrl());
                            book.setFavorite(model.isFavorite());
                            book.setPageCount(model.getPageCount());
                            libraryBooks.add(book);
                        }
                    }
                    Log.d(TAG, "Loaded " + libraryBooks.size() + " books.");
                    adapter.notifyDataSetChanged();
                    
                    if (libraryBooks.isEmpty()) {
                        Toast.makeText(LibraryDetailActivity.this, "This library is empty", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    Log.e(TAG, "Error loading books: " + message);
                    Toast.makeText(LibraryDetailActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private int getPlaceholderDrawable(String category) {
        if (category == null) return R.drawable.placeholder_book_1;
        
        switch (category.toLowerCase()) {
            case "fiction":
            case "fantasy":
            case "mystery":
                return R.drawable.placeholder_book_1;
            case "self-help":
            case "business":
            case "psychology":
                return R.drawable.placeholder_book_2;
            case "technology":
            case "tech":
            case "science":
                return R.drawable.placeholder_book_3;
            case "memoir":
            case "biography":
            case "history":
                return R.drawable.placeholder_book_4;
            default:
                return R.drawable.placeholder_book_1;
        }
    }

    @Override
    public void onReadClick(Book book) {
        Intent intent = new Intent(this, BookDetailActivity.class);
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.getSupabaseId());
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_TITLE, book.getTitle());
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_AUTHOR, book.getAuthor());
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_CATEGORY, book.getCategory());
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_DESCRIPTION, book.getDescription());
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_FILE_URL, book.getFileUrl());
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_COVER_URL, book.getCoverUrl());
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_PAGE_COUNT, book.getPageCount());
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_IS_FAVORITE, book.isFavorite());
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_COVER_RES, book.getCoverResId());
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(Book book, int position) {
        repository.toggleFavorite(book.getSupabaseId(), !book.isFavorite(), new SupabaseRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                mainHandler.post(() -> {
                    book.setFavorite(!book.isFavorite());
                    adapter.notifyItemChanged(position);
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> Toast.makeText(LibraryDetailActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
