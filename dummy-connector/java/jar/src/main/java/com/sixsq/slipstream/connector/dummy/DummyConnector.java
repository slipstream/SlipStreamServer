package com.sixsq.slipstream.connector.dummy;

/*
 * +=================================================================+
 * SlipStream Connector Dummy
 * =====
 * Copyright (C) 2014 SixSq Sarl (sixsq.com)
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

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.sixsq.slipstream.connector.CliConnectorBase;
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.SystemConfigurationParametersFactoryBase;
import com.sixsq.slipstream.credentials.Credentials;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.*;


public class DummyConnector extends CliConnectorBase {

	public static final String ZONE_TYPE = "Basic";
	public static final String CLOUD_SERVICE_NAME = "dummy";
	public static final String CLOUDCONNECTOR_PYTHON_MODULENAME = "slipstream_dummy.DummyClientCloud";

	public DummyConnector() {
		this(CLOUD_SERVICE_NAME);
	}

    public DummyConnector(String instanceName) {
		super(instanceName != null ? instanceName : CLOUD_SERVICE_NAME);
	}

	@Override
    public Connector copy(){
    	return new DummyConnector(getConnectorInstanceName());
    }

    protected String getZoneType(){
		return ZONE_TYPE;
	}

	protected String getEndpoint(User user) throws ValidationException {
		return "";
	}

	@Override
	public String getCloudServiceName() {
		return CLOUD_SERVICE_NAME;
	}

	@Override
	protected String getCloudConnectorPythonModule() {
		return CLOUDCONNECTOR_PYTHON_MODULENAME;
	}

	protected String getZone(User user) throws ValidationException {
		return ((UserParameter)user.getParameter(this.constructKey("zone"))).getValue();
	}
	@Override
	protected Map<String, String> getConnectorSpecificUserParams(User user)
			throws ConfigurationException, ValidationException {
		Map<String, String> userParams = new HashMap();
		userParams.put("endpoint", this.getEndpoint(user));
		userParams.put("zone", this.getZone(user));
		userParams.remove("endpoint");

		String domainName = getDomainName(user);
		if (domainName != null && ! domainName.isEmpty()) {
			userParams.put("domain-name", domainName);
		}

		return userParams;
	}


	protected String getNetworks(Run run, User user) throws ValidationException {
		return "";
	}

	protected String getInstanceType(Run run, User user) throws ValidationException {
		return isInOrchestrationContext(run) ?
				((UserParameter)user.getParameter(this.constructKey("orchestrator.instance.type"))).getValue() :
				this.getInstanceType(run);
	}


	@Override
	protected Map<String, String> getConnectorSpecificLaunchParams(Run run, User user)
			throws ConfigurationException, ValidationException {
		Map<String, String> launchParams = new HashMap();
		launchParams.put("instance-type", this.getInstanceType(run, user));
		launchParams.put("zone-type", this.getZoneType());
		launchParams.put("networks", this.getNetworks(run, user));
		this.putLaunchParamSecurityGroups(launchParams, run, user);
		launchParams.remove("zone-type");
		launchParams.remove("networks");
		launchParams.put("disk-size", getDiskSize(run, user));
		return launchParams;
	}

	protected String getDiskSize(Run run, User user) throws ValidationException {
		if (isInOrchestrationContext(run)) {
			String key = DummySystemConfigurationParametersFactory.ORCHESTRATOR_DISK_SIZE_KEY;
			return user.getParameter(constructKey(key)).getValue();
		} else {
			return getParameterValue(DummyImageParametersFactory.DISK_SIZE_KEY, run);
		}
	}

	@Override
	protected void putLaunchParamNativeContextualization(Map<String, String> launchParams, Run run)
			throws ValidationException {
		String key = SystemConfigurationParametersFactoryBase.NATIVE_CONTEXTUALIZATION_KEY;
		String value = SystemConfigurationParametersFactoryBase.NATIVE_CONTEXTUALIZATION_LINUX_ONLY;
		launchParams.put(key, value);
	}

	protected void validateBaseParameters(User user) throws ValidationException {
		return;
	}

	protected void validateCapabilities(Run run) throws ValidationException {
		if(isInOrchestrationContext(run) && run.getCategory() == ModuleCategory.Image) {
			throw new ValidationException("Image creation is not available on Dummy");
		}
	}

	@Override
	protected void validateLaunch(Run run, User user) throws ValidationException {
    	super.validateLaunch(run, user);

		if (run.getType() == RunType.Run && run.getCategory() == ModuleCategory.Image) {
			String extraDiskGb = getExtraDiskVolatile(run);
			if (extraDiskGb != null && !extraDiskGb.isEmpty()) {
				throw new ValidationException("Extra disk is not available on Dummy");
			}
		}

	}

	protected String getDomainName(User user) throws ValidationException {
		return user.getParameterValue(constructKey(DummyUserParametersFactory.KEY_DOMAIN_NAME), null);
	}

	@Override
	public Credentials getCredentials(User user) {
		return new DummyCredentials(user, getConnectorInstanceName());
	}

	@Override
	public Map<String, ModuleParameter> getImageParametersTemplate() throws ValidationException {
		return new DummyImageParametersFactory(getConnectorInstanceName()).getParameters();
	}

	@Override
	protected String constructKey(String key) throws ValidationException {
		return (new DummyUserParametersFactory(this.getConnectorInstanceName())).constructKey(new String[]{key});
	}


	@Override
	public Map<String, UserParameter> getUserParametersTemplate() throws ValidationException {
		return new DummyUserParametersFactory(getConnectorInstanceName()).getParameters();
	}

	@Override
	public Map<String, ServiceConfigurationParameter> getServiceConfigurationParametersTemplate()
			throws ValidationException {
		return new DummySystemConfigurationParametersFactory(getConnectorInstanceName()).getParameters();
	}

	@Override
	public void applyServiceOffer(Run run, String nodeInstanceName, JsonObject serviceOffer)
			throws ValidationException
	{
		setRuntimeParameterValueFromServiceOffer(run, serviceOffer, nodeInstanceName,
				constructCloudParameterName(ImageModule.INSTANCE_TYPE_KEY),
				"resource:instanceType");

		setRuntimeParameterValueFromServiceOffer(run, serviceOffer, nodeInstanceName,
				constructCloudParameterName(DummyImageParametersFactory.DISK_SIZE_KEY),
				"resource:disk");
	}

}
