package com.mymindmirror.backend.config;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestVectorStoreConfig {

    @Bean
    @Primary
    public VectorStore vectorStore() {
        // Mock the vector store for tests – we don't need real embeddings
        return mock(VectorStore.class);
    }
}