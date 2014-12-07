package com.sixsq.slipstream.user;

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
import java.util.Map;
import java.util.Map.Entry;

import org.restlet.data.Cookie;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.exceptions.BadlyFormedElementException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.InvalidElementException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.ParametersFactory;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.User.State;
import com.sixsq.slipstream.persistence.UserParameter;
import com.sixsq.slipstream.resource.ParameterizedResource;
import com.sixsq.slipstream.util.FileUtil;
import com.sixsq.slipstream.util.ModuleUriUtil;
import com.sixsq.slipstream.util.SerializationUtil;
import com.sixsq.slipstream.util.XmlUtil;

/**
 * @see UserResourceTest
 */
public class UserResource extends ParameterizedResource<User> {

	public static final String USERNAME_URI_ATTRIBUTE = "user";
	private static final String resourceRoot = User.RESOURCE_URL_PREFIX;

	@Get("txt")
	@Override
	public Representation toTxt() {
		return super.toTxt();
	}

	@Get("json")
	public Representation toJson() {
		User user = (User) getParameterized();

		try {
			mergeCloudSystemParameters(user);
		} catch (ConfigurationException e) {
			throwConfigurationException(e);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

		return super.toJson();
	}

	@Get("xml")
	public Representation toXml() {
		User user = (User) getParameterized();

		try {
			mergeCloudSystemParameters(user);
			mergeCloudConnectorParameters(user);
		} catch (ConfigurationException e) {
			throwConfigurationException(e);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

		return super.toXml();
	}

	@Override
	protected boolean isMachineAllowedToAccessThisResource(){
		return true;
	}

	private void mergeCloudSystemParameters(User user)
			throws ConfigurationException, ValidationException {
		Cookie cookie = CookieUtils.extractAuthnCookie(getRequest());
		String cloudServiceName = CookieUtils.getCookieCloudServiceName(cookie);
		if (cloudServiceName != null) {
			for (Entry<String, Parameter<ServiceConfiguration>> p : Configuration
					.getInstance().getParameters()
					.getParameters(cloudServiceName).entrySet()) {
				user.getParameters().put(p.getKey(),
						UserParameter.convert(p.getValue()));
			}
		}
		mergePublicKeyParameter(user);
	}

	private void mergeCloudConnectorParameters(User user) throws ConfigurationException, ValidationException {
		Cookie cookie = CookieUtils.extractAuthnCookie(getRequest());
		String cloudServiceName = CookieUtils.getCookieCloudServiceName(cookie);
		if (cloudServiceName != null && CookieUtils.isMachine(cookie) == true) {
			Connector connector = ConnectorFactory.getConnector(cloudServiceName);
			connector.setExtraUserParameters(user);
		}
	}

	@Override
	protected User prepareForSerialization() throws ConfigurationException, ValidationException {
		User user = getParameterized();

		Cookie cookie = CookieUtils.extractAuthnCookie(getRequest());
		String cloudServiceName = CookieUtils.getCookieCloudServiceName(cookie);
		if (cloudServiceName != null && CookieUtils.isMachine(cookie) == true) {
			Map<String, Parameter<User>> params = user.getParameters(ParameterCategory.General.name());
			params.putAll(user.getParameters(cloudServiceName));

			Map<String, UserParameter> userParameters = user.getParameters();
			userParameters.clear();
			for (Map.Entry<String,Parameter<User>> entry : params.entrySet()) {
				userParameters.put(entry.getKey(), (UserParameter)entry.getValue());
			}
		}

		return user;
	}

	private void mergePublicKeyParameter(User user)
			throws ConfigurationException, ValidationException {
		String pubKeyParameterName = ServiceConfiguration.RequiredParameters.CLOUD_CONNECTOR_ORCHESTRATOR_PUBLICSSHKEY
				.getName();
		ServiceConfigurationParameter pubKeySystemParameter = Configuration
				.getInstance().getParameters()
				.getParameter(pubKeyParameterName);
		String pubKeyFilePath = pubKeySystemParameter.getValue();
		if (FileUtil.exist(pubKeyFilePath)) {
			String pubKey = FileUtil.fileToString(pubKeyFilePath);
			pubKeyParameterName = "General.orchestrator.publicsshkey";
			UserParameter pubKeyUserParameter = new UserParameter(
					pubKeyParameterName, pubKey, "");
			pubKeyUserParameter.setCategory("General");
			user.getParameters().put(pubKeyParameterName, pubKeyUserParameter);
		}
	}

	@Get("html")
	public Representation toHtml() {
		return super.toHtml();
	}

	protected String getPageRepresentation() {
		return "user";
	}

	@Override
	protected String extractTargetUriFromRequest() {
		return (String) getRequest().getAttributes().get(USERNAME_URI_ATTRIBUTE);
	}

	@Override
	protected User getOrCreateParameterized(String name) {
		User user = getParameterized();
		if (user == null) {
			try {
				user = new User(name);
			} catch (ValidationException e) {
				throw (new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						e.getMessage()));
			}
		}
		return user;
	}

	@Override
	protected void authorize() {
		boolean isMachine = isMachine();

		setCanPut(!newTemplateResource() && !isMachine
				&& (getUser().isSuper() || !isExisting() || (newInQuery() && !isExisting()) || isItSelf()));
		setCanDelete((getUser().isSuper() || isItSelf()) && !isMachine);
		setCanGet(getUser().isSuper() || newTemplateResource() || isItSelf());
	}

	protected boolean newInQuery() {
		return extractNewFlagFromQuery();
	}

	protected boolean newTemplateResource() {
		return isExisting()
				&& NEW_NAME.equals(ModuleUriUtil
						.extractShortNameFromResourceUri(getParameterized()
								.getName()));
	}

	private boolean isItSelf() {
		return isExisting()
				&& getUser().getResourceUri().equals(
						getTargetUser().getResourceUri());
	}

	private User getTargetUser() {
		return (User) getParameterized();
	}

	@Override
	protected void addParametersForEditing() throws ValidationException,
			ConfigurationException {

		ParametersFactory.addParametersForEditing(getParameterized());
	}

	@Put("form")
	public void modifyOrCreateFromForm(Representation entity)
			throws ResourceException {

		if (!canPut()) {
			throwClientForbiddenError();
		}

		if (!isExisting()) {
			setParameterized(getOrCreateParameterized(getTargetParameterizeUri()));
		}

		try {
			addParametersForEditing();
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		} catch (ConfigurationException e) {
			throwConfigurationException(e);
		}

		processEntityAsForm(entity);

		try {
			updateOrCreate(getParameterized());
		} catch (ValidationException e) {
			throwClientBadRequest(e.getMessage());
		}

		setEmptyEntity(MediaType.APPLICATION_WWW_FORM);
	}

	@Put("xml")
	public void updateOrCreateFromXml(Representation entity)
			throws ResourceException {

		User user = xmlToUser();

		try {
			updateOrCreate(user);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

		if (isExisting()) {
			getResponse().setStatus(Status.SUCCESS_ACCEPTED);
		} else {
			getResponse().setStatus(Status.SUCCESS_CREATED);
		}

		setEmptyEntity(MediaType.APPLICATION_XML);
	}

	@Put("json")
	public void updateOrCreateFromJson(Representation entity)
			throws ResourceException {

		User user = jsonToUser();

		try {
			updateOrCreate(user);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

		if (isExisting()) {
			getResponse().setStatus(Status.SUCCESS_ACCEPTED);
		} else {
			getResponse().setStatus(Status.SUCCESS_CREATED);
		}

		setEmptyEntity(MediaType.APPLICATION_JSON);
	}

	private User xmlToUser() {
		return xmlToUser(extractEntityAsText());
	}

	private User xmlToUser(String xml) {

		String denormalized = XmlUtil.denormalize(xml);

		User user = null;
		try {
			user = (User) SerializationUtil.fromXml(denormalized, User.class);
		} catch (SlipStreamClientException e) {
			throwClientBadRequest("Invalid xml user: " + e.getMessage());
		}

		user.postDeserialization();

		return user;
	}

	private User jsonToUser() {
		return jsonToUser(extractEntityAsText());
	}

	private User jsonToUser(String json) {

		User user = null;
		try {
			user = (User) SerializationUtil.fromJson(json, User.class);
		} catch (SlipStreamClientException e) {
			throwClientBadRequest(e.getMessage());
		}

		user.postDeserialization();

		return user;
	}

	private void updateOrCreate(User user) throws ValidationException {

		checkCanPut();

		try {
			user.validate();
		} catch (ValidationException ex) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT, ex.getMessage());
		}

		try {
			User.validateMinimumInfo(user);
		} catch (InvalidElementException ex) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT, ex.getMessage());
		}

		if (!getTargetParameterizeUri().equals(user.getName())
				&& !getUser().isSuper()) {
			throwClientBadRequest("The uploaded user does not correspond to the target user uri");
		}

		// super users forces new users to become active
		if (!isExisting() && getUser().isSuper()) {
			State current = user.getState();
			State newState = current == State.NEW ? State.ACTIVE : current;
			user.setState(newState);
		}

		user.store();

		if (!isExisting()) {
			setParameterized(user);
		}

		setResponseForPut();
	}

	private void setResponseForPut() {
		if (isExisting()) {
			setResponseOkAndViewLocation(getParameterized().getResourceUri());
		} else {
			setResponseCreatedAndViewLocation(getParameterized().getResourceUri());
		}
	}

	private void processEntityAsForm(Representation entity) {

		Form form = extractForm(entity);
		processForm(form);
	}

	private Form extractForm(Representation entity) {
		Form form = null;
		try {
			String text = entity.getText();
			form = new Form(text);
		} catch (IOException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"cannot extract text from entity");
		}
		return form;
	}

	private void processForm(Form form) {

		UserFormProcessor processor = new UserFormProcessor(getParameterized(),
				getUser());
		try {
			processor.processForm(form);
		} catch (BadlyFormedElementException e) {
			throwClientError(e);
		} catch (SlipStreamClientException e) {
			throwClientError(e);
		} catch (IllegalArgumentException e) {
			throwClientError(e);
		}

	}

	public static String getResourceRoot() {
		return resourceRoot;
	}

	@Override
	protected User loadParameterized(String targetParameterizedName)
			throws ConfigurationException, ValidationException {
		return User.loadByName(targetParameterizedName);
	}

}
