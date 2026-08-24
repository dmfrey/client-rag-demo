package com.example.sharepointbatch.sharepoint.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// Selects which ListChangedItemsPort/DownloadItemContentPort adapter pair is active - "graph"
// (default, real SharePoint via Microsoft Graph) or "filesystem" (a mounted directory, e.g. a
// Cloud Foundry block-storage volume - see FilesystemSourceProperties). Exactly one pair is ever
// wired in; each adapter/the GraphServiceClient bean is gated with @ConditionalOnProperty against
// this same property so a filesystem-only deployment never needs real Graph credentials at all.
@ConfigurationProperties(prefix = "app.ingestion")
public record IngestionSourceProperties(@DefaultValue("graph") String source) {

    public static final String GRAPH = "graph";
    public static final String FILESYSTEM = "filesystem";
}
