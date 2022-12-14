package com.getjigra;

import static com.getjigra.Bridge.JIGRA_HTTP_SCHEME;
import static com.getjigra.FileUtils.readFile;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import androidx.annotation.Nullable;
import com.getjigra.util.JSONUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents the configuration options for Jigra
 */
public class JigConfig {

    private static final String LOG_BEHAVIOR_NONE = "none";
    private static final String LOG_BEHAVIOR_DEBUG = "debug";
    private static final String LOG_BEHAVIOR_PRODUCTION = "production";

    // Server Config
    private boolean html5mode = true;
    private String serverUrl;
    private String hostname = "localhost";
    private String androidScheme = JIGRA_HTTP_SCHEME;
    private String[] allowNavigation;

    // Android Config
    private String overriddenUserAgentString;
    private String appendedUserAgentString;
    private String backgroundColor;
    private boolean allowMixedContent = false;
    private boolean captureInput = false;
    private boolean webContentsDebuggingEnabled = false;
    private boolean loggingEnabled = true;
    private boolean initialFocus = true;

    // Embedded
    private String startPath;

    // Plugins
    private Map<String, PluginConfig> pluginsConfiguration = null;

    // Config Object JSON (legacy)
    private JSONObject configJSON = new JSONObject();

    /**
     * Constructs an empty config file.
     */
    private JigConfig() {}

    /**
     * Get an instance of the Config file object.
     * @deprecated use {@link #loadDefault(Context)} to load an instance of the Config object
     * from the jigra.config.json file, or use the {@link JigConfig.Builder} to construct
     * a JigConfig for embedded use.
     *
     * @param assetManager The AssetManager used to load the config file
     * @param config JSON describing a configuration to use
     */
    @Deprecated
    public JigConfig(AssetManager assetManager, JSONObject config) {
        if (config != null) {
            this.configJSON = config;
        } else {
            // Load the jigra.config.json
            loadConfig(assetManager);
        }

        deserializeConfig(null);
    }

    /**
     * Constructs a Jigra Configuration from config.json file.
     *
     * @param context The context.
     * @return A loaded config file, if successful.
     */
    public static JigConfig loadDefault(Context context) {
        JigConfig config = new JigConfig();

        if (context == null) {
            Logger.error("Jigra Config could not be created from file. Context must not be null.");
            return config;
        }

        config.loadConfig(context.getAssets());
        config.deserializeConfig(context);
        return config;
    }

    /**
     * Constructs a Jigra Configuration using ConfigBuilder.
     *
     * @param builder A config builder initialized with values
     */
    private JigConfig(Builder builder) {
        // Server Config
        this.html5mode = builder.html5mode;
        this.serverUrl = builder.serverUrl;
        this.hostname = builder.hostname;
        this.androidScheme = builder.androidScheme;
        this.allowNavigation = builder.allowNavigation;

        // Android Config
        this.overriddenUserAgentString = builder.overriddenUserAgentString;
        this.appendedUserAgentString = builder.appendedUserAgentString;
        this.backgroundColor = builder.backgroundColor;
        this.allowMixedContent = builder.allowMixedContent;
        this.captureInput = builder.captureInput;
        this.webContentsDebuggingEnabled = builder.webContentsDebuggingEnabled;
        this.loggingEnabled = builder.loggingEnabled;
        this.initialFocus = builder.initialFocus;

        // Embedded
        this.startPath = builder.startPath;

        // Plugins Config
        this.pluginsConfiguration = builder.pluginsConfiguration;
    }

    /**
     * Loads a Jigra Configuration JSON file into a Jigra Configuration object.
     */
    private void loadConfig(AssetManager assetManager) {
        try {
            String jsonString = readFile(assetManager, "jigra.config.json");
            configJSON = new JSONObject(jsonString);
        } catch (IOException ex) {
            Logger.error("Unable to load jigra.config.json. Run npx jig copy first", ex);
        } catch (JSONException ex) {
            Logger.error("Unable to parse jigra.config.json. Make sure it's valid json", ex);
        }
    }

    /**
     * Deserializes the config from JSON into a Jigra Configuration object.
     */
    private void deserializeConfig(@Nullable Context context) {
        boolean isDebug = context != null && (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

        // Server
        html5mode = JSONUtils.getBoolean(configJSON, "server.html5mode", html5mode);
        serverUrl = JSONUtils.getString(configJSON, "server.url", null);
        hostname = JSONUtils.getString(configJSON, "server.hostname", hostname);
        androidScheme = JSONUtils.getString(configJSON, "server.androidScheme", androidScheme);
        allowNavigation = JSONUtils.getArray(configJSON, "server.allowNavigation", null);

        // Android
        overriddenUserAgentString =
            JSONUtils.getString(configJSON, "android.overrideUserAgent", JSONUtils.getString(configJSON, "overrideUserAgent", null));
        appendedUserAgentString =
            JSONUtils.getString(configJSON, "android.appendUserAgent", JSONUtils.getString(configJSON, "appendUserAgent", null));
        backgroundColor =
            JSONUtils.getString(configJSON, "android.backgroundColor", JSONUtils.getString(configJSON, "backgroundColor", null));
        allowMixedContent =
            JSONUtils.getBoolean(
                configJSON,
                "android.allowMixedContent",
                JSONUtils.getBoolean(configJSON, "allowMixedContent", allowMixedContent)
            );
        captureInput = JSONUtils.getBoolean(configJSON, "android.captureInput", captureInput);
        webContentsDebuggingEnabled = JSONUtils.getBoolean(configJSON, "android.webContentsDebuggingEnabled", isDebug);

        String logBehavior = JSONUtils.getString(
            configJSON,
            "android.loggingBehavior",
            JSONUtils.getString(configJSON, "loggingBehavior", null)
        );
        if (logBehavior == null) {
            boolean hideLogs = JSONUtils.getBoolean(configJSON, "android.hideLogs", JSONUtils.getBoolean(configJSON, "hideLogs", false));
            logBehavior = hideLogs ? LOG_BEHAVIOR_NONE : LOG_BEHAVIOR_DEBUG;
        }
        switch (logBehavior.toLowerCase(Locale.ROOT)) {
            case LOG_BEHAVIOR_PRODUCTION:
                loggingEnabled = true;
                break;
            case LOG_BEHAVIOR_NONE:
                loggingEnabled = false;
                break;
            default: // LOG_BEHAVIOR_DEBUG
                loggingEnabled = isDebug;
        }

        initialFocus = JSONUtils.getBoolean(configJSON, "android.initialFocus", initialFocus);

        // Plugins
        pluginsConfiguration = deserializePluginsConfig(JSONUtils.getObject(configJSON, "plugins"));
    }

    public boolean isHTML5Mode() {
        return html5mode;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getHostname() {
        return hostname;
    }

    public String getStartPath() {
        return startPath;
    }

    public String getAndroidScheme() {
        return androidScheme;
    }

    public String[] getAllowNavigation() {
        return allowNavigation;
    }

    public String getOverriddenUserAgentString() {
        return overriddenUserAgentString;
    }

    public String getAppendedUserAgentString() {
        return appendedUserAgentString;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public boolean isMixedContentAllowed() {
        return allowMixedContent;
    }

    public boolean isInputCaptured() {
        return captureInput;
    }

    public boolean isWebContentsDebuggingEnabled() {
        return webContentsDebuggingEnabled;
    }

    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public boolean isInitialFocus() {
        return initialFocus;
    }

    public PluginConfig getPluginConfiguration(String pluginId) {
        PluginConfig pluginConfig = pluginsConfiguration.get(pluginId);
        if (pluginConfig == null) {
            pluginConfig = new PluginConfig(new JSONObject());
        }

        return pluginConfig;
    }

    /**
     * Get a JSON object value from the Jigra config.
     * @deprecated use {@link PluginConfig#getObject(String)}  to access plugin config values.
     * For main Jigra config values, use the appropriate getter.
     *
     * @param key A key to fetch from the config
     * @return The value from the config, if exists. Null if not
     */
    @Deprecated
    public JSONObject getObject(String key) {
        try {
            return configJSON.getJSONObject(key);
        } catch (Exception ex) {}
        return null;
    }

    /**
     * Get a string value from the Jigra config.
     * @deprecated use {@link PluginConfig#getString(String, String)} to access plugin config
     * values. For main Jigra config values, use the appropriate getter.
     *
     * @param key A key to fetch from the config
     * @return The value from the config, if exists. Null if not
     */
    @Deprecated
    public String getString(String key) {
        return JSONUtils.getString(configJSON, key, null);
    }

    /**
     * Get a string value from the Jigra config.
     * @deprecated use {@link PluginConfig#getString(String, String)} to access plugin config
     * values. For main Jigra config values, use the appropriate getter.
     *
     * @param key A key to fetch from the config
     * @param defaultValue A default value to return if the key does not exist in the config
     * @return The value from the config, if key exists. Default value returned if not
     */
    @Deprecated
    public String getString(String key, String defaultValue) {
        return JSONUtils.getString(configJSON, key, defaultValue);
    }

    /**
     * Get a boolean value from the Jigra config.
     * @deprecated use {@link PluginConfig#getBoolean(String, boolean)} to access plugin config
     * values. For main Jigra config values, use the appropriate getter.
     *
     * @param key A key to fetch from the config
     * @param defaultValue A default value to return if the key does not exist in the config
     * @return The value from the config, if key exists. Default value returned if not
     */
    @Deprecated
    public boolean getBoolean(String key, boolean defaultValue) {
        return JSONUtils.getBoolean(configJSON, key, defaultValue);
    }

    /**
     * Get an integer value from the Jigra config.
     * @deprecated use {@link PluginConfig#getInt(String, int)}  to access the plugin config
     * values. For main Jigra config values, use the appropriate getter.
     *
     * @param key A key to fetch from the config
     * @param defaultValue A default value to return if the key does not exist in the config
     * @return The value from the config, if key exists. Default value returned if not
     */
    @Deprecated
    public int getInt(String key, int defaultValue) {
        return JSONUtils.getInt(configJSON, key, defaultValue);
    }

    /**
     * Get a string array value from the Jigra config.
     * @deprecated use {@link PluginConfig#getArray(String)}  to access the plugin config
     * values. For main Jigra config values, use the appropriate getter.
     *
     * @param key A key to fetch from the config
     * @return The value from the config, if exists. Null if not
     */
    @Deprecated
    public String[] getArray(String key) {
        return JSONUtils.getArray(configJSON, key, null);
    }

    /**
     * Get a string array value from the Jigra config.
     * @deprecated use {@link PluginConfig#getArray(String, String[])}  to access the plugin
     * config values. For main Jigra config values, use the appropriate getter.
     *
     * @param key A key to fetch from the config
     * @param defaultValue A default value to return if the key does not exist in the config
     * @return The value from the config, if key exists. Default value returned if not
     */
    @Deprecated
    public String[] getArray(String key, String[] defaultValue) {
        return JSONUtils.getArray(configJSON, key, defaultValue);
    }

    private static Map<String, PluginConfig> deserializePluginsConfig(JSONObject pluginsConfig) {
        Map<String, PluginConfig> pluginsMap = new HashMap<>();

        // return an empty map if there is no pluginsConfig json
        if (pluginsConfig == null) {
            return pluginsMap;
        }

        Iterator<String> pluginIds = pluginsConfig.keys();

        while (pluginIds.hasNext()) {
            String pluginId = pluginIds.next();
            JSONObject value = null;

            try {
                value = pluginsConfig.getJSONObject(pluginId);
                PluginConfig pluginConfig = new PluginConfig(value);
                pluginsMap.put(pluginId, pluginConfig);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return pluginsMap;
    }

    /**
     * Builds a Jigra Configuration in code
     */
    public static class Builder {

        private Context context;

        // Server Config Values
        private boolean html5mode = true;
        private String serverUrl;
        private String hostname = "localhost";
        private String androidScheme = JIGRA_HTTP_SCHEME;
        private String[] allowNavigation;

        // Android Config Values
        private String overriddenUserAgentString;
        private String appendedUserAgentString;
        private String backgroundColor;
        private boolean allowMixedContent = false;
        private boolean captureInput = false;
        private Boolean webContentsDebuggingEnabled = null;
        private boolean loggingEnabled = true;
        private boolean initialFocus = false;

        // Embedded
        private String startPath = null;

        // Plugins Config Object
        private Map<String, PluginConfig> pluginsConfiguration = new HashMap<>();

        /**
         * Constructs a new JigConfig Builder.
         *
         * @param context The context
         */
        public Builder(Context context) {
            this.context = context;
        }

        /**
         * Builds a Jigra Config from the builder.
         *
         * @return A new Jigra Config
         */
        public JigConfig create() {
            if (webContentsDebuggingEnabled == null) {
                webContentsDebuggingEnabled = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            }

            return new JigConfig(this);
        }

        public Builder setPluginsConfiguration(JSONObject pluginsConfiguration) {
            this.pluginsConfiguration = deserializePluginsConfig(pluginsConfiguration);
            return this;
        }

        public Builder setHTML5mode(boolean html5mode) {
            this.html5mode = html5mode;
            return this;
        }

        public Builder setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        public Builder setHostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder setStartPath(String path) {
            this.startPath = path;
            return this;
        }

        public Builder setAndroidScheme(String androidScheme) {
            this.androidScheme = androidScheme;
            return this;
        }

        public Builder setAllowNavigation(String[] allowNavigation) {
            this.allowNavigation = allowNavigation;
            return this;
        }

        public Builder setOverriddenUserAgentString(String overriddenUserAgentString) {
            this.overriddenUserAgentString = overriddenUserAgentString;
            return this;
        }

        public Builder setAppendedUserAgentString(String appendedUserAgentString) {
            this.appendedUserAgentString = appendedUserAgentString;
            return this;
        }

        public Builder setBackgroundColor(String backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder setAllowMixedContent(boolean allowMixedContent) {
            this.allowMixedContent = allowMixedContent;
            return this;
        }

        public Builder setCaptureInput(boolean captureInput) {
            this.captureInput = captureInput;
            return this;
        }

        public Builder setWebContentsDebuggingEnabled(boolean webContentsDebuggingEnabled) {
            this.webContentsDebuggingEnabled = webContentsDebuggingEnabled;
            return this;
        }

        public Builder setLoggingEnabled(boolean enabled) {
            this.loggingEnabled = enabled;
            return this;
        }

        public Builder setInitialFocus(boolean focus) {
            this.initialFocus = focus;
            return this;
        }
    }
}
