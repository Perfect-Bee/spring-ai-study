package com.study.week6.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * 커스텀 Advisor 예시: 최대 응답 문자 수 제한
 *
 * 프롬프트에 최대 문자 수 제한 지시문을 추가하는 커스텀 Advisor입니다.
 * - 기본 최대 문자 수: 300자
 * - context에서 "maxCharLength" 키로 값을 전달하면 해당 값으로 재정의 가능
 * - 전처리 단계에서 시스템 메시지와 사용자 메시지 양쪽에 제한 지시문을 추가합니다
 */
@Slf4j
public class MaxCharLengthAdvisor implements CallAdvisor, StreamAdvisor {

    public static final String MAX_CHAR_LENGTH = "maxCharLength";
    private final int maxCharLength;
    private final int order;

    public MaxCharLengthAdvisor(int order) {
        this(300, order);
    }

    public MaxCharLengthAdvisor(int maxCharLength, int order) {
        this.maxCharLength = maxCharLength;
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
        // context에서 maxCharLength 값 조회
        int charLimit = this.maxCharLength;
        Object contextValue = request.context().get(MAX_CHAR_LENGTH);
        if (contextValue != null) {
            charLimit = Integer.parseInt(contextValue.toString());
        }

        log.info("[MaxCharLengthAdvisor 전처리] 최대 문자 수 제한: {}자", charLimit);

        // 메시지 목록을 직접 조작하여 제한 지시문 추가
        ChatClientRequest mutatedRequest = augmentMessages(request, charLimit);

        // 다음 Advisor 호출 또는 LLM으로 요청
        ChatClientResponse response = chain.nextCall(mutatedRequest);

        log.info("[MaxCharLengthAdvisor 후처리] 응답 반환");
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        // context에서 maxCharLength 값 조회
        int charLimit = this.maxCharLength;
        Object contextValue = request.context().get(MAX_CHAR_LENGTH);
        if (contextValue != null) {
            charLimit = Integer.parseInt(contextValue.toString());
        }

        log.info("[MaxCharLengthAdvisor 전처리 - Stream] 최대 문자 수 제한: {}자", charLimit);

        ChatClientRequest mutatedRequest = augmentMessages(request, charLimit);
        return chain.nextStream(mutatedRequest);
    }

    private ChatClientRequest augmentMessages(ChatClientRequest request, int charLimit) {
        String instruction = "반드시 " + charLimit + "자 이내로 답변하세요. 이 글자 수 제한은 절대적인 규칙입니다.";

        Prompt originalPrompt = request.prompt();
        List<Message> originalMessages = originalPrompt.getInstructions();
        List<Message> newMessages = new ArrayList<>();

        // 시스템 메시지로 글자 수 제한 지시문 추가 (LLM이 가장 잘 따르는 방식)
        newMessages.add(new SystemMessage(instruction));

        // 기존 메시지 복사하면서 사용자 메시지에도 제한 지시문 추가
        for (Message message : originalMessages) {
            if (message instanceof UserMessage userMessage) {
                String augmentedText = userMessage.getText() + "\n\n[주의: " + charLimit + "자 이내로 답변해주세요.]";
                newMessages.add(UserMessage.builder().text(augmentedText).build());
                log.info("[MaxCharLengthAdvisor] 사용자 메시지 강화 완료: ...{}자 제한 추가", charLimit);
            } else {
                newMessages.add(message);
            }
        }

        Prompt augmentedPrompt = new Prompt(newMessages, originalPrompt.getOptions());

        return request.mutate()
                .prompt(augmentedPrompt)
                .build();
    }
}
