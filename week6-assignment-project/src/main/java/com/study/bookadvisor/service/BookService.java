package com.study.bookadvisor.service;

import com.study.bookadvisor.dto.BookCreateRequest;
import com.study.bookadvisor.entity.Book;
import com.study.bookadvisor.repository.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 도서 관리 서비스
 *
 * RDB(원본) + VectorStore(검색 인덱스) 동기화
 *
 * 도서를 등록/삭제할 때 RDB와 VectorStore를 항상 동기화 상태로 유지해야 합니다.
 * RDB에만 저장하고 VectorStore에 반영하지 않으면 시맨틱 검색에서 누락되고,
 * VectorStore에만 있고 RDB에 없으면 데이터 정합성이 깨집니다.
 *
 * [동기화 전략]
 * - 등록: RDB INSERT → VectorStore add (같은 트랜잭션 내)
 * - 삭제: VectorStore delete → RDB DELETE
 * - 수정: VectorStore delete(기존) → RDB UPDATE → VectorStore add(새로운)
 */
@Service
@Slf4j
public class BookService {

    private final BookRepository bookRepository;
    private final VectorStore vectorStore;

    public BookService(BookRepository bookRepository, VectorStore vectorStore) {
        this.bookRepository = bookRepository;
        this.vectorStore = vectorStore;
    }

    /**
     * 전체 도서 목록 조회 (RDB)
     */
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    /**
     * 장르별 도서 목록 조회 (RDB)
     */
    public List<Book> getBooksByGenre(String genre) {
        return bookRepository.findByGenre(genre);
    }

    // ========================================================================
    // TODO 6: 도서 등록 시 RDB + VectorStore 동시 저장
    // ========================================================================
    /**
     * 새 도서를 등록하고, VectorStore에도 동기화합니다.
     *
     * [요구사항]
     * 1) BookCreateRequest로부터 Book 엔티티를 생성하여 RDB에 저장
     * 2) 저장된 Book을 VectorStore Document로 변환하여 VectorStore에 추가
     * 3) RDB에 저장된 Book 엔티티를 반환
     *
     * [힌트]
     * 1) Book 엔티티 생성:
     *    Book book = Book.builder()
     *        .title(request.title())
     *        .author(request.author())
     *        .genre(request.genre())
     *        .description(request.description())
     *        .rating(request.rating())
     *        .publishedYear(request.publishedYear())
     *        .isbn(request.isbn())
     *        .build();
     *
     * 2) RDB 저장:
     *    Book saved = bookRepository.save(book);
     *
     * 3) VectorStore Document 생성 & 저장:
     *    - saved.toEmbeddingText()로 임베딩용 텍스트 생성
     *    - 메타데이터에 bookId, title, author, genre, rating, publishedYear, isbn 포함
     *    - vectorStore.add(List.of(document))로 VectorStore에 저장
     *
     * 4) 반환: saved
     *
     * [주의사항 - 실무 관점]
     * - RDB 저장 후 VectorStore 저장이 실패하면 데이터 불일치가 발생합니다.
     * - 실무에서는 @Transactional + 보상 트랜잭션 또는 이벤트 기반 동기화를 사용합니다.
     * - 이 과제에서는 단순하게 순차 저장으로 구현합니다.
     *
     * @param request 도서 등록 요청
     * @return 저장된 Book 엔티티
     */
    public Book registerBook(BookCreateRequest request) {
        // TODO: 구현하세요
        throw new UnsupportedOperationException("TODO 6: 도서 등록 + VectorStore 동기화를 구현하세요");
    }

    /**
     * Book → VectorStore Document 변환 헬퍼
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
