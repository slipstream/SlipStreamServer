package com.sixsq.slipstream.connector.stratuslab;

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

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.ModuleParametersFactoryBase;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.NetworkType;
import com.sixsq.slipstream.persistence.Run;

public class StratusLabImageParametersFactory extends ModuleParametersFactoryBase {

	public static final String IP_TYPE_DEFAULT = NetworkType.Public.name().toLowerCase();
	public static final String INSTANCE_TYPE_DEFAULT = InstanceType.M1_SMALL.getValue();

	public static final String DISKSBUS_TYPE_KEY = "disks.bus.type";
	public static final String DISKSBUS_TYPE_DEFAULT = DisksBusType.VIRTIO.getValue();

	public StratusLabImageParametersFactory(String connectorInstanceName) throws ValidationException {
		super(connectorInstanceName);
	}

	@Override
	protected void initReferenceParameters() throws ValidationException {

		putMandatoryParameter(Run.RAM_PARAMETER_NAME, Run.RAM_PARAMETER_DESCRIPTION);
		putMandatoryParameter(Run.CPU_PARAMETER_NAME, Run.CPU_PARAMETER_DESCRIPTION);
		putEnumParameter(ImageModule.INSTANCE_TYPE_KEY, "Cloud instance type",
				InstanceType.getValues(), INSTANCE_TYPE_DEFAULT, true);
		putEnumParameter(DISKSBUS_TYPE_KEY, "VM disks bus type",
				DisksBusType.getValues(), DISKSBUS_TYPE_DEFAULT, true);
	}
}
