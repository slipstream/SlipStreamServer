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
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.sixsq.slipstream.event.Event;
import com.sixsq.slipstream.persistence.*;
import com.sixsq.slipstream.run.RunsQueryParameters;
import com.sixsq.slipstream.util.*;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.json.JSONObject;
import org.json.XML;
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
import com.sixsq.slipstream.resource.ParameterizedResource;
import com.sixsq.slipstream.run.RunViewList;

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

	private String toXmlString() {
		checkCanGet();

		Module prepared = null;
		try {
			prepared = prepareForSerialization();
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		} catch (ConfigurationException e) {
			Util.throwConfigurationException(e);
		}

		return XmlUtil.normalize(prepared);
	}

	@Get("xml")
	public Representation toXml() {
		String result = toXmlString();
		return new StringRepresentation(result, MediaType.APPLICATION_XML);
	}

	@Get("json")
	public Representation toJson() {
		String xml = toXmlString();
		JSONObject obj = XML.toJSONObject(xml);
		return new StringRepresentation(obj.toString(), MediaType.APPLICATION_JSON);
	}

	@Post("form")
	public void copyTo(Representation entity) throws ValidationException {
		Form form = new Form(entity);

		if (!isExisting()) {
			throwClientForbiddenError("Target project doesn't exist: " + getParameterized().getName());
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
			throwClientForbiddenError("You do not have read rights on the source module: " + source.getName());
		}

		String targetFullName = getParameterized().getName() + "/" + targetName;
		String targetUri = Module.constructResourceUri(targetFullName);

		Module target = Module.load(targetUri);
		if (target != null) {
			throwClientForbiddenError("Target module already exists: " + targetUri);
		}

		if (!getParameterized().getAuthz().canCreateChildren(getUser())) {
			throwClientForbiddenError("You do not have rights to create modules in this project");
		}

		try {
			target = source.copy();
			target.getAuthz().setUser(getUser().getName());
			target.getAuthz().clear();
			target.setName(targetFullName);
			target.store();
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

		postEventCopied(targetFullName);

		String absolutePath = RequestUtil.constructAbsolutePath(getRequest(), "/" + target.getResourceUri());
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

		Module latest = Module.loadLatest(getParameterized().getResourceUri());
		try {
			if (latest == null) {
				redirectToParent();
			} else {
				redirectToLatest(latest);
			}
		} catch (ValidationException e) {
			throwClientConflicError(e.getMessage());
		}

		postEventDeleted();
	}

	private void redirectToParent() throws ValidationException {
		String resourceUri = getParameterized().getResourceUri();
		String parentResourceUri = ModuleUriUtil.extractParentUriFromResourceUri(resourceUri);

		String absolutePath = RequestUtil.constructAbsolutePath(getRequest(), "/" + parentResourceUri);
		getResponse().setLocationRef(absolutePath);
		getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
	}

	private void redirectToLatest(Module latest) {
		String absolutePath = RequestUtil.constructAbsolutePath(getRequest(), "/" + latest.getResourceUri());
		getResponse().setLocationRef(absolutePath);
		getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
	}

	@Put("form")
	public void updateOrCreateFromForm(Representation entity) throws ResourceException {

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

		String absolutePath = RequestUtil.constructAbsolutePath(getRequest(), "/" + module.getResourceUri());
		getResponse().setLocationRef(absolutePath);

		if (!isExisting()) {
			getResponse().setStatus(Status.SUCCESS_CREATED);
		}

		setEmptyEntity(MediaType.APPLICATION_WWW_FORM);
	}

	@Put("multipart")
	public void updateOrCreateFromXmlMultipart(Representation entity) throws ResourceException {

		Module module = xmlMultipartToModule();

		updateOrCreate(module);

		String absolutePath = RequestUtil.constructAbsolutePath(getRequest(), "/" + module.getResourceUri());
		getResponse().setLocationRef(absolutePath);

		if (!isExisting()) {
			getResponse().setStatus(Status.SUCCESS_CREATED);
		}
		setEmptyEntity(MediaType.MULTIPART_ALL);
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

		if (!isExisting()) {
			getResponse().setStatus(Status.SUCCESS_CREATED);
		}
		getResponse().setLocationRef("/" + module.getResourceUri());
		setEmptyEntity(MediaType.APPLICATION_XML);
	}

	private Module xmlToModule() {

		Module module = xmlToModule(extractXml());

		// Reset user
		module.getAuthz().setUser(getUser().getName());

		return module;
	}

	public static Module xmlToModule(String xml) {

		String denormalized = XmlUtil.denormalize(xml);

		Class<? extends Module> moduleClass = getModuleClass(denormalized);

		Module module = null;
		try {
			module = (Module) SerializationUtil.fromXml(denormalized, moduleClass);
		} catch (SlipStreamClientException e) {
			e.printStackTrace();
			throwClientBadRequest("Invalid xml module: " + e.getMessage());
		}

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

		module.unpublish();

		module.store();

		if (isExisting()) {
			postEventUpdated();
		} else {
			postEventCreated();
		}
	}

	private void checkConsistentModule(String moduleUri, String targetUri)
			throws ValidationException {
		if (!targetUri.equals(moduleUri)) {
			throwClientBadRequest("The uploaded module does not correspond to the target module uri");
		}
	}

	@SuppressWarnings("unchecked")
	private static Class<? extends Module> getModuleClass(String moduleAsXml) {

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

	protected static String extractCategory(String moduleAsXml)
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

		return loadModule(targetParameterizedUri);
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

                // This "noop" disrupts some optimization or bug in the
                // OpenJDK JVM.  If this isn't present, then unit tests
                // intermittently fail with complaints that the request
                // doesn't contain a form entity.  Note that this doesn't
                // happen with the Oracle JVM.
                //
                // If we were sticking with Java in the longterm, this
                // would probably warrant more investigation, but the
                // kludge is probably sufficient for now.
                entity.toString();

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

		if (newInQuery() && !isExisting()) {
			processor.adjustModule(previous);
		}

		Module module = processor.getParametrized();

		module = copyAllParameters(module);

		category = module.getCategory();

		module = resetMandatoryParameters(module);

		return module;
	}

	private Module copyAllParameters(Module module) throws ValidationException
	{
		for (ModuleParameter p : module.getParameterList()) {
			module.setParameter(p.copy());
		}

		return module;
	}

	private Module resetMandatoryParameters(Module module)
			throws ValidationException {
		for (ModuleParameter referenceParameter : getOrCreateParameterized(
				"reference").getParameterList()) {
			ModuleParameter p = module.getParameter(referenceParameter
					.getName());

			if (p != null) {
				referenceParameter.setValue(p.getValue());
			}
			module.setParameter(referenceParameter);
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
		return (v == null) ? Module.DEFAULT_VERSION : Integer.parseInt(v);
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

		if (getParameterized() != null && getParameterized().isDeleted()
				&& !getUser().isSuper()) {
			throwClientForbiddenError("Module deleted");
		}
	}

	private boolean authorizeGet() {
		if (getUser().isSuper() || newTemplateResource()) {
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

		if (newTemplateResource()) {
			return false;
		}

		if (getUser().isSuper()) {
			return true;
		}

		if (newInQuery() && !isExisting()) {
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

		return isExisting() ? Module
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
		return c != null;
	}

	@Override
	protected void addParametersForEditing() throws ValidationException, ConfigurationException {
		ParametersFactory.addParametersForEditing(getParameterized());
	}

	@Override
	protected Module prepareForSerialization() throws ConfigurationException, ValidationException {
		Module module = getParameterized();
		// Add runs for this specific module version (will not apply to project)
		RunViewList runs = new RunViewList();
		RunsQueryParameters parameters = new RunsQueryParameters(getUser(), 0, Run.DEFAULT_LIMIT, null, null, null,
				module.getResourceUri(), getActiveOnly());
		runs.populate(parameters);
		module.setRuns(runs);
		return module;
	}

	private void postEventCreated() {
		postEventModule(Event.Severity.medium, "Created by '" + getUser().getName() + "'");
	}

	private void postEventUpdated() {
		postEventModule(Event.Severity.medium, "Updated by '" + getUser().getName() + "'");
	}

	private void postEventCopied(String target) {
		postEventModule(Event.Severity.medium, "Copied to '" + target + "' by '" + getUser().getName() + "'");
	}

	private void postEventDeleted() {
		postEventModule(Event.Severity.high, "Deleted by '" + getUser().getName() + "'");
	}

	private void postEventModule(Event.Severity severity, String message) {
		String resourceRef = null;
		try {
			resourceRef = ModuleUriUtil.extractVersionLessResourceUri(getTargetParameterizeUri());
		} catch (ValidationException e) {
			getLogger().log(Level.WARNING, "Failed to generate event for '" + getTargetParameterizeUri() + "'", e);
		}

		Event.postEvent(resourceRef, severity, message, getUser().getName(), Event.EventType.action);
	}

}
