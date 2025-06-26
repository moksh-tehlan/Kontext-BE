package com.moksh.kontext.config;

import io.qdrant.client.QdrantClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;


    @Bean
    public OpenAiApi openAiApi() {
        return OpenAiApi.builder()
                .apiKey(openAiApiKey)
                .build();
    }

    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .build();
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    public OpenAiEmbeddingModel openAiEmbeddingModel(OpenAiApi openAiApi) {
        return new OpenAiEmbeddingModel(openAiApi);
    }

    @Bean
    public EmbeddingModel embeddingModel(OpenAiEmbeddingModel openAiEmbeddingModel) {
        return openAiEmbeddingModel;
    }

    @Bean(name = "chat_vector_store")
    public VectorStore chatVectorStore(QdrantClient qdrantClient,OpenAiEmbeddingModel openAiEmbeddingModel) {
        return QdrantVectorStore.builder(
                qdrantClient,openAiEmbeddingModel
        )
                .collectionName("chat_vector_store").initializeSchema(true).build();
    }
}