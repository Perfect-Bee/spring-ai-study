package com.study.week6.controller;

import com.study.week6.dto.*;
import com.study.week6.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 임베딩 & 벡터 저장소 API 컨트롤러
 */
@RestController
@RequestMapping("/api/embedding")
@RequiredArgsConstructor
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    /**
     * 텍스트 임베딩 - 벡터 변환 결과 확인
     */
    @PostMapping("/embed")
    public EmbedResponse embedText(@RequestBody EmbedRequest request) {
        return embeddingService.embedText(request.text());
    }

    /**
     * 샘플 Document 저장
     */
    @PostMapping("/documents/sample")
    public ResultResponse addSampleDocuments() {
        String result = embeddingService.addSampleDocuments();
        return new ResultResponse(result);
    }

    /**
     * 커스텀 Document 저장
     */
    @PostMapping("/documents")
    public ResultResponse addDocument(@RequestBody DocumentAddRequest request) {
        Map<String, Object> metadata = request.metadata() != null ? request.metadata() : Map.of();
        embeddingService.addDocuments(List.of(new Document(request.content(), metadata)));
        return new ResultResponse("Document가 저장되었습니다.");
    }

    /**
     * 단순 유사도 검색
     */
    @PostMapping("/search/simple")
    public List<DocumentResult> searchSimple(@RequestBody SearchQueryRequest request) {
        return embeddingService.searchSimple(request.query());
    }

    /**
     * 상세 유사도 검색 (topK, similarityThreshold, filterExpression)
     */
    @PostMapping("/search/advanced")
    public List<DocumentResult> searchAdvanced(@RequestBody AdvancedSearchRequest request) {
        int topK = request.topK() != null ? request.topK() : 4;
        double threshold = request.similarityThreshold() != null ? request.similarityThreshold() : 0.0;
        return embeddingService.searchAdvanced(request.query(), topK, threshold, request.filterExpression());
    }

    /**
     * 필터 조건으로 Document 삭제
     */
    @DeleteMapping("/documents")
    public ResultResponse deleteDocuments(@RequestBody DeleteRequest request) {
        String result = embeddingService.deleteByFilter(request.filterExpression());
        return new ResultResponse(result);
    }
}
