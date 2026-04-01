package com.study.bookadvisor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "book")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 200)
    private String author;

    @Column(nullable = false, length = 100)
    private String genre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "NUMERIC(2,1)")
    private Double rating;

    @Column(name = "published_year", nullable = false)
    private Integer publishedYear;

    @Column(length = 20, unique = true)
    private String isbn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * VectorStore Document에 저장할 텍스트를 생성합니다.
     * 제목, 저자, 장르, 설명을 조합하여 임베딩에 적합한 텍스트를 만듭니다.
     */
    public String toEmbeddingText() {
        return String.format("[%s] %s (저자: %s, 장르: %s, 평점: %.1f) - %s",
                genre, title, author, genre, rating, description);
    }
}
