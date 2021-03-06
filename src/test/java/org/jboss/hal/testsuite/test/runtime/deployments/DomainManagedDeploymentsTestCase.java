package org.jboss.hal.testsuite.test.runtime.deployments;

import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.Graphene;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.hal.testsuite.cli.CliClient;
import org.jboss.hal.testsuite.cli.CliClientFactory;
import org.jboss.hal.testsuite.fragment.runtime.DeploymentContentRepositoryArea;
import org.jboss.hal.testsuite.fragment.runtime.DeploymentServerGroupArea;
import org.jboss.hal.testsuite.fragment.runtime.DeploymentWizard;
import org.jboss.hal.testsuite.page.runtime.DomainDeploymentPage;
import org.jboss.hal.testsuite.test.category.Domain;
import org.jboss.hal.testsuite.util.Console;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author mkrajcov <mkrajcov@redhat.com>
 */
@RunWith(Arquillian.class)
@Category(Domain.class)
public class DomainManagedDeploymentsTestCase {

    public static final String MAIN_SERVER_GROUP = "main-server-group";
    public static final String OTHER_SERVER_GROUP = "other-server-group";

    private static final String FILE_PATH = "src/test/resources/";
    private static final String FILE_NAME = "mockWar.war";
    private static final String NAME = "n_" + RandomStringUtils.randomAlphanumeric(5) + ".war";
    private static final String RUNTIME_NAME = "rn_" + RandomStringUtils.randomAlphanumeric(5) + ".war";

    private static CliClient client = CliClientFactory.getClient();
    private static DeploymentsOperations ops = new DeploymentsOperations(client);

    @Drone
    WebDriver browser;

    @Page
    DomainDeploymentPage page;

    @AfterClass
    public static void cleanUp() {
        ops.undeploy(NAME);
    }

    @Before
    public void before() {
        browser.navigate().refresh();
        Graphene.goTo(DomainDeploymentPage.class);
        Console.withBrowser(browser).waitUntilLoaded();
        Console.withBrowser(browser).maximizeWindow();
    }

    @Test
    @InSequence(0)
    public void createDeployment() throws InterruptedException {
        DeploymentContentRepositoryArea content = page.switchToContentRepository();
        File deployment = new File(FILE_PATH + FILE_NAME);

        DeploymentWizard wizard = content.add();

        boolean result = wizard.uploadDeployment(deployment)
                .nextFluent()
                .name(NAME)
                .runtimeName(RUNTIME_NAME)
                .finish();

        assertTrue("Deployment wizard should close", result);
        assertTrue("Deployment should exist", ops.exists(NAME));
    }

    @Test
    @InSequence(1)
    public void assignDeploymentToServerGroup() {
        DeploymentContentRepositoryArea content = page.switchToContentRepository();
        content.assignDeployment(NAME, MAIN_SERVER_GROUP);

        assertTrue("Deployment should be assigned to server group.", ops.isAssignedToServerGroup(MAIN_SERVER_GROUP, NAME));
    }

    @Test
    @InSequence(2)
    public void disableDeployment() {
        DeploymentServerGroupArea area = page.switchToServerGroup(MAIN_SERVER_GROUP);
        area.changeState(NAME);

        assertFalse("Deployment should be enabled", ops.isEnabledInServerGroup(MAIN_SERVER_GROUP, NAME));
    }

    @Test
    @InSequence(3)
    public void enableDeployment() {
        DeploymentServerGroupArea area = page.switchToServerGroup(MAIN_SERVER_GROUP);
        area.changeState(NAME);

        assertTrue("Deployment should be enabled", ops.isEnabledInServerGroup(MAIN_SERVER_GROUP, NAME));
    }

    @Test
    @InSequence(4)
    public void removeDeployment() {
        DeploymentContentRepositoryArea content = page.switchToContentRepository();
        content.removeAndConfirm(NAME);

        assertFalse("Deployment should not exist", ops.exists(NAME));
    }
}
