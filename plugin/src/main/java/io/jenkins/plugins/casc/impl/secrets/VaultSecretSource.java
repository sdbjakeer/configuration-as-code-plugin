package io.jenkins.plugins.casc.impl.secrets;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import hudson.Extension;
import io.jenkins.plugins.casc.SecretSource;
import org.apache.commons.lang.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Requires either CASC_VAULT_USER and CASC_VAULT_PW, or CASC_VAULT_TOKEN environment variables set
 * alongside with CASC_VAULT_PATHS and CASC_VAULT_URL
 */
@Extension

public class VaultSecretSource extends SecretSource {

    private final static Logger LOGGER = Logger.getLogger(VaultSecretSource.class.getName());
    private Map<String, String> secrets = new HashMap<>();

    private static final String CASC_VAULT_FILE = "CASC_VAULT_FILE";
    private static final String CASC_VAULT_PW = "CASC_VAULT_PW";
    private static final String CASC_VAULT_USER = "CASC_VAULT_USER";
    private static final String CASC_VAULT_URL = "CASC_VAULT_URL";
    private static final String CASC_VAULT_MOUNT = "CASC_VAULT_MOUNT";
    private static final String CASC_VAULT_TOKEN = "CASC_VAULT_TOKEN";
    private static final String CASC_VAULT_APPROLE = "CASC_VAULT_APPROLE";
    private static final String CASC_VAULT_APPROLE_SECRET = "CASC_VAULT_APPROLE_SECRET";
    private static final String CASC_VAULT_NAMESPACE = "CASC_VAULT_NAMESPACE";
    private static final String CASC_VAULT_ENGINE_VERSION = "CASC_VAULT_ENGINE_VERSION";
    private static final String CASC_VAULT_PATHS = "CASC_VAULT_PATHS";
    private static final String CASC_VAULT_PATH = "CASC_VAULT_PATH"; // TODO: deprecate!


    public VaultSecretSource() {
        Optional<String> vaultFile = Optional.ofNullable(System.getenv(CASC_VAULT_FILE));
        Properties prop = new Properties();
        vaultFile.ifPresent(file -> readPropertiesFromVaultFile(file, prop));

        // Parse variables
        Optional<String> vaultPw = getVariable(CASC_VAULT_PW, prop);
        Optional<String> vaultUser = getVariable(CASC_VAULT_USER, prop);
        Optional<String> vaultUrl = getVariable(CASC_VAULT_URL, prop);
        Optional<String> vaultMount = getVariable(CASC_VAULT_MOUNT, prop);
        Optional<String> vaultToken = getVariable(CASC_VAULT_TOKEN, prop);
        Optional<String> vaultAppRole = getVariable(CASC_VAULT_APPROLE, prop);
        Optional<String> vaultAppRoleSecret = getVariable(CASC_VAULT_APPROLE_SECRET, prop);
        Optional<String> vaultNamespace = getVariable(CASC_VAULT_NAMESPACE, prop);
        Optional<String> vaultEngineVersion = getVariable(CASC_VAULT_ENGINE_VERSION, prop);
        Optional<String[]> vaultPaths = getCommaSeparatedVariables(CASC_VAULT_PATHS, prop);
        if (!vaultPaths.isPresent()) {
            // checking old variable for backwards compatibility
            // TODO: deprecate!
            vaultPaths = getCommaSeparatedVariables(CASC_VAULT_PATH, prop);
        }

        // configure vault client
        Vault vault = null;
        VaultConfig vaultConfig = null;
        try {
            vaultConfig = new VaultConfig().address(vaultUrl.get());
            LOGGER.log(Level.FINE, "Attempting to connect to Vault: {0}", vaultUrl.get());
            if (vaultNamespace.isPresent()) {
                vaultConfig.nameSpace(vaultNamespace.get());
            }
            if (vaultEngineVersion.isPresent()) {
                vaultConfig.engineVersion(Integer.parseInt(vaultEngineVersion.get()));
            }
            vaultConfig = vaultConfig.build();
            vault = new Vault(vaultConfig);
        } catch (VaultException e) {
            LOGGER.log(Level.WARNING, "Could not configure vault connection", e);
        }

        Optional<String> authToken = Optional.empty();

        // attempt token login
        if (vaultToken.isPresent() && !authToken.isPresent()) {
            authToken = Optional.of(vaultToken.get());
        }

        // attempt AppRole login
        if (vaultAppRole.isPresent() && vaultAppRoleSecret.isPresent() && !authToken.isPresent()) {
            try {
                authToken = Optional.of(
                        vault.auth().loginByAppRole(vaultAppRole.get(), vaultAppRoleSecret.get()).getAuthClientToken()
                );
                LOGGER.log(Level.FINE, "Login to Vault using AppRole/SecretID successful");
            } catch (VaultException e) {
                LOGGER.log(Level.WARNING, "Could not login with AppRole", e);
            }
        }

        // attempt User/Pass login
        if (vaultUser.isPresent() && vaultPw.isPresent() && !authToken.isPresent()) {
            try {
                authToken = Optional.of(
                        vault.auth().loginByUserPass(vaultUser.get(), vaultPw.get(), vaultMount.get()).getAuthClientToken()
                );
                LOGGER.log(Level.FINE, "Login to Vault using User/Pass successful");
            } catch (VaultException e) {
                LOGGER.log(Level.WARNING, "Could not login with User/Pass", e);
            }
        }

        // Use authToken to read secrets from vault
        readSecretsFromVault(authToken.get(), vaultConfig, vault, vaultPaths.get());
    }

    private void readSecretsFromVault(String token, VaultConfig vaultConfig, Vault vault, String[] vaultPaths) {
        try {
            vaultConfig.token(token).build();
            for (String vaultPath : vaultPaths) {

                // check if we overwrite an existing key from another path
                Map<String,String> nextSecrets = vault.logical().read(vaultPath).getData();
                for (String key : nextSecrets.keySet()) {
                    if (secrets.containsKey(key)) {
                        LOGGER.log(Level.WARNING, "Key {0} exists in multiple vault paths.", key);
                    }
                }

                // merge
                secrets.putAll(nextSecrets);
            }
        } catch (VaultException e) {
            LOGGER.log(Level.WARNING, "Unable to fetch secret from Vault", e);
        }
    }

    private void readPropertiesFromVaultFile(String vaultFile, Properties prop) {
        try (FileInputStream input = new FileInputStream(vaultFile)) {
            prop.load(input);
            if (prop.isEmpty()) {
                LOGGER.log(Level.WARNING, "Vault secret file is empty");
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Failed to load Vault secrets from file", ex);
        }
    }

    @Override
    public Optional<String> reveal(String secret) {
        if (StringUtils.isBlank(secret)) return Optional.empty();
        return Optional.ofNullable(secrets.get(secret));
    }

    public Map<String, String> getSecrets() {
        return secrets;
    }

    public void setSecrets(Map<String, String> secrets) {
        this.secrets = secrets;
    }

    private Optional<String> getVariable(String key, Properties prop) {
        return prop.containsKey(key) ?
                Optional.ofNullable(prop.getProperty(key)) : Optional.ofNullable(System.getenv(key));
    }

    private Optional<String[]> getCommaSeparatedVariables(String key, Properties prop) {
        Optional<String> setting = getVariable(key, prop);
        return setting.isPresent() ?
                Optional.of(setting.get().split(",")) : Optional.empty();
    }
}
