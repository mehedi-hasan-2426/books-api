package com.example.booksapi.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class BookResponse {
    private Long id;
    private String title;
    private String author;
    private String genre;
    private Instant createdAt;
    private Instant modifiedAt;
}
