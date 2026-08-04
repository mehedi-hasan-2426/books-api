package com.example.booksapi.controller;

import com.example.booksapi.model.Book;
import com.example.booksapi.model.Role;
import com.example.booksapi.model.User;
import com.example.booksapi.repository.BookRepository;
import com.example.booksapi.repository.RefreshTokenRepository;
import com.example.booksapi.repository.UserRepository;
import com.example.booksapi.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class BookControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private BookRepository bookRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    private String authHeader;
    private Book savedBook;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.builder()
                .username("testuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(Role.ADMIN)
                .createdAt(Instant.now())
                .build());
        authHeader = "Bearer " + jwtService.generateToken(user.getUsername());

        savedBook = bookRepository.save(Book.builder()
                .title("Clean Code")
                .author("Robert C. Martin")
                .genre("Programming")
                .build());
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void listBooks_authenticated_returns200WithItemsArray() throws Exception {
        mockMvc.perform(get("/books").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].title").value("Clean Code"));
    }

    @Test
    void listBooks_unauthenticated_returns401WithProblemJson() throws Exception {
        mockMvc.perform(get("/books"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void getBook_existingId_returns200WithEtag() throws Exception {
        mockMvc.perform(get("/books/{id}", savedBook.getId()).header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.id").value(savedBook.getId()))
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.created_at").exists())
                .andExpect(jsonPath("$.modified_at").exists());
    }

    @Test
    void getBook_nonExistentId_returns404ProblemJson() throws Exception {
        mockMvc.perform(get("/books/9999").header("Authorization", authHeader))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.type").value("/problems/book-not-found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void createBook_validRequest_returns201WithLocation() throws Exception {
        mockMvc.perform(post("/books")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "DDIA", "author", "Kleppmann"))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void createBook_missingTitle_returns400ProblemJson() throws Exception {
        mockMvc.perform(post("/books")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("author", "Someone"))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void patchBook_missingIfMatch_returns428() throws Exception {
        mockMvc.perform(patch("/books/{id}", savedBook.getId())
                        .header("Authorization", authHeader)
                        .contentType("application/merge-patch+json")
                        .content(objectMapper.writeValueAsString(Map.of("title", "Updated"))))
                .andExpect(status().is(428));
    }

    @Test
    void deleteBook_existingId_returns204() throws Exception {
        mockMvc.perform(delete("/books/{id}", savedBook.getId()).header("Authorization", authHeader))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteBook_nonExistentId_returns404ProblemJson() throws Exception {
        mockMvc.perform(delete("/books/9999").header("Authorization", authHeader))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.type").value("/problems/book-not-found"));
    }
}
