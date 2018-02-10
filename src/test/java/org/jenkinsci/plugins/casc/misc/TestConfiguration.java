package org.jenkinsci.plugins.casc.misc;

import org.jenkinsci.plugins.casc.ConfigurationAsCode;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Loads resource as configuration-as-code
 */
public class TestConfiguration {
    private final String resource;

    public TestConfiguration(String resource) {
        this.resource = resource;
    }

    public void configure(Class<?> clazz) {
        try (Reader reader = new InputStreamReader(clazz.getResourceAsStream(resource), UTF_8)) {
            ((Map<String, Object>) new Yaml().loadAs(reader, Map.class))
                    .entrySet()
                    .forEach(ConfigurationAsCode::configureWith);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
