package com.example.sharepointbatch.sharepoint.batch;

import java.util.Optional;

// Persists the delta link between poll runs - deliberately outside Spring Batch's own
// ExecutionContext (a per-JobInstance scratch space, not durable across separate scheduled
// JobInstances) and outside ingestion-core (this is SharePoint-specific, not shared ingestion
// logic). Package-private: only this feature's own batch wiring needs it.
interface SyncStatePort {

    Optional<String> loadDeltaLink(String driveId);

    void saveDeltaLink(String driveId, String deltaLink);
}
