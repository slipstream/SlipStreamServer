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

import java.util.*;

import com.sixsq.slipstream.es.CljElasticsearchHelper;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.ParametersFactoryBase;
import com.sixsq.slipstream.persistence.ParameterType;
import com.sixsq.slipstream.persistence.QuotaParameter;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;

public abstract class SystemConfigurationParametersFactoryBase extends
		ParametersFactoryBase<ServiceConfigurationParameter> {

	protected Map<String, ServiceConfigurationParameter> referenceParameters = new HashMap<String, ServiceConfigurationParameter>();

	public static final String ORCHESTRATOR_USERNAME_KEY = "orchestrator.ssh.username";
	public static final String ORCHESTRATOR_PASSWORD_KEY = "orchestrator.ssh.password";

	public static final String NATIVE_CONTEXTUALIZATION_KEY = "native-contextualization";

	public static final String NATIVE_CONTEXTUALIZATION_NEVER = "never";
	public static final String NATIVE_CONTEXTUALIZATION_ALWAYS = "always";
	public static final String NATIVE_CONTEXTUALIZATION_LINUX_ONLY = "linux-only";
	public static final String NATIVE_CONTEXTUALIZATION_WINDOWS_ONLY = "windows-only";

	public static final String NATIVE_CONTEXTUALIZATION_DEFAULT = NATIVE_CONTEXTUALIZATION_LINUX_ONLY;

	public static List<String> getNativeContextualizationOptions() {
		String[] options = {NATIVE_CONTEXTUALIZATION_NEVER, NATIVE_CONTEXTUALIZATION_LINUX_ONLY, NATIVE_CONTEXTUALIZATION_WINDOWS_ONLY, NATIVE_CONTEXTUALIZATION_ALWAYS};
		return Arrays.asList(options);
	}

	public SystemConfigurationParametersFactoryBase(String category)
			throws ValidationException {
		super(category);
		initReferenceParameters();
	}

	protected void initReferenceParameters() throws ValidationException {
		putMandatoryOrchestrationImageId();
		putMandatoryQuotaVm();
		putMandatoryMaxIaasWorkers();
	}

	protected void initConnectorParameters(String connectorName) {
		List<ServiceConfigurationParameter> scps = CljElasticsearchHelper.getConnectorParameters(connectorName);
		for (ServiceConfigurationParameter scp : scps) {
			scp.setName(super.constructKey(scp.getName()));
			scp.setCategory(getCategory());
			assignParameter(scp);
		}
	}

	public Map<String, ServiceConfigurationParameter> getParameters() {
		return getReferenceParameters();
	}

	@Override
	protected Map<String, ServiceConfigurationParameter> getReferenceParameters() {
		return referenceParameters;
	}

	@Override
	protected ServiceConfigurationParameter createParameter(String name,
			String value, String description, boolean mandatory)
			throws ValidationException {
		ServiceConfigurationParameter parameter = new ServiceConfigurationParameter(
				name, value, description);
		parameter.setCategory(getCategory());
		parameter.setMandatory(mandatory);
		return parameter;
	}

	@Override
	protected ServiceConfigurationParameter createParameter(String name,
			String description, boolean mandatory) throws ValidationException {
		ServiceConfigurationParameter parameter = new ServiceConfigurationParameter(
				name, "", description);
		parameter.setCategory(getCategory());
		parameter.setMandatory(mandatory);
		return parameter;
	}

	@Override
	protected ServiceConfigurationParameter createParameter(String name,
			boolean value, String description) throws ValidationException {
		ServiceConfigurationParameter parameter = new ServiceConfigurationParameter(
				name, String.valueOf(value), description);
		parameter.setCategory(getCategory());
		return parameter;
	}

	protected void getAndAssignParameter(String paramName) {
		ServiceConfigurationParameter scp = CljElasticsearchHelper.getConnectorParameterDescription(paramName);
		if (null != scp) {
			scp.setName(super.constructKey(paramName));
			scp.setCategory(getCategory());
			assignParameter(scp);
		}
	}

	protected void putMandatoryOrchestrationImageId() throws ValidationException {
		getAndAssignParameter(UserParametersFactoryBase.ORCHESTRATOR_IMAGEID_PARAMETER_NAME);
	}

	protected void putMandatoryQuotaVm() throws ValidationException {
		getAndAssignParameter(QuotaParameter.QUOTA_VM_PARAMETER_NAME);
	}

	protected void putMandatoryMaxIaasWorkers() throws ValidationException {
		getAndAssignParameter(RuntimeParameter.MAX_JAAS_WORKERS_KEY);
	}

	// to be removed

	protected void putMandatoryOrchestratorInstanceType() throws ValidationException {
		getAndAssignParameter(UserParametersFactoryBase.ORCHESTRATOR_INSTANCE_TYPE_PARAMETER_NAME);
	}

	protected void putMandatoryOrchestratorSecurityGroups() throws ValidationException {
		getAndAssignParameter(SECURITY_GROUPS_PARAMETER_NAME);
	}

	protected void putMandatoryUpdateUrl() throws ValidationException {
		getAndAssignParameter(UserParametersFactoryBase.UPDATE_CLIENTURL_PARAMETER_NAME);
	}

	protected void putMandatoryEndpoint() throws ValidationException {
		getAndAssignParameter(UserParametersFactoryBase.ENDPOINT_PARAMETER_NAME);
	}

	protected void putMandatoryContextualizationType() throws ValidationException {
		getAndAssignParameter(NATIVE_CONTEXTUALIZATION_KEY);
	}

	protected void putMandatoryOrchestratorUsernameAndPassword() throws ValidationException {
		getAndAssignParameter(ORCHESTRATOR_USERNAME_KEY);
		getAndAssignParameter(ORCHESTRATOR_PASSWORD_KEY);
	}

}
