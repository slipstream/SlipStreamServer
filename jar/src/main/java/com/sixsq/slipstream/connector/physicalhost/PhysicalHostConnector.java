package com.sixsq.slipstream.connector.physicalhost;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2013 SixSq Sarl (sixsq.com)
 * =====
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
 * -=================================================================-
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorBase;
import com.sixsq.slipstream.connector.Credentials;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ServerExecutionEnginePluginException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;

public class PhysicalHostConnector extends ConnectorBase {

	private static Logger log = Logger.getLogger(PhysicalHostConnector.class.toString());
	
	public static final String CLOUD_SERVICE_NAME = "physicalhost";
	
	public PhysicalHostConnector() {
		this(CLOUD_SERVICE_NAME);
	}
	
	public PhysicalHostConnector(String instanceName) {
		super(instanceName);
	}

	public Connector copy() {
		return new PhysicalHostConnector(getConnectorInstanceName());
	}
	
	public String getCloudServiceName() {
		return CLOUD_SERVICE_NAME;
	}
	
	@Override
	public Credentials getCredentials(User user) {
		return new PhysicalHostCredentials(user, getConnectorInstanceName());
	}

	@Override
	protected String constructKey(String key) throws ValidationException {
		return new PhysicalHostUserParametersFactory(getConnectorInstanceName()).constructKey(key);
	}
	
	@Override
	public Map<String, UserParameter> getUserParametersTemplate() throws ValidationException {
		return new PhysicalHostUserParametersFactory(getConnectorInstanceName()).getParameters();
	}
	
	@Override
	public Map<String, ServiceConfigurationParameter> getServiceConfigurationParametersTemplate() 
			throws ValidationException {
		return new PhysicalHostSystemConfigurationParametersFactory(getConnectorInstanceName()).getParameters();
	}
	
	@Override
	public Run launch(Run run, User user) throws SlipStreamException {
		switch (run.getCategory()) {
		case Image:
			throw (new ServerExecutionEnginePluginException("Run or build an image is impossible with this connector."));
		case Deployment:
			launchDeployment(run, user);
			break;
		default:
			throw (new ServerExecutionEnginePluginException("The run category '" + run.getCategory() + "' is unknown by this connector."));
		}

		return run;
	}
	
	@Override
	public void terminate(Run run, User user) throws SlipStreamException {
		Credentials credentials = getCredentials(user);
		
		String username = credentials.getKey();
		String password = credentials.getSecret(); 
		String privateKey = user.getParameterValue(constructKey(PhysicalHostUserParametersFactory.PRIVATE_KEY), "");
		
		String sudo = getSudo(username);
		String command = sudo+" bash -c '"; 
		command += "kill -9 `ps -Af | grep python | grep slipstream | grep -v grep | awk \"{print $2}\"`; ";
		command += "rm -R /tmp/slipstream*; rm /tmp/tmp*; rm -R /opt/slipstream;";
		command += "'";
		
		for (String host : getCloudNodeInstanceIds(run)) {
			System.out.println(host);
			executeViaSsh(username, privateKey, password, host, command, 22);
		}
	}

	// Work only for Orchestrator host
	@Override
	public Properties describeInstances(User user) throws SlipStreamException {
		String host = getOrchestratorImageId();
		Properties statuses = new Properties();
		
		InetAddress addr;
		try {
			addr = InetAddress.getByName(host);
			String status = (addr.isReachable(1000))? "Up" : "Down" ;
			statuses.put(host, status);
		} catch (UnknownHostException e) {} catch (IOException e) {}
		
		return statuses;
	}

	@Override
	protected String getOrchestratorImageId() throws ConfigurationException, ValidationException {
		return Configuration.getInstance().getRequiredProperty(constructKey("cloud.connector.orchestrator.host"));
	}

	private void launchDeployment(Run run, User user)
			throws ServerExecutionEnginePluginException, ConfigurationException, SlipStreamClientException {

		Configuration configuration = Configuration.getInstance();
		Credentials credentials = getCredentials(user);
		
		String username = "";
		String password = "";
		String privateKey = "";		
		try {
			username = credentials.getKey();
			password = credentials.getSecret();
			privateKey = user.getParameterValue(constructKey(PhysicalHostUserParametersFactory.PRIVATE_KEY), "");
		} catch (SlipStreamClientException e1) {
			e1.printStackTrace();
		}
		if(privateKey == null) privateKey = "";
		
		String host = getOrchestratorImageId();
		
		String command = createContextualizationData(run, user, configuration);
		
		String logInfos = "\nConnecting to physical host " + host + "\n";
		logInfos += "Username: " + username + "\n";
		//logInfos += "Password: " + password + "\n";
		logInfos += "Command: " + command + "\n";
		log.info(logInfos);		
		log.info("Return code: " + executeViaSsh(username, privateKey, password, host, command, 22) );

		updateInstanceIdAndIpOnRun(run, host, host);

	}


	private String createContextualizationData(Run run, User user, Configuration configuration)
			throws ConfigurationException, ServerExecutionEnginePluginException, SlipStreamClientException {

		String logfilename = "orchestrator.slipstream.log";
		String bootstrap = "/tmp/slipstream.bootstrap";
		String username = user.getName();

		String sudo = getSudo(username);
		String userData = "echo '("+sudo+" bash -c '\\''sleep 5; ";
		userData += "export SLIPSTREAM_CLOUD=\"" + getCloudServiceName() + "\"; ";
		userData += "export SLIPSTREAM_CONNECTOR_INSTANCE=\"" + getConnectorInstanceName() + "\"; ";
		userData += "export SLIPSTREAM_NODENAME=\"" + getOrchestratorName(run) + "\"; ";	
		userData += "export SLIPSTREAM_DIID=\"" + run.getName() + "\"; ";
		userData += "export SLIPSTREAM_REPORT_DIR=\"" + SLIPSTREAM_REPORT_DIR + "\"; ";
		userData += "export SLIPSTREAM_SERVICEURL=\"" + configuration.baseUrl + "\"; ";	
		userData += "export SLIPSTREAM_BUNDLE_URL=\"" + configuration.getRequiredProperty("slipstream.update.clienturl") + "\"; ";	
		userData += "export SLIPSTREAM_BOOTSTRAP_BIN=\"" + configuration.getRequiredProperty("slipstream.update.clientbootstrapurl") + "\"; ";
		userData += "export PHYSICALHOST_ORCHESTRATOR_HOST=\"" + getOrchestratorImageId() + "\"; ";
		userData += "export SLIPSTREAM_CATEGORY=\"" + run.getCategory().toString() + "\"; ";
		userData += "export SLIPSTREAM_USERNAME=\"" + username + "\"; ";
		userData += "export SLIPSTREAM_COOKIE=" + getCookieForEnvironmentVariable(username) + "; ";
		userData += "export SLIPSTREAM_VERBOSITY_LEVEL=\"" + getVerboseParameterValue(user) + "\"; ";

		userData += "mkdir -p " + SLIPSTREAM_REPORT_DIR + ";";
		userData += "wget --no-check-certificate -O " + bootstrap + " $SLIPSTREAM_BOOTSTRAP_BIN > " + SLIPSTREAM_REPORT_DIR + "/" + logfilename + " 2>&1 "
				+ "&& chmod 0755 " + bootstrap + "; "
				+ bootstrap + " slipstream-orchestrator >> " + SLIPSTREAM_REPORT_DIR + "/" + logfilename + " 2>&1 "
				+ "'\\'') > /dev/null 2>&1 &' | at now";

		System.out.print(userData);

		return userData;
	}

	
	private int executeViaSsh(String user, String privateKey, String password, String host, String command, int port){
		int exitStatus = -1;
		
		try{
			JSch jsch=new JSch();

			if(privateKey == null) privateKey = new String("");
			if(!privateKey.isEmpty()) jsch.addIdentity("temp", privateKey.getBytes(), new String("").getBytes(), new String("").getBytes());
			
			Session session=jsch.getSession(user, host, port);

			UserInfo ui = new SshUserInfo(password);
			session.setUserInfo(ui);
			session.connect();

			Channel channel=session.openChannel("exec");
			((ChannelExec)channel).setCommand(command);
			channel.setInputStream(null);
		//channel.setOutputStream(System.out);
		//((ChannelExec)channel).setErrStream(System.err);
			channel.connect();

			while(true){
				Thread.sleep(1000);
				if(channel.isClosed()){
					System.out.println("exit-status: "+channel.getExitStatus());
					break;
				}
			}
			
			exitStatus = channel.getExitStatus();
			
			channel.disconnect();
			session.disconnect();

		}catch(Exception e){
			System.out.println(e);
		}
		
		return exitStatus;
	}
	
	private String getSudo(String username){
		if(username.equals("root")){
			return "";
		}else{
			return "sudo";
		}
	}

	
}




