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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/*
Default Plugins folder : ./plugins
requires a config.properties files to set the default plugins folder and file install properties
*/

public class CIShellContainer {

    private CIShellContainerActivator activator = null;
    private Felix felix = null;

    private CIShellContainer(String configPropertiesPath, String pluginsDirectoryPath, boolean debugMode) {

        try {
            Configurations configurations = new Configurations(configPropertiesPath, pluginsDirectoryPath, debugMode);

            //start building felix framework
            List<CIShellContainerActivator> list = new ArrayList<CIShellContainerActivator>();
            activator = new CIShellContainerActivator();
            list.add(activator);

            Map<String, Object> felixConfigurations = configurations.getFelixConfigurations();
            felixConfigurations.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, list);

            //start felix
            System.out.println("Starting OSGi Framework...");
            felix = new Felix(felixConfigurations);
            felix.init();
            felix.start();

            BundleContext context = felix.getBundleContext();

            if (configurations.getConfigProperties().get("installbundles") == null) {
                System.out.println("Please add the required bundles in the config properties file against key : installbundles");
            } else {
                String[] libs = configurations.getConfigProperties().get("installbundles").toString().split(",");
                for (String lib : libs) {
                    InputStream libStream = CIShellContainer.class.getResourceAsStream("/" + lib);
                    context.installBundle(lib, libStream);
                }
            }

            for (Bundle b : felix.getBundleContext().getBundles()) {
                b.start();
            }

            int ticks = configurations.getTimeoutPeriod() / configurations.getTickTime();
            while (ticks-- > 0) {
                if (getDataManagerService() != null &&
                        getSchedulerService() != null &&
                        getDataConversionService() != null &&
                        //getGUIBuilderService() != null &&
                        getLogService() != null &&
                        getMetaTypeService() != null
                ) {
                    break;
                }
                Thread.sleep(configurations.getTickTime());
            }

            System.out.println(Configurations.ASCII_ART);

            if (debugMode) {
                System.out.println("\nInstalled Bundles: ");
                for (Bundle b : getInstalledBundles()) {
                    System.out.println(b.getSymbolicName() + " : " + "State=" + b.getState());
                    if (b.getRegisteredServices() != null) {
                        System.out.println("\tRegistered Services: ");
                        for (ServiceReference serviceReference : b.getRegisteredServices()) {
                            System.out.println("\t* " + serviceReference.toString());
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
            System.err.println("Failed to start felix framework\n" + ex);
        }
    }

    public AlgorithmFactory getAlgorithmFactory(String pid) {

        try {
            Collection<ServiceReference<AlgorithmFactory>> serviceReferences = getBundleContext().getServiceReferences(AlgorithmFactory.class,
                    "(&(" + Constants.SERVICE_PID + "=" + pid + "))");

            if (serviceReferences != null && serviceReferences.size() > 0) {
                return getBundleContext().getService(serviceReferences.iterator().next());
            }

        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }

        return null;
    }

    public GUIBuilderService getGUIBuilderService() {
        return getService(GUIBuilderService.class);
    }

    public DataConversionService getDataConversionService() {
        return getService(DataConversionService.class);
    }

    public SchedulerService getSchedulerService() {
        return getService(SchedulerService.class);
    }

    public DataManagerService getDataManagerService() {
        return getService(DataManagerService.class);
    }

    public LogService getLogService() {
        return getService(LogService.class);
    }

    public MetaTypeService getMetaTypeService() {
        return getService(MetaTypeService.class);
    }

    public <S> S getService(Class<S> clazz) {
        BundleContext context = felix.getBundleContext();
        ServiceReference<S> serviceReference = context.getServiceReference(clazz);
        return serviceReference != null ? context.getService(serviceReference) : null;
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

    public static Builder getBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String configPropertiesPath = null;
        private String pluginsDirectoryPath = null;
        private boolean debugMode = false;

        public Builder configPropertiesPath(final String configPropertiesPath) {
            this.configPropertiesPath = configPropertiesPath;
            return this;
        }

        public Builder pluginsDirectoryPath(final String pluginsDirectoryPath) {
            this.pluginsDirectoryPath = pluginsDirectoryPath;
            return this;
        }

        public Builder inDebugMode() {
            this.debugMode = true;
            return this;
        }

        public CIShellContainer build() {
            return new CIShellContainer(configPropertiesPath, pluginsDirectoryPath, debugMode);
        }
    }

    public static void main(String[] args) {

        String pluginsPath = null;
        String propertyFilePath = null;
        boolean debugMode = false;

        for (String s : args) {
            if (s.contains(".properties")) {
                propertyFilePath = s;
            } else if (s.equalsIgnoreCase("--debug-mode")) {
                debugMode = true;
            } else {
                pluginsPath = s;
            }
        }

        new CIShellContainer(propertyFilePath, pluginsPath, debugMode);
    }
}
