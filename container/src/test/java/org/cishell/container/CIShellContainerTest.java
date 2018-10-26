package org.cishell.container;

import org.cishell.framework.algorithm.AlgorithmFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleException;

public class CIShellContainerTest {

    private CIShellContainer cishellContainer;

    @Before
    public void setup() {
        cishellContainer = new CIShellContainer("../examples/cishell/target/plugins/", null);
    }

    @Test
    public void CIShellServicesInstalled() throws InterruptedException {
        Assert.assertNotNull(cishellContainer.getDataManagerService());
        Assert.assertNotNull(cishellContainer.getSchedulerService());
        Assert.assertNotNull(cishellContainer.getDataConversionService());
        //Assert.assertNotNull(cishellContainer.getGUIBuilderService());
        Assert.assertNotNull(cishellContainer.getLogService());
        Assert.assertNotNull(cishellContainer.getMetaTypeService());
        Assert.assertNotNull(cishellContainer.waitAndGetService(AlgorithmFactory.class));
    }

    @After
    public void tearDown() throws BundleException, InterruptedException {
        cishellContainer.shutdownApplication();
    }
}