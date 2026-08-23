package com.example.sharepointbatch.sharepoint.adapter.out.graph;

import com.example.sharepointbatch.sharepoint.application.port.out.DownloadItemContentPort;
import com.example.sharepointbatch.sharepoint.configuration.SharePointProperties;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

// See GraphChangedItemsAdapter's note on this adapter family being unverified against a real
// tenant - the driveItem content-stream download path here is the other half of that.
@Component
class GraphItemDownloadAdapter implements DownloadItemContentPort {

    private final GraphServiceClient graphServiceClient;
    private final SharePointProperties properties;

    GraphItemDownloadAdapter(GraphServiceClient graphServiceClient, SharePointProperties properties) {
        this.graphServiceClient = graphServiceClient;
        this.properties = properties;
    }

    @Override
    public byte[] download(String driveItemId) {
        try (InputStream in = graphServiceClient.drives().byDriveId(properties.driveId())
                .items().byDriveItemId(driveItemId).content().get()) {
            return in.readAllBytes();
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
