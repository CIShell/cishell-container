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

			JarFile jarFile = new JarFile("lib/library-1.0.0-SNAPSHOT.jar");
			final Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				final JarEntry entry = entries.nextElement();
				if (entry.getName().contains(".jar")) {
					JarEntry fileEntry = jarFile.getJarEntry(entry.getName());
					System.out.println("Installing... "+entry.getName());
					installedBundles.add(context.installBundle(entry.getName(),jarFile.getInputStream(fileEntry)));
				}
			}    

			jarFile.close();

			for (Bundle b : felix.getBundleContext().getBundles()) {
				System.out.println(b.getSymbolicName()+" : "+"State="+b.getState()) ;
				b.start();
			}

			Thread.sleep(5000);
			System.out.println("Installed Bundles: ") ;
			for(Bundle b: getInstalledBundles()) {
				System.out.println(b.getSymbolicName()+" : "+"State="+b.getState()) ;
				if(b.getRegisteredServices()!=null) {
					for(ServiceReference s : b.getRegisteredServices()) {
						System.out.println("Services: "+s.toString());

					}
				}
			}

			System.out.println("Container Started...");
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

	public static DataManagerService getDataManagerService() {
		BundleContext context = felix.getBundleContext();
		ServiceReference serviceReference = context.getServiceReference(DataManagerService.class.getName());
		DataManagerService manager = null;
		
		if (serviceReference != null) {
			manager = (DataManagerService) context.getService(serviceReference);
		}

		return manager;
	}

	public static AlgorithmFactory getAlgorithmFactory(String pid) {
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

	public static LogService getLogService()  {
		BundleContext context = felix.getBundleContext();
		ServiceReference ref = context.getServiceReference(LogService.class.getName());
		LogService log = null;
		if (ref != null){
			log = (LogService) context.getService(ref);
		}
		return log;
	}

	public static Bundle[] getInstalledBundles() {
		return activator.getBundles();
	}


	public static BundleContext getContext() {
		return activator.getbundleContext();
	}

	public void shutdownApplication() throws BundleException, InterruptedException {
		// Shut down the felix framework when stopping the
		// host application.
		felix.stop();
		felix.waitForStop(0);
	}

}


