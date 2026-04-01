package com.study.bookadvisor.controller;

import com.study.bookadvisor.dto.*;
import com.study.bookadvisor.entity.Book;
import com.study.bookadvisor.service.BookChatService;
import com.study.bookadvisor.service.BookSearchService;
import com.study.bookadvisor.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final BookSearchService bookSearchService;
    private final BookChatService bookChatService;

    // ── RDB 도서 CRUD ──
    @GetMapping("/books")
    public List<BookResponse> getAllBooks() {
        return bookService.getAllBooks().stream().map(this::toResponse).toList();
    }

    @GetMapping("/books/genre/{genre}")
    public List<BookResponse> getBooksByGenre(@PathVariable String genre) {
        return bookService.getBooksByGenre(genre).stream().map(this::toResponse).toList();
    }

    @PostMapping("/books")
    public BookResponse registerBook(@RequestBody BookCreateRequest request) {
        Book saved = bookService.registerBook(request);
        return toResponse(saved);
    }

    // ── 시맨틱 검색 (VectorStore) ──
    @PostMapping("/search/simple")
    public List<BookSearchResult> searchSimple(@RequestBody SearchQueryRequest request) {
        return bookSearchService.searchSimple(request.query());
    }

    @PostMapping("/search/advanced")
    public List<BookSearchResult> searchAdvanced(@RequestBody BookSearchRequest request) {
        return bookSearchService.searchAdvanced(
                request.query(), request.topK(),
                request.similarityThreshold(), request.genre());
    }

    // ── 임베딩 정보 ──
    @PostMapping("/embedding")
    public EmbedResponse getEmbeddingInfo(@RequestBody EmbedRequest request) {
        return bookSearchService.getEmbeddingInfo(request.text());
    }

    // ── AI 도서 추천 채팅 ──
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String answer = bookChatService.chat(request.message(), request.conversationId());
        return new ChatResponse(answer, request.conversationId());
    }

    private BookResponse toResponse(Book book) {
        return new BookResponse(
                book.getId(), book.getTitle(), book.getAuthor(),
                book.getGenre(), book.getDescription(), book.getRating(),
                book.getPublishedYear(), book.getIsbn());
    }
}
