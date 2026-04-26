package com.example.bookcom.data;

import com.google.gson.annotations.SerializedName;

/**
 * Book model for Supabase database
 */
public class BookModel {
    @SerializedName("id")
    private String id;
    
    @SerializedName("user_id")
    private String userId;
    
    @SerializedName("library_id")
    private String libraryId;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("author")
    private String author;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("cover_url")
    private String coverUrl;
    
    @SerializedName("file_url")
    private String fileUrl;
    
    @SerializedName("category")
    private String category;
    
    @SerializedName("page_count")
    private int pageCount;
    
    @SerializedName("current_page")
    private int currentPage;
    
    @SerializedName("is_favorite")
    private boolean isFavorite;
    
    @SerializedName("file_size")
    private long fileSize;
    
    @SerializedName("file_name")
    private String fileName;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("updated_at")
    private String updatedAt;

    public BookModel() {}

    public BookModel(String userId, String title, String author, String category, String description) {
        this.userId = userId;
        this.title = title;
        this.author = author;
        this.category = category;
        this.description = description;
        this.isFavorite = false;
        this.currentPage = 0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getLibraryId() { return libraryId; }
    public void setLibraryId(String libraryId) { this.libraryId = libraryId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
