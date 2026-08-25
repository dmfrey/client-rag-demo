package com.example.clientragdemo.ingestion.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

// Imported by each consuming Spring Boot app's own root/feature configuration (backend's
// DocumentsConfiguration, sharepoint-batch's SharePointConfiguration) - this module has no
// @SpringBootApplication of its own, so nothing scans it unless a consumer explicitly @Imports
// this class, matching this repo's existing per-feature configuration convention just crossing a
// JAR boundary instead of a package one.
@Configuration
@ComponentScan(basePackages = "com.example.clientragdemo.ingestion")
@EnableJdbcRepositories(basePackages = "com.example.clientragdemo.ingestion.adapter.out.persistence")
@EnableConfigurationProperties(IngestionProperties.class)
public class IngestionCoreConfiguration {
}
