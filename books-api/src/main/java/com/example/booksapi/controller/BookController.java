package com.example.booksapi.controller;

import com.example.booksapi.dto.BookRequest;
import com.example.booksapi.dto.BookResponse;
import com.example.booksapi.dto.PaginatedResponse;
import com.example.booksapi.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<BookResponse>> listBooks(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String q) {

        limit = Math.min(Math.max(limit, 1), 100);
        return ResponseEntity.ok(bookService.listBooks(limit, cursor, q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBook(@PathVariable Long id) {
        BookResponse response = bookService.getBook(id);
        String etag = "\"" + response.getId() + "-" + response.getModifiedAt().toEpochMilli() + "\"";
        return ResponseEntity.ok().eTag(etag).body(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody BookRequest request) {
        BookResponse response = bookService.createBook(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    // If-Match required — 428 if missing, 412 if ETag doesn't match
    @PatchMapping(value = "/{id}", consumes = "application/merge-patch+json")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<BookResponse> patchBook(
            @PathVariable Long id,
            @RequestBody Map<String, Object> patch,
            @RequestHeader(value = "If-Match", required = false) String ifMatch) {

        if (ifMatch == null || ifMatch.isBlank()) {
            return ResponseEntity.status(428).build();
        }
        return ResponseEntity.ok(bookService.patchBook(id, patch, ifMatch));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
