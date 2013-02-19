package com.sixsq.slipstream.connector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.persistence.User;

public abstract class CliConnectorBase extends ConnectorBase {

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
		return wrapInSingleQuotes(super.getKey(user));
	}
	
	protected String getSecret(User user) {
		return wrapInSingleQuotes(super.getSecret(user));
	}

	private String wrapInSingleQuotes(String value) {
		return "'" + value + "'";
	}
}
