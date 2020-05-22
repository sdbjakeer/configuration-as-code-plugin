package io.jenkins.plugins.casc.yaml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class YamlSourceTest {

    @Test
    public void shouldHaveInformativeToStringForUrlSource() throws IOException {
        //given
        String testUrl = "http://example.com/foo/bar";
        //and
        YamlSource<String> yamlSource = YamlSource.of(testUrl);
        //expect
        assertEquals("YamlSource: " + testUrl, yamlSource.toString());
    }

    @Test
    public void shouldUseToStringOfSourceInToStringForInputStream() {
        //given
        InputStream testInputStream = new ByteArrayInputStream("IS content".getBytes(StandardCharsets.UTF_8));
        String testInputStreamToString = testInputStream.toString();
        //and
        YamlSource<InputStream> yamlSource = YamlSource.of(testInputStream);
        //expect
        assertEquals("YamlSource: " + testInputStreamToString, yamlSource.toString());
    }

    @Test
    public void shouldHaveInformativeToStringForPathSource() {
        Path path = new File("./test").toPath();
        String testPathToString = path.toString();
        YamlSource<Path> yamlSource = YamlSource.of(path);
        assertEquals("YamlSource: " + testPathToString, yamlSource.toString());
    }
}
