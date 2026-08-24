package com.example.clientragdemo.ingestion.configuration;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

// Apache PDFBox (pulled in transitively via Tika's PDF parser - see build.gradle's comment on
// spring-ai-tika-document-reader) ships none of its own native-image metadata: every classpath
// resource it reads at runtime (org/apache/pdfbox/resources/afm - font metrics for the 14
// standard PDF fonts, glyphlist, icc color profiles, text, ttf) is silently excluded from the
// native image unless registered. Undetectable without a real PDF exercising the exact resource
// path - the specific failure this fixes (missing ZapfDingbats.afm) only surfaced against a real
// production PDF upload, not any local/CI testing (Tika's own reachability metadata, if any,
// doesn't cover PDFBox's resources - confirmed empirically, not by inspecting PDFBox's jar, which
// has no META-INF/native-image directory at all). Registers the whole resources tree rather than
// one file at a time as different PDFs exercise different fonts/color spaces/CMaps. Lives in
// ingestion-core (not a consuming app) since this is about what ingestion-core's own
// DocumentIndexingAdapter/Tika parsing does, shared by every app that pulls in this library.
public class PdfBoxRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("org/apache/pdfbox/resources/**");
    }
}
