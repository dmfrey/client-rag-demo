package com.example.sharepointbatch.sharepoint.batch;

import com.example.clientragdemo.ingestion.application.port.in.DeleteDocumentUseCase;
import com.example.clientragdemo.ingestion.application.port.in.DeleteDocumentUseCase.DeleteDocumentCommand;
import com.example.clientragdemo.ingestion.application.port.in.IngestDocumentUseCase;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
class ChangedItemWriter implements ItemWriter<SyncOutcome> {

    private final IngestDocumentUseCase ingestDocumentUseCase;
    private final DeleteDocumentUseCase deleteDocumentUseCase;

    ChangedItemWriter(IngestDocumentUseCase ingestDocumentUseCase, DeleteDocumentUseCase deleteDocumentUseCase) {
        this.ingestDocumentUseCase = ingestDocumentUseCase;
        this.deleteDocumentUseCase = deleteDocumentUseCase;
    }

    @Override
    public void write(Chunk<? extends SyncOutcome> chunk) {
        for (SyncOutcome outcome : chunk) {
            switch (outcome) {
                case SyncOutcome.Ingest ingest -> ingestDocumentUseCase.execute(ingest.command());
                case SyncOutcome.Delete delete -> deleteDocumentUseCase.execute(new DeleteDocumentCommand(delete.documentId()));
            }
        }
    }
}
