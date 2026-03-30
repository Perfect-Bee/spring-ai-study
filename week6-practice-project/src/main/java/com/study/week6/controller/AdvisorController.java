package com.study.week6.controller;

import com.study.week6.dto.AdvisorChainRequest;
import com.study.week6.dto.MessageRequest;
import com.study.week6.dto.MessageResponse;
import com.study.week6.service.AdvisorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * Advisor API 컨트롤러
 */
@RestController
@RequestMapping("/api/advisor")
@RequiredArgsConstructor
public class AdvisorController {

    private final AdvisorService advisorService;

    /**
     * SimpleLoggerAdvisor 테스트 - 요청/응답 로깅
     */
    @PostMapping("/logging")
    public MessageResponse chatWithLogging(@RequestBody MessageRequest request) {
        String answer = advisorService.chatWithLogging(request.message());
        return new MessageResponse(answer);
    }

    /**
     * SafeGuardAdvisor 테스트 - 민감한 단어 필터링
     */
    @PostMapping("/safeguard")
    public MessageResponse chatWithSafeGuard(@RequestBody MessageRequest request) {
        String answer = advisorService.chatWithSafeGuard(request.message());
        return new MessageResponse(answer);
    }

    /**
     * Advisor Chain 테스트 - 여러 Advisor 조합 + context 공유
     */
    @PostMapping("/chain")
    public MessageResponse chatWithAdvisorChain(@RequestBody AdvisorChainRequest request) {
        String answer = advisorService.chatWithAdvisorChain(request.message(), request.maxCharLength());
        return new MessageResponse(answer);
    }

    /**
     * Advisor Chain + 스트리밍 테스트
     */
    @PostMapping(value = "/chain/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatWithAdvisorChainStream(@RequestBody AdvisorChainRequest request) {
        return advisorService.chatWithAdvisorChainStream(request.message(), request.maxCharLength());
    }
}
