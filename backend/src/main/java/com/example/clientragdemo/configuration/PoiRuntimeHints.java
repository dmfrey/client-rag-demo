package com.example.clientragdemo.configuration;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

// Apache POI's OOXML support (pulled in transitively via Tika's Word/Excel/PowerPoint parsers -
// see build.gradle's comment on spring-ai-tika-document-reader) needs two separate kinds of
// native-image registration, neither shipped by POI itself (no META-INF/native-image directory in
// its jars, confirmed by inspecting them directly):
//
// 1. Resources: its compiled XMLBeans schema definitions are ~9,000 individual .xsb files under
//    org/apache/poi/schemas, read lazily by name as XMLBeans walks a document's actual XML
//    structure - only surfaced by a real .docx failing with SchemaTypeLoaderException: Could not
//    locate compiled schema resource .../index.xsb.
// 2. Reflection: the generated interface+impl classes XMLBeans instantiates for each OOXML schema
//    element (e.g. DocumentDocument/DocumentDocumentImpl for the root of a .docx's word/document.xml)
//    live under org.openxmlformats.schemas - thousands of classes across drawingml/wordprocessingml/
//    office/etc. Without reflection access, XMLBeans silently falls back to a generic wrapper type
//    instead of the specific one POI expects, surfacing as
//    ClassCastException: XmlComplexContentImpl cannot be cast to ...DocumentDocument - a
//    completely different failure mode from the resource gap above, only found by getting far
//    enough into a real .docx to hit it. Scoped to wordprocessingml/office/drawingml (Word
//    documents can embed drawings/images via drawingml) rather than the full tree, which also
//    includes spreadsheetml/presentationml/Visio schemas this app's PDF/DOCX/TXT-only scope never
//    reaches.
public class PoiRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("org/apache/poi/schemas/**");

        PackageReflectionHints.registerPackage(hints, classLoader, "org.openxmlformats.schemas.wordprocessingml");
        PackageReflectionHints.registerPackage(hints, classLoader, "org.openxmlformats.schemas.office");
        PackageReflectionHints.registerPackage(hints, classLoader, "org.openxmlformats.schemas.drawingml");
    }
}
