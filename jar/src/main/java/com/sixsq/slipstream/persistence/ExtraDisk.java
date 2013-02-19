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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.simpleframework.xml.Attribute;

import com.sixsq.slipstream.connector.ConnectorBase;
import com.sixsq.slipstream.exceptions.ValidationException;

@Entity
public class ExtraDisk {

	@SuppressWarnings("unused")
	@Id
	@GeneratedValue
	private Long id;
	
	@Attribute
	private String name;

	@Attribute
	private String description;
	
	@Attribute
	private String mountPoint;
	
	@Attribute
	private String deviceName;
	
	@Attribute(required = false)
	private int size;
	
	@Attribute(required = false)
	private String moduleReferenceUri;

	@Transient
	private volatile ConnectorBase connector;

	public ExtraDisk() {
		super();
	}

	public ExtraDisk(String name, String description, ConnectorBase connector) {
		super();
		setName(name);
		setDescription(description);
		setCloudConnector(connector);
	}
	
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription() {
		return description;
	}
	public String getMountPoint() {
		return mountPoint;
	}
	public void setMountPoint(String mountPoint) {
		this.mountPoint = mountPoint;
	}
	public String getDeviceName() {
		return deviceName;
	}
	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}

	public void setModuleReferenceUri(String moduleReferenceUri) {
		this.moduleReferenceUri = moduleReferenceUri;
	}

	public String getModuleReferenceUri() {
		return moduleReferenceUri;
	}

	public void setCloudConnector(ConnectorBase connector) {
		this.connector = connector;
	}
	
	public void validateParameterValue(String param) throws ValidationException {
		connector.validateExtraDiskParameter(this.name, param);
	}

	public ExtraDisk copy() {
		ExtraDisk copy = new ExtraDisk(getName(), getDescription(), connector);
		copy.setDeviceName(getDeviceName());
		copy.setModuleReferenceUri(getModuleReferenceUri());
		copy.setMountPoint(getMountPoint());
		copy.setSize(getSize());
		return copy;
	}
	
}
