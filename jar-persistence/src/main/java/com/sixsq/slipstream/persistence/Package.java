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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.simpleframework.xml.Attribute;

import com.sixsq.slipstream.exceptions.ValidationException;

@SuppressWarnings("serial")
@Entity
public class Package implements Serializable {

	@Id
	@GeneratedValue
	Long id;

	@Attribute
	private String name;
	
	@Attribute(required = false)
	private String repository;

	@Attribute(required = false, name = "key")
	private String key_;

	@ManyToOne
	private ImageModule module;

	@SuppressWarnings("unused")
	private Package() {
	}

	public Package(String name) throws ValidationException {
		if (name == null || "".equals(name)) {
			throw new ValidationException(
					"Invalide package name, cannot be empty");
		}
		this.name = name;
	}

	public Package(String name, String repository) throws ValidationException {
		this(name);
		this.repository = repository;
	}

	public Package(String name, String repository, String key)
			throws ValidationException {
		this(name, repository);
		this.key_ = key;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getRepository() {
		return repository;
	}

	public void setRepository(String repository) {
		this.repository = repository;
	}

	public ImageModule getModule() {
		return module;
	}

	public void setModule(ImageModule module) {
		this.module = module;
	}

	public String getKey() {
		return key_;
	}

	public void setKey(String key) {
		this.key_ = key;
	}

	public Package copy() throws ValidationException {
		return new Package(getName(), getRepository(), getKey());
	}
	
	@Override
	public int hashCode() {
		if(name == null) {
			return super.hashCode();
		}
	    return name.hashCode();
	}
	
    @Override
	public boolean equals(Object o) {
		if(o instanceof Package) {
			Package other = (Package) o;
			boolean equalName = (name != null ? name.equals(other.getName()) : other.getName() == null);
			boolean equalRepository = (repository != null ? repository.equals(other.getRepository()) : other.getRepository() == null);
			boolean equalKey = (key_ != null ? key_.equals(other.getKey()) : other.getKey() == null);
			return equalName && equalRepository && equalKey;
		} else {
			return false;
		}
	}
}
