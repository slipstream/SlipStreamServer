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

import java.util.HashMap;
import java.util.Map;

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.ParametersFactoryBase;
import com.sixsq.slipstream.persistence.QuotaParameter;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;

public abstract class SystemConfigurationParametersFactoryBase extends
		ParametersFactoryBase<ServiceConfigurationParameter> {

	protected Map<String, ServiceConfigurationParameter> referenceParameters = new HashMap<String, ServiceConfigurationParameter>();

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
		parameter.setMandatory(mandatory);
		return parameter;
	}

	@Override
	protected ServiceConfigurationParameter createParameter(String name,
			String description, boolean mandatory) throws ValidationException {
		ServiceConfigurationParameter parameter = new ServiceConfigurationParameter(
				name, "", description);
		parameter.setMandatory(mandatory);
		return parameter;
	}

	@Override
	protected ServiceConfigurationParameter createParameter(String name,
			boolean value, String description) throws ValidationException {
		ServiceConfigurationParameter parameter = new ServiceConfigurationParameter(
				name, String.valueOf(value), description);
		return parameter;
	}

	protected void putMandatoryOrchestrationImageId()
			throws ValidationException {
		putMandatoryParameter(
				super.constructKey(UserParametersFactoryBase.ORCHESTRATOR_IMAGEID_PARAMETER_NAME),
				"Image Id of the orchestrator for " + getCategory());
	}

	protected void putMandatoryOrchestratorInstanceType()
			throws ValidationException {
		putMandatoryParameter(
				super.constructKey(UserParametersFactoryBase.ORCHESTRATOR_INSTANCE_TYPE_PARAMETER_NAME),
				"Orchestrator instance type  " + getCategory());
	}

	protected void putMandatoryUpdateUrl() throws ValidationException {
		putMandatoryParameter(
				super.constructKey(UserParametersFactoryBase.UPDATE_CLIENTURL_PARAMETER_NAME),
				"URL pointing to the tarball containing the client for "
						+ getCategory());
	}

	protected void putMandatoryEndpoint() throws ValidationException {
		putMandatoryParameter(
				super.constructKey(UserParametersFactoryBase.ENDPOINT_PARAMETER_NAME),
				"Service endpoint for " + getCategory()
						+ " (e.g. http://example.com:5000)");
	}

	protected void putMandatoryQuotaVm() throws ValidationException {
		putMandatoryParameter(
				super.constructKey(QuotaParameter.QUOTA_VM_PARAMETER_NAME),
				"VM quota for " + getCategory() + " (i.e. maximum number of VMs allowed)");
	}

	protected void putMandatoryMaxIaasWorkers() throws ValidationException {
		putMandatoryParameter(
				super.constructKey(RuntimeParameter.MAX_JAAS_WORKERS_KEY),
				RuntimeParameter.MAX_JAAS_WORKERS_DESCRIPTION,
				RuntimeParameter.MAX_JAAS_WORKERS_DEFAULT);
	}

}
