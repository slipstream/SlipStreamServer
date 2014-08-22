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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Text;

@SuppressWarnings("serial")
@Entity
public class Target implements Serializable {

	public static final String EXECUTE_TARGET = "execute";
	public static final String REPORT_TARGET = "report";
	public static final String ONVMADD_TARGET = "onvmadd";
	public static final String ONVMREMOVE_TARGET = "onvmremove";

	@Id
	@GeneratedValue
	Long id;

	@Text(required = false, data = true)
	@Column(length = 65536)
	private String script = "";

	@Attribute
	private String name;

	@ManyToOne
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

	public ImageModule getModule() {
		return module;
	}

	public void setModule(ImageModule module) {
		this.module = module;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}


	public Target copy() {
		return new Target(getName(), getScript());
	}

}
