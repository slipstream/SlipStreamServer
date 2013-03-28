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

import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.sixsq.slipstream.authz.SuperEnroler;
import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.connector.Credentials;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ServerExecutionEnginePluginException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.CloudImageIdentifier;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.persistence.Node;
import com.sixsq.slipstream.persistence.NodeParameter;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.run.RunView.RunViewList;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.SerializationUtil;

/**
 * Unit test:
 * 
 * @see RunListResourceTest.class
 * 
 */
public class RunListResource extends ServerResource {

	public static final String REFQNAME = "refqname";

	Configuration configuration;

	User user = null;

	boolean isEdit = false;

	String query = null;

	String refqname = null;

	boolean isEmbedded = false;

	static {
		ArrayList<String> names = new ArrayList<String>();
		names.add(RuntimeParameter.GLOBAL_NAMESPACE);
		names.add(Run.MACHINE_NAME);
		names.trimToSize();
	}

	@Override
	public void doInit() throws ResourceException {

		Request request = getRequest();

		configuration = RequestUtil.getConfigurationFromRequest(request);

		user = User.loadByName(request.getClientInfo().getUser().getName());

		extractParametersFromQuery();
	}

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
		return new StringRepresentation(result);
	}

	@Get("html")
	public Representation toHtml() {

		Request request = getRequest();
		String baseUrlSlash = RequestUtil.getBaseUrlSlash(request);
		RunViewList runViewList = fetchListView();

		Map<String, Object> parameters = new HashMap<String, Object>();

		if (isEmbedded) {
			parameters.put("isembedded", isEmbedded);
		}

		return HtmlUtil.transformToHtml(baseUrlSlash, "run",
				configuration.version, "run-list.xsl", user, runViewList,
				parameters);
	}

	private RunViewList fetchListView() {
		return fetchListView(query, user, isSuperRole());
	}

	static RunViewList fetchListView(String query, User user, boolean isSuper) {
		List<RunView> list;

		if (isSuper) {
			list = (query != null) ? Run.viewList(query, user) : Run
					.viewListAll(user);
		} else {
			list = (query != null) ? Run.viewList(query, user) : Run
					.viewList(user);
		}
		return new RunViewList(list);
	}

	private boolean isSuperRole() {
		return getClientInfo().getRoles().contains(SuperEnroler.SUPER);
	}

	private void extractParametersFromQuery() {
		Reference resourceRef = getRequest().getResourceRef();
		Form form = resourceRef.getQueryAsForm();

		String flag = form.getFirstValue("edit");
		isEdit = ("true".equalsIgnoreCase(flag) || "yes".equalsIgnoreCase(flag));

		query = form.getFirstValue("query");

		String embedded = form.getFirstValue("isembedded");
		isEmbedded = ("true".equalsIgnoreCase(embedded) || "yes"
				.equalsIgnoreCase(embedded));

	}

	@Post("form|txt")
	public void createRun(Representation entity) throws ResourceException,
			FileNotFoundException, IOException, SQLException,
			ClassNotFoundException, SlipStreamException {

		Form form = new Form(entity);
		setReference(form);

		Run run;
		try {
			Module module = loadReferenceModule();

			updateReference(module);

			overrideModule(form, module);

			module.validate();

			run = RunFactory.getRun(module, parseType(form),
					user.getDefaultCloudService(), user);

			run = addCredentials(run);

			createRepositoryResource(run);

			run = launch(run);

		} catch (SlipStreamClientException ex) {
			throw (new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					ex.getMessage()));
		}

		createAndSetPostResponseEntity(run);

		String location = Run.RESOURCE_URI_PREFIX + run.getName();

		getResponse().setStatus(Status.SUCCESS_CREATED);
		getResponse().setLocationRef(location);
	}

	private void createAndSetPostResponseEntity(Run run) {
		RunView view = new RunView(run.getRefqname(), run.getName(),
				run.getModuleResourceUrl(), run.getStatus(), run.getStart());
		view.vmstate = Run.INITIAL_NODE_STATE;

		String result = SerializationUtil.toXmlString(view);
		getResponse().setEntity(
				new StringRepresentation(result, MediaType.APPLICATION_XML));
	}

	private String getDefaultCloudService() {
		String cloudServiceName = user.getDefaultCloudService();
		if (cloudServiceName == null || "".equals(cloudServiceName)) {
			throw (new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					"No cloud selected, please edit your account"));
		}
		return cloudServiceName;
	}

	private void setReference(Form form) {
		refqname = form.getFirstValue(REFQNAME);

		if (refqname == null) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Missing refqname in POST");
		}
		refqname = refqname.trim();
	}

	private void overrideModule(Form form, Module module)
			throws ValidationException {
		if (module.getCategory() == ModuleCategory.Deployment) {
			overrideNodes(form, (DeploymentModule) module);
		}
		if (module.getCategory() == ModuleCategory.Image) {
			overrideImage(form, (ImageModule) module);
		}
	}

	private void overrideNodes(Form form, DeploymentModule deployment)
			throws ValidationException {

		Map<String, List<NodeParameter>> parametersPerNode = NodeParameter
				.parseNodeNameOverride(form);
		
		String defaultCloudService = getDefaultCloudService();
		
		for(Node node : deployment.getNodes().values()) {
			if (CloudImageIdentifier.DEFAULT_CLOUD_SERVICE.equals(node.getCloudService()))
				node.setCloudService(defaultCloudService);
		}

		for (String nodename : parametersPerNode.keySet()) {
			if (!deployment.getNodes().containsKey(nodename)) {
				throw new ValidationException("Unknown node: " + nodename);
			}

			Node node = deployment.getNodes().get(nodename);

			for (NodeParameter parameter : parametersPerNode.get(nodename)) {
				if (parameter.getName().equals(
						RuntimeParameter.MULTIPLICITY_PARAMETER_NAME)) {
					node.setMultiplicity(parameter.getValue());
					continue;
				}
				if (parameter.getName().equals(
						RuntimeParameter.CLOUD_SERVICE_NAME)) {
					String cloudService = (CloudImageIdentifier.DEFAULT_CLOUD_SERVICE.equals(parameter.getValue()) ? defaultCloudService
							: parameter.getValue());
					node.setCloudService(cloudService);
					continue;
				}
				if (!node.getParameters().containsKey(parameter.getName())) {
					throw new ValidationException("Unknown parameter: "
							+ parameter.getName() + " in node: " + nodename);
				}
				node.getParameters().get(parameter.getName())
						.setValue("'" + parameter.getValue() + "'");
			}

		}
	}

	private void overrideImage(Form form, ImageModule image)
			throws ValidationException {
		// TODO...
		// Map<String, List<NodeParameter>> parametersPerNode = NodeParameter
		// .parseNodeNameOverride(form);
		// for (NodeParameter parameter : parametersPerNode.get(nodename)) {
		// if (parameter.getName().equals(
		// RuntimeParameter.CLOUD_SERVICE_NAME)) {
		// node.setCloudService(parameter.getValue());
		// continue;
		// }
	}

	private RunType parseType(Form form) {
		String type = form.getFirstValue("type", true,
				RunType.Orchestration.toString());
		try {
			return RunType.valueOf(type);
		} catch (IllegalArgumentException e) {
			throw (new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unknown run type: " + type));
		}
	}

	private Run launch(Run run) throws SlipStreamException {
		Launcher launcher = new Launcher();
		return launcher.launch(run, user);
	}

	private Run addCredentials(Run run) throws ConfigurationException,
			ServerExecutionEnginePluginException, ValidationException {

		Credentials credentials = loadCredentialsObject();
		run.setCredentials(credentials);

		return run;
	}

	private Credentials loadCredentialsObject() throws ConfigurationException,
			ValidationException {

		Connector connector = ConnectorFactory.getCurrentConnector(user);
		return connector.getCredentials(user);
	}

	private void createRepositoryResource(Run run)
			throws ConfigurationException {
		String repositoryLocation;
		repositoryLocation = configuration
				.getRequiredProperty(ServiceConfiguration.RequiredParameters.SLIPSTREAM_REPORTS_LOCATION
						.getValue());

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

}
