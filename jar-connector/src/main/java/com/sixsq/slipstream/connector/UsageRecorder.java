package com.sixsq.slipstream.connector;

import java.util.*;
import java.util.logging.Logger;


/**
 * 
 * Set of static methods to start and end the recording of usage for a VM.
 * Collaborates with clojure code, and is called by Collector.
 * 
 * 
 * +=================================================================+
 * SlipStream Server (WAR) ===== Copyright (C) 2013 SixSq Sarl (sixsq.com) =====
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * -=================================================================- *
 */
public class UsageRecorder {

	public static final String METRIC_NAME_KEY = "name";
	public static final String METRIC_VALUE_KEY = "value";
	private static Logger logger = Logger.getLogger(UsageRecorder.class.getName());
	
	public static void insertStart(String instanceId, String user, String cloud, List<Map<String, String>> metrics) {
		try {
			logger.info("Inserting usage record START for " + describe(instanceId, user, cloud));

			TypePrincipal owner = new TypePrincipal(TypePrincipal.PrincipalType.USER, user);
			List<TypePrincipalRight> rules = Arrays.asList(
					new TypePrincipalRight(TypePrincipal.PrincipalType.USER, user, TypePrincipalRight.Right.ALL),
					new TypePrincipalRight(TypePrincipal.PrincipalType.ROLE, "ADMIN", TypePrincipalRight.Right.ALL));
			ACL acl = new ACL(owner, rules);

			UsageRecord usageRecord = new UsageRecord(acl, user, cloud, keyCloudVMInstanceID(cloud, instanceId), new Date(), null, metrics);
			UsageRecord.post(usageRecord);

			logger.info("DONE Insert usage record START for " + describe(instanceId, user, cloud));
		} catch (Exception e) {
			logger.severe("Unable to insert usage record START:" + e.getMessage());
		}
	}

	public static void insertEnd(String instanceId, String user, String cloud) {
		try {
			logger.info("Inserting usage record END for " + describe(instanceId, user, cloud));

			TypePrincipal owner = new TypePrincipal(TypePrincipal.PrincipalType.USER, user);
			List<TypePrincipalRight> rules = Arrays.asList(
					new TypePrincipalRight(TypePrincipal.PrincipalType.USER, user, TypePrincipalRight.Right.ALL),
					new TypePrincipalRight(TypePrincipal.PrincipalType.ROLE, "ADMIN", TypePrincipalRight.Right.ALL));
			ACL acl = new ACL(owner, rules);

			UsageRecord usageRecord = new UsageRecord(acl, user, cloud, keyCloudVMInstanceID(cloud, instanceId),
					null, new Date(), null);
			UsageRecord.post(usageRecord);

			logger.info("DONE Insert usage record END for " + describe(instanceId, user, cloud));
		} catch (Exception e) {
			logger.severe("Unable to insert usage record END:" + e.getMessage());
		}
	}

	public static List<Map<String, String>> createVmMetrics() {
		Map<String, String> metric = new HashMap<String, String>();
		metric.put(METRIC_NAME_KEY, "vm");
		metric.put(METRIC_VALUE_KEY, "1.0");
				
		return Arrays.asList(metric);
	}

	private static String keyCloudVMInstanceID(String cloud, String instanceId) {
		return cloud + ":" + instanceId;
	}

//	// TODO : factor out common functions with Event class
//	private static final String ISO_8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
//
//	private static final DateFormat ISO8601Formatter = new SimpleDateFormat(ISO_8601_PATTERN, Locale.US);
//
//	static {
//		ISO8601Formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
//	}

	private static String describe(String instanceId, String user, String cloud) {
		return "[" + user + ":" + cloud + "/" + instanceId + "]";
	}
}
