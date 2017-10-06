package com.sixsq.slipstream.run;

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

import java.util.*;
import java.util.logging.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.exceptions.QuotaException;
import com.sixsq.slipstream.persistence.*;
import com.sixsq.slipstream.util.ServiceOffersUtil;
import com.sixsq.slipstream.util.SscljProxy;
import org.restlet.Response;

import static com.sixsq.slipstream.util.SscljProxy.parseJson;
import static com.sixsq.slipstream.util.ServiceOffersUtil.getServiceOfferAttributeAsIntegerOrNull;
import static com.sixsq.slipstream.util.ServiceOffersUtil.cpuAttributeName;
import static com.sixsq.slipstream.util.ServiceOffersUtil.ramAttributeName;
import static com.sixsq.slipstream.util.ServiceOffersUtil.diskAttributeName;

import com.sixsq.slipstream.cookie.CookieUtils;

/**
 * Unit test:
 *
 * @see QuotaTest
 *
 */

public class Quota {

	private static java.util.logging.Logger logger = Logger.getLogger(ServiceOffersUtil.class.getName());

	public static void validate(User user, Run run, Map<String, CloudUsage> usage, String roles)
			throws ValidationException, QuotaException {
		Map<String, Integer> request = run.getCloudServiceUsage();
		validate_old(user, request, usage);
		validate_new(user, run);
	}

	private static void validate_old(User user, Map<String, Integer> request, Map<String, CloudUsage> usage)
			throws ValidationException, QuotaException {

		for (Map.Entry<String, Integer> entry : request.entrySet()) {
			String cloud = entry.getKey();
			int nodesRequested = entry.getValue();

			Integer quota = Integer.parseInt(getValue(user, cloud));

			Integer currentUsage = 0;
			if (usage.containsKey(cloud)) {
				currentUsage = usage.get(cloud).getUserVmUsage();
			}

			if ((currentUsage + nodesRequested) > quota) {
				String msg = String.format("Concurrent VM quota exceeded (quota=%d, current=%d, requested=%d)",
						quota, currentUsage, nodesRequested);
				throw new QuotaException(msg);
			}
		}
	}

	private static void validate_new(User user, Run run) throws QuotaException {

		int nbVms = 0;
		int nbCpu = 0;
		int nbRam = 0; // MB
		int nbDisk = 0; // GB

		Module module = run.getModule();

		try {
			user.setRoles("USER");
		} catch (ValidationException ignore) {}

		String nameRoles = SscljProxy.getNameRoles(user); //CookieUtils.claimsInToken()

		//Set<String> serviceOffersIds = new HashSet<>();
		Map<String, JsonObject> serviceOffersById = new HashMap<>();
		Map<String, String> serviceOffersIdsByNode = new HashMap<>();
		//Map<String, Integer> multiplicityByNode = new HashMap<>();

		switch (module.getCategory() ) {
			case Deployment:
				DeploymentModule deployment = (DeploymentModule) module;
				Map<String, Node> nodes = deployment.getNodes();

				for (String nodeName: nodes.keySet()) {
					String key = RuntimeParameter.constructParamName(nodeName, 1, RuntimeParameter.SERVICE_OFFER);
					String serviceOfferId = run.getRuntimeParameterValueOrDefaultIgnoreAbort(key, null);
					if (serviceOfferId != null) {
						//serviceOffersIds.add(serviceOfferId);
						serviceOffersIdsByNode.put(nodeName, serviceOfferId);
					}

					key = RuntimeParameter.constructParamName(nodeName, RuntimeParameter.MULTIPLICITY_PARAMETER_NAME);
					Integer multiplicity = Integer.valueOf(run.getRuntimeParameterValueOrDefaultIgnoreAbort(key, "0"));
					//multiplicityByNode.put(nodeName, multiplicity);
					nbVms += multiplicity;
				}

				nbVms ++;
				break;

			case Image:
				String key = RuntimeParameter.constructParamName(Run.MACHINE_NAME, RuntimeParameter.SERVICE_OFFER);
				String serviceOfferId = run.getRuntimeParameterValueOrDefaultIgnoreAbort(key, null);
				if (serviceOfferId != null) {
					//serviceOffersIds.add(serviceOfferId);
					serviceOffersIdsByNode.put(Run.MACHINE_NAME, serviceOfferId);
				}
				//multiplicityByNode.put(Run.MACHINE_NAME, 1);
				nbVms ++;
				break;
		}

		Set<String> serviceOffersIds = new HashSet<>(serviceOffersIdsByNode.values());

		for (String serviceOfferId: serviceOffersIds) {
			JsonObject serviceOffer = ServiceOffersUtil.getServiceOffer(serviceOfferId, false);
			if (serviceOffer != null) {
				serviceOffersById.put(serviceOfferId, serviceOffer);
			}
		}

		for (Map.Entry<String, String> entry: serviceOffersIdsByNode.entrySet()) {
			String nodename = entry.getKey();
			String serviceOfferId = entry.getValue();
			JsonObject serviceOffer = serviceOffersById.get(serviceOfferId);

			Integer cpu = getServiceOfferAttributeAsIntegerOrNull(serviceOffer, cpuAttributeName);
			if (cpu != null) nbCpu += cpu;

			Integer ram = getServiceOfferAttributeAsIntegerOrNull(serviceOffer, ramAttributeName);
			if (ram != null) nbRam += ram;

			Integer disk = getServiceOfferAttributeAsIntegerOrNull(serviceOffer, diskAttributeName);
			if (disk != null) nbDisk += disk;
		}


		String resource = SscljProxy.QUOTA_RESOURCE;
		Response response = SscljProxy.get(resource, nameRoles);
		Integer statusCode = response.getStatus().getCode();

		if (statusCode < 200 || statusCode >= 300) {
			logger.warning("Failed to retrieve quotas: " + response.getEntityAsText());
		} else {
			JsonArray quotas = parseJson(response.getEntityAsText()).getAsJsonArray("quotas");
			if (quotas == null) {
				logger.warning("Failed to retrieve quotas. Response doesn't contains 'quotas'");
			} else {
				for (int i = 0; i < quotas.size(); i++) {
					JsonObject quota = (JsonObject) quotas.get(i);
					String resourceCollect = quota.get("id").getAsString() + "/collect";
					JsonObject quotaCollect = parseJson(SscljProxy.post(resourceCollect, nameRoles).getEntityAsText());

					String aggregation = quotaCollect.get("aggregation").getAsString();
					Integer limit = quotaCollect.get("limit").getAsInt();
					Integer currentAll = quotaCollect.get("currentAll").getAsInt();
					Integer quotaLeft = limit - currentAll;
					Integer requested = 0;

					if ("count:id".equals(aggregation)) {
						requested = nbVms;
					} else if ("sum:serviceOffer/resource:vcpu".equals(aggregation)) {
						requested = nbCpu;
					} else if ("sum:serviceOffer/resource:ram".equals(aggregation)) {
						requested = nbRam;
					} else if ("sum:serviceOffer/resource:disk".equals(aggregation)) {
						requested = nbDisk;
					}

					if ((quotaLeft - requested) < 0) {
						String name = quotaCollect.get("name").getAsString();
						String msg = String.format("Quota '%s' exceeded (quota=%d, current=%d, requested=%d)",
								name, limit, currentAll, requested);
						throw new QuotaException(msg);
					}
				}
			}
		}
	}

	public static String getValue(User user,
			String cloud) throws ValidationException {
		String key = cloud + RuntimeParameter.PARAM_WORD_SEPARATOR
				+ QuotaParameter.QUOTA_VM_PARAMETER_NAME;

		Parameter<?> parameter = user.getParameter(key, cloud);
		if (parameter != null && Parameter.hasValueSet(parameter.getValue())) {
			return parameter.getValue();
		}

		ServiceConfiguration cfg = Configuration.getInstance().getParameters();
		parameter = cfg.getParameter(key, cloud);

		if (parameter != null && Parameter.hasValueSet(parameter.getValue())) {
			return parameter.getValue();
		}

		return QuotaParameter.QUOTA_VM_DEFAULT;
	}

}
