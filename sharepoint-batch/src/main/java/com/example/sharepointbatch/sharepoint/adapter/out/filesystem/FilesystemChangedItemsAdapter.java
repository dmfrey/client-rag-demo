package com.example.sharepointbatch.sharepoint.adapter.out.filesystem;

import com.example.sharepointbatch.sharepoint.application.domain.model.ChangedItem;
import com.example.sharepointbatch.sharepoint.application.port.out.ListChangedItemsPort;
import com.example.sharepointbatch.sharepoint.configuration.FilesystemSourceProperties;
import com.example.sharepointbatch.sharepoint.configuration.IngestionSourceProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
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
 *
 * Uses Files.walkFileTree with a visitor rather than the simpler Files.walk(root) stream: a real
 * CF block-storage volume mount's root always contains a lost+found directory (an ext4/most-Linux-
 * filesystem artifact, not something this app created), permission-denied for a non-root process -
 * confirmed on a real deploy. Files.walk's lazy stream throws an *unchecked* IOException
 * (java.io.UncheckedIOException) the moment iteration reaches an unreadable directory, which
 * propagated straight past this method's own try/catch (that only caught the checked IOException
 * Files.walk itself can throw on the initial call, not the one its lazy iteration throws later)
 * and aborted the entire scan - repeatedly, since ChangedItemReader retries a failed read() until
 * SkipLimitExceededException. lost+found specifically fails when walkFileTree tries to *open* it
 * to list its entries (its own owner/permission bits deny that, not just reading a specific file
 * inside it) - per Files.walkFileTree's own contract, that failure is delivered to
 * postVisitDirectory(dir, exc), not visitFileFailed (which only covers a failure visiting a single
 * file/directory *entry*, e.g. an unreadable file's attributes). SimpleFileVisitor's default
 * postVisitDirectory re-throws a non-null exc instead of swallowing it, so both methods need
 * overriding to let one inaccessible entry be skipped without losing the rest of the tree.
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

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attributes) {
                    if (attributes.isRegularFile()) {
                        String relativePath = root.relativize(path).toString();
                        String version = attributes.size() + ":" + attributes.lastModifiedTime().toMillis();
                        items.add(new ChangedItem(relativePath, path.getFileName().toString(), version, ChangedItem.ChangeType.UPSERT));
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path path, IOException ex) {
                    // A single file/directory entry couldn't be visited (e.g. its attributes
                    // couldn't be read) - skip it rather than aborting the whole scan.
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException ex) {
                    // A directory's own contents couldn't be listed (e.g. lost+found's
                    // permission bits deny opening it at all) - skip it rather than aborting the
                    // whole scan. Only reached with a non-null ex when something actually failed;
                    // normal completion (ex == null) always continues.
                    return FileVisitResult.CONTINUE;
                }
            });
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
