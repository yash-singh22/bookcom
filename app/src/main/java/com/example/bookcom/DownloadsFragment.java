package com.example.bookcom;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
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
import com.example.bookcom.data.DownloadManager;

import java.util.ArrayList;
import java.util.List;

public class DownloadsFragment extends Fragment implements BookAdapter.OnBookClickListener {

    private RecyclerView rvDownloads;
    private BookAdapter adapter;
    private List<Book> downloadedBooks;
    private LinearLayout llEmptyState;
    private TextView tvSubtitle;
    private DownloadManager downloadManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_downloads, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        downloadManager = DownloadManager.getInstance(requireContext());
        
        rvDownloads = view.findViewById(R.id.rv_downloads);
        llEmptyState = view.findViewById(R.id.ll_empty_state);
        tvSubtitle = view.findViewById(R.id.tv_subtitle);
        
        rvDownloads.setLayoutManager(new GridLayoutManager(getContext(), 2));
        downloadedBooks = new ArrayList<>();
        adapter = new BookAdapter(downloadedBooks, this);
        adapter.setShowDeleteButton(true); // Enable delete button for this fragment
        rvDownloads.setAdapter(adapter);
        
        loadDownloads();
    }

    private void loadDownloads() {
        List<BookModel> models = downloadManager.getDownloadedBooks();
        downloadedBooks.clear();
        
        if (models.isEmpty()) {
            llEmptyState.setVisibility(View.VISIBLE);
            rvDownloads.setVisibility(View.GONE);
            tvSubtitle.setText("0 books downloaded");
        } else {
            llEmptyState.setVisibility(View.GONE);
            rvDownloads.setVisibility(View.VISIBLE);
            tvSubtitle.setText(models.size() + " books downloaded");
            
            for (int i = 0; i < models.size(); i++) {
                BookModel model = models.get(i);
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
                downloadedBooks.add(book);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private int getPlaceholderDrawable(String category) {
        if (category == null) return R.drawable.placeholder_book_1;
        switch (category.toLowerCase()) {
            case "fiction": return R.drawable.placeholder_book_1;
            case "self-help": return R.drawable.placeholder_book_2;
            case "technology": return R.drawable.placeholder_book_3;
            case "biography": return R.drawable.placeholder_book_4;
            default: return R.drawable.placeholder_book_1;
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
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_IS_FAVORITE, book.isFavorite());
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(Book book, int position) {
        // Toggle locally or handle via repository if online
    }

    @Override
    public void onDeleteClick(Book book, int position) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Download")
                .setMessage("Are you sure you want to delete this book from your device?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    downloadManager.deleteDownload(book.getSupabaseId());
                    downloadedBooks.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, downloadedBooks.size());
                    
                    if (downloadedBooks.isEmpty()) {
                        llEmptyState.setVisibility(View.VISIBLE);
                        rvDownloads.setVisibility(View.GONE);
                        tvSubtitle.setText("0 books downloaded");
                    } else {
                        tvSubtitle.setText(downloadedBooks.size() + " books downloaded");
                    }
                    
                    Toast.makeText(getContext(), "Download deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
