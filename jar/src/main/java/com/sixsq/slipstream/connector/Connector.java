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

import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ExtraDisk;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;

/**
 * Interface providing cloud back-end interactions as well as connector
 * configuration, used in the user interface.
 */
public interface Connector {

	/**
	 * Launch Orchestrator virtual machine.
	 * 
	 * @param run
	 *            for which corresponding virtual machines must be launched
	 * @param user
	 *            owner of the run
	 * @throws SlipStreamException
	 */
	Run launch(Run run, User user) throws SlipStreamException;

	/**
	 * This object provides a convenience mapping from the user parameters from
	 * which we can simplify access to the abstracted cloud.
	 * 
	 * @param user
	 *            for which to extract the credentials object.
	 * @return credentials object providing an abstraction for accessing the the
	 *         cloud.
	 */
	Credentials getCredentials(User user);

	/**
	 * Terminate all running virtual machines associated with the run.
	 * 
	 * @param run
	 *            for which corresponding virtual machines must be terminated.
	 * @param user
	 *            owner of the run
	 * @throws SlipStreamException
	 */
	void terminate(Run run, User user) throws SlipStreamException;

	/**
	 * @param user
	 * @return properties representing each running machine instance. The
	 *         minimum key/value pairs the method returns includes:
	 *         <ul>
	 *         <li>virtual machine cloud ID/state</li>
	 *         </ul>
	 * @throws SlipStreamException
	 */
	Properties describeInstances(User user) throws SlipStreamException;

	/**
	 * @return list of extra disk objects an image can support for this
	 *         connector.
	 */
	List<ExtraDisk> getExtraDisks();

	/**
	 * @return map of user parameters available for the user to configure the
	 *         connector.
	 * @throws ValidationException
	 */
	Map<String, UserParameter> getUserParametersTemplate()
			throws ValidationException;

	/**
	 * @return map of service configuration parameters available to the system
	 *         administrator to configure the connector.
	 * @throws ValidationException
	 */
	Map<String, ServiceConfigurationParameter> getServiceConfigurationParametersTemplate()
			throws ValidationException;

	/**
	 * @return map of image module parameters available for image module in
	 *         order to instruct the connector how to provision the image.
	 * @throws ValidationException
	 */
	Map<String, ModuleParameter> getImageParametersTemplate()
			throws ValidationException;

	/**
	 * @return name of the cloud service the connector interfaces with.
	 */
	String getCloudServiceName();
	
	/**
	 * @return instance name of the cloud connector.
	 */
	String getConnectorInstanceName();
	
	String getOrchestratorName(Run run);

	Connector copy();
	
}
