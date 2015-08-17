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

import java.util.Map;
import java.util.Properties;

import com.sixsq.slipstream.credentials.Credentials;
import com.sixsq.slipstream.exceptions.NotImplementedException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;

public class ConnectorDummy extends ConnectorBase {

	public ConnectorDummy(String instanceName) {
		super(instanceName);
	}

	public Connector copy(){
		return new ConnectorDummy(getConnectorInstanceName());
	}

	@Override
	public Run launch(Run run, User user) throws SlipStreamException {
		return null;
	}

	@Override
	public Credentials getCredentials(User user) {
		return null;
	}

	@Override
	public void terminate(Run run, User user)
			throws SlipStreamException {
	}

	@Override
	public Map<String, Properties> describeInstances(User user, int timeout) {
		return null;
	}

	@Override
	public Map<String, ServiceConfigurationParameter> getServiceConfigurationParametersTemplate(){
		throw(new NotImplementedException());
	}

	@Override
	public Map<String, ModuleParameter> getImageParametersTemplate(){
		throw(new NotImplementedException());
	}

	@Override
	public Map<String, UserParameter> getUserParametersTemplate(){
		throw(new NotImplementedException());
	}

	@Override
	protected String getOrchestratorImageId(User user) {
		return null;
	}

	@Override
	protected String constructKey(String key) throws ValidationException {
		return "";
	}

	@Override
	public String getCloudServiceName() {
		return null;
	}
}
