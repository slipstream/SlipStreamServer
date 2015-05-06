package com.sixsq.slipstream.persistence;

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

import org.junit.Test;

import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.util.SerializationUtil;

public class ModuleServializationDeserializationTest {

	@Test
	public void checkJsonProjectModuleSerialization() throws SlipStreamClientException {

		ProjectModule module = new ProjectModule("checkJsonProjectModuleSerialization");
		String json = SerializationUtil.toJsonString(module);
		module = (ProjectModule) SerializationUtil.fromJson(json, ProjectModule.class, module.createDeserializer());
	}

	@Test
	public void checkXmlProjectModuleSerialization() throws SlipStreamClientException {

		ProjectModule module = new ProjectModule("checkXmlProjectModuleSerialization");
		String xml = SerializationUtil.toXmlString(module);
		module = (ProjectModule) SerializationUtil.fromXml(xml, ProjectModule.class);

	}

	@Test
	public void checkJsonDeploymentModuleSerialization() throws SlipStreamClientException {

		DeploymentModule module = new DeploymentModule("checkJsonDeploymentModuleSerialization");
		String json = SerializationUtil.toJsonString(module);
		module = (DeploymentModule) SerializationUtil.fromJson(json, DeploymentModule.class,
				module.createDeserializer());

	}

	@Test
	public void checkXmlDeploymentModuleSerialization() throws SlipStreamClientException {

		DeploymentModule module = new DeploymentModule("checkXmlDeploymentModuleSerialization");
		String xml = SerializationUtil.toXmlString(module);
		module = (DeploymentModule) SerializationUtil.fromXml(xml, DeploymentModule.class);

	}

	@Test
	public void checkJsonImageModuleSerialization() throws SlipStreamClientException {

		ImageModule module = new ImageModule("checkJsonImageModuleSerialization");
		String json = SerializationUtil.toJsonString(module);
		module = (ImageModule) SerializationUtil.fromJson(json, ImageModule.class, module.createDeserializer());

	}

	@Test
	public void checkXmlImageModuleSerialization() throws SlipStreamClientException {

		ImageModule module = new ImageModule("checkXmlImageModuleSerialization");
		String xml = SerializationUtil.toXmlString(module);
		module = (ImageModule) SerializationUtil.fromXml(xml, ImageModule.class);

	}

}