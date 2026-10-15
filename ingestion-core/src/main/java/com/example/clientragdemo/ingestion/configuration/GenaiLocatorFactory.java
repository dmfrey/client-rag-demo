package com.example.clientragdemo.ingestion.configuration;

import io.pivotal.cfenv.boot.genai.GenaiLocator;

// Builds a GenaiLocator for one bound GenAI service, looked up by its service instance name.
// java-cfenv-boot-tanzu-genai's own auto-configured locator is not used: it exposes a single
// locator (one genai.locator.* property set), so with a separate service per model the bindings
// would overwrite each other.
@FunctionalInterface
public interface GenaiLocatorFactory {

    GenaiLocator forService(String serviceName);
}
