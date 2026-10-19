package com.example.clientragdemo.ingestion.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.clientragdemo.ingestion.application.domain.model.ContentType;
import com.example.clientragdemo.ingestion.application.port.out.IndexDocumentChunksPort;
import com.example.clientragdemo.ingestion.application.port.out.TranscribeImagePort;
import com.example.clientragdemo.ingestion.configuration.IngestionProperties;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

// Image files and scanned (text-less) PDFs go through the optional vision model; without one they
// fail with a clear message instead of finishing READY with no chunks.
class DocumentIndexingAdapterVisionTest {

    private VectorStore vectorStore;
    private final List<String> transcribed = new ArrayList<>();
    private final TranscribeImagePort vision = (image, mimeType) -> {
        transcribed.add(mimeType);
        return "page text " + transcribed.size();
    };

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
    }

    private IndexDocumentChunksPort adapter(TranscribeImagePort port, int maxPages) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(VectorStore.class, () -> vectorStore);
            context.registerBean(IngestionProperties.class, () -> new IngestionProperties(800, maxPages));
            if (port != null) {
                context.registerBean(TranscribeImagePort.class, () -> port);
            }
            context.registerBean("adapter", Class.forName(
                    "com.example.clientragdemo.ingestion.adapter.out.vectorstore.DocumentIndexingAdapter"));
            context.refresh();
            return context.getBean(IndexDocumentChunksPort.class);
        }
        catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] scannedPdf(int pages) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) {
                PDPage page = new PDPage();
                document.addPage(page);
                // a filled rectangle and no text at all - what a scan looks like to a text extractor
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.addRect(100, 100, 200, 100);
                    content.fill();
                }
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void anImageIsTranscribedAndIndexed() {
        int chunks = adapter(vision, 100).index(1L, "photo.png", new byte[] {1, 2, 3}, ContentType.PNG);

        assertThat(chunks).isGreaterThan(0);
        assertThat(transcribed).containsExactly("image/png");
        verify(vectorStore).add(anyList());
    }

    @Test
    void anImageWithoutAVisionModelFailsClearly() {
        assertThatThrownBy(() -> adapter(null, 100).index(1L, "photo.jpg", new byte[] {1}, ContentType.JPEG))
                .hasMessageContaining("need a vision model");
        verify(vectorStore, never()).add(anyList());
    }

    @Test
    void aScannedPdfIsRenderedPageByPageAndTranscribed() throws Exception {
        int chunks = adapter(vision, 100).index(2L, "scan.pdf", scannedPdf(2), ContentType.PDF);

        assertThat(chunks).isGreaterThan(0);
        assertThat(transcribed).containsExactly("image/png", "image/png");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> added = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(added.capture());
        assertThat(added.getValue()).extracting(Document::getText).anyMatch(t -> t.contains("page text"));
    }

    @Test
    void aScannedPdfWithoutAVisionModelFailsClearly() throws Exception {
        assertThatThrownBy(() -> adapter(null, 100).index(2L, "scan.pdf", scannedPdf(1), ContentType.PDF))
                .hasMessageContaining("needs a vision model");
    }

    @Test
    void aScannedPdfOverThePageLimitFailsBeforeAnyModelCall() throws Exception {
        assertThatThrownBy(() -> adapter(vision, 2).index(2L, "big.pdf", scannedPdf(3), ContentType.PDF))
                .hasMessageContaining("3 pages")
                .hasMessageContaining("vision-max-pages");
        assertThat(transcribed).isEmpty();
    }

    @Test
    void aVisionModelThatFindsNothingFailsTheDocument() {
        TranscribeImagePort blank = (image, mimeType) -> "  ";
        assertThatThrownBy(() -> adapter(blank, 100).index(1L, "empty.png", new byte[] {1}, ContentType.PNG))
                .hasMessageContaining("by the vision model");
    }
}
