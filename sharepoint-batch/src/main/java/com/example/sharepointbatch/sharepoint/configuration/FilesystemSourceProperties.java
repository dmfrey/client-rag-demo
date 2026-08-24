package com.example.sharepointbatch.sharepoint.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// path is the mount point of a volume-mounted directory (e.g. a bound Cloud Foundry
// block-storage service - see manifest.yml) that FilesystemChangedItemsAdapter scans and
// FilesystemItemDownloadAdapter reads from. Only meaningful when app.ingestion.source=filesystem.
@ConfigurationProperties(prefix = "app.filesystem-source")
public record FilesystemSourceProperties(String path) {
}
