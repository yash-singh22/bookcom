package com.example.bookcom.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bookcom.Book;
import com.example.bookcom.R;

import java.util.ArrayList;
import java.util.List;

public class SelectableBookAdapter extends RecyclerView.Adapter<SelectableBookAdapter.SelectableBookViewHolder> {

    private List<Book> books;
    private List<String> selectedBookIds = new ArrayList<>();

    public SelectableBookAdapter(List<Book> books) {
        this.books = books;
    }

    @NonNull
    @Override
    public SelectableBookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selectable_book, parent, false);
        return new SelectableBookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SelectableBookViewHolder holder, int position) {
        Book book = books.get(position);
        holder.bind(book);
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    public List<String> getSelectedBookIds() {
        return selectedBookIds;
    }

    class SelectableBookViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBookCover;
        TextView tvBookTitle;
        TextView tvBookAuthor;
        CheckBox cbSelectBook;

        public SelectableBookViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBookCover = itemView.findViewById(R.id.iv_book_cover);
            tvBookTitle = itemView.findViewById(R.id.tv_book_title);
            tvBookAuthor = itemView.findViewById(R.id.tv_book_author);
            cbSelectBook = itemView.findViewById(R.id.cb_select_book);
        }

        void bind(final Book book) {
            tvBookTitle.setText(book.getTitle());
            tvBookAuthor.setText(book.getAuthor());
            
            if (book.getCoverUrl() != null && !book.getCoverUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(book.getCoverUrl())
                        .placeholder(R.drawable.placeholder_book_1)
                        .into(ivBookCover);
            } else {
                ivBookCover.setImageResource(book.getCoverResId());
            }

            cbSelectBook.setOnCheckedChangeListener(null); 
            cbSelectBook.setChecked(selectedBookIds.contains(book.getSupabaseId()));

            cbSelectBook.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!selectedBookIds.contains(book.getSupabaseId())) {
                        selectedBookIds.add(book.getSupabaseId());
                    }
                } else {
                    selectedBookIds.remove(book.getSupabaseId());
                }
            });

            itemView.setOnClickListener(v -> cbSelectBook.toggle());
        }
    }
}
