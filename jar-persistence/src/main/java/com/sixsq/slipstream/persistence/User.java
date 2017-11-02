package com.sixsq.slipstream.persistence;

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

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.InvalidElementException;
import com.sixsq.slipstream.exceptions.SlipStreamDatabaseException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.user.Passwords;
import com.sixsq.slipstream.user.UserView;
import com.sixsq.slipstream.util.SscljProxy;
import org.restlet.Response;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementMap;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.persistence.*;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Unit test:
 *
 *
 */
@SuppressWarnings("serial")
//@NamedQueries({
//		@NamedQuery(name = "allUsers", query = "SELECT u FROM User u"),
//		@NamedQuery(name = "activeUsers", query = "SELECT u FROM User u WHERE u.state = 'ACTIVE'"),
//		@NamedQuery(name = "userViewList", query = "SELECT NEW com.sixsq.slipstream.user.UserView(u.name, u.firstName, u.lastName, u.email, u.state, u.lastOnline, u.lastExecute, u.activeSince, u.organization, u.roles, u.isSuperUser) FROM User u") })
public class User extends Metadata {

	private static final String USERNAME = "internal";
	private static final String ROLE = "ADMIN";
	private static final String USERNAME_ROLE = USERNAME + " " + ROLE;

	public static final String REQUEST_KEY = "authenticated_user";
	public static final String REQUEST_ROLES_KEY = "roles";

	public static final String RESOURCE_NAME = "user";
	public static final String RESOURCE_URL_PREFIX = RESOURCE_NAME + "/";

	public static final int ACTIVE_TIMEOUT_MINUTES = 1;

	public static final String NEW_NAME = "new";

	private static transient final Random rnd = new Random();

	public enum State {
		NEW, ACTIVE, DELETED, SUSPENDED
	}

	private static transient final List<String> FORBIDDEN_ROLES = Arrays.asList("ADMIN", "USER", "ROLE", "ANON");

	// FIXME: not used
//	@Attribute(required = false)
//	@Column(length = 1000)
//	private String authnToken;
//
//	@Attribute(required = false)
//	private String githubLogin;
//
//	@Attribute(required = false)
//	private String cycloneLogin;

	private String href;

	@Attribute
	@SerializedName("id")
	private String resourceUri;

	@Attribute
	@SerializedName("username")
	private String name;

	@Attribute(required = false)
	@SerializedName("emailAddress")
	private String email;

	@Attribute(required = false)
	private String firstName;

	@Attribute(required = false)
	private String lastName;

	@Attribute(required = false)
	private String organization;

	@Attribute(required = false)
	private String roles;

	private String password;

	@Attribute(required = false, name = "issuper")
	private boolean isSuperUser = false;

	@Attribute
//	@Enumerated(EnumType.STRING)
	private State state;

	@Attribute(required = false)
	private Date lastOnline = null;

	@Attribute(required = false)
	private Date lastExecute = null;

	@Attribute(required = false)
	private Date activeSince = null;

	protected transient Map<String, UserParameter> parameters;

	@SuppressWarnings("unused")
	private User() {

	}

	public User(String name, String password) throws ValidationException,
			NoSuchAlgorithmException, UnsupportedEncodingException {
		this(name);
		hashAndSetPassword(password);
	}

	public User(String name) throws ValidationException {
		setName(name);
		this.state = State.NEW;
	}

	@ElementMap(name = "parameters", required = false, valueType = UserParameter.class)
	protected void setParameters(Map<String, UserParameter> parameters) {
		this.parameters = parameters;
	}

	@ElementMap(name = "parameters", required = false, valueType = UserParameter.class)
	public Map<String, UserParameter> getParameters() {
	    if (null == parameters) {
	    	parameters = loadParameters(this);
		}
		return parameters;
	}

	public Map<String, UserParameter> getParameters(String category) {
		Map<String, UserParameter> filteredParameters = new HashMap<>();
		for (UserParameter parameter : getParameters().values()) {
			String pCategory = parameter.getCategory();
			if (pCategory.equals(category)) {
				filteredParameters.put(parameter.getName(), parameter);
			}
		}

		return filteredParameters;
	}

	private void validateParameters() throws ValidationException {
		for (Map.Entry<String, UserParameter> p : getParameters().entrySet
				()) {
			p.getValue().validateValue();
		}
	}

	public void validate() throws ValidationException {
		boolean isEmpty = (name == null) || ("".equals(name));
		boolean isNewValue = User.NEW_NAME.equals(name);
		boolean isInvalid = isEmpty || isNewValue;
		if (isInvalid) {
			throw (new ValidationException("Invalid name"));
		}
		validateParameters();
	}

	public boolean isOnline() {
		return isOnline(lastOnline);
	}

	public static boolean isOnline(Date lastOnline) {
		if (lastOnline == null) {
			return false;
		}

		boolean isOnline = false;

		Date now = new Date();
		if (millisecondsToMinutes(now.getTime() - lastOnline.getTime()) < ACTIVE_TIMEOUT_MINUTES) {
			isOnline = true;
		}
		return isOnline;
	}

	private static long millisecondsToMinutes(long milliseconds) {
		return milliseconds / 1000 / 60;
	}

	public static String constructResourceUri(String name) {
		return RESOURCE_URL_PREFIX + name;
	}

	public boolean isSuper() {
		return isSuperUser;
	}

	public String getOrganizationManagedForUserCreator() {
		if (this.roles != null) {
			String[] rolesArray = roles.split(",");
			for (String r : rolesArray) {
				if (r.startsWith("USERCREATOR")) {
					if (r.indexOf("_") > -1)
						return r.substring(r.indexOf("_") + 1);
					else
						return null;
				}
			}
		}
		return null;
	}

	public void setSuper(boolean isSuperUser) {
		this.isSuperUser = isSuperUser;
	}

	public String getResourceUri() {
		return resourceUri;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		this.resourceUri = User.constructResourceUri(name);
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getLastName() {
		return lastName;
	}

	/**
	 * @return full name build out of available information
	 */
	public String getFullName() {
		String fullName = getName();
		if (lastName != null) {
			fullName = lastName;
		}
		if (firstName != null) {
			fullName = firstName + "+" + fullName;
		}
		return fullName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getHashedPassword() {
		return password;
	}

	public void setHashedPassword(String password) {
		this.password = password;
	}

	public void hashAndSetPassword(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		String hashedPassword = "";
		if (password != null && !password.isEmpty()) {
			hashedPassword = Passwords.hash(password);
		}
		setHashedPassword(hashedPassword);
	}

	public boolean setPasswordFromUserIfNull(User user) throws ValidationException {
		if (password == null && user != null) {
			setHashedPassword(user.password);
			return true;
		}
		return false;
	}

	@Attribute(name = "password", required = false)
	public String getPassword() {
		// We don't want to serialize the password into the XML.
		return null;
	}

	@Attribute(name = "password", required = false)
	public void setPassword(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		hashAndSetPassword(password);
	}

	public String randomizePassword() {
		password = randomPassword();
		return password;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public String getSummaryString() {
		StringBuilder sb = new StringBuilder();
		sb.append("username:     ");
		sb.append(getName());
		sb.append("\n");

		sb.append("name:         ");
		sb.append(getFullName());
		sb.append("\n");

		sb.append("organization: ");
		sb.append(getOrganization());
		sb.append("\n");

		sb.append("email:        ");
		sb.append(getEmail());
		sb.append("\n");

		return sb.toString();
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
		if(state == State.ACTIVE) {
			activeSince = new Date();
		}
	}

	public String getDefaultCloudService() {
		UserParameter parameter = getDefaultCloudServiceParameter();
		return parameter == null ? "" : parameter.getValue();
	}

	public UserParameter getParameter(String name) {
		return getParameters().get(name);
	}

	public UserParameter getParameter(String name, String category) {
		UserParameter parameter = getParameter(name);
		if (parameter != null && parameter.getCategory().equals(category)) {
			return parameter;
		} else {
			return null;
		}
	}

	private UserParameter getDefaultCloudServiceParameter() {
		return getParameter(constructCloudServiceKey());
	}

	private String constructCloudServiceKey() {
		return Parameter.constructKey(ParameterCategory.getDefault(),
				UserParameter.DEFAULT_CLOUD_SERVICE_PARAMETER_NAME);
	}

	public void setParameter(UserParameter parameter) throws ValidationException {
		parameters.put(parameter.getName(), parameter);
		setContainer(parameter);
	}

	public void setDefaultCloudServiceName(String defaultCloudServiceName)
			throws ValidationException {
		UserParameter parameter = getDefaultCloudServiceParameter();
		if (parameter == null) {
			parameter = new UserParameter(constructCloudServiceKey());
			setParameter(parameter);
		}
		parameter.setValue(defaultCloudServiceName);
	}

	private static String randomPassword() {
		long v = rnd.nextLong();
		while (v == Long.MIN_VALUE) {
			v = rnd.nextLong();
		}
		return Long.toString(Math.abs(v), 36);
	}

	public static void validateMinimumInfo(User user)
			throws InvalidElementException {

		if (user.getName() == null) {
			throw new InvalidElementException("Missing username");
		}

		// Only check for a valid username if the account is new.  Existing accounts
		// from external authentication may have usernames that do not follow this
		// pattern.
		State state = user.getState();
		String username = user.getName();
		if (state == State.NEW && !Pattern.matches("\\w[\\w\\d.]+", username)) {
			throw new InvalidElementException(
					"Username must start with a letter and contain only letters and digits.");
		}

		// The first name cannot be empty.
		String firstname = user.getFirstName();
		if (firstname == null || "".equals(firstname)) {
			throw new InvalidElementException("First name cannot be empty.");
		}

		// Nor the last name.
		String lastname = user.getLastName();
		if (lastname == null || "".equals(lastname)) {
			throw new InvalidElementException("Last name cannot be empty.");
		}

		// For security reasons, the password must not be null or the empty
		// string.
		String password = user.getHashedPassword();
		if (password == null || "".equals(password)) {
			throw new InvalidElementException("Password cannot be empty.");
		}

		// Ensure that the email address is valid.
		try {
			String email = user.getEmail();
			if (email != null) {
				InternetAddress address = new InternetAddress(email);
				address.validate();
			} else {
				String msg = "Email address cannot be empty.";
				throw new InvalidElementException(msg);
			}
		} catch (AddressException e) {
			String msg = "Invalid email address.\n" + e.getMessage();
			throw new InvalidElementException(msg);
		}

	}

	public static User loadByName(String name) throws ConfigurationException,
			ValidationException {
		return loadByName(name, null);
	}

	public static User loadByName(String name, ServiceConfiguration sc)
			throws ConfigurationException, ValidationException {
		return load(User.constructResourceUri(name), sc);
	}

	public static User load(String resourceUrl, ServiceConfiguration sc)
			throws ConfigurationException, ValidationException {
		User user = load(resourceUrl);

		if (sc != null && user != null) {
			user.addSystemParametersIntoUser(sc);
		}

		return user;
	}

	public static User load(String resourceUrl) throws ConfigurationException,
			ValidationException {
		Response resp = SscljProxy.get(SscljProxy.BASE_RESOURCE + resourceUrl, USERNAME_ROLE);
		if (badResponse(resp)) return null;
		User user = (new Gson()).fromJson(resp.getEntityAsText(), User.class);
		user.parameters = loadParameters(user);
		return user;
	}

	private static Map<String, UserParameter> loadParameters(User user) {
		return new HashMap<>();
//	    Logger log = Logger.getLogger("com.sixsq.slipstream.User");
//	    log.info("LOADING PARAMETERS ....");
////		Form queryParameters = new Form();
////		queryParameters.add("$filter", "");
////		SscljProxy.get("", getName(), queryParameters);
//		Response response = SscljProxy.get("api/credential", user.getName());
//		log.info("response: " + response.getEntityAsText());
//		Map<String, UserParameter> params = new HashMap<>();
//		try {
//			params.put("my-parameter", new UserParameter("name", "value",
//					"description"));
//		} catch (ValidationException e) {
//			e.printStackTrace();
//		}
//		return params;
	}

	public void addSystemParametersIntoUser(ServiceConfiguration sc)
			throws ConfigurationException, ValidationException {
		for (ServiceConfigurationParameter p : sc.getParameters().values()) {
			try {
				UserParameter userParam = new UserParameter(p.getName(),
						p.getValue(""), "");
				userParam.setCategory("System");
				setParameter(userParam);
			} catch (ValidationException e) {
			}
		}
	}

	public void remove() {
	    if (resourceUri != null) {
			SscljProxy.delete(SscljProxy.BASE_RESOURCE + resourceUri, USERNAME_ROLE, true);
		}
	}

	public static void removeNamedUser(String name) {
		SscljProxy.delete(SscljProxy.BASE_RESOURCE + User.constructResourceUri(name),
				USERNAME_ROLE, true);
	}

	@SuppressWarnings("unchecked")
	public static List<User> list() {
		Response resp = SscljProxy.get(SscljProxy.BASE_RESOURCE + RESOURCE_NAME, USERNAME_ROLE);

		if (badResponse(resp)) return new ArrayList<>();

		Users records = Users.fromJson(resp.getEntityAsText());

		if (records == null) return new ArrayList<>();

		return records.getUsers();
	}

	@SuppressWarnings("unchecked")
	public static boolean isSuperAlone() {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("allUsers");
		q.setMaxResults(1);
		List<User> list = q.getResultList();
		em.close();
		return list.size() == 1 && list.get(0).getName().equals("super");
	}

	@SuppressWarnings("unchecked")
	public static List<User> listActive() {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("activeUsers");
		List<User> list = q.getResultList();
		em.close();
		return list;
	}

	@SuppressWarnings("unchecked")
	public static List<UserView> viewList() {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("userViewList");
		List<UserView> list = q.getResultList();
		em.close();
		return list;
	}

	public void setContainer(UserParameter parameter) {
		parameter.setContainer(this);
	}

	private void storeParameters() {
		System.out.println("STORING PARAMETERS....");
	}

	private static class UserTemplate {
		private User userTemplate;
		public UserTemplate(User user) {
			userTemplate = user;
		}
	}

//	@Override
	public User store() {
		storeParameters();
		Response resp = SscljProxy.get(SscljProxy.BASE_RESOURCE + resourceUri, USERNAME_ROLE);
		if (badResponse(resp)) {
		    href = "user-template/auto";
			resp = SscljProxy.post(SscljProxy.BASE_RESOURCE + RESOURCE_NAME, USERNAME_ROLE, new UserTemplate(this));
			href = null;
		} else {
			resp = SscljProxy.put(SscljProxy.BASE_RESOURCE + resourceUri, USERNAME_ROLE, this);
		}
		if (badResponse(resp)) {
			throw new SlipStreamDatabaseException("Failed to persist User: "
					+ resp.toString());
		}
		return this;
	}

	public Date getLastOnline() {
		return lastOnline;
	}

	public void setLastOnline(Date date) {
		this.lastOnline = date;
	}

	public void setLastOnline() {
		this.lastOnline = new Date();
	}

	public void setLastExecute() {
		this.lastExecute = new Date();
	}


	public String getParameterValue(String name, String defaultValue) {
		UserParameter parameter = getParameter(name);
		return parameter == null ? defaultValue : parameter.getValue(defaultValue);
	}

	public int getTimeout() {
		String key = Parameter.constructKey(ParameterCategory.General.toString(), UserParameter.KEY_TIMEOUT);
		return Integer.parseInt(getParameterValue(key, "0"));
	}

	public String getKeepRunning() {
		String key = Parameter.constructKey(ParameterCategory.getDefault(),	UserParameter.KEY_KEEP_RUNNING);
		return getParameterValue(key, UserParameter.KEEP_RUNNING_DEFAULT);
	}

	public void setKeepRunning(String value) throws ValidationException {
		String key = Parameter.constructKey(ParameterCategory.getDefault(),	UserParameter.KEY_KEEP_RUNNING);
		List<String> keepRunningOptions = UserParameter.getKeepRunningOptions();

		if (!keepRunningOptions.contains(value)) {
			throw new ValidationException("Value of " + UserParameter.KEY_KEEP_RUNNING
					+ "should be one of the following: " + keepRunningOptions.toString());
		}

		UserParameter parameter = getParameter(key);
		if (parameter == null) {
			parameter = new UserParameter(key);
			setParameter(parameter);
		}
		parameter.setValue(value);
	}

	public String getMailUsage() {
		String key = Parameter.constructKey(ParameterCategory.getDefault(),	UserParameter.KEY_MAIL_USAGE);
		return getParameterValue(key, UserParameter.MAIL_USAGE_DEFAULT);
	}

	public boolean setRolesFromUserIfNull(User user) throws ValidationException {
		if (roles == null && user != null) {
			setRoles(user.roles);
			return true;
		}
		return false;
	}

	public String getRoles() {
		return roles;
	}

	public void setRoles(String roles) throws ValidationException {
		if (roles != null) {
			checkValidRoles(roles);
		}
		this.roles = roles;
	}

	private void checkNoForbiddenRoles(String roles) throws ValidationException {
		if (roles == null || roles.isEmpty()) {
			return;
		}
		for (String role : roles.split(",")) {
			String trimedUppercaseRole = role.trim().toUpperCase();
			if (FORBIDDEN_ROLES.contains(trimedUppercaseRole)) {
				throw new ValidationException("List of roles '" + roles + "' contains forbidden role : '" + trimedUppercaseRole + "'");
			}
		}
	}

	private void checkValidRoles(String roles) throws ValidationException {
		String validRole = "(([a-zA-Z][\\w\\d._-]*))*";
		String spacesCommaSpaces = "(\\s)*,(\\s)*";
		boolean isValid = Pattern.matches(validRole + "(" + spacesCommaSpaces + validRole + ")*", roles);

		if(!isValid){
			throw new ValidationException("Invalid roles " + roles);
		} else {
			checkNoForbiddenRoles(roles);
		}
	}

	private static boolean badResponse(Response resp) {
		return resp == null || resp.getStatus().isError();
	}
}
