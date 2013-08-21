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


import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Query;

import org.simpleframework.xml.Attribute;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.ExecutionControlUserParametersFactory;
import com.sixsq.slipstream.connector.ParametersFactory;
import com.sixsq.slipstream.connector.UserParametersFactoryBase;
import com.sixsq.slipstream.exceptions.InvalidElementException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.user.UserView;

/**
 * Unit test:
 * 
 * @see UserTest
 * 
 */
@SuppressWarnings("serial")
@Entity
@NamedQueries({
		@NamedQuery(name = "activeUsers", query = "SELECT u FROM User u WHERE u.state = 'ACTIVE'"),
		@NamedQuery(name = "userView", query = "SELECT NEW com.sixsq.slipstream.user.UserView(u.name, u.firstName, u.lastName, u.state) FROM User u") })
public class User extends Parameterized<User, UserParameter> {

	public final static String RESOURCE_URL_PREFIX = "user/";

	public static final String NEW_NAME = "new";

	public enum State {
		NEW, ACTIVE, DELETED, SUSPENDED
	}

	@Attribute
	@Id
	private String resourceUri;

	@Attribute
	private String name;

	@Attribute(required = false)
	private String email;

	@Attribute(required = false)
	private String firstName;

	@Attribute(required = false)
	private String lastName;

	@Attribute(required = false)
	private String organization;

	private String password;

	@Attribute(required = false, name = "issuper")
	private boolean isSuperUser = false;

	@Attribute
	@Enumerated(EnumType.STRING)
	private State state;

	@SuppressWarnings("unused")
	private User() {

	}

	public User(String name, String password) throws ValidationException {
		this(name);
		setPassword(password);
	}

	public User(String name) throws ValidationException {
		setName(name);
		this.state = State.NEW;
	}

	@Override
	public void validate() throws ValidationException {
		boolean isEmpty = (name == null) || ("".equals(name));
		boolean isNewValue = User.NEW_NAME.equals(name);
		boolean isInvalid = isEmpty || isNewValue;
		if (isInvalid) {
			throw (new ValidationException("Invalid name"));
		}
		validateParameters();
	}

	public static String constructResourceUri(String name) {
		return RESOURCE_URL_PREFIX + name;
	}

	public boolean isSuper() {
		return isSuperUser;
	}

	public void setSuper(boolean isSuperUser) {
		this.isSuperUser = isSuperUser;
	}

	@Override
	public String getResourceUri() {
		return resourceUri;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
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

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
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
	}

	public String getDefaultCloudService() {
		UserParameter parameter = getDefaultCloudServiceParameter();
		return parameter == null ? "" : parameter.getValue();
	}

	protected UserParameter getDefaultCloudServiceParameter() {
		return getParameter(ParametersFactory.constructKey(
				ExecutionControlUserParametersFactory.CATEGORY,
				UserParametersFactoryBase.DEFAULT_CLOUD_SERVICE_PARAMETER_NAME));
	}

	public void setDefaultCloudServiceName(String defaultCloudServiceName)
			throws ValidationException {
		UserParameter parameter = getDefaultCloudServiceParameter();
		if (parameter == null) {
			parameter = new UserParameter(
					ParametersFactory
							.constructKey(
									ExecutionControlUserParametersFactory.CATEGORY,
									UserParametersFactoryBase.DEFAULT_CLOUD_SERVICE_PARAMETER_NAME));
			setParameter(parameter);
		}
		parameter.setValue(defaultCloudServiceName);
	}

	private static String randomPassword() {
		Random r = new Random();
		return Long.toString(Math.abs(r.nextLong()), 36);
	}

	public static void validateMinimumInfo(User user)
			throws InvalidElementException {

		if (user.getName() == null) {
			throw new InvalidElementException("Missing username");
		}

		String username = user.getName();
		if (!Pattern.matches("\\w[\\w\\d.]+", username)) {
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
		String password = user.getPassword();
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

	public static User loadByName(String name) {
		return loadByName(name, true);
	}
	
	public static User loadByName(String name, boolean mergeSysteParameters) {
		return load(User.constructResourceUri(name), mergeSysteParameters);
	}

	public static User load(String resourceUrl) {
		return load(resourceUrl, true);
	}
	
	public static User load(String resourceUrl, boolean mergeSysteParameters) {
		EntityManager em = PersistenceUtil.createEntityManager();
		User user = em.find(User.class, resourceUrl);
		em.close();
		
		if(mergeSysteParameters && user != null) addSystemParametersIntoUser(user);
		
		return user;
	}
	
	public static void addSystemParametersIntoUser(User user){
		ServiceConfiguration sc = Configuration.getInstance().getParameters();
		for (Map.Entry<String, ServiceConfigurationParameter> entry : sc.getParameters().entrySet()) {
			try {
				UserParameter userParam = new UserParameter(entry.getKey(), entry.getValue().getValue(""), "");
				userParam.setCategory("System");
				user.setParameter(userParam);
			} catch (ValidationException e) {}
		}
	}

	public static void removeNamedUser(String name) {
		remove(User.constructResourceUri(name), User.class);
	}

	@SuppressWarnings("unchecked")
	public static List<User> list() {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("activeUsers");
		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	public static List<UserView> viewList() {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("userView");
		return q.getResultList();
	}

	@Override
	public void setContainer(UserParameter parameter) {
		parameter.setContainer(this);
	}

	@Override
	public User store() {
		return (User) super.store();
	}

}
