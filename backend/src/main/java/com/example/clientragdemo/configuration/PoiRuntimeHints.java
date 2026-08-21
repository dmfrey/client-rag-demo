package com.example.clientragdemo.configuration;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

// Apache POI's OOXML support (pulled in transitively via Tika's Word/Excel/PowerPoint parsers -
// see build.gradle's comment on spring-ai-tika-document-reader) ships its compiled XMLBeans
// schema definitions as ~9,000 individual .xsb resource files under org/apache/poi/schemas
// (confirmed by inspecting poi-ooxml-full's jar directly - like PDFBox, it has no
// META-INF/native-image directory of its own). XMLBeans' schema type loader reads these lazily,
// by name, as it walks a document's actual XML structure, so which ones are needed depends on
// which document features/schema types a given DOCX exercises - only surfaced in the first place
// by a real .docx failing with SchemaTypeLoaderException: Could not locate compiled schema
// resource .../index.xsb. Registering the whole tree up front avoids discovering the next
// missing one 9,000 separate times, one real document upload and one native-image rebuild at a
// time.
public class PoiRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("org/apache/poi/schemas/**");
    }
}
