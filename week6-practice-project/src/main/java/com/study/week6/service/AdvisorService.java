package com.study.week6.service;

import com.study.week6.advisor.MaxCharLengthAdvisor;
import com.study.week6.advisor.RequestTimingAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Advisor 서비스
 *
 * 다양한 내장/커스텀 Advisor를 활용한 LLM 호출을 연습합니다.
 *
 * 1) SimpleLoggerAdvisor - 요청/응답 로깅
 * 2) SafeGuardAdvisor - 민감한 단어 필터링
 * 3) MaxCharLengthAdvisor (커스텀) - 최대 응답 문자 수 제한
 * 4) RequestTimingAdvisor (커스텀) - 요청 처리 시간 측정
 * 5) Advisor Chain - 여러 Advisor 체인 구성
 * 6) Context 공유 - advisorSpec.param() 으로 Advisor에 데이터 전달
 */
@Service
@Slf4j
public class AdvisorService {

    private final ChatClient loggerClient;
    private final ChatClient safeGuardClient;
    private final ChatClient chainClient;

    public AdvisorService(ChatModel chatModel) {
        // 1. 로깅 Advisor가 적용된 ChatClient
        this.loggerClient = ChatClient.builder(chatModel)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
                )
                .build();

        // 2. SafeGuard Advisor가 적용된 ChatClient
        SafeGuardAdvisor safeGuardAdvisor = new SafeGuardAdvisor(
                List.of("욕설", "계좌번호", "폭력", "폭탄", "해킹"),
                "해당 질문은 민감한 콘텐츠 요청이므로 응답할 수 없습니다.",
                Ordered.HIGHEST_PRECEDENCE
        );
        this.safeGuardClient = ChatClient.builder(chatModel)
                .defaultAdvisors(safeGuardAdvisor)
                .build();

        // 3. 여러 Advisor Chain이 적용된 ChatClient
        this.chainClient = ChatClient.builder(chatModel)
                .defaultAdvisors(
                        new RequestTimingAdvisor(Ordered.HIGHEST_PRECEDENCE),
                        new MaxCharLengthAdvisor(Ordered.HIGHEST_PRECEDENCE + 1),
                        new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
                )
                .build();
    }

    /**
     * SimpleLoggerAdvisor 적용 - 요청/응답이 DEBUG 로그로 출력됩니다.
     */
    public String chatWithLogging(String question) {
        return loggerClient.prompt()
                .user(question)
                .call()
                .content();
    }

    /**
     * SafeGuardAdvisor 적용 - 민감한 단어 포함 시 차단됩니다.
     */
    public String chatWithSafeGuard(String question) {
        return safeGuardClient.prompt()
                .user(question)
                .call()
                .content();
    }

    /**
     * Advisor Chain 적용 - RequestTimingAdvisor + MaxCharLengthAdvisor + SimpleLoggerAdvisor
     * context를 통해 maxCharLength를 전달합니다.
     */
    public String chatWithAdvisorChain(String question, Integer maxCharLength) {
        int charLimit = (maxCharLength != null && maxCharLength > 0) ? maxCharLength : 200;

        return chainClient.prompt()
                .advisors(advisorSpec ->
                        advisorSpec.param(MaxCharLengthAdvisor.MAX_CHAR_LENGTH, charLimit))
                .user(question)
                .call()
                .content();
    }

    /**
     * Advisor Chain + 스트리밍 응답
     */
    public Flux<String> chatWithAdvisorChainStream(String question, Integer maxCharLength) {
        int charLimit = (maxCharLength != null && maxCharLength > 0) ? maxCharLength : 200;

        return chainClient.prompt()
                .advisors(advisorSpec ->
                        advisorSpec.param(MaxCharLengthAdvisor.MAX_CHAR_LENGTH, charLimit))
                .user(question)
                .stream()
                .content();
    }
}
