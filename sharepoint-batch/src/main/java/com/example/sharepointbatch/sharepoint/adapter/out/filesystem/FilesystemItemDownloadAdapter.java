package com.example.sharepointbatch.sharepoint.adapter.out.filesystem;

import com.example.sharepointbatch.sharepoint.application.port.out.DownloadItemContentPort;
import com.example.sharepointbatch.sharepoint.configuration.FilesystemSourceProperties;
import com.example.sharepointbatch.sharepoint.configuration.IngestionSourceProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

// driveItemId here is the relative path FilesystemChangedItemsAdapter used as each ChangedItem's
// identity - resolved back against the same configured root to read the file's bytes.
@Component
@ConditionalOnProperty(prefix = "app.ingestion", name = "source", havingValue = IngestionSourceProperties.FILESYSTEM)
class FilesystemItemDownloadAdapter implements DownloadItemContentPort {

    private final FilesystemSourceProperties properties;

    FilesystemItemDownloadAdapter(FilesystemSourceProperties properties) {
        this.properties = properties;
    }

    @Override
    public byte[] download(String driveItemId) {
        Path path = Path.of(properties.path()).resolve(driveItemId);
        try {
            return Files.readAllBytes(path);
        }
        catch (IOException ex) {
            throw new UncheckedIOException("Failed to read filesystem ingestion source file " + path, ex);
        }
    }
}
