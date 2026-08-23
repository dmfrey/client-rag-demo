package com.example.sharepointbatch.sharepoint.application.domain.model;

// UPSERT covers both "new file" and "modified file" - Graph's delta response doesn't distinguish
// them itself (see ListChangedItemsPort), so that distinction is left to whoever consumes this
// (ChangedItemProcessor derives it from whether a Document row already exists for this item).
public record ChangedItem(String driveItemId, String filename, String eTag, ChangeType changeType) {

    public enum ChangeType { UPSERT, DELETE }
}
