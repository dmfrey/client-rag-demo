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
//    office/spreadsheetml/presentationml/etc. Without reflection access, XMLBeans silently falls
//    back to a generic wrapper type instead of the specific one POI expects, surfacing as
//    ClassCastException: XmlComplexContentImpl cannot be cast to ...DocumentDocument - a
//    completely different failure mode from the resource gap above, only found by getting far
//    enough into a real .docx to hit it.
//
// Scoped to wordprocessingml alone (903 classes) rather than the whole org.openxmlformats.schemas
// tree (~4,700+ across every OOXML format, since this app only accepts PDF/DOCX/TXT) - registering
// office+drawingml too (on the theory that a .docx could embed drawings) actually broke the
// native-image build outright: the analysis phase's own deadlock watchdog aborted a real CI run at
// ~10GB heap. Narrowed back to exactly what's proven necessary (DocumentDocument/DocumentDocumentImpl
// are in wordprocessingml) rather than registering defensively; add more scoped packages here only
// if a real document exercising them actually fails, the same way this whole file's scope was found.
public class PoiRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("org/apache/poi/schemas/**");

        // org.apache.poi.schemas itself (the "system.ooxml" type-loader infrastructure -
        // TypeSystemHolder and friends, ~5 classes total, cheap) also needs reflection, not just
        // the resources above - a shared-namespace type (xml:space/xml:lang, standard XML
        // boilerplate every schema references) failed to resolve with SchemaTypeLoaderException
        // even after DocumentDocument/DocumentDocumentImpl (org.openxmlformats.schemas.*, below)
        // started working, since that infrastructure class itself wasn't reflectively accessible.
        PackageReflectionHints.registerPackage(hints, classLoader, "org.apache.poi.schemas");
        PackageReflectionHints.registerPackage(hints, classLoader, "org.openxmlformats.schemas.wordprocessingml");

        // A .docx with a theme part (colors/fonts - present in any document saved by real Word,
        // not just ones with explicit drawings) hits the same generic-wrapper ClassCastException
        // as DocumentDocument above, but for XWPFTheme's ThemeDocument
        // (org.openxmlformats.schemas.drawingml.x2006.main - 713 classes). Scoped to just the
        // "main" drawingml subpackage, not the whole drawingml tree (chart/diagram/spreadsheetDrawing
        // are Excel/PowerPoint-only and this app only accepts PDF/DOCX/TXT) - registering
        // wordprocessingml+officeDocument+drawingml (all subpackages) together previously caused
        // the native-image analysis phase's deadlock watchdog to abort a real CI run at ~10GB heap
        // (see git history), so new packages get added one proven-necessary subpackage at a time
        // rather than a whole top-level tree at once.
        PackageReflectionHints.registerPackage(hints, classLoader, "org.openxmlformats.schemas.drawingml.x2006.main");
    }
}
