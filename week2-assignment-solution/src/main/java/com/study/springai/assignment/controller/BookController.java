package com.study.springai.assignment.controller;

import com.study.springai.assignment.dto.BookAnalysisRequest;
import com.study.springai.assignment.dto.BookRecommendRequest;
import com.study.springai.assignment.dto.BookRecommendation;
import com.study.springai.assignment.service.BookService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    // 탭 1: 도서 추천 (Ch2 - 기본 텍스트 대화)
    @PostMapping("/recommend")
    public String recommendBooks(@RequestBody BookRecommendRequest request) {
        return bookService.recommendBooks(request);
    }

    // 탭 2: 도서 분석 (Ch3 - 프롬프트 템플릿)
    @PostMapping("/analyze")
    public String analyzeBook(@RequestBody BookAnalysisRequest request) {
        return bookService.analyzeBook(request);
    }

    // 탭 3: 구조화된 추천 (Ch4 - 구조화된 출력)
    @PostMapping("/structured")
    public List<BookRecommendation> getStructuredRecommendations(@RequestBody BookRecommendRequest request) {
        return bookService.getStructuredRecommendations(request);
    }

    // 탭 4: 제로-샷 도서 분류
    @PostMapping("/zero-shot")
    public Map<String, String> classifyZeroShot(@RequestBody Map<String, String> request) {
        return Map.of("result", bookService.classifyBookZeroShot(request.get("bookDescription")));
    }

    // 탭 5: 스텝-백 도서 분석
    @PostMapping("/step-back")
    public Map<String, String> analyzeStepBack(@RequestBody Map<String, String> request) {
        return Map.of("result", bookService.analyzeWithStepBack(request.get("title"), request.get("question")));
    }
}
