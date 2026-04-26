package com.example.bookcom.ui;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;

import com.example.bookcom.R;
import com.example.bookcom.data.BookModel;
import com.example.bookcom.data.SupabaseRepository;

public class UploadBookDialog {

    private final Context context;
    private final Dialog dialog;
    private final SupabaseRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Views
    private LinearLayout dropZone;
    private LinearLayout selectedFileInfo;
    private TextView tvDropTitle, tvDropSubtitle, tvFileName, tvError;
    private Button btnSelectFile, btnCancel, btnUpload;
    private ImageButton btnClose, btnRemoveFile;
    private EditText etTitle, etAuthor, etDescription;
    private Spinner spinnerGenre;
    private ProgressBar progressBar;

    // State
    private Uri selectedFileUri;
    private String selectedFileName;
    private ActivityResultLauncher<String> filePickerLauncher;
    private OnBookUploadedListener listener;

    public interface OnBookUploadedListener {
        void onBookUploaded(BookModel book);
    }

    public UploadBookDialog(Context context) {
        this.context = context;
        this.repository = SupabaseRepository.getInstance(context);
        this.dialog = new Dialog(context);
        setupDialog();
    }

    public void setFilePickerLauncher(ActivityResultLauncher<String> launcher) {
        this.filePickerLauncher = launcher;
    }

    public void setOnBookUploadedListener(OnBookUploadedListener listener) {
        this.listener = listener;
    }

    private void setupDialog() {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_upload_book, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        initViews(view);
        setupGenreSpinner();
        setupListeners();
    }

    private void initViews(View view) {
        dropZone = view.findViewById(R.id.drop_zone);
        selectedFileInfo = view.findViewById(R.id.selected_file_info);
        tvDropTitle = view.findViewById(R.id.tv_drop_title);
        tvDropSubtitle = view.findViewById(R.id.tv_drop_subtitle);
        tvFileName = view.findViewById(R.id.tv_file_name);
        tvError = view.findViewById(R.id.tv_error);
        btnSelectFile = view.findViewById(R.id.btn_select_file);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnUpload = view.findViewById(R.id.btn_upload);
        btnClose = view.findViewById(R.id.btn_close);
        btnRemoveFile = view.findViewById(R.id.btn_remove_file);
        etTitle = view.findViewById(R.id.et_title);
        etAuthor = view.findViewById(R.id.et_author);
        etDescription = view.findViewById(R.id.et_description);
        spinnerGenre = view.findViewById(R.id.spinner_genre);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupGenreSpinner() {
        String[] genres = context.getResources().getStringArray(R.array.book_genres);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                R.layout.item_spinner,
                genres
        );
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerGenre.setAdapter(adapter);
    }

    private void setupListeners() {
        btnClose.setOnClickListener(v -> dismiss());
        btnCancel.setOnClickListener(v -> dismiss());

        btnSelectFile.setOnClickListener(v -> {
            if (filePickerLauncher != null) {
                filePickerLauncher.launch("application/pdf");
            }
        });

        dropZone.setOnClickListener(v -> {
            if (filePickerLauncher != null) {
                filePickerLauncher.launch("application/pdf");
            }
        });

        btnRemoveFile.setOnClickListener(v -> removeSelectedFile());

        btnUpload.setOnClickListener(v -> uploadBook());
    }

    public void onFileSelected(Uri uri) {
        if (uri == null) return;

        selectedFileUri = uri;
        selectedFileName = getFileName(uri);

        // Update UI to show selected file
        tvDropTitle.setVisibility(View.GONE);
        tvDropSubtitle.setVisibility(View.GONE);
        btnSelectFile.setVisibility(View.GONE);
        selectedFileInfo.setVisibility(View.VISIBLE);
        tvFileName.setText(selectedFileName);

        // Auto-fill title from filename
        if (etTitle.getText().toString().isEmpty()) {
            String titleFromFile = selectedFileName;
            if (titleFromFile.toLowerCase().endsWith(".pdf")) {
                titleFromFile = titleFromFile.substring(0, titleFromFile.length() - 4);
            }
            etTitle.setText(titleFromFile);
        }

        hideError();
    }

    private void removeSelectedFile() {
        selectedFileUri = null;
        selectedFileName = null;

        // Reset UI
        tvDropTitle.setVisibility(View.VISIBLE);
        tvDropSubtitle.setVisibility(View.VISIBLE);
        btnSelectFile.setVisibility(View.VISIBLE);
        selectedFileInfo.setVisibility(View.GONE);
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void uploadBook() {
        hideError();

        String title = etTitle.getText().toString().trim();
        String author = etAuthor.getText().toString().trim();
        String genre = spinnerGenre.getSelectedItem().toString();
        String description = etDescription.getText().toString().trim();

        // Validation
        if (title.isEmpty()) {
            showError(context.getString(R.string.error_title_required));
            return;
        }

        if (genre.equals(context.getString(R.string.select_genre))) {
            showError(context.getString(R.string.error_genre_required));
            return;
        }

        setLoading(true);

        // Create book model
        BookModel book = new BookModel();
        book.setTitle(title);
        book.setAuthor(author.isEmpty() ? "Unknown Author" : author);
        book.setCategory(genre);
        book.setDescription(description);
        book.setFileName(selectedFileName);

        // If file is selected, upload it first
        if (selectedFileUri != null) {
            repository.uploadPdfFile(selectedFileUri, new SupabaseRepository.UploadCallback() {
                @Override
                public void onSuccess(String fileUrl, long fileSize) {
                    book.setFileUrl(fileUrl);
                    book.setFileSize(fileSize);
                    saveBookToDatabase(book);
                }

                @Override
                public void onError(String message) {
                    mainHandler.post(() -> {
                        setLoading(false);
                        showError(message);
                    });
                }
            });
        } else {
            // Save without file
            saveBookToDatabase(book);
        }
    }

    private void saveBookToDatabase(BookModel book) {
        repository.createBook(book, new SupabaseRepository.DataCallback<BookModel>() {
            @Override
            public void onSuccess(BookModel createdBook) {
                mainHandler.post(() -> {
                    setLoading(false);
                    if (listener != null) {
                        listener.onBookUploaded(createdBook);
                    }
                    dismiss();
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    setLoading(false);
                    showError(message);
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        btnUpload.setEnabled(!loading);
        btnCancel.setEnabled(!loading);
        btnClose.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        dialog.setCancelable(!loading);
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
    }

    public void show() {
        resetDialog();
        dialog.show();
    }

    public void dismiss() {
        dialog.dismiss();
    }

    private void resetDialog() {
        etTitle.setText("");
        etAuthor.setText("");
        etDescription.setText("");
        spinnerGenre.setSelection(0);
        removeSelectedFile();
        hideError();
        setLoading(false);
    }
}
