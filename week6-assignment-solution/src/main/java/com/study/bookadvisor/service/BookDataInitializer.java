package com.study.bookadvisor.service;

import com.study.bookadvisor.entity.Book;
import com.study.bookadvisor.repository.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 애플리케이션 시작 시 RDB에 저장된 도서 데이터를 VectorStore에 동기화합니다.
 *
 * [실무 패턴]
 * - RDB가 원본 데이터(Single Source of Truth), VectorStore는 검색 인덱스 역할
 * - 앱 시작 시 RDB → VectorStore 동기화를 수행하여 벡터 인덱스를 최신 상태로 유지
 * - 각 Document의 메타데이터에 bookId, genre, author, rating 등을 포함하여
 *   유사도 검색 시 필터 조건으로 활용 가능
 */
@Component
@Slf4j
public class BookDataInitializer implements ApplicationRunner {

    private final BookRepository bookRepository;
    private final VectorStore vectorStore;

    public BookDataInitializer(BookRepository bookRepository, VectorStore vectorStore) {
        this.bookRepository = bookRepository;
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Book> books = bookRepository.findAll();

        if (books.isEmpty()) {
            log.warn("RDB에 도서 데이터가 없습니다. Docker init 스크립트를 확인하세요.");
            return;
        }

        log.info("=== RDB → VectorStore 도서 데이터 동기화 시작 ({} 권) ===", books.size());

        List<Document> documents = books.stream()
                .map(this::toDocument)
                .toList();

        vectorStore.add(documents);

        log.info("=== VectorStore 도서 데이터 동기화 완료 ({} 건) ===", documents.size());
    }

    /**
     * Book 엔티티를 VectorStore Document로 변환합니다.
     *
     * - text: 임베딩에 사용되는 텍스트 (제목 + 저자 + 장르 + 설명)
     * - metadata: 필터 검색 시 사용할 수 있는 부가 정보
     */
    private Document toDocument(Book book) {
        Map<String, Object> metadata = Map.of(
                "bookId", book.getId(),
                "title", book.getTitle(),
                "author", book.getAuthor(),
                "genre", book.getGenre(),
                "rating", book.getRating(),
                "publishedYear", book.getPublishedYear(),
                "isbn", book.getIsbn() != null ? book.getIsbn() : ""
        );

        return new Document(book.toEmbeddingText(), metadata);
    }
}
