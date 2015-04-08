package com.sixsq.slipstream.connector;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

import com.sixsq.slipstream.usage.Record;

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

	private static Logger logger = Logger.getLogger(UsageRecorder.class.getName());

	static {
		try {
			Record.init();	
			logger.info("Init done for Clojure Persistence for usage records");
		} catch (Exception e) {
			logger.severe("Unable to init Clojure Persistence for usage records:" + e.getMessage());
		}
	}
	
	public static void insertStart(String instanceId, String user, String cloud, List<Map<String, Object>> metrics) {
		try {
			logger.info("Inserting usage record START for " + describe(instanceId, user, cloud));
			Map<String, Object> record = new HashMap<String, Object>();
			record.put("cloud_vm_instanceid", keyCloudVMInstanceID(cloud, instanceId));
			record.put("user", user);
			record.put("cloud", cloud);
			record.put("start_timestamp", nowISO8601());
			record.put("metrics", metrics);
			Record.insertStart(record);
			logger.info("DONE Insert usage record START for " + describe(instanceId, user, cloud));
		} catch (Exception e) {
			logger.severe("Unable to insert usage record START:" + e.getMessage());
		}
	}

	public static void insertEnd(String instanceId, String user, String cloud) {
		try {
			logger.info("Inserting usage record END for " + describe(instanceId, user, cloud));
			Map<String, Object> record = new HashMap<String, Object>();
			record.put("cloud_vm_instanceid", keyCloudVMInstanceID(cloud, instanceId));
			record.put("end_timestamp", nowISO8601());
			Record.insertEnd(record);
			logger.info("DONE Insert usage record END for " + describe(instanceId, user, cloud));
		} catch (Exception e) {
			logger.severe("Unable to insert usage record END:" + e.getMessage());
		}
	}

	public static List<Map<String, Object>> createVmMetric() {
		Map<String, Object> metric = new HashMap<String, Object>();
		metric.put("name", "vm");
		metric.put("value", 1.0);
				
		return Arrays.asList(metric);		
	}

	private static String keyCloudVMInstanceID(String cloud, String instanceId) {
		return cloud + ":" + instanceId;
	}

	// TODO : factor out common functions with Event class
	private static final String ISO_8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	private static final DateFormat ISO8601Formatter = new SimpleDateFormat(ISO_8601_PATTERN, Locale.US);

	static {
		ISO8601Formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	private static String nowISO8601() {
		return ISO8601Formatter.format(new Date());
	}

	private static String describe(String instanceId, String user, String cloud) {
		return "[" + user + ":" + cloud + "/" + instanceId + "]";
	}
}
