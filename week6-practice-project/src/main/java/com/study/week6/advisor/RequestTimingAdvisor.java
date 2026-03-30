package com.study.week6.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import reactor.core.publisher.Flux;

/**
 * 커스텀 Advisor 예시: 요청 처리 시간 측정
 *
 * CallAdvisor와 StreamAdvisor를 모두 구현하여
 * 동기/비동기 양쪽 호출 시 LLM 요청의 전처리/후처리 시간을 측정합니다.
 */
@Slf4j
public class RequestTimingAdvisor implements CallAdvisor, StreamAdvisor {

    private final int order;

    public RequestTimingAdvisor(int order) {
        this.order = order;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        log.info("[RequestTimingAdvisor 전처리] LLM 요청 시작");
        long startTime = System.currentTimeMillis();

        ChatClientResponse response = chain.nextCall(request);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[RequestTimingAdvisor 후처리] LLM 응답 완료 - 소요 시간: {}ms", elapsed);
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        log.info("[RequestTimingAdvisor 전처리] LLM 스트림 요청 시작");
        long startTime = System.currentTimeMillis();

        return chain.nextStream(request)
                .doOnComplete(() -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    log.info("[RequestTimingAdvisor 후처리] LLM 스트림 완료 - 소요 시간: {}ms", elapsed);
                });
    }
}
