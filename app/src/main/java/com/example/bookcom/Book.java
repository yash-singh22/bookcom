package com.example.bookcom;

public class Book {
    private int id;
    private String supabaseId; // UUID from Supabase
    private String title;
    private String author;
    private String category;
    private String description;
    private String fileUrl;
    private String coverUrl;
    private int pageCount;
    private int currentPage;
    private int coverResId;
    private boolean isFavorite;

    public Book(int id, String title, String author, String category, int coverResId) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.coverResId = coverResId;
        this.isFavorite = false;
        this.currentPage = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public String getSupabaseId() {
        return supabaseId;
    }

    public void setSupabaseId(String supabaseId) {
        this.supabaseId = supabaseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getCoverResId() {
        return coverResId;
    }

    public void setCoverResId(int coverResId) {
        this.coverResId = coverResId;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }
}
