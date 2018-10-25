package org.cishell.container;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.cishell.app.service.datamanager.DataManagerService;
import org.cishell.app.service.scheduler.SchedulerService;
import org.cishell.framework.algorithm.AlgorithmFactory;
import org.cishell.service.conversion.DataConversionService;
import org.cishell.service.guibuilder.GUIBuilderService;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.MetaTypeService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


/*
Default Plugins folder : ./plugins
requires a config.properties files to set the default plugins folder and file install properties
*/

public class CIShellContainer {

    private CIShellContainerActivator activator = null;
    private Felix felix = null;
    private static boolean debugMode = false;
    private static final String ASCII_ART =
            "\n   _______________ __         ____   ______            __        _                " +
                    "\n  / ____/  _/ ___// /_  ___  / / /  / ____/___  ____  / /_____ _(_)___  ___  _____" +
                    "\n / /    / / \\__ \\/ __ \\/ _ \\/ / /  / /   / __ \\/ __ \\/ __/ __ `/ / __ \\/ _ \\/ ___/" +
                    "\n/ /____/ / ___/ / / / /  __/ / /  / /___/ /_/ / / / / /_/ /_/ / / / / /  __/ /    " +
                    "\n\\____/___//____/_/ /_/\\___/_/_/   \\____/\\____/_/ /_/\\__/\\__,_/_/_/ /_/\\___/_/     \n";

    public CIShellContainer() {
        this(null, null);
    }

    public CIShellContainer(String pluginsDirPath, String configPropertiesPath) {

        InputStream inputStream = null;

        try {
            //load default config.properties
            Properties properties = new Properties();

            if (configPropertiesPath != null) {
                inputStream = new FileInputStream(configPropertiesPath);
                System.out.println("Loading configurations from : " + configPropertiesPath);
                properties.load(inputStream);
            } else {
                properties.load(CIShellContainer.class.getResourceAsStream("/config.properties"));
                System.out.println("Loading configurations from : /config.properties");
            }

            //if plugins directory was not specified from command line, set it from config.properties file
            if (pluginsDirPath == null && properties.get("pluginsDir") != null) {
                pluginsDirPath = (String) properties.get("pluginsDir");
            }

            //if plugins directory was not specified from both command line and config.properties file,
            //then set the default path
            if (pluginsDirPath == null) {
                pluginsDirPath = "plugins/";
            }

            System.out.println("Loading plugins from : " + pluginsDirPath);

            if (!new File(pluginsDirPath).exists()) {
                System.out.println("Plugins directory not found!");
            }

            System.out.println("Building OSGi Framework...");
            List<CIShellContainerActivator> list = new ArrayList<CIShellContainerActivator>();
            activator = new CIShellContainerActivator();
            list.add(activator);

            //load felix config from given parameters and properties file
            Map<String, Object> felixConfig = getFelixConfig(properties);
            felixConfig.put("felix.fileinstall.dir", pluginsDirPath);
            felixConfig.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, list);

            //start felix
            System.out.println("Starting OSGi Framework...");
            felix = new Felix(felixConfig);
            felix.init();
            felix.start();

            BundleContext context = felix.getBundleContext();

            if (properties.get("installbundles") == null) {
                System.out.println("Please add the required bundles in the config properties file against key : installbundles");
            } else {
                String[] libs = properties.get("installbundles").toString().split(",");
                for (String lib : libs) {
                    InputStream libStream = CIShellContainer.class.getResourceAsStream("/" + lib);
                    context.installBundle(lib, libStream);
                }
            }

            for (Bundle b : felix.getBundleContext().getBundles()) {
                b.start();
            }

            int ticks = 10;
            if (properties.get("ticks") != null) {
                ticks = Integer.parseInt(properties.get("ticks").toString());
            }

            int tickTime = 500;
            if (properties.get("tickTime") != null) {
                tickTime = Integer.parseInt(properties.get("tickTime").toString());
            }

            while (ticks-- > 0) {
                if (getDataManagerService() != null &&
                        getSchedulerService() != null &&
                        getDataConversionService() != null &&
                        getLogService() != null &&
                        getMetaTypeService() != null
                ) {
                    break;
                }
                Thread.sleep(tickTime);
            }

            System.out.println(ASCII_ART);

            if (debugMode) {
                System.out.println("\nInstalled Bundles: ");
                for (Bundle b : getInstalledBundles()) {
                    System.out.println(b.getSymbolicName() + " : " + "State=" + b.getState());
                    if (b.getRegisteredServices() != null) {
                        System.out.println("\tRegistered Services: ");
                        for (ServiceReference s : b.getRegisteredServices()) {
                            System.out.println("\t* " + s.toString());
                        }
                    }
                }
            }

            System.out.println("\nCIShell Services Installed:");
            System.out.println("---------------------------");
            System.out.println("* Data Manager Service: " + (this.getDataManagerService() != null ? "installed" : "not installed"));
            System.out.println("* Scheduler Service: " + (this.getSchedulerService() != null ? "installed" : "not installed"));
            System.out.println("* Data Conversion Service: " + (this.getDataConversionService() != null ? "installed" : "not installed"));
            System.out.println("* GUI Builder Service: " + (this.getGUIBuilderService() != null ? "installed" : "not installed"));
            System.out.println("* Log Service: " + (this.getLogService() != null ? "installed" : "not installed"));
            System.out.println("* MetaType Service: " + (this.getMetaTypeService() != null ? "installed" : "not installed"));
            System.out.println();

        } catch (Exception ex) {
            System.err.println("Could not create framework: " + ex);
            ex.printStackTrace();
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public AlgorithmFactory getAlgorithmFactory(String pid) {
        BundleContext context = felix.getBundleContext();
        ServiceReference[] refs;

        try {
            refs = context.getServiceReferences(AlgorithmFactory.class.getName(),
                    "(&(" + Constants.SERVICE_PID + "=" + pid + "))");
            if (refs != null && refs.length > 0) {
                return (AlgorithmFactory) context.getService(refs[0]);
            } else {
                return null;
            }
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    public GUIBuilderService getGUIBuilderService() {
        return (GUIBuilderService) this.getService(GUIBuilderService.class);
    }

    public DataConversionService getDataConversionService() {
        return (DataConversionService) this.getService(DataConversionService.class);
    }

    public SchedulerService getSchedulerService() {
        return (SchedulerService) this.getService(SchedulerService.class);
    }

    public DataManagerService getDataManagerService() {
        return (DataManagerService) this.getService(DataManagerService.class);
    }

    public LogService getLogService() {
        return (LogService) this.getService(LogService.class);
    }

    public MetaTypeService getMetaTypeService() {
        return (MetaTypeService) this.getService(MetaTypeService.class);
    }

    public Object getService(Class c) {
        BundleContext context = felix.getBundleContext();
        ServiceReference ref = context.getServiceReference(c.getName());
        return ref != null ? context.getService(ref) : null;
    }

    public Bundle[] getInstalledBundles() {
        return activator.getBundles();
    }

    public BundleContext getBundleContext() {
        return activator.getbundleContext();
    }

    public void shutdownApplication() throws BundleException, InterruptedException {
        // Shut down the felix framework when stopping the host application.
        felix.stop();
        felix.waitForStop(0);
    }

    private Map<String, Object> getFelixConfig(Properties properties) {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        config.put("felix.fileinstall.poll", properties.get("poll"));
        config.put("ds.showerrors", properties.get("showerrors"));
        config.put("felix.fileinstall.bundles.startTransient", properties.get("startTransient"));
        config.put("felix.fileinstall.bundles.new.start", properties.get("start"));
        config.put("felix.fileinstall.noInitialDelay", properties.get("noInitialDelay"));
        config.put("org.osgi.framework.bootdelegation", "sun.*");
        config.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, properties.get("systempackages"));
        config.put("org.apache.felix.http.jettyEnabled", "true");
        config.put("org.apache.felix.http.whiteboardEnabled", "true");
        config.put("org.apache.felix.http.enable", "true");
        config.put("org.apache.felix.http.mbeans", "true");
        if (debugMode) {
            config.put("ds.showtrace", properties.get("showtrace"));
            config.put("org.apache.felix.http.debugMode", "true");
        }

        return config;
    }

    public static void main(String[] args) {

        String pluginsPath = null;
        String propertyFilePath = null;

        for (String s : args) {
            if (s.contains(".properties")) {
                propertyFilePath = s;
            } else if (s.equalsIgnoreCase("--debug-mode")) {
                CIShellContainer.debugMode = true;
            } else {
                pluginsPath = s;
            }
        }

        CIShellContainer ciShellContainer = new CIShellContainer(pluginsPath, propertyFilePath);
    }
}
