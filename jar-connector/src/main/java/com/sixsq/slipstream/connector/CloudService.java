package com.sixsq.slipstream.connector;

import com.sixsq.slipstream.persistence.CloudImageIdentifier;

public class CloudService {

	public static boolean isDefaultCloudService(String cloudServiceName) {
		return "".equals(cloudServiceName)
				|| CloudImageIdentifier.DEFAULT_CLOUD_SERVICE.equals(cloudServiceName)
				|| cloudServiceName == null;
	}
}
