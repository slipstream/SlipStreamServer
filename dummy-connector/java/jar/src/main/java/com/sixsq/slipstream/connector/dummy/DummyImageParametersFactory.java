package com.sixsq.slipstream.connector.dummy;

/*
 * +=================================================================+
 * SlipStream Connector Dummy
 * =====
 * Copyright (C) 2014 SixSq Sarl (sixsq.com)
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


public class DummyImageParametersFactory extends ModuleParametersFactoryBase {

	public static final String DISK_SIZE_KEY = "disk";

	public DummyImageParametersFactory(String connectorInstanceName) throws ValidationException {
		super(connectorInstanceName);
	}

	@Override
	protected void initReferenceParameters() throws ValidationException {
		putEnumParameter(ImageModule.INSTANCE_TYPE_KEY,
				"Cloud instance type", DummyInstanceType.getValues(),
				DummyInstanceType.MICRO.getValue(),
				"You might have to request access to Dummy for some instance types",
				true,
				10);

		putEnumParameter(DISK_SIZE_KEY,
				"Size of the root disk",
				DummyDiskSize.getValues(),
				DummyDiskSize.TEN.getValue(),
				"Some disk sizes might not be available with all operating system and all instance type",
				true,
				20);

		addSecurityGroupsParameter();
	}

}
