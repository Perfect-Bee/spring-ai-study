package com.study.springai.assignment.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class AiConfig {

    // ================================================================
    //  기본 프로필: OpenAI
    // ================================================================
    @Bean
    @Profile("!ollama")
    ChatClient chatClient(@Qualifier("openAiChatModel") ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    // ================================================================
    //  Ollama 프로필: 로컬 LLM
    //  실행: ./gradlew bootRun --args='--spring.profiles.active=ollama'
    // ================================================================
    @Bean
    @Profile("ollama")
    ChatClient ollamaChatClient(@Qualifier("ollamaChatModel") ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
