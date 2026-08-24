package com.example.clientragdemo.ingestion.configuration;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

// XMLBeans itself (not POI - see PoiRuntimeHints for POI's own OOXML schema gaps) ships four
// built-in "system" schema type systems compiled into its own jar under
// org/apache/xmlbeans/metadata/system/{sXMLCONFIG,sXMLLANG,sXMLSCHEMA,sXMLTOOLS} - these back
// shared XML-namespace constructs every schema can reference (sXMLLANG is the one that defines
// xml:space/xml:lang/xml:base/xml:id, i.e. http://www.w3.org/XML/1998/namespace).
// SchemaTypeLoaderImpl.build() looks these four up by Class.forName("org.apache.xmlbeans.metadata
// .system." + holder + ".TypeSystemHolder", ...) and deliberately swallows ClassNotFoundException
// per-holder so a genuinely absent one is silently skipped - which is exactly what happens without
// registration, since GraalVM's static analysis never reaches these classes (nothing in ingestion-
// core's own code references them by name) and native-image never puts them in the image, so the
// lookup fails as "class not found" rather than "reflection not registered" and the resulting
// linker simply has no path to the xml:space/xml:lang types. Surfaced as a real .docx failing with
// SchemaTypeLoaderException: Cannot resolve type for handle _XY_Q=space|R=space@http://www.w3.org
// /XML/1998/namespace - a completely different package tree from PoiRuntimeHints's
// org.apache.poi.schemas (which only covers POI/OOXML-specific schema data, not XMLBeans' own
// built-ins), only found by getting far enough into a real document to hit a paragraph using one of
// these shared attributes. Registers the whole org.apache.xmlbeans.metadata tree - only 4 actual
// classes (one per holder) plus ~660 small .xsb resource files, cheap relative to the
// multi-thousand-class registrations in PoiRuntimeHints/each consuming app's own OpenAiRuntimeHints.
// Lives in ingestion-core (not a consuming app) since this is about what ingestion-core's own
// DocumentIndexingAdapter/Tika parsing does, shared by every app that pulls in this library.
public class XmlBeansRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("org/apache/xmlbeans/metadata/**");
        PackageReflectionHints.registerPackage(hints, classLoader, "org.apache.xmlbeans.metadata");
    }
}
