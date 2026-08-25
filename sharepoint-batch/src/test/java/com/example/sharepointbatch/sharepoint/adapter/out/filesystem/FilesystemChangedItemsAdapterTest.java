package com.example.sharepointbatch.sharepoint.adapter.out.filesystem;

import com.example.sharepointbatch.sharepoint.application.domain.model.ChangedItem;
import com.example.sharepointbatch.sharepoint.application.port.out.ListChangedItemsPort.DeltaPage;
import com.example.sharepointbatch.sharepoint.configuration.FilesystemSourceProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FilesystemChangedItemsAdapterTest {

    @TempDir
    Path root;

    @Test
    void scansFilesRecursivelyAsUpsertsWithARelativePathIdentity() throws IOException {
        Files.writeString(root.resolve("alpha.txt"), "alpha content");
        Path sub = Files.createDirectory(root.resolve("sub"));
        Files.writeString(sub.resolve("beta.txt"), "beta content");

        FilesystemChangedItemsAdapter adapter = new FilesystemChangedItemsAdapter(new FilesystemSourceProperties(root.toString()));
        DeltaPage page = adapter.list("ignored");

        assertThat(page.items()).hasSize(2);
        assertThat(page.items()).extracting(ChangedItem::driveItemId).containsExactlyInAnyOrder("alpha.txt", "sub/beta.txt");
        assertThat(page.items()).allMatch(item -> item.changeType() == ChangedItem.ChangeType.UPSERT);
        assertThat(page.items()).allMatch(item -> item.eTag() != null && !item.eTag().isBlank());
        assertThat(page.nextLink()).isNull();
        assertThat(page.deltaLink()).isNotNull();
    }

    @Test
    void changingAFilesContentChangesItsVersion() throws IOException {
        Path file = Files.writeString(root.resolve("alpha.txt"), "v1");
        FilesystemChangedItemsAdapter adapter = new FilesystemChangedItemsAdapter(new FilesystemSourceProperties(root.toString()));
        String firstVersion = adapter.list("ignored").items().get(0).eTag();

        // Force a distinct mtime (some filesystems have 1s mtime resolution) so size+mtime
        // actually changes even though the new content happens to be the same length.
        Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() + 5000));
        Files.writeString(root.resolve("alpha.txt"), "v2");

        String secondVersion = adapter.list("ignored").items().get(0).eTag();
        assertThat(secondVersion).isNotEqualTo(firstVersion);
    }

    @Test
    void skipsADirectoryThatCannotBeListedRatherThanAbortingTheWholeScan() throws IOException {
        // Reproduces a real CF block-storage volume mount: its root always contains a lost+found
        // directory owned by root, permission-denied for this process to list.
        Files.writeString(root.resolve("visible.txt"), "visible content");
        Path restricted = Files.createDirectory(root.resolve("lost+found"));
        Files.writeString(restricted.resolve("hidden.txt"), "hidden content");
        assertThat(restricted.toFile().setReadable(false, false)).isTrue();

        try {
            FilesystemChangedItemsAdapter adapter = new FilesystemChangedItemsAdapter(new FilesystemSourceProperties(root.toString()));
            DeltaPage page = adapter.list("ignored");

            assertThat(page.items()).extracting(ChangedItem::driveItemId).containsExactly("visible.txt");
        }
        finally {
            restricted.toFile().setReadable(true, false);
        }
    }
}
