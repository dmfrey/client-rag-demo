package com.example.sharepointbatch.sharepoint.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// tenantId/clientId/clientSecret identify the Azure AD app registration the client provides
// (application permission Sites.Selected, scoped to just siteId, per CLAUDE.md/the plan) -
// client-credentials/app-only auth, no interactive user. driveId is the target document
// library's drive id (resolved once via Graph Explorer or a one-off Graph call against siteId,
// not re-resolved by this app at runtime - keeps the delta-query path a single, simple GET).
@ConfigurationProperties(prefix = "app.sharepoint")
public record SharePointProperties(
        String tenantId,
        String clientId,
        String clientSecret,
        String siteId,
        String driveId,
        @DefaultValue("50") int skipLimit
) {}
