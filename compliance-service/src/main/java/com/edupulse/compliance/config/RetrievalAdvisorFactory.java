package com.edupulse.compliance.config;

import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds the RetrievalAugmentationAdvisor that searches the OWASP knowledge base stored in Qdrant.
 */
@Component
public class RetrievalAdvisorFactory {

    // Metadata keys written by the ingestion pipeline
    public static final String SOURCE = "source";
    public static final String PAGE_NUMBER = "page_number";
    private static final String UNKNOWN = "unknown";

    private final VectorStore vectorStore;
    private final RagConfigData ragConfigData;

    public RetrievalAdvisorFactory(VectorStore vectorStore, RagConfigData ragConfigData) {
        this.vectorStore = vectorStore;
        this.ragConfigData = ragConfigData;
    }

    /**
     * @param filterExpression optional metadata filter (e.g. source == 'owasp-top10-2021.pdf'); null means no filter
     */
    public RetrievalAugmentationAdvisor create(Filter.Expression filterExpression) {
        var retrieverBuilder = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(ragConfigData.topK())
                .similarityThreshold(ragConfigData.similarityThreshold());
        if (filterExpression != null) {
            retrieverBuilder.filterExpression(filterExpression);
        }
        DocumentRetriever retriever = retrieverBuilder.build();

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(query -> withSourceHeaders(retriever.retrieve(query)))
                // If no relevant chunk is found, the model must refuse instead of answering from its own knowledge
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(false)
                        .build())
                .build();
    }

    // The model only sees the chunk text, so the citation header is prepended to it
    private List<Document> withSourceHeaders(List<Document> documents) {
        return documents.stream()
                .map(document -> document.mutate()
                        .text("[source:%s, page_number:%s]%n%s".formatted(
                                document.getMetadata().getOrDefault(SOURCE, UNKNOWN),
                                document.getMetadata().getOrDefault(PAGE_NUMBER, UNKNOWN),
                                document.getText()))
                        .build())
                .toList();
    }
}
