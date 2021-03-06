package org.wso2.bam.integration.tests.toolbox;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.bam.toolbox.deployer.stub.BAMToolboxDepolyerServiceStub;
import org.wso2.carbon.integration.framework.ClientConnectionUtil;
import org.wso2.carbon.integration.framework.LoginLogoutUtil;
import org.wso2.carbon.integration.framework.utils.FrameworkSettings;

import static org.testng.Assert.assertTrue;

/**
 * Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class DefaultToolBoxTestCase {
    private static final Log log = LogFactory.getLog(DefaultToolBoxTestCase.class);

    private LoginLogoutUtil util = new LoginLogoutUtil();

    private static final String TOOLBOX_DEPLOYER_SERVICE = "/services/BAMToolboxDepolyerService";

    private BAMToolboxDepolyerServiceStub toolboxStub;

    private String deployedToolBox = "";

    private static final int RETRY_COUNT = 30;

    @BeforeClass(groups = {"wso2.bam"})
    public void init() throws Exception {
        ConfigurationContext configContext = ConfigurationContextFactory.
                createConfigurationContextFromFileSystem(null);
        String EPR = "https://" + FrameworkSettings.HOST_NAME +
                ":" + FrameworkSettings.HTTPS_PORT + TOOLBOX_DEPLOYER_SERVICE;
        String loggedInSessionCookie = util.login();
        toolboxStub = new BAMToolboxDepolyerServiceStub(configContext, EPR);
        ServiceClient client = toolboxStub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING,
                loggedInSessionCookie);
    }

    @Test(groups = {"wso2.bam"})
    public void defaultToolBoxDeployment() throws Exception {
        deployedToolBox = getToolBoxName();

        toolboxStub.deployBasicToolBox(1);
         log.info("Installing toolbox...");

        String toolBoxname = deployedToolBox.replaceAll(".tbox", "");
        boolean installed = false;
        int noOfTry = 1;

        while (!installed && noOfTry <= RETRY_COUNT) {
            Thread.sleep(1000);

            //get List of deployed toolboxes
            BAMToolboxDepolyerServiceStub.ToolBoxStatusDTO statusDTO = toolboxStub.getDeployedToolBoxes("1", "");
            String[] deployed = statusDTO.getDeployedTools();

            if (null != deployed) {

                for (String aTool : deployed) {
                    aTool = aTool.replaceAll(".tbox", "");
                    if (aTool.equalsIgnoreCase(toolBoxname)) {
                        installed = true;
                        break;
                    }
                }
            }
            noOfTry++;
        }

        assertTrue(installed, "Default installation of toolbox :" + toolBoxname + " failed!!");

    }


    @Test(groups = {"wso2.bam"}, dependsOnMethods = "defaultToolBoxDeployment")
    public void undeployDefaultToolbox() throws Exception {
        String toolBoxname = deployedToolBox.replaceAll(".tbox", "");
               toolboxStub.undeployToolBox(new String[]{toolBoxname});

               boolean unInstalled = false;

               log.info("Un installing toolbox...");

               int noOfTry = 1;

               while (!unInstalled && noOfTry <= RETRY_COUNT) {
                   Thread.sleep(1000);

                   BAMToolboxDepolyerServiceStub.ToolBoxStatusDTO statusDTO = toolboxStub.getDeployedToolBoxes("1", "");
                   String[] deployedTools = statusDTO.getDeployedTools();
                   String[] undeployingTools = statusDTO.getToBeUndeployedTools();
                   boolean isUninstalled = true;

                   if (null != undeployingTools) {
                       for (String aTool : undeployingTools) {
                           if (aTool.equalsIgnoreCase(toolBoxname)) {
                               isUninstalled = false;
                               break;
                           }
                       }
                   }

                   if (null != deployedTools && isUninstalled) {
                       for (String aTool : deployedTools) {
                           if (aTool.equalsIgnoreCase(toolBoxname)) {
                               isUninstalled = false;
                               break;
                           }
                       }
                   }
                   unInstalled = isUninstalled;
                   noOfTry++;
               }


        assertTrue(unInstalled, "Uninstalling default toolbox" + deployedToolBox + " is not successful");
    }


    private String getToolBoxName() throws Exception {
        BAMToolboxDepolyerServiceStub.BasicToolBox[] toolBoxes = toolboxStub.getBasicToolBoxes();
        if (null == toolBoxes || toolBoxes.length == 0) {
            throw new Exception("No default toolboxes available..");
        }
        return toolBoxes[0].getTBoxFileName();
    }


     @AfterClass(groups = {"wso2.bam"})
     public void logout() throws Exception {
        ClientConnectionUtil.waitForPort(9443);
        util.logout();
    }
}
