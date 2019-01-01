package org.cishell.container;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleException;

public class CIShellContainerTest {

    private CIShellContainer cishellContainer;

    @Before
    public void setup() {
        cishellContainer = CIShellContainer.getBuilder().pluginsDirectoryPath("../examples/cishell/target/plugins/").build();
    }

    @Test
    public void CIShellServicesInstalled() {
        Assert.assertNotNull(cishellContainer.getDataManagerService());
        Assert.assertNotNull(cishellContainer.getSchedulerService());
        Assert.assertNotNull(cishellContainer.getDataConversionService());
        //Assert.assertNotNull(cishellContainer.getGUIBuilderService());
        Assert.assertNotNull(cishellContainer.getLogService());
        Assert.assertNotNull(cishellContainer.getMetaTypeService());
    }

    @After
    public void tearDown() throws BundleException, InterruptedException {
        cishellContainer.shutdownApplication();
    }
}