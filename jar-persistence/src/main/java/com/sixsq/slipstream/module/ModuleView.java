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

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.OneToOne;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import com.sixsq.slipstream.persistence.Authz;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.util.ModuleUriUtil;

@Root(name = "item")
public class ModuleView {

	@Element(required = false)
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	private Authz authz;

	@Attribute
	public final String resourceUri;

	@Attribute
	public String getName() {
		return ModuleUriUtil.extractShortNameFromResourceUri(resourceUri);
	}

	@Attribute
	public int getVersion() {
		return ModuleUriUtil.extractVersionFromResourceUri(resourceUri);
	}

	@Attribute
	public final ModuleCategory category;

	@Attribute(required = false)
	public final String customVersion;

	@Attribute(required = false)
	public final String description;

	public ModuleView(String resourceUri, String description, ModuleCategory category,
			String customVersion, Authz authz) {

		this.resourceUri = resourceUri;
		this.description = description;
		this.category = category;
		this.customVersion = customVersion;
		this.authz = authz;
	}

	@Root(name = "list")
	public static class ModuleViewList {

		@ElementList(inline = true, required = false)
		private final List<ModuleView> list;

		public List<ModuleView> getList() {
			return list;
		}

		public ModuleViewList(List<ModuleView> list) {
			this.list = list;
		}
	}

	public String getCustomVersion() {
		return customVersion;
	}

	public Authz getAuthz() {
		return authz;
	}

	public String getDescription() {
		return description;
	}

}
