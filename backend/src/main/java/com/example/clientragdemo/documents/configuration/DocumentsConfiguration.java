package com.example.clientragdemo.documents.configuration;

import com.example.clientragdemo.ingestion.configuration.IngestionCoreConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = "com.example.clientragdemo.documents")
@Import(IngestionCoreConfiguration.class)
class DocumentsConfiguration {
}
