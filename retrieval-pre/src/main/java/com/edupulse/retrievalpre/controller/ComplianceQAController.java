package com.edupulse.retrievalpre.controller;

import com.edupulse.retrievalpre.config.RetrievalAdvisorFactory;
import com.edupulse.retrievalpre.filter.SafeFilterParser;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compliance")
public class ComplianceQAController {

    private static final String SYSTEM_PROMPT = """
            You are a web application security compliance assistant based on official OWASP guidance.
            Provide clear and concise answers, prevention steps and recommendations.
            Use ONLY the provided CONTEXT. If the answer is not in the CONTEXT, say "I don't know".
            Each CONTEXT passage starts with a header like [source:filename.pdf, page:12].
            Cite every source you use by copying the header values exactly, e.g. [source:filename.pdf, page:12].
            """;

    private final ChatClient chatClient;
    private final RetrievalAdvisorFactory advisorFactory;
    private final SafeFilterParser filterParser;

    public ComplianceQAController(ChatClient chatClient, RetrievalAdvisorFactory advisorFactory,
                                  SafeFilterParser filterParser) {
        this.chatClient = chatClient;
        this.advisorFactory = advisorFactory;
        this.filterParser = filterParser;
    }

    /**
     * @param question the user question (plain text body)
     * @param filter   optional metadata filter, e.g. source == 'owasp-top10-2021.pdf' && page > 3
     */
    @PostMapping("/ask")
    public String ask(@RequestBody String question,
                      @RequestParam(value = "filter", required = false) String filter) {
        // Only a validated filter object reaches the vector store; no filter means the standard flow.
        // An invalid filter throws InvalidFilterException, answered with 400 by ApiExceptionHandler.
        Filter.Expression filterExpression = filterParser.parse(filter).orElse(null);

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(question)
                .advisors(advisorFactory.create(filterExpression))
                .call()
                .content();
    }
}
