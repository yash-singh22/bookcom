package com.example.bookcom.data;

import com.google.gson.annotations.SerializedName;

/**
 * Library model for Supabase database
 */
public class LibraryModel {
    @SerializedName("id")
    private String id;
    
    @SerializedName("user_id")
    private String userId;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("color")
    private String color;
    
    @SerializedName("book_count")
    private int bookCount;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("updated_at")
    private String updatedAt;

    public LibraryModel() {}

    public LibraryModel(String userId, String name, String description, String color) {
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.color = color;
        this.bookCount = 0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public int getBookCount() { return bookCount; }
    public void setBookCount(int bookCount) { this.bookCount = bookCount; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
