package org.cishell.container;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleException;

import static org.junit.Assert.assertNotNull;

public class CIShellContainerTest {

    private CIShellContainer cishellContainer;

    @Before
    public void setup() {
        cishellContainer = CIShellContainer.getBuilder().pluginsDirectoryPath("../examples/cishell/target/plugins/").build();
    }

    @Test
    public void CIShellServicesInstalled() {
        assertNotNull(cishellContainer.getDataManagerService());
        assertNotNull(cishellContainer.getSchedulerService());
        assertNotNull(cishellContainer.getDataConversionService());
        //assertNotNull(cishellContainer.getGUIBuilderService());
        assertNotNull(cishellContainer.getLogService());
        assertNotNull(cishellContainer.getMetaTypeService());
    }

    @After
    public void tearDown() throws BundleException, InterruptedException {
        cishellContainer.shutdownApplication();
    }
}