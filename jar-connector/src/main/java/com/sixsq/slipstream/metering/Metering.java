package com.sixsq.slipstream.metering;

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

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.CloudUsage;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.Vm;
import com.sixsq.slipstream.util.Logger;

public class Metering {

	@SuppressWarnings("unused")
	public static String populate(User user)
			throws ConfigurationException, ValidationException, NotFoundException, AbortException {
		return populate(user.getName());
	}

	public static String populate(String user)
		throws ConfigurationException, ValidationException, NotFoundException, AbortException {
		Map<String, Integer> usageData = produceCloudUsageData(user);
		String data = transformUsageDataForGraphite(user, usageData);
		sendToGraphite(data);

		return data;
	}

	@SuppressWarnings("unused")
	public static String populate(User user, Connector connector)
			throws ConfigurationException, ValidationException, NotFoundException, AbortException {
		return populate(user.getName(), connector);
	}

	public static String populate(String user, Connector connector)
			throws ConfigurationException, ValidationException, NotFoundException, AbortException {
		List<String> cloudServiceNamesList = new ArrayList<String>();
		cloudServiceNamesList.add(connector.getConnectorInstanceName());

		Map<String, Integer> usageData = produceCloudUsageData(user, cloudServiceNamesList);
		String data = transformUsageDataForGraphite(user, usageData);
		sendToGraphite(data);

		return data;
	}

	public static void populateVmMetrics(String user, String cloud, int cpu, float ram, float disk, Map<String, Integer> instanceTypes) {
		String data = "";

		data += generateGraphiteData(user, cloud, "cpu-nb", cpu);
		data += generateGraphiteData(user, cloud, "ram-mb", ram);
		data += generateGraphiteData(user, cloud, "disk-gb", disk);

		for (Map.Entry<String, Integer> instanceType: instanceTypes.entrySet()) {
			data += generateGraphiteData(user, cloud, "instance-type." + instanceType.getKey(), instanceType.getValue());
		}

		sendToGraphite(data);
	}

	public static Map<String, Integer> produceCloudUsageData(String user)
			throws ConfigurationException, ValidationException {
		return produceCloudUsageData(user, ConnectorFactory.getCloudServiceNamesList());
	}

	public static Map<String, Integer> produceCloudUsageData(String user, List<String> cloudServiceNamesList)
			throws ConfigurationException, ValidationException {
		Map<String, Integer> cloudUsage = new HashMap<String, Integer>();
		Map<String, CloudUsage> vmUsage = Vm.usage(user);

		for (String cloud : cloudServiceNamesList) {
			Integer currentUsage = 0;

			if (vmUsage.containsKey(cloud)) {
				currentUsage += vmUsage.get(cloud).getUserUsage();
			}

			cloudUsage.put(cloud, currentUsage);
		}

		return cloudUsage;
	}

	public static String sendToGraphite(String data) {
		String errorMessageBase = "Measurements. Failed to send usage data to Graphite.";
		try {
			Socket conn = new Socket("localhost", 2003);
			DataOutputStream out = new DataOutputStream(conn.getOutputStream());
			out.writeBytes(data);
			conn.close();
		} catch (UnknownHostException e) {
			Logger.severe(errorMessageBase + " 'Unknown host' : " + e.getMessage());
		} catch (IOException e) {
			Logger.severe(errorMessageBase + " 'IO exception' : " + e.getMessage());
		}
		return data;
	}

	private static String generateUsageMetricName(String user, String cloud, String name) {
		return "slipstream." + user + ".usage." + name + "." + cloud;
	}

	private static String generateGraphiteData(String user, String cloud, String name, float value) {
		int timestamp = (int) (System.currentTimeMillis() / 1000L);
		return generateUsageMetricName(user, cloud, name) + " " + String.valueOf(value) + " " + String.valueOf(timestamp) + "\n";
	}

	public static String transformUsageDataForGraphite(String  user, Map<String, Integer> usages) {
		String buffer = "";
		for (Map.Entry<String, Integer> usage : usages.entrySet()) {
			String cloud = usage.getKey();
			buffer += generateGraphiteData(user, cloud, "instance", usage.getValue());
		}
		return buffer;
	}

}
