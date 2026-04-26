package com.example.bookcom;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

    private List<Book> books;
    private OnBookClickListener listener;
    private boolean showDeleteButton = false;

    public interface OnBookClickListener {
        void onReadClick(Book book);
        void onFavoriteClick(Book book, int position);
        default void onDeleteClick(Book book, int position) {}
    }

    public BookAdapter(List<Book> books, OnBookClickListener listener) {
        this.books = books;
        this.listener = listener;
    }

    public void setShowDeleteButton(boolean show) {
        this.showDeleteButton = show;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book_card, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);
        holder.bind(book, position);
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    public void updateBooks(List<Book> newBooks) {
        this.books = newBooks;
        notifyDataSetChanged();
    }

    class BookViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBookCover;
        TextView tvCategory;
        TextView tvBookTitle;
        TextView tvBookAuthor;
        LinearLayout btnRead;
        FrameLayout btnFavorite;
        ImageView ivFavorite;
        ImageButton btnDelete;

        BookViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBookCover = itemView.findViewById(R.id.iv_book_cover);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvBookTitle = itemView.findViewById(R.id.tv_book_title);
            tvBookAuthor = itemView.findViewById(R.id.tv_book_author);
            btnRead = itemView.findViewById(R.id.btn_read);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);
            ivFavorite = itemView.findViewById(R.id.iv_favorite);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        void bind(Book book, int position) {
            // Set book cover
            if (book.getCoverUrl() != null && !book.getCoverUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(book.getCoverUrl())
                        .into(ivBookCover);
            } else {
                ivBookCover.setImageResource(book.getCoverResId());
            }

            // Set category tag
            tvCategory.setText(book.getCategory());
            
            // Set book title
            tvBookTitle.setText(book.getTitle());
            
            // Set book author
            tvBookAuthor.setText(book.getAuthor());

            // Set favorite icon state
            if (book.isFavorite()) {
                ivFavorite.setImageResource(R.drawable.ic_favorite_filled);
            } else {
                ivFavorite.setImageResource(R.drawable.ic_favorite_outline);
            }

            // Show/Hide delete button
            if (showDeleteButton) {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteClick(book, position);
                    }
                });
            } else {
                btnDelete.setVisibility(View.GONE);
            }

            // Read button click
            btnRead.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReadClick(book);
                }
            });

            // Favorite button click
            btnFavorite.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFavoriteClick(book, position);
                }
            });
        }
    }
}
