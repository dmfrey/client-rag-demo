package com.example.sharepointbatch.sharepoint.batch;

import com.example.sharepointbatch.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// Reuses the same @SpringBootTest/@Import signature as SharePointSyncJobIT so Spring's test
// context cache reuses the same ApplicationContext/Testcontainers rather than starting a second
// one.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SyncStateJdbcAdapterIT {

    @Autowired
    private SyncStatePort syncStatePort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearSyncState() {
        jdbcTemplate.update("delete from sharepoint_sync_state");
    }

    @Test
    void loadReturnsEmptyForAnUnknownDrive() {
        assertThat(syncStatePort.loadDeltaLink("unknown-drive")).isEmpty();
    }

    @Test
    void saveInsertsThenUpdatesTheSameDrivesLink() {
        syncStatePort.saveDeltaLink("drive-1", "link-v1");
        assertThat(syncStatePort.loadDeltaLink("drive-1")).contains("link-v1");

        syncStatePort.saveDeltaLink("drive-1", "link-v2");

        Optional<String> reloaded = syncStatePort.loadDeltaLink("drive-1");
        assertThat(reloaded).contains("link-v2");

        Integer rowCount = jdbcTemplate.queryForObject(
                "select count(*) from sharepoint_sync_state where drive_id = ?", Integer.class, "drive-1");
        assertThat(rowCount).isEqualTo(1);
    }
}
