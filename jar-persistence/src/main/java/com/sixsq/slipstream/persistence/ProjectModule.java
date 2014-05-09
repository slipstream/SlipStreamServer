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


import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Transient;

import org.simpleframework.xml.ElementList;

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.module.ModuleView;

/**
 * For unittests
 * @see ProjectModuleTest
 *
 */
@Entity
@SuppressWarnings("serial")
public class ProjectModule extends Module {

	@Transient
	@ElementList(required = false)
	private List<ModuleView> children = new ArrayList<ModuleView>();

	protected ProjectModule() {
		super();
	}

	public ProjectModule(String name) throws ValidationException {
		super(name, ModuleCategory.Project);
	}

	public List<ModuleView> getChildren() {
		return children;
	}

	public void setChildren(List<ModuleView> children) {
		this.children = children;
	}

	public static ProjectModule load(String resourceUri) {
		ProjectModule project = (ProjectModule) Module.load(resourceUri);
		if(project == null) {
			return null;
		}
		project.setChildren(Module.viewList(resourceUri));
		return project;
	}

	public ProjectModule store() {
		return (ProjectModule)super.store();
	}

	public ProjectModule copy() throws ValidationException {
		return (ProjectModule) copyTo(new ProjectModule(getName()));
	}
}
