package com.example.bookcom;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookcom.data.SupabaseRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfReaderActivity extends AppCompatActivity {

    private static final String TAG = "PdfReaderActivity";
    public static final String EXTRA_PDF_URL = "pdf_url";
    public static final String EXTRA_BOOK_TITLE = "book_title";
    public static final String EXTRA_BOOK_ID = "book_id";
    public static final String EXTRA_CURRENT_PAGE = "current_page";

    private RecyclerView rvPdfPages;
    private ImageButton btnBack;
    private TextView tvTitle;
    private ProgressBar progressBar;

    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private int totalPages = 0;
    private int initialPage = 0;
    private String bookId;

    private ExecutorService executor;
    private Handler mainHandler;
    private File pdfFile;
    private SupabaseRepository repository;
    private LinearLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_reader);

        repository = SupabaseRepository.getInstance(this);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        bookId = getIntent().getStringExtra(EXTRA_BOOK_ID);
        initialPage = getIntent().getIntExtra(EXTRA_CURRENT_PAGE, 0);

        initViews();
        setupClickListeners();
        loadPdf();
    }

    private void initViews() {
        rvPdfPages = findViewById(R.id.rv_pdf_pages);
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);
        progressBar = findViewById(R.id.progress_bar);

        String title = getIntent().getStringExtra(EXTRA_BOOK_TITLE);
        if (title != null) {
            tvTitle.setText(title);
        }

        layoutManager = new LinearLayoutManager(this);
        rvPdfPages.setLayoutManager(layoutManager);
        
        rvPdfPages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                saveProgress();
            }
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            saveProgressAndFinish();
        });
    }

    private void loadPdf() {
        String pdfUrl = getIntent().getStringExtra(EXTRA_PDF_URL);
        
        if (pdfUrl == null || pdfUrl.isEmpty()) {
            Toast.makeText(this, "No PDF URL provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        rvPdfPages.setVisibility(View.GONE);

        executor.execute(() -> {
            try {
                // Check if it's a local file path
                if (pdfUrl.startsWith("/") || pdfUrl.startsWith("file://")) {
                    pdfFile = new File(pdfUrl.replace("file://", ""));
                    if (!pdfFile.exists()) {
                        throw new Exception("Local PDF file not found at: " + pdfUrl);
                    }
                } else {
                    // It's a remote URL, need to download it to a temp file
                    pdfFile = new File(getCacheDir(), "temp_book.pdf");
                    
                    URL url = new URL(pdfUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();

                    InputStream inputStream = connection.getInputStream();
                    FileOutputStream outputStream = new FileOutputStream(pdfFile);

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    outputStream.close();
                    inputStream.close();
                    connection.disconnect();
                }

                mainHandler.post(() -> {
                    try {
                        openPdfRenderer();
                        progressBar.setVisibility(View.GONE);
                        rvPdfPages.setVisibility(View.VISIBLE);
                        
                        PdfPageAdapter adapter = new PdfPageAdapter(pdfRenderer);
                        rvPdfPages.setAdapter(adapter);
                        
                        // Scroll to the last read page
                        if (initialPage > 0 && initialPage < totalPages) {
                            layoutManager.scrollToPosition(initialPage);
                        }
                    } catch (Exception e) {
                        showError("Failed to open PDF: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                mainHandler.post(() -> showError("Failed to load PDF: " + e.getMessage()));
            }
        });
    }

    private void openPdfRenderer() throws Exception {
        parcelFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
        pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        totalPages = pdfRenderer.getPageCount();
    }

    private void saveProgress() {
        if (bookId == null) return;
        
        int currentPage = layoutManager.findFirstVisibleItemPosition();
        if (currentPage != RecyclerView.NO_POSITION) {
            repository.updateReadingProgress(bookId, currentPage, new SupabaseRepository.DataCallback<Void>() {
                @Override public void onSuccess(Void data) { Log.d(TAG, "Progress saved: " + currentPage); }
                @Override public void onError(String message) { Log.e(TAG, "Failed to save progress: " + message); }
            });
        }
    }

    private void saveProgressAndFinish() {
        saveProgress();
        finish();
    }

    @Override
    public void onBackPressed() {
        saveProgressAndFinish();
        super.onBackPressed();
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (pdfRenderer != null) pdfRenderer.close();
            if (parcelFileDescriptor != null) parcelFileDescriptor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (executor != null) executor.shutdown();
    }

    // RecyclerView Adapter to render PDF pages
    private static class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.PageViewHolder> {
        private final PdfRenderer renderer;

        public PdfPageAdapter(PdfRenderer renderer) {
            this.renderer = renderer;
        }

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf_page, parent, false);
            return new PageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            try {
                PdfRenderer.Page page = renderer.openPage(position);
                
                // Render at a higher quality (scale 2x)
                int width = holder.itemView.getContext().getResources().getDisplayMetrics().widthPixels;
                float ratio = (float) page.getHeight() / page.getWidth();
                int height = (int) (width * ratio);

                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                holder.ivPage.setImageBitmap(bitmap);
                
                page.close();
            } catch (Exception e) {
                Log.e(TAG, "Error rendering page " + position, e);
            }
        }

        @Override
        public int getItemCount() {
            return renderer != null ? renderer.getPageCount() : 0;
        }

        static class PageViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPage;
            PageViewHolder(@NonNull View itemView) {
                super(itemView);
                ivPage = itemView.findViewById(R.id.iv_pdf_page_item);
            }
        }
    }
}
