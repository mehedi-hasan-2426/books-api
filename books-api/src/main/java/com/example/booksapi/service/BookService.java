package com.example.booksapi.service;

import com.example.booksapi.dto.BookRequest;
import com.example.booksapi.dto.BookResponse;
import com.example.booksapi.dto.PaginatedResponse;
import com.example.booksapi.exception.BookNotFoundException;
import com.example.booksapi.exception.PreconditionFailedException;
import com.example.booksapi.model.Book;
import com.example.booksapi.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    public PaginatedResponse<BookResponse> listBooks(int limit, String cursor, String q) {
        long cursorId = decodeCursor(cursor);
        PageRequest pageable = PageRequest.of(0, limit);

        List<Book> books = (q != null && !q.isBlank())
                ? bookRepository.findByCursorAndQuery(cursorId, q, pageable)
                : bookRepository.findByCursor(cursorId, pageable);

        String nextCursor = (books.size() == limit)
                ? encodeCursor(books.get(books.size() - 1).getId())
                : null;

        return PaginatedResponse.<BookResponse>builder()
                .items(books.stream().map(this::toResponse).collect(Collectors.toList()))
                .next(nextCursor)
                .build();
    }

    public BookResponse getBook(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public BookResponse createBook(BookRequest request) {
        Book book = Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .genre(request.getGenre())
                .build();
        return toResponse(bookRepository.save(book));
    }

    @Transactional
    public BookResponse patchBook(Long id, Map<String, Object> patch, String ifMatch) {
        Book book = findById(id);

        if (!computeEtag(book).equals(ifMatch)) {
            throw new PreconditionFailedException("ETag mismatch — resource has been modified");
        }

        if (patch.containsKey("title") && patch.get("title") != null) {
            book.setTitle((String) patch.get("title"));
        }
        if (patch.containsKey("author") && patch.get("author") != null) {
            book.setAuthor((String) patch.get("author"));
        }
        if (patch.containsKey("genre")) {
            // Explicit null in merge-patch clears the field
            book.setGenre(patch.get("genre") != null ? (String) patch.get("genre") : null);
        }

        return toResponse(bookRepository.save(book));
    }

    @Transactional
    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new BookNotFoundException(id);
        }
        bookRepository.deleteById(id);
    }

    public String computeEtag(Book book) {
        return "\"" + book.getId() + "-" + book.getModifiedAt().toEpochMilli() + "\"";
    }

    private Book findById(Long id) {
        return bookRepository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
    }

    private BookResponse toResponse(Book book) {
        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .genre(book.getGenre())
                .createdAt(book.getCreatedAt())
                .modifiedAt(book.getModifiedAt())
                .build();
    }

    private long decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return 0L;
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            return Long.parseLong(decoded);
        } catch (Exception e) {
            return 0L;
        }
    }

    private String encodeCursor(Long id) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(id.toString().getBytes(StandardCharsets.UTF_8));
    }
}
