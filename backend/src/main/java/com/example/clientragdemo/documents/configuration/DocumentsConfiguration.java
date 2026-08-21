package com.example.clientragdemo.documents.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@Configuration
@ComponentScan(basePackages = "com.example.clientragdemo.documents")
@EnableJdbcRepositories(basePackages = "com.example.clientragdemo.documents.adapter.out.persistence")
@EnableConfigurationProperties(DocumentIngestionProperties.class)
class DocumentsConfiguration {
}
