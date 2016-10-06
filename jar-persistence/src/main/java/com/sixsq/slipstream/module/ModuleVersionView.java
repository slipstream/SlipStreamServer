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

import com.sixsq.slipstream.persistence.Authz;
import com.sixsq.slipstream.persistence.Commit;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.util.ModuleUriUtil;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import javax.persistence.CascadeType;
import javax.persistence.OneToOne;
import java.util.Date;
import java.util.List;

@Root(name = "item")
public class ModuleVersionView {

	@Element(required = false)
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	private Authz authz;

	@Attribute
	public final String resourceUri;

	@Attribute
	public String getName() {
		return ModuleUriUtil.extractShortNameFromResourceUri(resourceUri);
	}

	@Attribute(required = false)
	public final Date lastModified;

	@Attribute
	public final int version;

	@Attribute
	public final ModuleCategory category;

	@Element(required = false)
	public final Commit commit;

	@Attribute
	public final String moduleName;

	public ModuleVersionView(String resourceUri, int version,
			Date lastModified, Commit commit, Authz authz,
			ModuleCategory category, String moduleName) {

		this.resourceUri = resourceUri;
		this.version = version;
		this.commit = commit;
		this.authz = authz;
		this.category = category;
		this.moduleName = moduleName;

		if (lastModified != null) {
			this.lastModified = (Date) lastModified.clone();
		} else {
			this.lastModified = null;
		}
	}

	@Root(name = "versionList")
	public static class ModuleVersionViewList {

		@Attribute
		public final String moduleCategory;

		@Attribute
		public final String moduleName;

		@ElementList(inline = true, required = false)
		public final List<ModuleVersionView> list;

		public ModuleVersionViewList(List<ModuleVersionView> list) {
			this.list = list;
			if (list.isEmpty()) {
				this.moduleCategory = "";
				this.moduleName = "";
			} else {
				ModuleVersionView firstVersion = list.get(0);
				this.moduleCategory = firstVersion.category.name();
				this.moduleName = firstVersion.moduleName;
			}
		}
	}

	public Authz getAuthz() {
		return authz;
	}

}
