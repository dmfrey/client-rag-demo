package com.example.sharepointbatch.sharepoint.batch;

import com.example.clientragdemo.ingestion.application.port.in.IngestDocumentUseCase.IngestDocumentCommand;

// What ChangedItemProcessor decided to do with one ChangedItem, for ChangedItemWriter to
// execute. A null processor result (neither variant) means "nothing to do" - see
// ChangedItemProcessor for the cases that produce one (unchanged eTag, unsupported file type, a
// delete for an item never ingested).
sealed interface SyncOutcome {

    record Ingest(IngestDocumentCommand command) implements SyncOutcome {}

    record Delete(Long documentId) implements SyncOutcome {}
}
