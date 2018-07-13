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

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.cishell.app.service.datamanager.DataManagerService;
import org.cishell.framework.algorithm.AlgorithmFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;


public class CIShellContainer {

	private static CIShellContainerActivator activator = null;
	private static Felix felix = null;

	public static void main(String[] args) {

		String pluginsPath = null;
		String propertyFileName = null;

		for(String s : args) {
			if(s.contains(".properties")) {
				propertyFileName = s;
			}else {
				pluginsPath = s; 
			}
		}

		CIShellContainer csc = new CIShellContainer(pluginsPath, propertyFileName);
	}

	public CIShellContainer(String pluginsPath, String propertyFileName){
		
		InputStream input = null;
		if(pluginsPath == null) {
			pluginsPath = "../plugins/";
		}
		
		try {
			Properties prop = new Properties();
			prop.load(CIShellContainer.class.getResourceAsStream("/config.properties"));
			if(propertyFileName != null) {
				input = new FileInputStream(propertyFileName);
				prop.load(input);
			}else {
				try {
					input = new FileInputStream("config.properties");
					prop.load(input);
				}catch(Exception e) {
					System.out.println("No config.properties file found in folder!");
				}
			}
			
			if(prop.get("pluginsDir")!= null) {
				pluginsPath = (String) prop.get("pluginsDir");
			}
			
			if(new File(pluginsPath).exists()) {
				System.out.println("Plugins directory: "+pluginsPath);
			}else {
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
			config.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, prop.get("systempackages"));
			config.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, list);

			felix = new Felix(config);
			felix.init();
			felix.start();

			BundleContext context = felix.getBundleContext();
			List<Bundle> installedBundles = new ArrayList<Bundle>();

			Manifest manifest = new Manifest(CIShellContainer.class.getResourceAsStream("/META-INF/MANIFEST.MF"));
	      	String[] libs = ((String)manifest.getMainAttributes().get(Attributes.Name.CLASS_PATH)).split(" ");
	      	System.out.println(libs);
	      	for (String lib : libs) {
	      		if ((!lib.contains("lib/org.apache.felix.framework")) && (!lib.contains("animal-sniffer-annotations")) && (!lib.contains("org.osgi.core")) && (!lib.contains("xml"))) {
	      			InputStream libStream = CIShellContainer.class.getResourceAsStream("/" + lib);
	      			installedBundles.add(context.installBundle(lib, libStream));	
	      		}
	      	}

			for (Bundle b : felix.getBundleContext().getBundles()) {
				System.out.println(b.getSymbolicName()+" : "+"State="+b.getState()) ;
				b.start();
			}

			Thread.sleep(15000);
			System.out.println("Installed Bundles: ") ;
			for(Bundle b: getInstalledBundles()) {
				System.out.println(b.getSymbolicName()+" : "+"State="+b.getState()) ;
				b.start();
				if(b.getRegisteredServices()!=null) {
					for(ServiceReference s : b.getRegisteredServices()) {
						System.out.println("Services: "+s.toString());

					}
				}
			}

			System.out.println("Container Started...");

			System.out.println(getLogService());

		} catch (Exception ex){
			System.err.println("Could not create framework: " + ex);
			ex.printStackTrace();
		}finally {
			try {
				if(input!=null)
					input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public  DataManagerService getDataManagerService() {
		BundleContext context = felix.getBundleContext();
		ServiceReference serviceReference = context.getServiceReference(DataManagerService.class.getName());
		DataManagerService manager = null;
		
		if (serviceReference != null) {
			manager = (DataManagerService) context.getService(serviceReference);
		}

		return manager;
	}

	public  AlgorithmFactory getAlgorithmFactory(String pid) {
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

	public  LogService getLogService()  {
		BundleContext context = felix.getBundleContext();
		ServiceReference ref = context.getServiceReference(LogService.class.getName());
		LogService log = null;
		if (ref != null){
			log = (LogService) context.getService(ref);
		}
		return log;
	}

	public  Bundle[] getInstalledBundles() {
		return activator.getBundles();
	}


	public  BundleContext getContext() {
		return activator.getbundleContext();
	}

	public void shutdownApplication() throws BundleException, InterruptedException {
		// Shut down the felix framework when stopping the
		// host application.
		felix.stop();
		felix.waitForStop(0);
	}

}


