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

import com.sixsq.slipstream.event.Event;
import org.restlet.data.Cookie;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.*;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorBase;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.connector.ExecutionControlUserParametersFactory;
import com.sixsq.slipstream.connector.UserParametersFactoryBase;
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
import com.sixsq.slipstream.util.FileUtil;
import com.sixsq.slipstream.util.ModuleUriUtil;
import com.sixsq.slipstream.util.SerializationUtil;
import com.sixsq.slipstream.util.XmlUtil;

/**
 * @see UserResourceTest
 */
public class UserResource extends UserParameterizedResource {

	public static final String USERNAME_URI_ATTRIBUTE = "user";
	private static final String resourceRoot = User.RESOURCE_URL_PREFIX;

	@Get("txt")
	@Override
	public Representation toTxt() {
		return super.toTxt();
	}

	@Get("xml")
	public Representation toXml() {
		User user = (User) getParameterized();

		try {
			mergeCloudSystemParameters(user);
			mergeCloudConnectorParameters(user);
			replaceUserPublicSshKeyWithServerSshPublicKeyIfEmpty(user);
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

	private void replaceUserPublicSshKeyWithServerSshPublicKeyIfEmpty(User user)
			throws ConfigurationException, ValidationException {

		Cookie cookie = CookieUtils.extractAuthnCookie(getRequest());

		if (CookieUtils.isMachine(cookie) == true) {
			UserParameter userPublicSshKey = user.getParameter(ExecutionControlUserParametersFactory.CATEGORY + "."
					+ UserParametersFactoryBase.SSHKEY_PARAMETER_NAME);

			if (userPublicSshKey != null && userPublicSshKey.getValue("").trim().isEmpty()) {
				String serverPublicSshKey = FileUtil.fileToString(ConnectorBase.getServerPublicSshKeyFilename());
				userPublicSshKey.setValue(serverPublicSshKey);
				user.getParameters().put(userPublicSshKey.getName(), userPublicSshKey);
			}
		}
	}

	private void mergeCloudSystemParameters(User user) throws ConfigurationException, ValidationException {
		Cookie cookie = CookieUtils.extractAuthnCookie(getRequest());
		String cloudServiceName = CookieUtils.getCookieCloudServiceName(cookie);
		if (cloudServiceName != null) {
			for (Entry<String, Parameter<ServiceConfiguration>> p : Configuration.getInstance().getParameters()
					.getParameters(cloudServiceName).entrySet()) {
				user.getParameters().put(p.getKey(), UserParameter.convert(p.getValue()));
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
			Map<String, UserParameter> params = user.getParameters(ParameterCategory.General.name());
			params.putAll(user.getParameters(cloudServiceName));

			Map<String, UserParameter> userParameters = user.getParameters();
			userParameters.clear();
			for (Map.Entry<String, UserParameter> entry : params.entrySet()) {
				userParameters.put(entry.getKey(), entry.getValue());
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
		boolean notMachine = !isMachine();
		String targetUserOrganization = null;
		try {
			targetUserOrganization = getTargetUser().getOrganization();
		} catch (Exception e) {}
		String organizationManagedByUser = getUser().getOrganizationManagedForUserCreator();
		boolean isOrganizationManagedByUser = false;
		if (isExisting() && targetUserOrganization != null && organizationManagedByUser != null) {
			isOrganizationManagedByUser = targetUserOrganization.equals(organizationManagedByUser);
		}

		setCanPut(!newTemplateResource()
				  && notMachine
				  && (getUser().isSuper()
				      || !isExisting()
				      || (newInQuery() && !isExisting())
				      || isItSelf()
				      || isOrganizationManagedByUser));

		setCanDelete((getUser().isSuper()
				        || isItSelf()
				        || isOrganizationManagedByUser)
				      && notMachine);

		setCanGet(getUser().isSuper() || newTemplateResource() || isItSelf() || isOrganizationManagedByUser);
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
	public void updateOrCreateFromForm(Representation entity)
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
			ParametersFactory.addParametersForEditing(user);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		} catch (ConfigurationException e) {
			throwConfigurationException(e);
		}

		if (!getUser().isSuper()) {
			if (user.isSuper()) {
				throwClientForbiddenError("Only super users are authorized to create a privileged user!");
			}
			if (user.getRoles() != null) {
				throwClientForbiddenError("Only super users are authorized to update roles!");
			}
		}

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

	private User xmlToUser() {
		return xmlToUser(extractXml());
	}

	public static User xmlToUser(String xml) {

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

	private String extractXml() {
		return getRequest().getEntityAsText();
	}

	private void updateOrCreate(User user) throws ValidationException {

		checkCanPut();

		try {
			user.validate();
		} catch (ValidationException ex) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT, ex.getMessage());
		}

		User existingUser = getParameterized();
		user.setRolesFromUserIfNull(existingUser);
		user.setPasswordFromUserIfNull(existingUser);

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

		user = user.store();

		if (!isExisting()) {
			setParameterized(user);
			postEventCreated(user);
		} else {
			postEventUpdated(user);
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

	@Delete
	@Override
	public void deleteResource() {

		if (!canDelete()) {
			throwClientForbiddenError();
		}

		getTargetUser().setState(State.DELETED);
		getTargetUser().store();

		postEventDeleted(getParameterized());
	}

	public static void postEventRegistered(User user) {
		postEventUser(Event.Severity.medium, "User '" + user.getName() + "' has registered", user.getName(), "system");
	}

	public static void postEventValidated(User user) {
		postEventUser(Event.Severity.medium, "User '" + user.getName() + "' has validated his account", user.getName(),
				"system");
	}

	public static void postEventPasswordReseted(User user) {
		postEventUser(Event.Severity.medium, "User '" + user.getName() + "' has reseted his password", user.getName(),
				"system");
	}

	private void postEventCreated(User user) {
		postEventUserAction(Event.Severity.medium, "created", user);
	}

	private void postEventUpdated(User user) {
		postEventUserAction(Event.Severity.medium, "updated", user);
	}

	private void postEventDeleted(User user) {
		postEventUserAction(Event.Severity.high, "deleted", user);
	}

	private void postEventUserAction(Event.Severity severity, String action, User user) {
		String message = "User '" + user.getName() + "' " + action + " by '" + getUser().getName() + "'";

		postEventUser(severity, message, user.getName());
	}

	private void postEventUser(Event.Severity severity, String message, String username) {
		postEventUser(severity, message, username, getUser().getName());
	}

	private static void postEventUser(Event.Severity severity, String message, String username, String ownerUsername) {
		String resourceRef = getResourceRoot() + username;

		Event.postEvent(resourceRef, severity, message, ownerUsername, Event.EventType.action);
	}

}
