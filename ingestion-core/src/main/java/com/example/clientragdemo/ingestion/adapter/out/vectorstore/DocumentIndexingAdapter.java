package com.example.clientragdemo.ingestion.adapter.out.vectorstore;

import com.example.clientragdemo.ingestion.application.domain.model.ContentType;
import com.example.clientragdemo.ingestion.application.port.out.DeleteDocumentChunksPort;
import com.example.clientragdemo.ingestion.application.port.out.IndexDocumentChunksPort;
import com.example.clientragdemo.ingestion.configuration.IngestionProperties;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Uses org.springframework.ai.document.Document (fully-qualified throughout) to avoid a name
 * clash with this feature's own domain model, also named Document.
 */
@Component
class DocumentIndexingAdapter implements IndexDocumentChunksPort, DeleteDocumentChunksPort {

    private final VectorStore vectorStore;
    private final IngestionProperties ingestionProperties;

    DocumentIndexingAdapter(VectorStore vectorStore, IngestionProperties ingestionProperties) {
        this.vectorStore = vectorStore;
        this.ingestionProperties = ingestionProperties;
    }

    @Override
    public int index(Long documentId, String filename, byte[] content, ContentType contentType) {
        org.springframework.core.io.Resource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        List<org.springframework.ai.document.Document> parsed = new TikaDocumentReader(resource).get();

        Map<String, Object> metadata = Map.of("document_id", documentId, "filename", filename);
        List<org.springframework.ai.document.Document> tagged = parsed.stream()
                .map(document -> new org.springframework.ai.document.Document(document.getText(), metadata))
                .toList();

        List<org.springframework.ai.document.Document> chunks = TokenTextSplitter.builder()
                .withChunkSize(ingestionProperties.chunkSize())
                .build()
                .apply(tagged);

        vectorStore.add(chunks);

        return chunks.size();
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        vectorStore.delete(new FilterExpressionBuilder().eq("document_id", documentId).build());
    }
}
