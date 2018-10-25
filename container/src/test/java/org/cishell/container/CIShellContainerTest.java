package org.cishell.container;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleException;

public class CIShellContainerTest {

    private CIShellContainer cishellContainer;
    private static final String CONVERTERGRAPH_ALGORITHM = "org.cishell.algorithm.convertergraph.ConverterGraphAlgorithm";


    @Before
    public void setup() {
        cishellContainer = new CIShellContainer("../examples/cishell/target/plugins/", null);
    }

    @Test
    public void CIShellServicesInstalled() throws InterruptedException {
        int ticks = 10;
        while (ticks-- > 0) {
            if (cishellContainer.getAlgorithmFactory(CONVERTERGRAPH_ALGORITHM) != null) {
                break;
            }
            Thread.sleep(500);
        }

        Assert.assertNotNull(cishellContainer.getDataManagerService());
        Assert.assertNotNull(cishellContainer.getSchedulerService());
        Assert.assertNotNull(cishellContainer.getDataConversionService());
        //Assert.assertNotNull(cishellContainer.getGUIBuilderService());
        Assert.assertNotNull(cishellContainer.getLogService());
        Assert.assertNotNull(cishellContainer.getMetaTypeService());
        Assert.assertNotNull(cishellContainer.getAlgorithmFactory(CONVERTERGRAPH_ALGORITHM));
    }

    @After
    public void tearDown() throws BundleException, InterruptedException {
        cishellContainer.shutdownApplication();
    }
}