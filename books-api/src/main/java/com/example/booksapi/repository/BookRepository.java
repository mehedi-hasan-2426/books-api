package com.example.booksapi.repository;

import com.example.booksapi.model.Book;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("SELECT b FROM Book b WHERE b.id > :cursor ORDER BY b.id ASC")
    List<Book> findByCursor(@Param("cursor") Long cursor, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.id > :cursor AND " +
           "(LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY b.id ASC")
    List<Book> findByCursorAndQuery(@Param("cursor") Long cursor, @Param("q") String q, Pageable pageable);
}
