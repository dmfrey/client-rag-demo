package com.example.sharepointbatch.sharepoint.adapter.out.filesystem;

import com.example.sharepointbatch.sharepoint.configuration.FilesystemSourceProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FilesystemItemDownloadAdapterTest {

    @TempDir
    Path root;

    @Test
    void readsFileContentByItsRelativePathIdentity() throws IOException {
        Files.createDirectory(root.resolve("sub"));
        Files.writeString(root.resolve("sub/beta.txt"), "beta content");

        FilesystemItemDownloadAdapter adapter = new FilesystemItemDownloadAdapter(new FilesystemSourceProperties(root.toString()));
        byte[] content = adapter.download("sub/beta.txt");

        assertThat(new String(content, StandardCharsets.UTF_8)).isEqualTo("beta content");
    }
}
