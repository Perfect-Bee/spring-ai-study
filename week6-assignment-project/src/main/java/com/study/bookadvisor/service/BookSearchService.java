package com.study.bookadvisor.service;

import com.study.bookadvisor.dto.BookSearchResult;
import com.study.bookadvisor.dto.EmbedResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 시맨틱 도서 검색 서비스
 *
 * VectorStore를 활용하여 자연어 질의로 유사한 도서를 검색합니다.
 * 사용자가 "재미있는 SF 소설 추천해줘" 같은 자연어로 검색하면,
 * 의미적으로 유사한 도서를 벡터 유사도 기반으로 찾아줍니다.
 */
@Service
@Slf4j
public class BookSearchService {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    public BookSearchService(VectorStore vectorStore, EmbeddingModel embeddingModel) {
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
    }

    // ========================================================================
    // TODO 1: 단순 유사도 검색
    // ========================================================================
    /**
     * 자연어 쿼리로 유사한 도서를 검색합니다.
     *
     * [요구사항]
     * - vectorStore.similaritySearch()를 사용하여 검색
     * - 검색 결과(List<Document>)를 List<BookSearchResult>로 변환하여 반환
     * - toSearchResult() 헬퍼 메소드를 활용하세요
     *
     * [힌트]
     * - VectorStore의 similaritySearch(String query) 메소드 사용
     *
     * @param query 자연어 검색 쿼리 (예: "자바 프로그래밍 입문서 추천해줘")
     * @return 유사도 높은 도서 목록
     */
    public List<BookSearchResult> searchSimple(String query) {
        // TODO: 구현하세요
        throw new UnsupportedOperationException("TODO 1: 단순 유사도 검색을 구현하세요");
    }

    // ========================================================================
    // TODO 2: 상세 조건 유사도 검색
    // ========================================================================
    /**
     * topK, similarityThreshold, 장르 필터를 적용한 상세 유사도 검색을 수행합니다.
     *
     * [요구사항]
     * - SearchRequest.builder()를 사용하여 검색 조건을 구성
     * - query, topK, similarityThreshold를 설정
     * - genre가 null이 아니고 비어있지 않으면 filterExpression으로
     *   "genre == '장르값'" 조건을 추가
     * - 검색 결과를 List<BookSearchResult>로 변환하여 반환
     *
     * [힌트]
     * - SearchRequest.builder().query(...).topK(...).similarityThreshold(...)
     *   .filterExpression("genre == '프로그래밍'").build()
     * - filterExpression은 메타데이터 필드를 조건으로 사용
     *
     * @param query               자연어 검색 쿼리
     * @param topK                상위 K개 결과
     * @param similarityThreshold 유사도 임계값 (0.0 ~ 1.0)
     * @param genre               장르 필터 (null이면 필터 없음)
     * @return 조건에 맞는 도서 목록
     */
    public List<BookSearchResult> searchAdvanced(String query, int topK,
                                                  double similarityThreshold, String genre) {
        // TODO: 구현하세요
        throw new UnsupportedOperationException("TODO 2: 상세 조건 유사도 검색을 구현하세요");
    }

    // ========================================================================
    // TODO 3: 임베딩 정보 확인
    // ========================================================================
    /**
     * 텍스트를 임베딩하고 벡터 정보를 반환합니다.
     *
     * [요구사항]
     * - embeddingModel.embedForResponse()를 사용하여 텍스트를 임베딩
     * - 결과에서 모델명, 차원수, 벡터 길이, 벡터 샘플(처음 5개)을 추출
     * - EmbedResponse DTO로 반환
     *
     * [힌트]
     * - EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
     * - response.getMetadata().getModel() → 모델명
     * - embeddingModel.dimensions() → 차원수
     * - response.getResults().get(0).getOutput() → float[] 벡터
     * - 벡터 샘플은 처음 5개만 List<Float>로 변환
     * - return new EmbedResponse(model, dimensions, vectorLength, vectorSample);
     *
     * @param text 임베딩할 텍스트
     * @return 임베딩 결과 정보
     */
    public EmbedResponse getEmbeddingInfo(String text) {
        // TODO: 구현하세요
        throw new UnsupportedOperationException("TODO 3: 임베딩 정보 확인을 구현하세요");
    }

    /**
     * Document를 BookSearchResult로 변환하는 헬퍼 메소드
     */
    private BookSearchResult toSearchResult(Document doc) {
        return new BookSearchResult(
                doc.getId(),
                doc.getText(),
                doc.getMetadata(),
                doc.getScore()
        );
    }
}
