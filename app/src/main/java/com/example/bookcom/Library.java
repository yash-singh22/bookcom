package com.example.bookcom;

public class Library {
    private String id;
    private String name;
    private String color;
    private int bookCount;

    public Library(String id, String name, String color, int bookCount) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.bookCount = bookCount;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getBookCount() {
        return bookCount;
    }

    public void setBookCount(int bookCount) {
        this.bookCount = bookCount;
    }
}
