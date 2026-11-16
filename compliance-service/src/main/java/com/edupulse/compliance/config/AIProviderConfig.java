package com.edupulse.compliance.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RagConfigData.class)
public class AIProviderConfig {

    /**
     * Plain chat client without default advisors.
     * The retrieval advisor is attached per request, so a metadata filter can be applied when needed.
     */
    @Bean
    ChatClient complianceChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }
}
