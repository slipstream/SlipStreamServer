package com.sixsq.slipstream.persistence;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.*;

import flexjson.JSON;

@SuppressWarnings("serial")
public class Target implements Serializable {

//	private static final String PRERECIPE_TARGET = "prerecipe";
//	private static final String RECIPE_TARGET = "recipe";
//	private static final String EXECUTE_TARGET = "execute";
//	private static final String REPORT_TARGET = "report";
//	private static final String ONVMADD_TARGET = "onvmadd";
//	private static final String ONVMREMOVE_TARGET = "onvmremove";
//
	public static final String TARGET_PRERECIPE_NAME = "prerecipe";
	public static final String TARGET_RECIPE_NAME = "recipe";
	public static final String[] TARGET_BUILD_SCRIPT_NAMES = { TARGET_PRERECIPE_NAME, TARGET_RECIPE_NAME };
	public static final String[] TARGET_SCRIPT_NAMES = { TARGET_PRERECIPE_NAME, TARGET_RECIPE_NAME, "execute", "report", "onvmadd", "onvmremove" };

	@ElementList(data = true, entry = "script")
	private List<String> inheritedScripts = new ArrayList<String>();

	@Element(data = true)
	private String script;

	@Attribute(required = false)
	private String name;

	@JSON(include = false)
	private ImageModule module;

	@SuppressWarnings("unused")
	private Target() {
	}

	public Target(String name) {
		this.name = name;
	}

	public Target(String name, String script) {
		this(name);
		this.script = script;
	}

	public Target(String name, String script, List<String> inheritedScripts) {
		this(name, script);
		this.inheritedScripts = inheritedScripts;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public ImageModule getModule() {
		return module;
	}

	public void setModule(ImageModule module) {
		this.module = module;
	}

	public String getName() {
		return name;
	}

	private void setName(String name) {
		this.name = name;
	}

	public List<String> getInheritedScripts() {
		return inheritedScripts;
	}

	public void setInheritedScripts(List<String> inheritedScripts) {
		this.inheritedScripts = inheritedScripts;
	}

	public boolean equals(Target obj) {
		if (obj == null) {
			return false;
		}
		if (obj.getName() == null) {
			return name == null;
		}
		if (!obj.getName().equals(name)) {
			return false;
		}
		if (!(script == null ? obj.getScript() == null : script.equals(obj.getScript()))) {
			return false;
		}
		return equalStringList(obj.getInheritedScripts());
	}

	private boolean equalStringList(List<String> list) {
		if (list == null) {
			return getInheritedScripts() == null;
		}
		if (getInheritedScripts() == null) {
			return list == null;
		}
		if (list.size() != getInheritedScripts().size()) {
			return false;
		}
		for (int i = 0; i < list.size(); i++) {
			if (!list.get(i).equals(getInheritedScripts().get(i))) {
				return false;
			}
		}
		return true;
	}

	public Target copy() {
		return new Target(getName(), getScript(), getInheritedScripts());
	}

	@JSON(include = false)
	public boolean isTargetSet() {
		return Parameter.hasValueSet(script);
	}

	@JSON(include = false)
	public boolean isInheritedTargetSet() {
		return !inheritedScripts.isEmpty();
	}
}
