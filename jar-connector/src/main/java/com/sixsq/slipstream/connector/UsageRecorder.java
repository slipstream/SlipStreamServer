package com.sixsq.slipstream.connector;

import com.sixsq.slipstream.persistence.Vm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;


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

	public static boolean isMuted = false;

	private static Set<String> recordedVmInstanceIds = new HashSet<String>();
	private static Map<String, Long> closeAttemptsVmInstanceIds = new HashMap<String, Long>();

	public static void muteForTests() {
		isMuted = true;
		logger.severe("You should NOT see this message in production: usage will *not* be recorded");
	}

	public static void insertStart(String instanceId, String user, String cloud, List<UsageMetric> metrics) {
		try {

			if(isMuted) {
				return;
			}

			if(hasRecorded(cloud, instanceId)){
				logger.fine("Already recorded => avoiding inserting usage record START for "
						+ metrics + ", " + describe(instanceId, user, cloud));
				return;
			}

			logger.info("Inserting usage record START for " + metrics + ", " + describe(instanceId, user, cloud));

			UsageRecord usageRecord = new UsageRecord(getAcl(user), user, cloud,
					keyCloudVMInstanceID(cloud, instanceId), new Date(), null, metrics);
			UsageRecord.post(usageRecord);

			recordedVmInstanceIds.add(keyCloudVMInstanceID(cloud, instanceId));

			logger.info("DONE Insert usage record START for " + describe(instanceId, user, cloud));
		} catch (Exception e) {
			logger.severe("Unable to insert usage record START:" + e.getMessage());
		}
	}

	public static void insertEnd(String instanceId, String user, String cloud, List<UsageMetric> metrics) {
		try {

			if(isMuted) {
				return;
			}

			if(hasAttemptedToClose(cloud, instanceId)) {
				recordAttemptClose(cloud, instanceId);
				return;
			}

			logger.info("Inserting usage record END, metrics" + metrics + ", for " + describe(instanceId, user, cloud));

			UsageRecord usageRecord = new UsageRecord(getAcl(user), user, cloud,
					keyCloudVMInstanceID(cloud, instanceId), null, new Date(), metrics);
			UsageRecord.post(usageRecord);

			recordedVmInstanceIds.remove(keyCloudVMInstanceID(cloud, instanceId));
			recordAttemptClose(cloud, instanceId);

			logger.info("DONE Insert usage record END for " + describe(instanceId, user, cloud));
		} catch (Exception e) {
			logger.severe("Unable to insert usage record END:" + e.getMessage());
		}
	}

	private static boolean hasRecorded(String cloud, String instanceId) {
		logger.info("UsageRecorder, recordedVmInstanceIds = " + recordedVmInstanceIds);
		return recordedVmInstanceIds.contains(keyCloudVMInstanceID(cloud, instanceId));
	}

	private static boolean hasAttemptedToClose(String cloud, String instanceId) {
		return closeAttemptsVmInstanceIds.get(keyCloudVMInstanceID(cloud, instanceId)) != null;
	}

	private static void recordAttemptClose(String cloud, String instanceId) {
		purgeOldAttemptsClose();

		closeAttemptsVmInstanceIds.put(keyCloudVMInstanceID(cloud, instanceId), System.currentTimeMillis());
	}

	private static void purgeOldAttemptsClose() {
		long now = System.currentTimeMillis();
		long oneHourMillis = 1000 * 60 * 60;

		closeAttemptsVmInstanceIds =
				closeAttemptsVmInstanceIds.entrySet()
						.stream()
						.filter(e -> (now - e.getValue()) < oneHourMillis)
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private static ACL getAcl(String user) {
		TypePrincipal owner = new TypePrincipal(TypePrincipal.PrincipalType.USER, user);
		List<TypePrincipalRight> rules = Arrays.asList(
				new TypePrincipalRight(TypePrincipal.PrincipalType.USER, user, TypePrincipalRight.Right.ALL),
				new TypePrincipalRight(TypePrincipal.PrincipalType.ROLE, "ADMIN", TypePrincipalRight.Right.ALL));
		return new ACL(owner, rules);
	}

	public static List<UsageMetric> createVmMetrics(Vm vm) {
		List<UsageMetric> metrics = new ArrayList<UsageMetric>();

		metrics.add(new UsageMetric("vm", "1.0"));

		Integer cpu = vm.getCpu();
		if (cpu != null) {
			metrics.add(new UsageMetric(ConnectorBase.VM_CPU, cpu.toString()));
		}

		Float ram = vm.getRam();
		if (ram != null) {
			metrics.add(new UsageMetric(ConnectorBase.VM_RAM, ram.toString()));
		}

		Float disk = vm.getDisk();
		if (disk != null) {
			metrics.add(new UsageMetric(ConnectorBase.VM_DISK, disk.toString()));
		}

		String instanceType = vm.getInstanceType();
		if (instanceType != null && !instanceType.isEmpty()) {
			metrics.add(new UsageMetric("instance-type." + instanceType, "1.0"));
		}
				
		return metrics;
	}

	private static String keyCloudVMInstanceID(String cloud, String instanceId) {
		return cloud + ":" + instanceId;
	}

	private static String describe(String instanceId, String user, String cloud) {
		return "[" + user + ":" + cloud + "/" + instanceId + "]";
	}
}
