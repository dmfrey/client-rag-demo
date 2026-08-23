package com.example.sharepointbatch.sharepoint.batch;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class SyncStateJdbcAdapter implements SyncStatePort {

    private final JdbcTemplate jdbcTemplate;

    SyncStateJdbcAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<String> loadDeltaLink(String driveId) {
        return jdbcTemplate.query(
                        "select delta_link from sharepoint_sync_state where drive_id = ?",
                        (rs, rowNum) -> rs.getString("delta_link"), driveId)
                .stream().findFirst();
    }

    @Override
    public void saveDeltaLink(String driveId, String deltaLink) {
        int updated = jdbcTemplate.update(
                "update sharepoint_sync_state set delta_link = ?, updated_at = now() where drive_id = ?",
                deltaLink, driveId);
        if (updated == 0) {
            jdbcTemplate.update(
                    "insert into sharepoint_sync_state (drive_id, delta_link, updated_at) values (?, ?, now())",
                    driveId, deltaLink);
        }
    }
}
