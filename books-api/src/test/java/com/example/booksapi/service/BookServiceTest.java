package com.example.booksapi.service;

import com.example.booksapi.dto.BookRequest;
import com.example.booksapi.dto.BookResponse;
import com.example.booksapi.dto.PaginatedResponse;
import com.example.booksapi.exception.BookNotFoundException;
import com.example.booksapi.model.Book;
import com.example.booksapi.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private Book testBook;

    @BeforeEach
    void setUp() {
        testBook = Book.builder()
                .id(1L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .genre("Programming")
                .createdAt(Instant.now())
                .modifiedAt(Instant.now())
                .build();
    }

    @Test
    void listBooks_noFilter_returnsPaginatedResponse() {
        when(bookRepository.findByCursor(eq(0L), any(Pageable.class))).thenReturn(List.of(testBook));

        PaginatedResponse<BookResponse> result = bookService.listBooks(10, null, null);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getTitle()).isEqualTo("Clean Code");
        assertThat(result.getNext()).isNull(); // 1 item < limit of 10 → no next cursor
    }

    @Test
    void listBooks_fullPage_returnsNextCursor() {
        when(bookRepository.findByCursor(eq(0L), any(Pageable.class))).thenReturn(List.of(testBook));

        PaginatedResponse<BookResponse> result = bookService.listBooks(1, null, null);

        assertThat(result.getNext()).isNotNull(); // 1 item == limit of 1 → cursor returned
    }

    @Test
    void listBooks_withQuery_callsFilteredRepository() {
        when(bookRepository.findByCursorAndQuery(eq(0L), eq("clean"), any(Pageable.class)))
                .thenReturn(List.of(testBook));

        PaginatedResponse<BookResponse> result = bookService.listBooks(10, null, "clean");

        assertThat(result.getItems()).hasSize(1);
        verify(bookRepository, never()).findByCursor(anyLong(), any());
    }

    @Test
    void getBook_existingId_returnsBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        BookResponse response = bookService.getBook(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Clean Code");
    }

    @Test
    void getBook_nonExistentId_throwsBookNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBook(99L))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void createBook_validRequest_savesAndReturnsBook() {
        BookRequest request = new BookRequest();
        request.setTitle("The Pragmatic Programmer");
        request.setAuthor("David Thomas");

        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        BookResponse response = bookService.createBook(request);

        assertThat(response).isNotNull();
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void deleteBook_existingId_deletesSuccessfully() {
        when(bookRepository.existsById(1L)).thenReturn(true);

        bookService.deleteBook(1L);

        verify(bookRepository).deleteById(1L);
    }

    @Test
    void deleteBook_nonExistentId_throwsBookNotFoundException() {
        when(bookRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> bookService.deleteBook(99L))
                .isInstanceOf(BookNotFoundException.class);
        verify(bookRepository, never()).deleteById(any());
    }

    @Test
    void computeEtag_returnsQuotedIdAndTimestamp() {
        String etag = bookService.computeEtag(testBook);

        assertThat(etag).startsWith("\"").endsWith("\"");
        assertThat(etag).contains(String.valueOf(testBook.getId()));
    }
}
