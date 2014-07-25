package com.sixsq.slipstream.run;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.credentials.Credentials;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ServerExecutionEnginePluginException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.RunFactory;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.NodeParameter;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.Vm;
import com.sixsq.slipstream.resource.BaseResource;
import com.sixsq.slipstream.run.RunView.RunViewList;
import com.sixsq.slipstream.util.ConfigurationUtil;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.SerializationUtil;

/**
 * Unit test:
 *
 * @see RunListResourceTest.class
 *
 */
public class RunListResource extends BaseResource {

	public static final String TYPE = "type";
	public static final String REFQNAME = "refqname";
	public static final String MUTABLE_RUN_KEY = "mutable";
	public static final String IGNORE_ABORT_QUERY = "ignoreabort";
	String refqname = null;

	@Get("txt")
	public Representation toTxt() {
		RunViewList runViewList = fetchListView();
		String result = SerializationUtil.toXmlString(runViewList);
		return new StringRepresentation(result);
	}

	@Get("xml")
	public Representation toXml() {

		RunViewList runViewList = fetchListView();
		String result = SerializationUtil.toXmlString(runViewList);
		return new StringRepresentation(result, MediaType.APPLICATION_XML);
	}

	@Get("html")
	public Representation toHtml() {

		RunViewList runViewList = fetchListView();

		return new StringRepresentation(HtmlUtil.toHtml(runViewList,
				getPageRepresentation(), getTransformationType(), getUser()),
				MediaType.TEXT_HTML);
	}

	private RunViewList fetchListView() {

		Reference resourceRef = getRequest().getResourceRef();
		Form form = resourceRef.getQueryAsForm();
		String query = form.getFirstValue("query");

		RunViewList list = null;
		try {
			list = fetchListView(query, getUser());
		} catch (ConfigurationException e) {
			throwConfigurationException(e);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}
		return list;
	}

	static RunViewList fetchListView(String query, User user)
			throws ConfigurationException, ValidationException {
		List<RunView> list;

		if (user.isSuper()) {
			list = (query != null) ? Run.viewList(query, user) : Run
					.viewListAll();
		} else {
			list = (query != null) ? Run.viewList(query, user) : Run
					.viewList(user);
		}
		return new RunViewList(list);
	}

	/**
	 * We need to merge data from different sources: - the form (entity) - the
	 * default module
	 *
	 * In the case of orchestrator + VM(s) (i.e. deployment and build) we also
	 * need to merge: - the default from each node
	 *
	 * The service cloud is the part that causes most trouble, since it can be
	 * defined at all levels.
	 */
	@Post("form|txt")
	public void createRun(Representation entity) throws ResourceException,
			FileNotFoundException, IOException, SQLException,
			ClassNotFoundException, SlipStreamException {

		Form form = new Form(entity);
		setReference(form);

		Run run;
		try {
			Module module = loadReferenceModule();

			authorizePost(module);

			updateReference(module);
			module.validate();

			User user = getUser();
			user = User.loadByName(user.getName()); // ensure user is loaded from database

			Map<String, List<Parameter<?>>> userChoices = getUserChoicesFromForm(module.getCategory(), form);

			run = RunFactory.getRun(module, parseType(form), user, userChoices);

			run = addCredentials(run);

			setRunMutability(run, form);

			if (Configuration.isQuotaEnabled()) {
				Quota.validate(user, run.getCloudServiceUsage(), Vm.usage(user.getName()));
			}

			createRepositoryResource(run);

			run.store();

			launch(run);

		} catch (SlipStreamClientException ex) {
			throw (new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					ex.getMessage()));
		}

		String location = "/" + Run.RESOURCE_URI_PREFIX + run.getName();
		String absolutePath = RequestUtil.constructAbsolutePath(location);

		getResponse().setStatus(Status.SUCCESS_CREATED);
		getResponse().setLocationRef(absolutePath);
	}

	private void setRunMutability(Run run, Form form) {
		String mutable = form.getFirstValue(MUTABLE_RUN_KEY, "");
		if (mutable.trim().equals("true")) {
			run.setMutable();
		}
	}

	private void authorizePost(Module module) {
		if(!module.getAuthz().canPost(getUser())) {
			throwClientForbiddenError("User does not have the rights to execute this module");
		}
	}

	private void setReference(Form form) {
		refqname = form.getFirstValue(REFQNAME);

		if (refqname == null) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Missing refqname in POST");
		}
		refqname = refqname.trim();
	}

	public static Map<String, List<Parameter<?>>> getUserChoicesFromForm(ModuleCategory category, Form form) throws ValidationException {

		Map<String, List<Parameter<?>>> parametersPerNode = new HashMap<String, List<Parameter<?>>>();

		for (Entry<String, String> entry : form.getValuesMap().entrySet()) {
			if (notUserChoiceForNode(entry)) {
				continue;
			}

			// parameter--node--[nodename]--[paramname]
			String[] parts = entry.getKey().split("--");
			String nodeName = "";
			String parameterName = "";

			if (category == ModuleCategory.Deployment) {
				if (parts.length != 4) {
					throw new ValidationException("Invalid key format: " + entry.getKey());
				}
				nodeName = parts[2];
				parameterName = parts[3];
			} else {
				if (parts.length != 2) {
					throw new ValidationException("Invalid key format: " + entry.getKey());
				}
				nodeName = Run.MACHINE_NAME;
				parameterName = parts[1];
			}

			String value = entry.getValue();
			if (category == ModuleCategory.Deployment) {
				if (!parametersPerNode.containsKey(nodeName)) {
					parametersPerNode.put(nodeName, new ArrayList<Parameter<?>>());
				}
				Parameter<?> parameter = new NodeParameter(parameterName);
				value = NodeParameter.isStringValue(value) ? value : "'" + value + "'";
				parameter.setValue(value);
				parametersPerNode.get(nodeName).add(parameter);
			} else {
				if (!parametersPerNode.containsKey(nodeName)) {
					parametersPerNode.put(nodeName, new ArrayList<Parameter<?>>());
				}
				Parameter<?> parameter = new ModuleParameter(parameterName);
				parameter.setValue(value);
				parametersPerNode.get(nodeName).add(parameter);
			}
		}

		return parametersPerNode;
	}

	private static boolean notUserChoiceForNode(Entry<String, String> entry) {
		List<String> keysToFilter = new ArrayList<String>();
		keysToFilter.add(RunListResource.REFQNAME);
		keysToFilter.add(RunListResource.MUTABLE_RUN_KEY);
		keysToFilter.add(RunListResource.TYPE);

		if (keysToFilter.contains(entry.getKey())) {
			return true;
		}
		return false;
	}

	private RunType parseType(Form form) {
		String type = form.getFirstValue(RunListResource.TYPE, true, RunType.Orchestration.toString());
		try {
			return RunType.valueOf(type);
		} catch (IllegalArgumentException e) {
			throw (new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Unknown run type: " + type));
		}
	}

	private Run launch(Run run) throws SlipStreamException {
		slipstream.async.Launcher.launch(run, getUser());
		return run;
	}

	private Run addCredentials(Run run) throws ConfigurationException,
			ServerExecutionEnginePluginException, ValidationException {

		Credentials credentials = loadCredentialsObject();
		run.setCredentials(credentials);

		return run;
	}

	private Credentials loadCredentialsObject() throws ConfigurationException,
			ValidationException {

		Connector connector = ConnectorFactory.getCurrentConnector(getUser());
		return connector.getCredentials(getUser());
	}

	private void createRepositoryResource(Run run)
			throws ConfigurationException {
		String repositoryLocation;
		repositoryLocation = ConfigurationUtil
				.getConfigurationFromRequest(getRequest())
				.getRequiredProperty(
						ServiceConfiguration.RequiredParameters.SLIPSTREAM_REPORTS_LOCATION
								.getName());

		String absRepositoryLocation = repositoryLocation + "/" + run.getName();

		boolean createdOk = new File(absRepositoryLocation).mkdirs();
		// Create the repository structure
		if (!createdOk) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Error creating repository structure");
		}
	}

	private Module loadReferenceModule() throws ValidationException {
		Module module = Module.load(refqname);
		if (module == null) {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
					"Coudn't find reference module: " + refqname);
		}
		return module;
	}

	private void updateReference(Module module) {
		refqname = module.getName();
	}

	@Override
	protected String getPageRepresentation() {
		return "runs";
	}

}
