package com.example.sharepointbatch.sharepoint.configuration;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.example.clientragdemo.ingestion.configuration.IngestionCoreConfiguration;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = "com.example.sharepointbatch.sharepoint")
@EnableConfigurationProperties(SharePointProperties.class)
@Import(IngestionCoreConfiguration.class)
public class SharePointConfiguration {

    @Bean
    GraphServiceClient graphServiceClient(SharePointProperties properties) {
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .tenantId(properties.tenantId())
                .clientId(properties.clientId())
                .clientSecret(properties.clientSecret())
                .build();
        return new GraphServiceClient(credential, "https://graph.microsoft.com/.default");
    }
}
