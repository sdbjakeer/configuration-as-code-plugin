package org.jenkinsci.plugins.casc;

import org.jenkinsci.plugins.casc.model.Scalar;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class PrimitiveConfiguratorTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public final EnvironmentVariables environment = new EnvironmentVariables();

    @Test
    public void _boolean() throws Exception {
        Configurator c = Configurator.lookup(boolean.class);
        final Object value = c.configure(new Scalar("true"));
        assertTrue((Boolean) value);
    }

    @Test
    public void _int() throws Exception {
        Configurator c = Configurator.lookup(int.class);
        final Object value = c.configure(new Scalar("123"));
        assertEquals(123, (int) value);
    }

    @Test
    public void _Integer() throws Exception {
        Configurator c = Configurator.lookup(Integer.class);
        final Object value = c.configure(new Scalar("123"));
        assertTrue(123 == ((Integer) value).intValue());
    }

    @Test
    public void _string() throws Exception {
        Configurator c = Configurator.lookup(String.class);
        final Object value = c.configure(new Scalar("abc"));
        assertEquals("abc", value);
    }

    @Test
    public void _Integer_env() throws Exception {
        environment.set("ENV_FOR_TEST", "123");
        Configurator c = Configurator.lookup(Integer.class);
        final Object value = c.configure(new Scalar("${ENV_FOR_TEST}"));
        assertTrue(123 == ((Integer) value).intValue());
    }

    @Test
    public void _string_env() throws Exception {
        environment.set("ENV_FOR_TEST", "abc");
        Configurator c = Configurator.lookup(String.class);
        final Object value = c.configure(new Scalar("${ENV_FOR_TEST}"));
        assertEquals("abc", value);
    }

    @Test
    public void _string_env_default() throws Exception {
        environment.set("NOT_THERE", "abc");
        Configurator c = Configurator.lookup(String.class);
        final Object value = c.configure(new Scalar("${ENV_FOR_TEST:-unsecured-token}"));
        assertEquals("unsecured-token", value);
    }

    @Test
    public void _int_env_default() throws Exception {
        Configurator c = Configurator.lookup(Integer.class);
        final Object value = c.configure(new Scalar("${ENV_FOR_TEST:-123}"));
        assertEquals(123, value);
    }
}
