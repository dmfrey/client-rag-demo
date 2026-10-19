package com.example.clientragdemo.ingestion.adapter.out.vectorstore;

import com.example.clientragdemo.ingestion.application.domain.model.ContentType;
import com.example.clientragdemo.ingestion.application.port.out.DeleteDocumentChunksPort;
import com.example.clientragdemo.ingestion.application.port.out.IndexDocumentChunksPort;
import com.example.clientragdemo.ingestion.application.port.out.TranscribeImagePort;
import com.example.clientragdemo.ingestion.configuration.IngestionProperties;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
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
    private final ObjectProvider<TranscribeImagePort> transcribeImagePort;

    DocumentIndexingAdapter(VectorStore vectorStore, IngestionProperties ingestionProperties,
                            ObjectProvider<TranscribeImagePort> transcribeImagePort) {
        this.vectorStore = vectorStore;
        this.ingestionProperties = ingestionProperties;
        this.transcribeImagePort = transcribeImagePort;
    }

    @Override
    public int index(Long documentId, String filename, byte[] content, ContentType contentType) {
        List<org.springframework.ai.document.Document> parsed = extract(filename, content, contentType);

        Map<String, Object> metadata = Map.of("document_id", documentId, "filename", filename);
        List<org.springframework.ai.document.Document> tagged = parsed.stream()
                .map(document -> new org.springframework.ai.document.Document(document.getText(), metadata))
                .toList();

        List<org.springframework.ai.document.Document> chunks = TokenTextSplitter.builder()
                .withChunkSize(ingestionProperties.chunkSize())
                .build()
                .apply(tagged);

        if (chunks.isEmpty()) {
            throw new IllegalStateException("No text chunks were produced from " + filename);
        }

        vectorStore.add(chunks);

        return chunks.size();
    }

    private List<org.springframework.ai.document.Document> extract(String filename, byte[] content, ContentType contentType) {
        TranscribeImagePort vision = transcribeImagePort.getIfAvailable();

        if (contentType.isImage()) {
            if (vision == null) {
                throw new IllegalStateException("Cannot read " + filename + ": image files need a vision model, and none is"
                        + " configured (bind a vision GenAI service and set app.genai.vision-service)");
            }
            return textDocuments(filename, List.of(vision.transcribe(content, contentType.mimeType())));
        }

        org.springframework.core.io.Resource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        List<org.springframework.ai.document.Document> parsed = new TikaDocumentReader(resource).get();
        if (hasText(parsed)) {
            return parsed;
        }

        // Tika has no OCR here (it needs a Tesseract binary), so an image-only/scanned PDF parses to
        // nothing. With a vision model bound, read the pages through it; otherwise fail loudly rather
        // than mark a document READY with zero chunks.
        if (contentType == ContentType.PDF && vision != null) {
            return textDocuments(filename, transcribePages(filename, content, vision));
        }
        throw new IllegalStateException("No text could be extracted from " + filename
                + (contentType == ContentType.PDF
                        ? " (a scanned or image-only PDF needs a vision model, and none is configured)"
                        : ""));
    }

    private List<String> transcribePages(String filename, byte[] pdf, TranscribeImagePort vision) {
        int pages = PdfPageRenderer.pageCount(pdf);
        if (pages > ingestionProperties.visionMaxPages()) {
            throw new IllegalStateException(filename + " has " + pages + " pages; scanned PDFs are limited to "
                    + ingestionProperties.visionMaxPages() + " pages (app.documents.ingestion.vision-max-pages)");
        }
        return PdfPageRenderer.render(pdf).stream()
                .map(png -> vision.transcribe(png, "image/png"))
                .toList();
    }

    private static List<org.springframework.ai.document.Document> textDocuments(String filename, List<String> texts) {
        List<org.springframework.ai.document.Document> documents = texts.stream()
                .filter(text -> text != null && !text.isBlank())
                .map(org.springframework.ai.document.Document::new)
                .toList();
        if (documents.isEmpty()) {
            throw new IllegalStateException("No text could be read from " + filename + " by the vision model");
        }
        return documents;
    }

    private static boolean hasText(List<org.springframework.ai.document.Document> documents) {
        return documents.stream().anyMatch(document -> document.getText() != null && !document.getText().isBlank());
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        vectorStore.delete(new FilterExpressionBuilder().eq("document_id", documentId).build());
    }
}
