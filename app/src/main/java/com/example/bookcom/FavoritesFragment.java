package com.example.bookcom;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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

public class FavoritesFragment extends Fragment implements BookAdapter.OnBookClickListener {

    private RecyclerView rvFavorites;
    private BookAdapter bookAdapter;
    private List<Book> favoriteBooks;
    private SupabaseRepository repository;
    private TextView tvEmptyMessage;
    private LinearLayout llEmptyState;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        repository = SupabaseRepository.getInstance(requireContext());

        rvFavorites = view.findViewById(R.id.rv_favorites);
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message);
        llEmptyState = view.findViewById(R.id.ll_empty_state);

        rvFavorites.setLayoutManager(new GridLayoutManager(getContext(), 2));

        favoriteBooks = new ArrayList<>();
        bookAdapter = new BookAdapter(favoriteBooks, this);
        rvFavorites.setAdapter(bookAdapter);

        loadFavoriteBooks();

        return view;
    }

    private void loadFavoriteBooks() {
        Log.d("FAVORITES_FRAG", "loadFavoriteBooks called");
        
        repository.getFavoriteBooks(new SupabaseRepository.DataCallback<List<BookModel>>() {
            @Override
            public void onSuccess(List<BookModel> bookModels) {
                Log.d("FAVORITES_FRAG", "onSuccess - received " + (bookModels != null ? bookModels.size() : 0) + " books");
                
                mainHandler.post(() -> {
                    favoriteBooks.clear();
                    
                    if (bookModels == null || bookModels.isEmpty()) {
                        Log.d("FAVORITES_FRAG", "No favorite books - showing empty state");
                        llEmptyState.setVisibility(View.VISIBLE);
                        rvFavorites.setVisibility(View.GONE);
                    } else {
                        Log.d("FAVORITES_FRAG", "Processing " + bookModels.size() + " favorite books");
                        llEmptyState.setVisibility(View.GONE);
                        rvFavorites.setVisibility(View.VISIBLE);
                        
                        for (int i = 0; i < bookModels.size(); i++) {
                            BookModel model = bookModels.get(i);
                            Log.d("FAVORITES_FRAG", "Book " + i + ": " + model.getTitle() + 
                                  " (id=" + model.getId() + ", fav=" + model.isFavorite() + ")");
                            
                            Book book = new Book(
                                    i,
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
                            favoriteBooks.add(book);
                        }
                    }
                    bookAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(String message) {
                Log.e("FAVORITES_FRAG", "Error loading favorites: " + message);
                mainHandler.post(() -> {
                    Toast.makeText(getContext(), "Error loading favorites: " + message, Toast.LENGTH_LONG).show();
                    llEmptyState.setVisibility(View.VISIBLE);
                    rvFavorites.setVisibility(View.GONE);
                    tvEmptyMessage.setText("Error:\n" + message);
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

    public void refreshFavorites() {
        loadFavoriteBooks();
    }

    @Override
    public void onReadClick(Book book) {
        Intent intent = new Intent(requireActivity(), BookDetailActivity.class);
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
                    bookAdapter.notifyItemChanged(position);
                    loadFavoriteBooks(); // Reload to update list
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    Toast.makeText(getContext(), "Error updating favorite: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
