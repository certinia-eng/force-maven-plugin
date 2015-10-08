package com.financialforce.maven;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.sforce.soap.metadata.DeployOptions;
import com.sforce.soap.metadata.DeployResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.TestLevel;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

@Mojo( name = "compile", defaultPhase = LifecyclePhase.COMPILE )
public class Deploy
    extends AbstractMojo
{
	private static final String SERVICE_ENDPOINT = "/services/Soap/u/34.0";
	
    @Parameter(property = "sf.username")
    private String username;
    
    @Parameter(property = "sf.password")
    private String password;

    @Parameter(property = "sf.serverurl", defaultValue = "https://login.salesforce.com")
    private String serverUrl;

    @Parameter(property = "sf.deployRoot")
    private String deployRoot;
    
    @Parameter(defaultValue = "${project.build.directory}")
    private String projectBuildDir;
    
	private MetadataConnection metadataConnection;

	private String projectBaseDir;

    public void execute()
        throws MojoExecutionException
    {
    	initializeProjectBaseDir();
    	validateMojoParameters();
    	
    	try {
    		createMetadataConnection();
    		DeployOptions deployOptions = createDeployOptions();
			byte[] zip = ZipUtil.zipRoot(getFileForPath(this.deployRoot));
			String deployId = metadataConnection.deploy(zip, deployOptions ).getId();
			
	        DeployResult deployResult = null;
	        
	        do {
	            Thread.sleep(1000);
	            deployResult = metadataConnection.checkDeployStatus(deployId, false);
	            System.out.println("Status is: " + deployResult.getStatus());
	        }
	        
	        while (!deployResult.isDone());

	        if (!deployResult.isSuccess() && deployResult.getErrorStatusCode() != null) {
	            throw new Exception(deployResult.getErrorStatusCode() + " msg: " + deployResult.getErrorMessage());
	        }
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    		throw new MojoExecutionException("Deploy MOJO failed to execute", e);
    	}
    }

	private void initializeProjectBaseDir()
	{
		projectBaseDir = this.projectBuildDir.substring(0, projectBuildDir.lastIndexOf(File.separator));
	}

    private void validateMojoParameters() throws MojoExecutionException {
    	if (StringUtils.isEmpty(this.username)) {
    		throw new MojoExecutionException("Please specify force-maven-plugin option -Dusername");
    	}
    	
    	if (StringUtils.isEmpty(this.password)) {
    		throw new MojoExecutionException("Please specify force-maven-plugin option -Dpassword");
    	}
    }
    
    private void createMetadataConnection()
        	throws ConnectionException {
        final ConnectorConfig loginConfig = new ConnectorConfig();
		loginConfig.setAuthEndpoint(serverUrl + SERVICE_ENDPOINT);
        loginConfig.setServiceEndpoint(serverUrl + SERVICE_ENDPOINT);
        loginConfig.setManualLogin(true);
        
        
        LoginResult loginResult  = new PartnerConnection(loginConfig).login(username, password);
        final ConnectorConfig metadataConfig = new ConnectorConfig();
        metadataConfig.setServiceEndpoint(loginResult.getMetadataServerUrl());
        metadataConfig.setSessionId(loginResult.getSessionId());
        this.metadataConnection = new MetadataConnection(metadataConfig);
    }
    
	private DeployOptions createDeployOptions() {
		DeployOptions deployOptions = new DeployOptions();
		deployOptions.setRollbackOnError(true);
		deployOptions.setSinglePackage(true);
		deployOptions.setTestLevel(TestLevel.NoTestRun);
		return deployOptions;
	}
	
	protected File getFileForPath(String path) {
    	File file = null;
    	if (path != null) {
    		file = new File(projectBaseDir, path);
    		if (!file.exists()) {
    			file = new File(path);
    		}
    	}
    	return file;
    }
}