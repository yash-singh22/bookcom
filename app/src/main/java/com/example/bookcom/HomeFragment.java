package com.example.bookcom;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookcom.data.BookModel;
import com.example.bookcom.data.SupabaseRepository;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements BookAdapter.OnBookClickListener {

    private static final String TAG = "HomeFragment";
    private static final String ARG_CATEGORY = "category";
    
    private RecyclerView rvBooks;
    private BookAdapter bookAdapter;
    private List<Book> bookList;
    private TextView tvSubtitle;
    private TextView tvBooksCount;
    private ImageButton btnGridView;
    private ImageButton btnListView;
    private boolean isGridView = true;
    
    private String category;
    private SupabaseRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }
    
    public static HomeFragment newInstance(String category) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            category = getArguments().getString(ARG_CATEGORY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        repository = SupabaseRepository.getInstance(requireContext());
        
        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        loadBooks();
    }

    private void initViews(View view) {
        rvBooks = view.findViewById(R.id.rv_books);
        tvSubtitle = view.findViewById(R.id.tv_subtitle);
        tvBooksCount = view.findViewById(R.id.tv_books_count);
        btnGridView = view.findViewById(R.id.btn_grid_view);
        btnListView = view.findViewById(R.id.btn_list_view);
        
        if (category != null) {
            TextView tvTitle = view.findViewById(R.id.tv_title);
            if (tvTitle != null) {
                tvTitle.setText(category);
            }
        }
    }

    private void setupRecyclerView() {
        bookList = new ArrayList<>();
        
        // Setup RecyclerView with Grid Layout (responsive columns)
        int spanCount = getSpanCount();
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
        rvBooks.setLayoutManager(layoutManager);
        
        bookAdapter = new BookAdapter(bookList, this);
        rvBooks.setAdapter(bookAdapter);
    }
    
    private void loadBooks() {
        Log.d(TAG, "loadBooks: Requesting books from repository... Category: " + category);
        
        SupabaseRepository.DataCallback<List<BookModel>> callback = new SupabaseRepository.DataCallback<List<BookModel>>() {
            @Override
            public void onSuccess(List<BookModel> books) {
                Log.d(TAG, "onSuccess: Received " + (books != null ? books.size() : 0) + " books from server.");
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        Log.w(TAG, "onSuccess: Fragment not added, skipping UI update.");
                        return;
                    }
                    
                    bookList.clear();
                    
                    if (books != null) {
                        // Convert BookModel to Book for adapter
                        for (int i = 0; i < books.size(); i++) {
                            BookModel model = books.get(i);
                            Book book = new Book(
                                    i + 1,
                                    model.getTitle(),
                                    model.getAuthor(),
                                    model.getCategory(),
                                    getPlaceholderDrawable(model.getCategory())
                            );
                            book.setSupabaseId(model.getId());
                            book.setFavorite(model.isFavorite());
                            book.setDescription(model.getDescription());
                            book.setFileUrl(model.getFileUrl());
                            book.setCoverUrl(model.getCoverUrl());
                            book.setPageCount(model.getPageCount());
                            bookList.add(book);
                        }
                    } else {
                        Log.w(TAG, "onSuccess: Received null book list from repository.");
                    }
                    
                    Log.d(TAG, "onSuccess: Updating adapter with " + bookList.size() + " books.");
                    bookAdapter.notifyDataSetChanged();
                    updateBookCounts();
                });
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "onError: Failed to load books: " + message);
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Error loading books: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        };

        if (category == null) {
            repository.getBooks(callback);
        } else {
            repository.getBooksByCategory(category, callback);
        }
    }
    
    private int getPlaceholderDrawable(String category) {
        if (category == null) return R.drawable.placeholder_book_1;
        
        switch (category.toLowerCase()) {
            case "fiction":
            case "fantasy":
            case "mystery":
            case "romance":
                return R.drawable.placeholder_book_1;
            case "self-help":
            case "business":
            case "psychology":
            case "biography":
            case "memoir":
                return R.drawable.placeholder_book_2;
            case "technology":
            case "tech":
            case "science":
                return R.drawable.placeholder_book_3;
            case "academic":
            case "textbook":
            case "history":
                return R.drawable.placeholder_book_4;
            default:
                return R.drawable.placeholder_book_1;
        }
    }
    
    public void refreshBooks() {
        loadBooks();
    }

    /**
     * Search books by title or author
     * @param query Search query string
     */
    public void searchBooks(String query) {
        if (query == null || query.trim().isEmpty()) {
            // If search is empty, reload current view
            loadBooks();
            return;
        }

        // Search books from Supabase
        repository.searchBooks(query.trim(), new SupabaseRepository.DataCallback<List<BookModel>>() {
            @Override
            public void onSuccess(List<BookModel> books) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    
                    bookList.clear();
                    
                    if (books != null) {
                        // If we are in a category view, filter search results by that category
                        for (int i = 0; i < books.size(); i++) {
                            BookModel model = books.get(i);
                            
                            if (category != null && !category.equals(model.getCategory())) {
                                continue;
                            }

                            Book book = new Book(
                                    i + 1,
                                    model.getTitle(),
                                    model.getAuthor(),
                                    model.getCategory(),
                                    getPlaceholderDrawable(model.getCategory())
                            );
                            book.setSupabaseId(model.getId());
                            book.setFavorite(model.isFavorite());
                            book.setDescription(model.getDescription());
                            book.setFileUrl(model.getFileUrl());
                            book.setCoverUrl(model.getCoverUrl());
                            book.setPageCount(model.getPageCount());
                            bookList.add(book);
                        }
                    }
                    
                    bookAdapter.notifyDataSetChanged();
                    updateBookCounts();
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Search failed: " + message, Toast.LENGTH_SHORT).show();
                    // If search fails, reload current view
                    loadBooks();
                });
            }
        });
    }

    /**
     * Calculate span count based on screen orientation
     * Portrait: 2 columns for grid, 1 for list
     * Landscape: 4 columns for grid, 2 for list
     */
    private int getSpanCount() {
        if (!isAdded()) return 2;
        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        if (isGridView) {
            return isLandscape ? 4 : 2;
        } else {
            return isLandscape ? 2 : 1;
        }
    }

    private void updateBookCounts() {
        if (!isAdded()) return;
        int count = bookList.size();
        if (category != null) {
            tvSubtitle.setText(String.format("Discover %d books in %s", count, category));
        } else {
            tvSubtitle.setText(String.format(getString(R.string.subtitle_discover_manage), count));
        }
        tvBooksCount.setText(String.format(getString(R.string.books_found), count));
    }

    private void setupClickListeners() {
        // Grid view button
        btnGridView.setOnClickListener(v -> {
            isGridView = true;
            btnGridView.setBackgroundResource(R.drawable.bg_category_tag);
            btnListView.setBackgroundResource(android.R.color.transparent);
            int spanCount = getSpanCount();
            GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
            rvBooks.setLayoutManager(layoutManager);
        });

        // List view button
        btnListView.setOnClickListener(v -> {
            isGridView = false;
            btnListView.setBackgroundResource(R.drawable.bg_category_tag);
            btnGridView.setBackgroundResource(android.R.color.transparent);
            int spanCount = getSpanCount();
            GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
            rvBooks.setLayoutManager(layoutManager);
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Update the grid span count when orientation changes
        if (rvBooks != null) {
            int spanCount = getSpanCount();
            GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
            rvBooks.setLayoutManager(layoutManager);
        }
    }

    @Override
    public void onReadClick(Book book) {
        // Open Book Detail Activity
        Intent intent = new Intent(getContext(), BookDetailActivity.class);
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
        // Toggle the favorite status in the database
        repository.toggleFavorite(book.getSupabaseId(), !book.isFavorite(), 
            new SupabaseRepository.DataCallback<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        book.setFavorite(!book.isFavorite());
                        bookAdapter.notifyItemChanged(position);
                        
                        String message = book.isFavorite() ? 
                            "Added to favorites: " + book.getTitle() : 
                            "Removed from favorites: " + book.getTitle();
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String message) {
                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        Toast.makeText(getContext(), "Error updating favorite: " + message, Toast.LENGTH_SHORT).show();
                    });
                }
            });
    }
}
