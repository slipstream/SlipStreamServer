package com.sixsq.slipstream.module;

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

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import com.sixsq.slipstream.persistence.Authz;
import com.sixsq.slipstream.persistence.ModuleCategory;

@Root(name = "item")
public class ModuleViewPublished extends ModuleView {

	@Attribute(required = false)
	private final boolean published = true;

	@Attribute(required = false)
	private final String logoLink;

	public ModuleViewPublished(String id, String description, ModuleCategory category, String customVersion,
			Authz authz, String logoLink) {

		super(id, description, category, customVersion, authz);
		this.logoLink = logoLink;
	}

	public boolean isPublished() {
		return published;
	}

}
