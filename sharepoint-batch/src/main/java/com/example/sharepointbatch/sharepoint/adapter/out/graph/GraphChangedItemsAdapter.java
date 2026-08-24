package com.example.sharepointbatch.sharepoint.adapter.out.graph;

import com.example.sharepointbatch.sharepoint.application.domain.model.ChangedItem;
import com.example.sharepointbatch.sharepoint.application.port.out.ListChangedItemsPort;
import com.example.sharepointbatch.sharepoint.configuration.IngestionSourceProperties;
import com.example.sharepointbatch.sharepoint.configuration.SharePointProperties;
import com.microsoft.graph.drives.item.items.item.delta.DeltaGetResponse;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns Microsoft Graph's delta-query paging (@odata.nextLink until a final @odata.deltaLink -
 * see https://learn.microsoft.com/graph/api/driveitem-delta) into this app's own source-agnostic
 * ChangedItem/DeltaPage shape. Folder items are filtered out - only file-type drive items (or any
 * deleted item, whose facets Graph may omit) are surfaced for ingestion. .withUrl(link) is
 * Kiota's standard mechanism for following an opaque continuation/delta URL rather than
 * reconstructing the request from path parameters - used for every call here (including the
 * first, where link is the base delta URL ResolveDeltaLinkTasklet constructs), so this adapter
 * never needs to distinguish "first call" from "resuming a stored delta link".
 *
 * NOTE: unverified against a real SharePoint tenant - there's no way to test against one until
 * the client supplies app registration + site/drive credentials (see the plan). Compiles against
 * the real microsoft-graph SDK jar, but the exact request shape (in particular whether Graph
 * accepts .withUrl() here with no further per-call parameters) should be the first thing smoke-
 * tested once those credentials arrive, before trusting this against production data.
 */
@Component
@ConditionalOnProperty(prefix = "app.ingestion", name = "source", havingValue = IngestionSourceProperties.GRAPH, matchIfMissing = true)
class GraphChangedItemsAdapter implements ListChangedItemsPort {

    private final GraphServiceClient graphServiceClient;
    private final SharePointProperties properties;

    GraphChangedItemsAdapter(GraphServiceClient graphServiceClient, SharePointProperties properties) {
        this.graphServiceClient = graphServiceClient;
        this.properties = properties;
    }

    @Override
    public DeltaPage list(String link) {
        DeltaGetResponse response = graphServiceClient.drives().byDriveId(properties.driveId())
                .items().byDriveItemId("root").delta().withUrl(link).get();

        List<ChangedItem> items = new ArrayList<>();
        if (response != null && response.getValue() != null) {
            for (DriveItem driveItem : response.getValue()) {
                boolean deleted = driveItem.getDeleted() != null;
                if (!deleted && driveItem.getFile() == null) {
                    // Not a deletion and not a file (i.e. a folder) - nothing to ingest.
                    continue;
                }
                items.add(new ChangedItem(
                        driveItem.getId(),
                        driveItem.getName(),
                        driveItem.getETag(),
                        deleted ? ChangedItem.ChangeType.DELETE : ChangedItem.ChangeType.UPSERT));
            }
        }

        String nextLink = response != null ? response.getOdataNextLink() : null;
        String deltaLink = response != null ? response.getOdataDeltaLink() : null;
        return new DeltaPage(items, nextLink, deltaLink);
    }
}
