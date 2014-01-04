package com.sixsq.slipstream.connector;

import java.util.HashSet;

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.DeploymentFactory;
import com.sixsq.slipstream.persistence.CloudImageIdentifier;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.persistence.Run;

public class CloudService {

	public static boolean isDefaultCloudService(String cloudServiceName) {
		return "".equals(cloudServiceName)
				|| CloudImageIdentifier.DEFAULT_CLOUD_SERVICE
						.equals(cloudServiceName) || cloudServiceName == null;
	}

	public static HashSet<String> getCloudServicesList(Run run)
			throws ValidationException {

		HashSet<String> cloudServicesList;

		if (run.getCategory() == ModuleCategory.Deployment) {
			cloudServicesList = DeploymentFactory.getCloudServicesList(run);
		} else {
			cloudServicesList = new HashSet<String>();
			cloudServicesList.add(run.getCloudService());
		}

		return cloudServicesList;
	}
}
