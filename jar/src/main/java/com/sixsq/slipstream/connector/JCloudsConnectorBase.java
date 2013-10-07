package com.sixsq.slipstream.connector;

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

import java.util.Properties;

import org.jclouds.Constants;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.rest.RestContext;

import com.sixsq.slipstream.exceptions.InvalidElementException;
import com.sixsq.slipstream.exceptions.ServerExecutionEnginePluginException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.User;

@SuppressWarnings("deprecation")
public abstract class JCloudsConnectorBase<S, A> extends ConnectorBase {

	@Override
	abstract public String getCloudServiceName();

	@Override
	abstract public Run launch(Run run, User user) throws SlipStreamException;

	@Override
	abstract public Credentials getCredentials(User user);

	@Override
	abstract public void terminate(Run run, User user) throws SlipStreamException;

	@Override
	abstract public Properties describeInstances(User user) throws SlipStreamException;

	public JCloudsConnectorBase(String instanceName) {
		super(instanceName);
	}

	abstract public String getJcloudsDriverName();

	protected RestContext<S, A> createContext(
			Credentials credentials)
			throws ServerExecutionEnginePluginException {
		try {
			return initContext(credentials);
		} catch (InvalidElementException ex) {
			ex.printStackTrace();
			throw (new ServerExecutionEnginePluginException(ex.getMessage()));
		}
	}

	private RestContext<S, A> initContext(
			Credentials credentials) throws InvalidElementException {
		checkCredentials(credentials);
		return new ComputeServiceContextFactory().createContext(
				getJcloudsDriverName(), credentials.getKey(),
				credentials.getSecret()).getProviderSpecificContext();
	}
	
	protected RestContext<S, A> createContext(Credentials credentials, Properties overrides) throws InvalidElementException {
		return initContext(credentials, overrides);
	}

	private RestContext<S, A> initContext(Credentials credentials, Properties overrides) throws InvalidElementException {
		checkCredentials(credentials);
		
		overrides.setProperty(Constants.PROPERTY_IDENTITY,   credentials.getKey());
		overrides.setProperty(Constants.PROPERTY_CREDENTIAL, credentials.getSecret());
		
		ComputeServiceContextFactory cscf = new ComputeServiceContextFactory();
		ComputeServiceContext        csc  = cscf.createContext(getJcloudsDriverName(), overrides);
		//ComputeServiceContext        csc  = cscf.createContext(COMPUTESERVICE_NAME, credentials.getKey(), credentials.getSecret());
		RestContext<S, A>            rc   = csc.getProviderSpecificContext();
		return rc;
	}

}
