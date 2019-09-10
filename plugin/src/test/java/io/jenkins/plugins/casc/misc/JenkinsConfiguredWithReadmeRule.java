package io.jenkins.plugins.casc.misc;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.core.StringContains;
import org.jvnet.hudson.test.JenkinsRule;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

/**
 * @author v1v (Victor Martinez)
 */
public class JenkinsConfiguredWithReadmeRule extends JenkinsRule {

    @Override
    public void before() throws Throwable {
        super.before();
        ConfiguredWithReadme configuredWithReadme = getConfiguredWithReadme();
        if (Objects.nonNull(configuredWithReadme)) {

            final Class<?> clazz = env.description().getTestClass();
            final String[] resource = configuredWithReadme.value();

            // TODO: transform from README to Code

            final List<String> configs = Arrays.stream(resource)
                .map(s -> clazz.getResource(s).toExternalForm())
                .collect(Collectors.toList());

            try {
                ConfigurationAsCode.get().configure(configs);
            } catch (Throwable t) {
                if (!configuredWithReadme.expected().isInstance(t)) {
                    throw new AssertionError("Unexpected exception ", t);
                } else {
                    if (!StringUtils.isBlank(configuredWithReadme.message())) {
                        boolean match = new StringContains(configuredWithReadme.message())
                            .matches(t.getMessage());
                        if (!match) {
                            throw new AssertionError(
                                "Exception did not contain the expected string: "
                                    + configuredWithReadme.message() + "\nMessage was:\n" + t
                                    .getMessage());
                        }
                    }
                }
            }
        }
    }

    private ConfiguredWithReadme getConfiguredWithReadme() {
        ConfiguredWithReadme configuredWithReadme = env.description()
            .getAnnotation(ConfiguredWithReadme.class);
        if (Objects.nonNull(configuredWithReadme)) {
            return configuredWithReadme;
        }
        for (Field field : env.description().getTestClass().getFields()) {
            if (field.isAnnotationPresent(ConfiguredWithReadme.class)) {
                int m = field.getModifiers();
                Class<?> clazz = field.getType();
                if (isPublic(m) && isStatic(m) &&
                    clazz.isAssignableFrom(JenkinsConfiguredWithReadmeRule.class)) {
                    configuredWithReadme = field.getAnnotation(ConfiguredWithReadme.class);
                    if (Objects.nonNull(configuredWithReadme)) {
                        return configuredWithReadme;
                    }
                } else {
                    throw new IllegalStateException(
                        "Field must be public static JenkinsConfiguredWithReadmeRule");
                }
            }
        }
        return null;
    }

    //TODO: Looks like API defect, exception should be thrown
    /**
     * Exports the Jenkins configuration to a string.
     * @return YAML as string
     * @param strict Fail if any export operation returns error
     * @throws Exception Export error
     * @throws AssertionError Failed to export the configuration
     * @since 1.25
     */
    public String exportToString(boolean strict) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConfigurationAsCode.get().export(out);
        final String s = out.toString(StandardCharsets.UTF_8.name());
        if (strict && s.contains("Failed to export")) {
            throw new AssertionError("Failed to export the configuration: " + s);
        }
        return s;
    }
}
