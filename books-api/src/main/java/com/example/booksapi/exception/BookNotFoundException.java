package com.example.booksapi.exception;

public class BookNotFoundException extends RuntimeException {

    private final Long id;

    public BookNotFoundException(Long id) {
        super("No book with id " + id + " exists");
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
