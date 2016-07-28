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

import java.util.Map;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.exceptions.QuotaException;
import com.sixsq.slipstream.persistence.*;

/**
 * Unit test:
 *
 * @see QuotaTest
 *
 */

public class Quota {

	public static void validate(User user, Map<String, Integer> request, Map<String, CloudUsage> usage)
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
						currentUsage, nodesRequested, quota);
				throw new QuotaException(msg);
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
