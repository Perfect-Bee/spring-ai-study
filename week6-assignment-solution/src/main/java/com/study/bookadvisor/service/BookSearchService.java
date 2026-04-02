package com.study.bookadvisor.service;

import com.study.bookadvisor.dto.BookSearchResult;
import com.study.bookadvisor.dto.EmbedResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
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

    /**
     * 자연어 쿼리로 유사한 도서를 검색합니다.
     */
    public List<BookSearchResult> searchSimple(String query) {
        List<Document> documents = vectorStore.similaritySearch(query);
        return documents.stream()
                .map(this::toSearchResult)
                .toList();
    }

    /**
     * topK, similarityThreshold, 장르 필터를 적용한 상세 유사도 검색을 수행합니다.
     */
    public List<BookSearchResult> searchAdvanced(String query, int topK,
                                                  double similarityThreshold, String genre) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold);

        // 장르 필터가 있으면 filterExpression으로 추가
        if (genre != null && !genre.isBlank()) {
            builder.filterExpression("genre == '" + genre + "'");
        }

        List<Document> documents = vectorStore.similaritySearch(builder.build());
        return documents.stream()
                .map(this::toSearchResult)
                .toList();
    }

    /**
     * 텍스트를 임베딩하고 벡터 정보를 반환합니다.
     */
    public EmbedResponse getEmbeddingInfo(String text) {
        // 임베딩 수행
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));

        // 메타데이터에서 모델명 추출
        String model = response.getMetadata().getModel();
        int dimensions = embeddingModel.dimensions();

        // 벡터 결과 추출
        Embedding embedding = response.getResults().get(0);
        float[] vector = embedding.getOutput();

        // 벡터 샘플 (처음 5개)
        List<Float> vectorSample = new ArrayList<>();
        for (int i = 0; i < Math.min(5, vector.length); i++) {
            vectorSample.add(vector[i]);
        }

        return new EmbedResponse(
                model != null ? model : "unknown",
                dimensions,
                vector.length,
                vectorSample
        );
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
