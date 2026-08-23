package com.example.sharepointbatch.sharepoint.application.port.out;

import com.example.sharepointbatch.sharepoint.application.domain.model.ChangedItem;

import java.util.List;

// Wraps Microsoft Graph's delta-query paging (see the Graph adapter under adapter/out/graph) -
// this port and DownloadItemContentPort are the only places SharePoint/Graph specifics are
// allowed to leak into this app's own port boundary; everything past here is source-agnostic.
public interface ListChangedItemsPort {

    DeltaPage list(String link);

    // Exactly one of nextLink/deltaLink is non-null: nextLink means more pages follow (call list
    // again with it); deltaLink means this was the final page - persist it for the next poll.
    record DeltaPage(List<ChangedItem> items, String nextLink, String deltaLink) {}
}
