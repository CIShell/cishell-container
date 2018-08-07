package org.cishell.container;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;

import org.cishell.app.service.datamanager.DataManagerService;
import org.cishell.app.service.scheduler.SchedulerService;
import org.cishell.framework.algorithm.AlgorithmFactory;
import org.cishell.service.conversion.DataConversionService;
import org.cishell.service.guibuilder.GUIBuilderService;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.MetaTypeService;


/*
Default Plugins folder : ./plugins
requires a config.properties files to set the default plugins folder and file install properties
*/

public class CIShellContainer {

	private CIShellContainerActivator activator = null;
	private Felix felix = null;

	public CIShellContainer(String pluginsPath, String propertyFileName) {
		
		InputStream input = null;
		int timeout = 5000;

		if (pluginsPath == null) {
			pluginsPath = "./plugins/";
		}
		
		try {
			// load default config.properties
			Properties prop = new Properties();
			prop.load(CIShellContainer.class.getResourceAsStream("/config.properties"));
			if (propertyFileName != null) {
				input = new FileInputStream(propertyFileName);
				prop.load(input);
			} else {
				try {
					input = new FileInputStream("config.properties");
					prop.load(input);
				} catch(Exception e) {
					System.out.println("No config.properties file found in folder!");
				}
			}
			
			if (prop.get("pluginsDir")!= null) {
				pluginsPath = (String) prop.get("pluginsDir");
			}
			
			if (new File(pluginsPath).exists()) {
				System.out.println("Plugins directory: "+pluginsPath);
			} else {
				System.out.println("Plugins directory not found!");
			}

			System.out.println("Building OSGi Framework...");
			List<CIShellContainerActivator> list = new ArrayList<CIShellContainerActivator>();
			activator = new CIShellContainerActivator();
			list.add(activator);
			
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
			config.put("felix.fileinstall.poll", prop.get("poll"));
			config.put("felix.fileinstall.dir", pluginsPath);
			config.put("ds.showtrace", prop.get("showtrace"));
			config.put("ds.showerrors", prop.get("showerrors"));
			config.put("felix.fileinstall.bundles.startTransient",prop.get("startTransient"));
			config.put("felix.fileinstall.bundles.new.start", prop.get("start"));
			config.put("felix.fileinstall.noInitialDelay", prop.get("noInitialDelay"));
			config.put("org.osgi.framework.bootdelegation", "sun.*");
			config.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, prop.get("systempackages"));
			config.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, list);
			config.put("org.apache.felix.http.jettyEnabled", "true");
			config.put("org.apache.felix.http.whiteboardEnabled", "true");
			config.put("org.apache.felix.http.debug", "true");
			config.put("org.apache.felix.http.enable", "true");
			config.put("org.apache.felix.http.mbeans", "true");

			felix = new Felix(config);
			felix.init();
			felix.start();

			BundleContext context = felix.getBundleContext();
			List<Bundle> installedBundles = new ArrayList<Bundle>();

			if (prop.get("installbundles") == null){
				System.out.println("Please add the required bundles in the property file under key : installbundles");
			} else {
		      	String[] libs = prop.get("installbundles").toString().split(",");
		      	for (String lib : libs) {
	      			InputStream libStream = CIShellContainer.class.getResourceAsStream("/" + lib);
	      			installedBundles.add(context.installBundle(lib, libStream));	
		      	}
	      	}

			for (Bundle b : felix.getBundleContext().getBundles()) {
				System.out.println(b.getSymbolicName()+" : "+"State="+b.getState());
				b.start();
			}

			if (prop.get("sleep") != null){
				timeout = Integer.valueOf(prop.get("sleep").toString());
			}

			System.out.println("");
			System.out.println("Waiting(secs)... : "+timeout);
			Thread.sleep(timeout);
			System.out.println("Installed Bundles: ");
			for (Bundle b: getInstalledBundles()) {
				System.out.println(b.getSymbolicName()+" : "+"State="+b.getState());
				if (b.getRegisteredServices()!=null) {
					for (ServiceReference s : b.getRegisteredServices()) {
						System.out.println("Services: "+s.toString());
					}
				}
			}

			System.out.println("\nCIShell Services Installed:\n");
			System.out.println("* Data Manager: " + (this.getDataManagerService() != null ? "installed" : "not installed"));
			System.out.println("* Scheduler Service: " + (this.getSchedulerService() != null ? "installed" : "not installed"));
			System.out.println("* Data Conversion Service: " + (this.getDataConversionService() != null ? "installed" : "not installed"));
			System.out.println("* GUI Builder Service: " + (this.getGUIBuilderService() != null ? "installed" : "not installed"));
			System.out.println("* Log Service: " + (this.getLogService() != null ? "installed" : "not installed"));
			System.out.println("* MetaType Service: " + (this.getMetaTypeService() != null ? "installed" : "not installed"));
			System.out.println("* Test Algorithm (org.cishell.algorithm.convertergraph.ConverterGraphAlgorithm): " + 
				(this.getAlgorithmFactory("org.cishell.algorithm.convertergraph.ConverterGraphAlgorithm") != null ? "installed" : "not installed"));

			System.out.println("\nContainer Started...");
		} catch (Exception ex) {
			System.err.println("Could not create framework: " + ex);
			ex.printStackTrace();
		} finally {
			try {
				if (input!=null)
					input.close();
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
					"(&("+Constants.SERVICE_PID+"="+pid+"))");
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

	public  Bundle[] getInstalledBundles() {
		return activator.getBundles();
	}

	public  BundleContext getBundleContext() {
		return activator.getbundleContext();
	}

	public void shutdownApplication() throws BundleException, InterruptedException {
		// Shut down the felix framework when stopping the host application.
		felix.stop();
		felix.waitForStop(0);
	}

	public static void main(String[] args) {
		String pluginsPath = null;
		String propertyFileName = null;

		for(String s : args) {
			if (s.contains(".properties")) {
				propertyFileName = s;
			} else {
				pluginsPath = s; 
			}
		}

		CIShellContainer csc = new CIShellContainer(pluginsPath, propertyFileName);
	}
}
