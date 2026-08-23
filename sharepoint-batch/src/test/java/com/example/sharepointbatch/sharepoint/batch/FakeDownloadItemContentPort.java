package com.example.sharepointbatch.sharepoint.batch;

import com.example.sharepointbatch.sharepoint.application.port.out.DownloadItemContentPort;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Primary;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

// See FakeListChangedItemsPort's note on @Primary - the real GraphItemDownloadAdapter is still
// component-scanned into the test context too.
@TestComponent
@Primary
class FakeDownloadItemContentPort implements DownloadItemContentPort {

    private final Map<String, byte[]> contentByDriveItemId = new LinkedHashMap<>();

    void whenItem(String driveItemId, String content) {
        contentByDriveItemId.put(driveItemId, content.getBytes(StandardCharsets.UTF_8));
    }

    void reset() {
        contentByDriveItemId.clear();
    }

    @Override
    public byte[] download(String driveItemId) {
        byte[] content = contentByDriveItemId.get(driveItemId);
        if (content == null) {
            throw new IllegalStateException("FakeDownloadItemContentPort has no content configured for driveItemId: " + driveItemId);
        }
        return content;
    }
}
