package com.example.bookcom;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookcom.adapter.SelectableBookAdapter;
import com.example.bookcom.data.BookModel;
import com.example.bookcom.data.SupabaseRepository;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class AddBooksToLibraryActivity extends AppCompatActivity {

    public static final String EXTRA_LIBRARY_ID = "library_id";
    public static final String EXTRA_LIBRARY_NAME = "library_name";

    private RecyclerView rvBooksToAdd;
    private SelectableBookAdapter adapter;
    private List<Book> allBooks;
    private SupabaseRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String libraryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_books_to_library);

        repository = SupabaseRepository.getInstance(this);
        libraryId = getIntent().getStringExtra(EXTRA_LIBRARY_ID);
        String libraryName = getIntent().getStringExtra(EXTRA_LIBRARY_NAME);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Add to " + libraryName);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        rvBooksToAdd = findViewById(R.id.rv_books_to_add);
        rvBooksToAdd.setLayoutManager(new LinearLayoutManager(this));

        allBooks = new ArrayList<>();
        adapter = new SelectableBookAdapter(allBooks);
        rvBooksToAdd.setAdapter(adapter);

        loadAllBooks();

        Button btnAddSelected = findViewById(R.id.btn_add_selected_books);
        btnAddSelected.setOnClickListener(v -> addSelectedBooksToLibrary());
    }

    private void loadAllBooks() {
        repository.getBooks(new SupabaseRepository.DataCallback<List<BookModel>>() {
            @Override
            public void onSuccess(List<BookModel> bookModels) {
                mainHandler.post(() -> {
                    allBooks.clear();
                    for (int i = 0; i < bookModels.size(); i++) {
                        BookModel model = bookModels.get(i);
                        Book book = new Book(i, model.getTitle(), model.getAuthor(), model.getCategory(), R.drawable.placeholder_book_1);
                        book.setSupabaseId(model.getId());
                        book.setCoverUrl(model.getCoverUrl());
                        book.setDescription(model.getDescription());
                        book.setFileUrl(model.getFileUrl());
                        book.setFavorite(model.isFavorite());
                        allBooks.add(book);
                    }
                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> Toast.makeText(AddBooksToLibraryActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void addSelectedBooksToLibrary() {
        List<String> selectedBookIds = adapter.getSelectedBookIds();
        if (selectedBookIds.isEmpty()) {
            Toast.makeText(this, "No books selected", Toast.LENGTH_SHORT).show();
            return;
        }

        repository.addBooksToLibrary(libraryId, selectedBookIds, new SupabaseRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                mainHandler.post(() -> {
                    Toast.makeText(AddBooksToLibraryActivity.this, "Books added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> Toast.makeText(AddBooksToLibraryActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
