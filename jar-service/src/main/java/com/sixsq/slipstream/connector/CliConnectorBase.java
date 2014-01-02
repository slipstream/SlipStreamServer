package com.sixsq.slipstream.connector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.sixsq.slipstream.credentials.Credentials;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.User;

public abstract class CliConnectorBase extends ConnectorBase {

	@Override
	abstract public String getCloudServiceName();

	@Override
	abstract public Run launch(Run run, User user) throws SlipStreamException;

	@Override
	abstract public Credentials getCredentials(User user);

	@Override
	abstract public void terminate(Run run, User user) throws SlipStreamException;

	@Override
	abstract public Properties describeInstances(User user) throws SlipStreamException;
	
	public static Properties parseDescribeInstanceResult(String result)
			throws SlipStreamException {
		Properties states = new Properties();
		List<String> lines = new ArrayList<String>();
		Collections.addAll(lines, result.split("\n"));
		lines.remove(0);
		for (String line : lines) {
			String[] parts = line.trim().split("\\s+");
			if (parts.length < 2) {
				throw (new SlipStreamException(
						"Error returned by launch command. Got: " + result));
			}
			String instanceIdKey = parts[0];
			String status = parts[1];
			states.put(instanceIdKey, status);
		}
		return states;
	}

	public static String[] parseRunInstanceResult(String result)
			throws SlipStreamClientException {
		String[] parts = result.trim().split(",");
		if (parts.length != 2) {
			throw (new SlipStreamClientException(
					"Error returned by launch command. Got: " + result));
		}
		return parts;
	}

	public CliConnectorBase(String instanceName) {
		super(instanceName);
	}

	protected String getKey(User user) {
		return wrapInSingleQuotesOrNull(super.getKey(user));
	}
	
	protected String getSecret(User user) {
		return wrapInSingleQuotesOrNull(super.getSecret(user));
	}

	protected String wrapInSingleQuotesOrNull(String value) {
		if (value == null || value.isEmpty()) {
			return null;
		} else {
			return wrapInSingleQuotes(value);
		}
	}
	
	protected String wrapInSingleQuotes(String value) {
		return "'" + value.replaceAll("'","'\\\\''") + "'";
	}
	
	protected String getEndpoint(User user) {
		return wrapInSingleQuotes(super.getEndpoint(user));
	}

	protected String getErrorMessageLastPart(User user) {
		return ", please edit your <a href='/user/" + user.getName()
				+ "'> user account</a>";
	}

	protected void validateCredentials(User user) throws ValidationException {
		String errorMessageLastPart = getErrorMessageLastPart(user);

		if (getKey(user) == null) {
			throw (new ValidationException(
					"Cloud Username cannot be empty"
							+ errorMessageLastPart));
		}
		if (getSecret(user) == null) {
			throw (new ValidationException(
					"Cloud Password cannot be empty"
							+ errorMessageLastPart));
		}
	}

}
