package com.example.bookcom;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.LibraryViewHolder> {

    private List<Library> libraries;
    private OnLibraryActionsListener listener;

    public interface OnLibraryActionsListener {
        void onLibraryClick(Library library);
        void onDeleteLibrary(Library library);
    }

    public LibraryAdapter(List<Library> libraries, OnLibraryActionsListener listener) {
        this.libraries = libraries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LibraryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_library_card, parent, false);
        return new LibraryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LibraryViewHolder holder, int position) {
        Library library = libraries.get(position);
        holder.bind(library);
    }

    @Override
    public int getItemCount() {
        return libraries.size();
    }

    class LibraryViewHolder extends RecyclerView.ViewHolder {
        View viewLibraryColor;
        TextView tvLibraryName;
        TextView tvBookCount;
        ImageButton btnDeleteLibrary;

        public LibraryViewHolder(@NonNull View itemView) {
            super(itemView);
            viewLibraryColor = itemView.findViewById(R.id.view_library_color);
            tvLibraryName = itemView.findViewById(R.id.tv_library_name);
            tvBookCount = itemView.findViewById(R.id.tv_book_count);
            btnDeleteLibrary = itemView.findViewById(R.id.btn_delete_library);
        }

        void bind(final Library library) {
            tvLibraryName.setText(library.getName());
            tvBookCount.setText(String.format("%d books", library.getBookCount()));

            try {
                if (viewLibraryColor.getBackground() != null) {
                    viewLibraryColor.getBackground().setColorFilter(Color.parseColor(library.getColor()), PorterDuff.Mode.SRC_ATOP);
                }
            } catch (Exception e) {
                // Set a default color if parsing fails
                if (viewLibraryColor.getBackground() != null) {
                    viewLibraryColor.getBackground().setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_ATOP);
                }
            }

            itemView.setOnClickListener(v -> listener.onLibraryClick(library));
            btnDeleteLibrary.setOnClickListener(v -> listener.onDeleteLibrary(library));
        }
    }
}
