package com.example.capstone;

public class Book {
    private String title;
    private String publisher;
    private String author;
    private int totalPages;
    private String coverImageURL;

    public Book(String title, String publisher, String author, int totalPages, String coverImageURL) {
        this.title = title;
        this.publisher = publisher;
        this.author = author;
        this.totalPages = totalPages;
        this.coverImageURL = coverImageURL;
    }

    // Getters and setters for each attribute
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public String getCoverImageURL() {
        return coverImageURL;
    }

    public void setCoverImageURL(String coverImageURL) {
        this.coverImageURL = coverImageURL;
    }
}