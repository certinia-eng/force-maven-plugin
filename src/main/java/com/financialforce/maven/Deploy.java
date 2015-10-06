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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
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

    @Parameter(defaultValue = "${project.build.directory}")
    private String projectBuildDir;
    
	private MetadataConnection metadataConnection;

    public void execute()
        throws MojoExecutionException
    {
    	validateMojoParameters();
    	
    	try {
    		createMetadataConnection();
    		DeployOptions deployOptions = createDeployOptions();
    		byte[] zip = zipFolder(createProjectFolder());
    		
			String deployId = metadataConnection.deploy(zip, deployOptions ).getId();

			// Wait for the deploy to complete
	        int poll = 0;
	        DeployResult deployResult = null;
	        boolean fetchDetails;
	        
	        do {
	            Thread.sleep(3000);
	            fetchDetails = (poll % 3 == 0);
	            
	            deployResult = metadataConnection.checkDeployStatus(deployId, fetchDetails);
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

	private File createProjectFolder()
	{
		String sfProjectDir = projectBuildDir.substring(0, projectBuildDir.lastIndexOf(File.separator));
		return new File(sfProjectDir + File.separator + "src");
	}
    
    private void validateMojoParameters() throws MojoExecutionException {
//    	if (StringUtils.isEmpty(this.username)) {
//    		throw new MojoExecutionException("Please specify force-maven-plugin option -Dusername");
//    	}
//    	
//    	if (StringUtils.isEmpty(this.password)) {
//    		throw new MojoExecutionException("Please specify force-maven-plugin option -Dpassword");
//    	}
    }
    
    private void createMetadataConnection()
    	throws ConnectionException {
    		final ConnectorConfig loginConfig = new ConnectorConfig();
    		loginConfig.setAuthEndpoint(serverUrl + SERVICE_ENDPOINT);
	        loginConfig.setServiceEndpoint(serverUrl + SERVICE_ENDPOINT);
	        loginConfig.setManualLogin(true);
	        
	        
	        LoginResult loginResult  = new PartnerConnection(loginConfig).login(username, password);
//	        LoginResult loginResult = (new EnterpriseConnection(loginConfig)).login(
//	                username, password);
	        final ConnectorConfig metadataConfig = new ConnectorConfig();
	        metadataConfig.setServiceEndpoint(loginResult.getMetadataServerUrl());
	        metadataConfig.setSessionId(loginResult.getSessionId());
	        this.metadataConnection = new MetadataConnection(metadataConfig);
    }

	private DeployOptions createDeployOptions()
	{
		DeployOptions deployOptions = new DeployOptions();
		deployOptions.setRollbackOnError(true);
		deployOptions.setTestLevel(TestLevel.NoTestRun);
		return deployOptions;
	}

	private byte[] zipFolder(final File folder) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            processFolder(folder, zipOutputStream, folder.getPath().length() + 1);
            OutputStream outputStream = new FileOutputStream (this.projectBuildDir + File.separator + "test.zip"); 
            byteArrayOutputStream.writeTo(outputStream);
            return byteArrayOutputStream.toByteArray();
        }
    }

    private static void processFolder(final File folder,
    								  final ZipOutputStream zipOutputStream,
    								  final int prefixLength) throws IOException {
        for (final File file : folder.listFiles()) {
            if (file.isFile()) {
                final ZipEntry zipEntry = new ZipEntry(file.getPath().substring(prefixLength));
                zipOutputStream.putNextEntry(zipEntry);
                
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    IOUtils.copy(inputStream, zipOutputStream);
                }
        		
                zipOutputStream.closeEntry();
            } else if (file.isDirectory()) {
                processFolder(file, zipOutputStream, prefixLength);
            }
        }
    }    
}
