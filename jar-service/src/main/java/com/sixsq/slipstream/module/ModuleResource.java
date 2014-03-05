package com.sixsq.slipstream.module;

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
import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.exceptions.BadlyFormedElementException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.Util;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.ParametersFactory;
import com.sixsq.slipstream.factory.RunFactory;
import com.sixsq.slipstream.persistence.Authz;
import com.sixsq.slipstream.persistence.CloudImageIdentifier;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Metadata;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.ProjectModule;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.resource.ParameterizedResource;
import com.sixsq.slipstream.run.RunView.RunViewList;
import com.sixsq.slipstream.util.ModuleUriUtil;
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.SerializationUtil;
import com.sixsq.slipstream.util.XmlUtil;

/**
 * Unit test see
 * 
 * @see ModuleResourceTest
 * 
 */
public class ModuleResource extends ParameterizedResource<Module> {

	private ModuleCategory category = null;
	public static final String COPY_SOURCE_FORM_PARAMETER_NAME = "source_uri";
	public static final String COPY_TARGET_FORM_PARAMETER_NAME = "target_name";

	@Override
	protected String getPageRepresentation() {
		return "module";
	}

	@Get("xml")
	public Representation toXml() {
		checkCanGet();

		Module prepared = null;
		try {
			prepared = prepareForSerialization();
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		} catch (ConfigurationException e) {
			Util.throwConfigurationException(e);
		}

		String result = XmlUtil.normalize(prepared);
		return new StringRepresentation(result, MediaType.APPLICATION_XML);
	}

	@Post("form")
	public void copyTo(Representation entity) throws ValidationException {
		Form form = new Form(entity);

		if (!isExisting()) {
			throwClientForbiddenError("Target project doesn't exist: "
					+ getParameterized().getName());
		}

		String sourceUri = form.getFirstValue(COPY_SOURCE_FORM_PARAMETER_NAME);
		if (sourceUri == null) {
			throwClientBadRequest("Missing source uri form parameter");
		}

		String targetName = form.getFirstValue(COPY_TARGET_FORM_PARAMETER_NAME);
		if (targetName == null) {
			throwClientBadRequest("Missing target name form parameter");
		}

		Module source = Module.load(sourceUri);
		if (source == null) {
			throwClientBadRequest("Unknown source module: " + sourceUri);
		}

		if (!source.getAuthz().canGet(getUser())) {
			throwClientForbiddenError("You do not have read rights on the source module: "
					+ source.getName());
		}

		String targetFullName = getParameterized().getName() + "/" + targetName;
		String targetUri = Module.constructResourceUri(targetFullName);

		Module target = Module.load(targetUri);
		if (target != null) {
			throwClientForbiddenError("Target module already exists: "
					+ targetUri);
		}

		if (!getParameterized().getAuthz().canCreateChildren(getUser())) {
			throwClientForbiddenError("You do not have rights to create modules in this project");
		}

		target = source.copy();
		target.getAuthz().setUser(getUser().getName());
		target.getAuthz().clear();
		target.setName(targetFullName);
		target.store();

		String absolutePath = RequestUtil.constructAbsolutePath("/" + target.getResourceUri());
		getResponse().setLocationRef(absolutePath);
		getResponse().setStatus(Status.SUCCESS_CREATED);
	}

	@Delete
	public void deleteModule() {

		if (!canDelete()) {
			throwClientForbiddenError();
		}

		getParameterized().setDeleted(true);
		getParameterized().store(false);

		Module latest = null;
		latest = Module.loadLatest(getParameterized().getResourceUri());
		try {
			if (latest == null) {
				redirectToParent();
			} else {
				redirectToLatest(latest);
			}
		} catch (ValidationException e) {
			throwClientConflicError(e.getMessage());
		}
	}

	private void redirectToParent() throws ValidationException {
		String resourceUri = getParameterized().getResourceUri();
		String parentResourceUri = ModuleUriUtil
				.extractParentUriFromResourceUri(resourceUri);

		String absolutePath = RequestUtil.constructAbsolutePath("/" + parentResourceUri);
		getResponse().setLocationRef(absolutePath);
		getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
	}

	private void redirectToLatest(Module latest) {
		String absolutePath = RequestUtil.constructAbsolutePath("/" + latest.getResourceUri());
		getResponse().setLocationRef(absolutePath);
		getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
	}

	@Put("form")
	public void updateOrCreateFromForm(Representation entity)
			throws ResourceException {

		if (entity == null) {
			throwClientBadRequest("Empty form");
		}

		Module module = null;
		try {
			module = (Module) processEntityAsForm(entity);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

		updateOrCreate(module);

		String absolutePath = RequestUtil.constructAbsolutePath("/" + module.getResourceUri());
		getResponse().setLocationRef(absolutePath);

		if (isNew()) {
			getResponse().setStatus(Status.SUCCESS_CREATED);
		}
	}

	@Put("multipart")
	public void updateOrCreateFromXmlMultipart(Representation entity)
			throws ResourceException {

		Module module = xmlMultipartToModule();

		updateOrCreate(module);

		String absolutePath = RequestUtil.constructAbsolutePath("/" + module.getResourceUri());
		getResponse().setLocationRef(absolutePath);

		if (isNew()) {
			getResponse().setStatus(Status.SUCCESS_CREATED);
		}
	}

	private Module xmlMultipartToModule() {
		return xmlToModule(extractXmlMultipart());
	}

	private String extractXmlMultipart() {

		RestletFileUpload upload = new RestletFileUpload(
				new DiskFileItemFactory());

		List<FileItem> items;

		Request request = getRequest();
		try {
			items = upload.parseRequest(request);
		} catch (FileUploadException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					e.getMessage());
		}

		String module = null;
		for (FileItem fi : items) {
			if (fi.getName() != null) {
				module = getContent(fi);
			}
		}
		if (module == null) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"the file is empty");
		}

		return module;
	}

	@Put("xml")
	public void updateOrCreateFromXml(Representation entity)
			throws ResourceException {

		Module module = xmlToModule();

		updateOrCreate(module);

		if (isExisting()) {
			getResponse().setStatus(Status.SUCCESS_ACCEPTED);
		} else {
			getResponse().setStatus(Status.SUCCESS_CREATED);
		}
	}

	private Module xmlToModule() {
		return xmlToModule(extractXml());
	}

	private Module xmlToModule(String xml) {

		String denormalized = XmlUtil.denormalize(xml);

		Class<? extends Module> moduleClass = getModuleClass(denormalized);

		Module module = null;
		try {
			module = (Module) SerializationUtil.fromXml(denormalized,
					moduleClass);
		} catch (SlipStreamClientException e) {
			e.printStackTrace();
			throwClientBadRequest("Invalid xml module: " + e.getMessage());
		}

		// Reset user
		module.getAuthz().setUser(getUser().getName());

		module.postDeserialization();

		return module;
	}

	private String extractXml() {
		return getRequest().getEntityAsText();
	}

	private void updateOrCreate(Module module) {

		checkCanPut();

		try {
			module.validate();
		} catch (ValidationException ex) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT, ex);
		}

		String moduleUri = null;
		String targetUri = null;
		try {
			moduleUri = ModuleUriUtil.extractVersionLessResourceUri(module
					.getResourceUri());
			targetUri = ModuleUriUtil
					.extractVersionLessResourceUri(getTargetParameterizeUri());
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

		try {
			checkConsistentModule(moduleUri, targetUri);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

		module.store();

	}

	private void checkConsistentModule(String moduleUri, String targetUri)
			throws ValidationException {
		if (!targetUri.equals(moduleUri)) {
			throwClientBadRequest("The uploaded module does not correspond to the target module uri");
		}

		// Check that the new proposed module doesn't already exists.
		// We need to do this here since the standard AA process runs before
		// the module name is extracted from the request.
		if (isNew()) {
			if (loadModule(moduleUri) != null) {
				throwClientForbiddenError("Cannot create this resource. Does it already exist?");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Class<? extends Module> getModuleClass(String moduleAsXml) {

		String category = null;
		try {
			category = extractCategory(moduleAsXml);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			throwServerError("Failed to parse module");
		} catch (SAXException e) {
			e.printStackTrace();
			throwClientBadRequest("Invalid xml document");
		} catch (IOException e) {
			e.printStackTrace();
			throwServerError("Failed to parse module");
		}

		String className = "com.sixsq.slipstream.persistence." + category
				+ "Module";
		Class<? extends Module> moduleClass = null;
		try {
			moduleClass = (Class<? extends Module>) Class.forName(className);
		} catch (ClassNotFoundException e) {
			throwClientBadRequest("Unknown category");
		}
		return moduleClass;
	}

	protected String extractCategory(String moduleAsXml)
			throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		db = dbf.newDocumentBuilder();
		StringReader reader = new StringReader(moduleAsXml);
		Document document = db.parse(new InputSource(reader));

		return document.getDocumentElement().getAttributes()
				.getNamedItem("category").getNodeValue();
	}

	protected String getContent(FileItem fi) {
		return fi.getString();
	}

	@Override
	protected Module loadParameterized(String targetParameterizedUri)
			throws ValidationException {

		Module module = loadModule(targetParameterizedUri);

		resolveImageIdIfAppropriate(module);

		return module;

	}

	private void resolveImageIdIfAppropriate(Module module)
			throws ConfigurationException, ValidationException {
		try {
			RunFactory.resolveImageIdIfAppropriate(module, getUser());
		} catch (ValidationException ex) {
			// ok, the user might not be fully configured
		}
	}

	public Module loadModule(String targetParameterizedUri)
			throws ValidationException {
		Module module = Module.load(targetParameterizedUri);
		if (module != null) {
			if (module.getCategory() == ModuleCategory.Project) {
				List<ModuleView> children = Module.viewList(module
						.getResourceUri());
				((ProjectModule) module).setChildren(children);
			}
		}
		return module;
	}

	@Override
	protected void setIsEdit() throws ConfigurationException,
			ValidationException {
		super.setIsEdit();

		if (isExisting()) {
			// Add connector information for the transformation
			List<String> serviceCloudNames = ConnectorFactory
					.getCloudServiceNamesList();
			serviceCloudNames.add(CloudImageIdentifier.DEFAULT_CLOUD_SERVICE);
			getParameterized().setCloudNames(
					serviceCloudNames.toArray(new String[0]));
		}
	}

	private Metadata processEntityAsForm(Representation entity)
			throws ValidationException {

		// Add the default module parameters if the module is not new,
		// to ensure that all mandatory parameters are present.
		// This is required to avoid inconsistent modules, for example
		// when connectors are added in the configuration
		Module previous = getParameterized();
		if (previous != null) {
			try {
				ParametersFactory.addParametersForEditing(previous);
			} catch (ValidationException e) {
				throwClientConflicError(e.getMessage());
			} catch (ConfigurationException e) {
				throwConfigurationException(e);
			}
		}

		Form form = extractFormFromEntity(entity);
		ModuleFormProcessor processor = ModuleFormProcessor
				.createFormProcessorInstance(getCategory(form), getUser());

		try {
			processor.processForm(form);
		} catch (BadlyFormedElementException e) {
			throwClientError(e);
		} catch (SlipStreamClientException e) {
			throwClientError(e);
		}

		if (!isNew()) {
			processor.adjustModule(previous);
		}

		Module module = processor.getParametrized();

		category = module.getCategory();

		module = resetMandatoryParameters(module);

		return module;
	}

	private Module resetMandatoryParameters(Module module)
			throws ValidationException {
		for (ModuleParameter referenceParameter : getOrCreateParameterized(
				"reference").getParameterList()) {
			ModuleParameter p = module.getParameter(referenceParameter
					.getName());
			if (p == null) {
				throw (new ValidationException("Missing mandatory parameter: "
						+ referenceParameter.getName()));
			}
			p.setCategory(referenceParameter.getCategory());
			p.setDescription(referenceParameter.getDescription());
			p.setEnumValues(referenceParameter.getEnumValues());
			p.setInstructions(referenceParameter.getInstructions());
			p.setMandatory(referenceParameter.isMandatory());
			p.setReadonly(referenceParameter.isReadonly());
			p.setType(referenceParameter.getType());
		}
		return module;
	}

	@Override
	protected String extractTargetUriFromRequest() {

		String module = (String) getRequest().getAttributes().get("module");

		int version = extractVersion();
		String moduleName = (version == Module.DEFAULT_VERSION ? module
				: module + "/" + version);
		return Module.constructResourceUri(moduleName);
	}

	private int extractVersion() {
		String v = (String) getRequest().getAttributes().get("version");
		return (v == null) ? -1 : Integer.parseInt(v);
	}

	@Override
	protected void authorize() {

		setCanPut(authorizePut());

		if (isExisting()) {
			setCanDelete(authorizeDelete());
		}

		if (isExisting()) {
			setCanGet(authorizeGet());
		}

		if (getParameterized() != null && getParameterized().isDeleted() && !getUser().isSuper()) {
			throwClientForbiddenError("Module deleted");
		}
	}

	private boolean authorizeGet() {
		if (getUser().isSuper() || isNew()) {
			return true;
		}
		return getParameterized().getAuthz().canGet(getUser());
	}

	private boolean authorizeDelete() {
		if (getUser().isSuper()) {
			return true;
		}
		return getParameterized().getAuthz().canDelete(getUser());
	}

	protected boolean authorizePut() {

		if (getUser().isSuper()) {
			return true;
		}
		if (isNew()) {
			// check parent
			String parentResourceUri = null;
			try {
				parentResourceUri = ModuleUriUtil
						.extractParentUriFromResourceUri(getTargetParameterizeUri());
			} catch (ValidationException e) {
				return false;
			}
			Module parent = Module.load(parentResourceUri);
			if (parent == null) {
				// this is the root module. All can put on it (for now)
				return true;
			} else {
				return parent.getAuthz().canCreateChildren(getUser());
			}
		}
		boolean isExisting = isExisting(); // also true for isNew

		return isExisting ? Module
				.loadLatest(getParameterized().getResourceUri()).getAuthz()
				.canPut(getUser()) : true;
	}

	@Override
	protected Module getOrCreateParameterized(String name)
			throws ValidationException {
		Module module = null;
		switch (getCategory()) {
		case Project:
			module = new ProjectModule(name);
			break;
		case Image:
			module = new ImageModule(name);
			break;
		case Deployment:
			module = new DeploymentModule(name);
			break;
		default:
			throwClientError("Unknown category");
		}
		module.setAuthz(new Authz(getUser().getName(), module));
		ParametersFactory.addParametersForEditing(module);
		return module;
	}

	private ModuleCategory getCategory(Form form) {
		return getCategory(form.getFirstValue("category"));
	}

	private ModuleCategory getCategory() {
		if (category == null) {
			String c = (String) getRequest().getAttributes().get("category");
			category = (c == null) ? ModuleCategory.Project : getCategory(c);
		}
		return category;
	}

	private ModuleCategory getCategory(String category) {
		ModuleCategory c = null;
		try {
			c = ModuleCategory.valueOf(category);
		} catch (NullPointerException e) {
			throwClientError("Missing category attribute");
		} catch (IllegalArgumentException e) {
			throwClientError("Invalid category attribute, got: " + category);
		}
		return c;
	}

	@Override
	protected boolean isChooser() {
		String c = (String) getRequest().getAttributes().get("chooser");
		return (c == null) ? false : true;
	}

	@Override
	protected void addParametersForEditing() throws ValidationException,
			ConfigurationException {

		ParametersFactory.addParametersForEditing(getParameterized());
	}

	@Override
	protected Module prepareForSerialization() throws ConfigurationException,
			ValidationException {
		Module module = getParameterized();
		// Add runs for this specific module version (will not apply to project)
		RunViewList runs = new RunViewList(Run.viewList(
				module.getResourceUri(), getUser()));
		module.setRuns(runs);
		return module;
	}

}
