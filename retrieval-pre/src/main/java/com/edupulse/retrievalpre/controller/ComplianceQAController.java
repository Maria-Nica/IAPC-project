package com.edupulse.retrievalpre.controller;

import com.edupulse.retrievalpre.config.RetrievalAdvisorFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
    private final FilterExpressionTextParser filterParser = new FilterExpressionTextParser();

    public ComplianceQAController(ChatClient chatClient, RetrievalAdvisorFactory advisorFactory) {
        this.chatClient = chatClient;
        this.advisorFactory = advisorFactory;
    }

    /**
     * @param question the user question (plain text body)
     * @param filter   optional metadata filter, e.g. source == 'owasp-top10-2021.pdf'
     */
    @PostMapping("/ask")
    public String ask(@RequestBody String question,
                      @RequestParam(value = "filter", required = false) String filter) {
        Filter.Expression filterExpression = (filter == null || filter.isBlank()) ? null : parseFilter(filter);

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(question)
                .advisors(advisorFactory.create(filterExpression))
                .call()
                .content();
    }

    private Filter.Expression parseFilter(String filter) {
        try {
            return filterParser.parse(filter);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filter expression: " + filter, e);
        }
    }
}
