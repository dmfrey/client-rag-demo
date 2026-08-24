package com.example.sharepointbatch.sharepoint.adapter.out.filesystem;

import com.example.sharepointbatch.sharepoint.application.domain.model.ChangedItem;
import com.example.sharepointbatch.sharepoint.application.port.out.ListChangedItemsPort;
import com.example.sharepointbatch.sharepoint.configuration.FilesystemSourceProperties;
import com.example.sharepointbatch.sharepoint.configuration.IngestionSourceProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Stands in for the Graph adapter when reading from a mounted directory instead of real
 * SharePoint (e.g. a Cloud Foundry block-storage volume - see FilesystemSourceProperties and
 * manifest.yml) - a real alternative production input, not just test scaffolding, per the
 * client's own suggestion that an NFS-style mount might substitute for SharePoint in some
 * environments.
 *
 * A plain filesystem has no delta-link/paging concept, so this always does a full recursive scan
 * of the configured directory in one page (link is accepted for interface compatibility with
 * ChangedItemReader but otherwise ignored - every call returns everything, not a continuation).
 * size+lastModifiedTime stands in for Graph's eTag as a lightweight change-detection signal.
 *
 * Deletions are NOT detected - that requires diffing this scan against every previously-ingested
 * document from this source (LoadAllDocumentsPort filtered by source), a different shape of check
 * than "list what's currently on disk", and out of scope for this pass. A real gap if this became
 * a permanent production input rather than a one-off test corpus.
 */
@Component
@ConditionalOnProperty(prefix = "app.ingestion", name = "source", havingValue = IngestionSourceProperties.FILESYSTEM)
class FilesystemChangedItemsAdapter implements ListChangedItemsPort {

    private final FilesystemSourceProperties properties;

    FilesystemChangedItemsAdapter(FilesystemSourceProperties properties) {
        this.properties = properties;
    }

    @Override
    public DeltaPage list(String link) {
        Path root = Path.of(properties.path());
        List<ChangedItem> items = new ArrayList<>();

        try (var paths = Files.walk(root)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                if (!Files.isRegularFile(path)) {
                    continue;
                }
                BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
                String relativePath = root.relativize(path).toString();
                String version = attributes.size() + ":" + attributes.lastModifiedTime().toMillis();
                items.add(new ChangedItem(relativePath, path.getFileName().toString(), version, ChangedItem.ChangeType.UPSERT));
            }
        }
        catch (IOException ex) {
            throw new UncheckedIOException("Failed to scan filesystem ingestion source at " + root, ex);
        }

        // deltaLink non-null (not nextLink) signals "final page" to ChangedItemReader - a full
        // scan is always exactly one page. The value itself is never read back meaningfully (see
        // ResolveDeltaLinkTasklet), it just needs to be non-null.
        return new DeltaPage(items, null, "filesystem-scan-complete");
    }
}
