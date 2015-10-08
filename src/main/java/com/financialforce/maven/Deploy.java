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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.DeployOptions;
import com.sforce.soap.metadata.DeployResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.Package;
import com.sforce.soap.metadata.PackageTypeMembers;
import com.sforce.soap.metadata.RetrieveMessage;
import com.sforce.soap.metadata.RetrieveRequest;
import com.sforce.soap.metadata.RetrieveResult;
import com.sforce.soap.metadata.RetrieveStatus;
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
			byte[] zip = zipRoot(getFileForPath("/Users/phardake/stash/apex-mocks/src"));
			String deployId = metadataConnection.deploy(zip, deployOptions ).getId();
			
			// Wait for the deploy to complete
	        int poll = 0;
	        DeployResult deployResult = null;
	        
	        do {
	            Thread.sleep(1000);
	            deployResult = metadataConnection.checkDeployStatus(deployId, (poll % 3 == 0));
	            System.out.println("Status is: " + deployResult.getStatus());
	        }
	        while (!deployResult.isDone());

//	        retrieve();
	        
	        if (!deployResult.isSuccess() && deployResult.getErrorStatusCode() != null) {
	            throw new Exception(deployResult.getErrorStatusCode() + " msg: " + deployResult.getErrorMessage());
	        }
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    		throw new MojoExecutionException("Deploy MOJO failed to execute", e);
    	}
    	
    }
    
    private void retrieve() throws Exception {
		RetrieveRequest retrieveRequest = new RetrieveRequest();
		Package unpackaged = new Package();
		List<PackageTypeMembers> types = new ArrayList<PackageTypeMembers>();
		PackageTypeMembers classes = new PackageTypeMembers();
		classes.setName("ApexClass");
		classes.setMembers(new String[] {"*"});
		types.add(classes );
		unpackaged.setTypes(types.toArray(new PackageTypeMembers[types.size()]) );
		retrieveRequest.setUnpackaged(unpackaged );
		
		AsyncResult asyncResult = metadataConnection.retrieve(retrieveRequest);
        String asyncResultId = asyncResult.getId();
        // Wait for the retrieve to complete
        int poll = 0;
        long waitTimeMilliSecs = 1000;
        RetrieveResult result = null;
        do {
            Thread.sleep(waitTimeMilliSecs);
            // Double the wait time for the next iteration
            result = metadataConnection.checkRetrieveStatus(asyncResultId, true);
            System.out.println("Retrieve Status: " + result.getStatus());
        } while (!result.isDone());
		 
        if (result.getStatus() == RetrieveStatus.Failed) {
            throw new Exception(result.getErrorStatusCode() + " msg: " +
                    result.getErrorMessage());
        } else if (result.getStatus() == RetrieveStatus.Succeeded) {     
            // Print out any warning messages
            StringBuilder buf = new StringBuilder();
            if (result.getMessages() != null) {
                for (RetrieveMessage rm : result.getMessages()) {
                    buf.append(rm.getFileName() + " - " + rm.getProblem());
                }
            }
            if (buf.length() > 0) {
                System.out.println("Retrieve warnings:\n" + buf);
            }
		     
            // Write the zip to the file system
            System.out.println("Writing results to zip file");
            ByteArrayInputStream bais = new ByteArrayInputStream(result.getZipFile());
            File resultsFile = new File("retrieveResults.zip");
            FileOutputStream os = new FileOutputStream(resultsFile);
            try {
                ReadableByteChannel src = Channels.newChannel(bais);
                FileChannel dest = os.getChannel();
                copy(src, dest);
                 
                System.out.println("Results written to " + resultsFile.getAbsolutePath());
            } finally {
                os.close();
            }
        }
    }

	protected File getFileForPath(String path) {
    	File file = null;
    	if (path != null) {
    		file = new File(this.projectBuildDir.substring(0, projectBuildDir.lastIndexOf(file.separator)), path);
    		if (!file.exists()) {
    			file = new File(path);
    		}
    	}
    	return file;
    }
    
//	private File createProjectFolder()
//	{
//		String sfProjectDir = projectBuildDir.substring(0, projectBuildDir.lastIndexOf(File.separator));
//		return new File(sfProjectDir + File.separator + "src");
//	}
    
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
		deployOptions.setSinglePackage(true);
		deployOptions.setTestLevel(TestLevel.NoTestRun);
		return deployOptions;
	}
	
	public static byte[] zipRoot(File rootDir) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(bos);
		
		File[] tops = rootDir.listFiles();
		if ((tops == null) || (tops.length == 0)) {
			throw new IOException("No files found in " + rootDir);
		}
		zipFiles("", tops, zos);
		zos.close();
		return bos.toByteArray();
	}
	
	public static void zipFiles(String relPath, File[] files, ZipOutputStream os) throws IOException {
		for (File file : files) {
			zipFile(relPath, file, os);
		}
	}
	
	public static void zipFile(String relPath, File file, ZipOutputStream os) throws IOException {
		String filePath = relPath + file.getName();
		
		if ((file.isDirectory()) && (!file.getName().startsWith("."))) {
			filePath = filePath + '/';
			ZipEntry dir = new ZipEntry(filePath);
			dir.setTime(file.lastModified());
			os.putNextEntry(dir);
			os.closeEntry();
			
			zipFiles(filePath, file.listFiles(), os);
		}
		else if ((!file.getName().startsWith(".")) && (!file.getName().endsWith("~"))) {
			addFile(filePath, file, os);
		}
	}
	
	private static ZipEntry addFile(String filename, File file, ZipOutputStream os) throws IOException {
		ZipEntry entry = new ZipEntry(filename);
		entry.setTime(file.lastModified());
		entry.setSize(file.length());
		os.putNextEntry(entry);
		FileInputStream is = new FileInputStream(file);
		
		try {
			FileChannel src = is.getChannel();
			WritableByteChannel dest = Channels.newChannel(os);
			copy(src, dest);
			os.closeEntry();
			return entry;
		}
		finally {
			is.close();
		}
	}
			  
	private static void copy(ReadableByteChannel src, WritableByteChannel dest) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(8092);
		while (src.read(buffer) != -1) {
			buffer.flip();
			while (buffer.hasRemaining()) {
				dest.write(buffer);
			}
			buffer.clear();
		}
	}	
}