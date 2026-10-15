package com.example.sharepointbatch.sharepoint.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// path is the mount point of a volume-mounted directory (e.g. a bound Cloud Foundry
// block-storage service - see manifest.yml) that FilesystemChangedItemsAdapter scans and
// FilesystemItemDownloadAdapter reads from. Only meaningful when app.ingestion.source=filesystem.
//
// On Cloud Foundry the path normally isn't set at all: FilesystemVolumeEnvironmentPostProcessor
// reads it from the bound volume service's own VCAP_SERVICES entry (the mount path is only known
// after binding). An explicit app.filesystem-source.path overrides that; with neither, the default
// below applies.
@ConfigurationProperties(prefix = "app.filesystem-source")
public record FilesystemSourceProperties(String path) {

    static final String DEFAULT_PATH = "/mnt/rag-docs";

    public FilesystemSourceProperties {
        if (path == null || path.isBlank()) {
            path = DEFAULT_PATH;
        }
    }
}
