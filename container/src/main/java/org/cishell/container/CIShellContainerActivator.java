package org.cishell.container;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class CIShellContainerActivator implements BundleActivator
{
    private BundleContext bundleContext = null;

    public void start(BundleContext context)
    {
        bundleContext = context;
    }

    public void stop(BundleContext context)
    {
        bundleContext = null;
    }

    public Bundle[] getBundles()
    {
        if (bundleContext != null)
        {
            return bundleContext.getBundles();
        }
        return null;
    }
    
    

	public BundleContext getbundleContext() {
		return bundleContext;
	}
    
    
    
}