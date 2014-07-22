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
import java.util.HashMap;
import java.util.Map;

import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.Vm;
import com.sixsq.slipstream.util.Logger;


public class Metering {

	public static String populate(User user) throws ConfigurationException,
			ValidationException, NotFoundException, AbortException {

		Map<String, Integer> cloudUsage = new HashMap<String, Integer>();
		Map<String, Integer> vmUsage = Vm.usage(user.getName());

		for (String cloud : ConnectorFactory.getCloudServiceNamesList()) {
			Integer currentUsage = vmUsage.get(cloud);
			if (currentUsage == null) currentUsage = 0;

			cloudUsage.put(cloud, currentUsage);
		}

		return sendToGraphite(user, cloudUsage);
	}

	private static String sendToGraphite(User user, Map<String, Integer> usages) {
		String buffer = "";
		try {
			Socket conn = new Socket("localhost", 2003);
			DataOutputStream out = new DataOutputStream(conn.getOutputStream());

			for (Map.Entry<String, Integer> usage: usages.entrySet()) {
				String cloud = usage.getKey();
				String metricName = "slipstream." + user.getName() + ".usage.instance." + cloud;
				int timestamp = (int) (System.currentTimeMillis() / 1000L);
				int value = usage.getValue();
				buffer += metricName + " " + String.valueOf(value) + " " + String.valueOf(timestamp) + "\n";
			}
			out.writeBytes(buffer);
			conn.close();
		} catch (UnknownHostException e) {
			Logger.severe("Mesurements - Unknown host: " + e.getMessage());
		} catch (IOException e) {
			Logger.severe("Mesurements - IO exception: " + e.getMessage());
		}
		return buffer;
	}

}
