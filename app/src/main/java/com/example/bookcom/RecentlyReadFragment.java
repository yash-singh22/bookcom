package com.example.bookcom;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookcom.data.BookModel;
import com.example.bookcom.data.SupabaseRepository;

import java.util.ArrayList;
import java.util.List;

public class RecentlyReadFragment extends Fragment implements BookAdapter.OnBookClickListener {

    private RecyclerView rvRecentlyRead;
    private BookAdapter bookAdapter;
    private List<Book> recentlyReadBooks;
    private SupabaseRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recently_read, container, false);

        repository = SupabaseRepository.getInstance(requireContext());

        rvRecentlyRead = view.findViewById(R.id.rv_recently_read);
        rvRecentlyRead.setLayoutManager(new LinearLayoutManager(getContext()));

        recentlyReadBooks = new ArrayList<>();
        bookAdapter = new BookAdapter(recentlyReadBooks, this);
        rvRecentlyRead.setAdapter(bookAdapter);

        loadRecentlyReadBooks();

        return view;
    }

    private void loadRecentlyReadBooks() {
        repository.getRecentlyReadBooks(new SupabaseRepository.DataCallback<List<BookModel>>() {
            @Override
            public void onSuccess(List<BookModel> bookModels) {
                mainHandler.post(() -> {
                    recentlyReadBooks.clear();
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
                        book.setFavorite(model.isFavorite());
                        book.setDescription(model.getDescription());
                        book.setFileUrl(model.getFileUrl());
                        book.setCoverUrl(model.getCoverUrl());
                        book.setPageCount(model.getPageCount());
                        recentlyReadBooks.add(book);
                    }
                    bookAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
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
        repository.toggleFavorite(book.getSupabaseId(), !book.isFavorite(), new SupabaseRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                mainHandler.post(() -> {
                    book.setFavorite(!book.isFavorite());
                    bookAdapter.notifyItemChanged(position);
                    String message = book.isFavorite() ? "Added to favorites" : "Removed from favorites";
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
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
