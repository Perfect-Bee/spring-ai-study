package com.study.week6.service;

import com.study.week6.dto.DocumentResult;
import com.study.week6.dto.EmbedResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 임베딩과 벡터 저장소 서비스
 *
 * EmbeddingModel과 VectorStore를 활용한 텍스트 임베딩, Document 저장/검색/삭제 기능을 제공합니다.
 */
@Service
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    public EmbeddingService(EmbeddingModel embeddingModel, VectorStore vectorStore) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    /**
     * 텍스트를 임베딩하고 결과 정보를 반환합니다.
     */
    public EmbedResponse embedText(String text) {
        // 임베딩 수행
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));

        // 메타데이터 정보
        EmbeddingResponseMetadata metadata = response.getMetadata();
        String modelName = metadata.getModel();
        int dimensions = embeddingModel.dimensions();

        // 임베딩 결과
        Embedding embedding = response.getResults().get(0);
        float[] vector = embedding.getOutput();

        log.info("모델 이름: {}", modelName);
        log.info("모델의 임베딩 차원: {}", dimensions);
        log.info("벡터 차원: {}", vector.length);

        // 벡터 샘플 (처음 10개만)
        List<Float> vectorSample = new ArrayList<>();
        for (int i = 0; i < Math.min(10, vector.length); i++) {
            vectorSample.add(vector[i]);
        }

        return new EmbedResponse(
                modelName != null ? modelName : "unknown",
                dimensions,
                vector.length,
                vectorSample
        );
    }

    /**
     * Document를 벡터 저장소에 저장합니다.
     */
    public void addDocuments(List<Document> documents) {
        vectorStore.add(documents);
        log.info("{}개의 Document가 벡터 저장소에 저장되었습니다.", documents.size());
    }

    /**
     * 샘플 Document를 벡터 저장소에 저장합니다.
     */
    public String addSampleDocuments() {
        List<Document> documents = List.of(
                new Document("대통령 선거는 5년마다 있습니다.", Map.of("source", "헌법", "year", 1987)),
                new Document("대통령 임기는 4년입니다.", Map.of("source", "헌법", "year", 1980)),
                new Document("국회의원은 법률안을 심의·의결합니다.", Map.of("source", "헌법", "year", 1987)),
                new Document("자동차를 사용하려면 등록을 해야합니다.", Map.of("source", "자동차관리법", "year", 2020)),
                new Document("대통령은 행정부의 수반입니다.", Map.of("source", "헌법", "year", 1987)),
                new Document("국회의원은 4년마다 투표로 뽑습니다.", Map.of("source", "헌법", "year", 1987)),
                new Document("승용차는 정규적인 점검이 필요합니다.", Map.of("source", "자동차관리법", "year", 2020))
        );
        vectorStore.add(documents);
        return documents.size() + "개의 샘플 Document가 저장되었습니다.";
    }

    /**
     * 단순 텍스트 쿼리로 유사도 검색을 수행합니다.
     */
    public List<DocumentResult> searchSimple(String query) {
        List<Document> documents = vectorStore.similaritySearch(query);
        return toDocumentResults(documents);
    }

    /**
     * 상세 조건으로 유사도 검색을 수행합니다.
     */
    public List<DocumentResult> searchAdvanced(String query, int topK, double similarityThreshold, String filterExpression) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold);

        if (filterExpression != null && !filterExpression.isBlank()) {
            builder.filterExpression(filterExpression);
        }

        List<Document> documents = vectorStore.similaritySearch(builder.build());
        return toDocumentResults(documents);
    }

    /**
     * 필터 조건으로 Document를 삭제합니다.
     */
    public String deleteByFilter(String filterExpression) {
        vectorStore.delete(filterExpression);
        return "삭제 완료: " + filterExpression;
    }

    private List<DocumentResult> toDocumentResults(List<Document> documents) {
        if (documents == null) return List.of();
        return documents.stream()
                .map(doc -> new DocumentResult(
                        doc.getId(),
                        doc.getText(),
                        doc.getMetadata(),
                        doc.getScore()
                ))
                .toList();
    }
}
