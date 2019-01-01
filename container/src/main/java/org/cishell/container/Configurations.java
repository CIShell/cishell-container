package org.cishell.container;

import org.osgi.framework.Constants;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

class Configurations {

    static final String ASCII_ART =
            "\n   _______________ __         ____   ______            __        _                " +
                    "\n  / ____/  _/ ___// /_  ___  / / /  / ____/___  ____  / /_____ _(_)___  ___  _____" +
                    "\n / /    / / \\__ \\/ __ \\/ _ \\/ / /  / /   / __ \\/ __ \\/ __/ __ `/ / __ \\/ _ \\/ ___/" +
                    "\n/ /____/ / ___/ / / / /  __/ / /  / /___/ /_/ / / / / /_/ /_/ / / / / /  __/ /    " +
                    "\n\\____/___//____/_/ /_/\\___/_/_/   \\____/\\____/_/ /_/\\__/\\__,_/_/_/ /_/\\___/_/     \n";

    private String pluginsDirectoryPath = null;
    private Map<String, Object> felixConfigurations = new HashMap<String, Object>();
    private boolean debugMode;
    private int timeoutPeriod;
    private int tickTime;
    private Properties configProperties = new Properties();

    Configurations(String configPropertiesPath, String pluginsDirectoryPath, boolean debugMode) throws IOException {
        this.debugMode = debugMode;
        System.out.println("Debug mode is " + (debugMode ? "ON" : "OFF"));

        //load default config.configProperties
        if (configPropertiesPath != null) {
            InputStream inputStream = new FileInputStream(configPropertiesPath);
            System.out.println("Loading configurations from '" + configPropertiesPath + "'");
            configProperties.load(inputStream);
        } else {
            configProperties.load(CIShellContainer.class.getResourceAsStream("/config.properties"));
            System.out.println("Loading default configurations");
        }

        //load configurations from the configProperties file
        loadConfigurationsFromConfigProperties();

        //if plugins directory was not specified from both command line and config.properties file,
        //then set the default path
        if (pluginsDirectoryPath == null) {
            pluginsDirectoryPath = "plugins/";
        }

        File pluginsDirectory = new File(pluginsDirectoryPath);

        if (pluginsDirectory.exists()) {
            System.out.println("Loading plugins from : " + pluginsDirectory.getCanonicalPath());
        } else {
            throw new FileNotFoundException("Plugins directory doesn't exist!");
        }

        felixConfigurations.put("felix.fileinstall.dir", pluginsDirectoryPath);
    }

    private void loadConfigurationsFromConfigProperties() {
        //if plugins directory was not specified from command line, set it from config.configProperties file
        if (pluginsDirectoryPath == null && configProperties.get("pluginsDir") != null) {
            pluginsDirectoryPath = (String) configProperties.get("pluginsDir");
        }

        timeoutPeriod = configProperties.get("timeoutPeriod") != null ? Integer.parseInt(configProperties.get("timeoutPeriod").toString()) : 20000;
        tickTime = configProperties.get("tickTime") != null ? Integer.parseInt(configProperties.get("tickTime").toString()) : 200;

        //load felix configurations
        felixConfigurations.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        felixConfigurations.put("felix.fileinstall.poll", configProperties.get("poll"));
        felixConfigurations.put("ds.showerrors", configProperties.get("showerrors"));
        felixConfigurations.put("felix.fileinstall.bundles.startTransient", configProperties.get("startTransient"));
        felixConfigurations.put("felix.fileinstall.bundles.new.start", configProperties.get("start"));
        felixConfigurations.put("felix.fileinstall.noInitialDelay", configProperties.get("noInitialDelay"));
        felixConfigurations.put("org.osgi.framework.bootdelegation", "sun.*");
        felixConfigurations.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, configProperties.get("systempackages"));
        felixConfigurations.put("org.apache.felix.http.jettyEnabled", "true");
        felixConfigurations.put("org.apache.felix.http.whiteboardEnabled", "true");
        felixConfigurations.put("org.apache.felix.http.enable", "true");
        felixConfigurations.put("org.apache.felix.http.mbeans", "true");
        if (debugMode) {
            felixConfigurations.put("ds.showtrace", configProperties.get("showtrace"));
            felixConfigurations.put("org.apache.felix.http.debugMode", "true");
        }
    }

    Map<String, Object> getFelixConfigurations() {
        return felixConfigurations;
    }

    int getTimeoutPeriod() {
        return timeoutPeriod;
    }

    int getTickTime() {
        return tickTime;
    }

    Properties getConfigProperties() {
        return configProperties;
    }
}
