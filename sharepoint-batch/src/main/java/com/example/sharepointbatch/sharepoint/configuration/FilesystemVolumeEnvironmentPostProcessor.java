package com.example.sharepointbatch.sharepoint.configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

// Fills in app.filesystem-source.path from the mount path of the bound volume service (a Cloud
// Foundry block-storage service, rag-docs-volume by default) when nothing sets it explicitly.
//
// The container path a bound volume gets is only knowable AFTER binding - CF assigns a GUID-based
// one per binding (/var/vcap/data/<binding-instance-guid>) and block-storage publishes no
// bind-time parameter to choose it - so it can't be written into a manifest, and it changes every
// time the binding is recreated. It is, however, right there in the app's own VCAP_SERVICES
// (<service>.volume_mounts[].container_dir), so this reads it from there instead of a hand-set
// APP_FILESYSTEM_SOURCE_PATH that had to be re-copied after every fresh bind.
//
// An explicit app.filesystem-source.path (env var, system property, config file) always wins - the
// resolved value is added as the lowest-precedence property source, and nothing is done if the
// property is already set. app.filesystem-source.volume-service names the binding to read.
public class FilesystemVolumeEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PATH_PROPERTY = "app.filesystem-source.path";
    static final String VOLUME_SERVICE_PROPERTY = "app.filesystem-source.volume-service";
    static final String DEFAULT_VOLUME_SERVICE = "rag-docs-volume";
    static final String PROPERTY_SOURCE_NAME = "cfenvFilesystemVolume";

    private final Supplier<CfEnv> cfEnv;

    public FilesystemVolumeEnvironmentPostProcessor() {
        this(CfEnv::new);
    }

    FilesystemVolumeEnvironmentPostProcessor(Supplier<CfEnv> cfEnv) {
        this.cfEnv = cfEnv;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.containsProperty(PATH_PROPERTY)) {
            return;
        }
        String volumeService = environment.getProperty(VOLUME_SERVICE_PROPERTY, DEFAULT_VOLUME_SERVICE);
        String mountPath = mountPath(volumeService);
        if (mountPath != null) {
            environment.getPropertySources()
                    .addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, Map.of(PATH_PROPERTY, mountPath)));
        }
    }

    private String mountPath(String volumeService) {
        CfService service;
        try {
            service = cfEnv.get().findServiceByName(volumeService);
        } catch (RuntimeException e) {
            return null; // not on Cloud Foundry, or no such binding - the default path applies
        }
        if (service.getMap().get("volume_mounts") instanceof List<?> mounts) {
            for (Object mount : mounts) {
                if (mount instanceof Map<?, ?> m && m.get("container_dir") instanceof String dir && !dir.isBlank()) {
                    return dir;
                }
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        // After config data is loaded, so an explicit setting in application*.yaml is seen above.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
