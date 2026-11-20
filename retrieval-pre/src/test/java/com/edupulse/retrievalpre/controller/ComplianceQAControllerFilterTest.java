package com.edupulse.retrievalpre.controller;

import com.edupulse.retrievalpre.filter.FilterProperties;
import com.edupulse.retrievalpre.filter.SafeFilterParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the HTTP contract for invalid filters: 400 with the list of valid fields.
 * The request is rejected before any call to the LLM or the vector store, so those are not needed here.
 */
class ComplianceQAControllerFilterTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SafeFilterParser parser = new SafeFilterParser(new FilterProperties(
                List.of("doc_id", "source", "version", "is_latest", "page"),
                List.of("eq", "ne", "in", "gt", "gte", "lt", "lte")));

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ComplianceQAController(null, null, parser))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void invalidFieldReturns400WithValidFields() throws Exception {
        mockMvc.perform(post("/api/compliance/ask")
                        .param("filter", "password == 'x'")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("What is XSS?"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid filter"))
                .andExpect(jsonPath("$.detail").value(containsString("password")))
                .andExpect(jsonPath("$.validFields", contains("doc_id", "source", "version", "is_latest", "page")))
                .andExpect(jsonPath("$.validOperators", contains("eq", "ne", "in", "gt", "gte", "lt", "lte")));
    }

    @Test
    void malformedFilterReturns400() throws Exception {
        mockMvc.perform(post("/api/compliance/ask")
                        .param("filter", "===")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("What is XSS?"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Malformed filter expression"));
    }
}
