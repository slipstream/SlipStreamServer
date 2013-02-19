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
import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.simpleframework.xml.Attribute;

import com.sixsq.slipstream.exceptions.ValidationException;

@MappedSuperclass
@SuppressWarnings("serial")
public abstract class Metadata implements Serializable {

	protected Date creation = new Date();

	@Attribute(required = false)
	@Temporal(TemporalType.TIMESTAMP)
	protected Date lastModified;

	@SuppressWarnings("unused")
	@Version
	private int jpaVersion;

	@Attribute(required = false)
	protected ModuleCategory category;

	@Attribute(required = false)
	@Lob
	protected String description;

	@Attribute(required = false)
	protected boolean deleted;

	protected Metadata() {
	}

	public abstract String getResourceUri();

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public String getParent() {
		return "";
	}

	public abstract String getName();

	public abstract void setName(String name) throws ValidationException;

	@Attribute(required = false)
	@Temporal(TemporalType.TIMESTAMP)
	public Date getCreation() {
		return creation;
	}

	@Attribute(required = false)
	@Temporal(TemporalType.TIMESTAMP)
	public void setCreation(Date creation) {
		this.creation = creation;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified() {
		this.lastModified = new Date();
	}

	public ModuleCategory getCategory() {
		return category;
	}

	public void setCategory(ModuleCategory category) {
		this.category = category;
	}

	public void setCategory(String category) {
		this.category = ModuleCategory.valueOf(category);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public static String URLEncode(String url) {
		if (url == null) {
			return url;
		}
		return url.replace(" ", "+");
	}

	public void validate() throws ValidationException {
		boolean isInvalid = false;
		isInvalid = (getName() == null) || ("".equals(getName()));
		if (isInvalid) {
			throw (new ValidationException("Name cannot be empty"));
		}
	}

	public Metadata store() {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		Metadata obj = em.merge(this);
		transaction.commit();
		em.close();
		return obj;
	}

	public void remove() {
		remove(getResourceUri(), this.getClass());
	}

	public static void remove(String resourceUri, Class<? extends Metadata> c) {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		Metadata fromDb = em.find(c, resourceUri);
		if (fromDb != null) {
			em.remove(fromDb);
		}
		transaction.commit();
		em.close();
	}

	protected void throwValidationException(String error)
			throws ValidationException {
		throw new ValidationException(error);
	}

	protected Metadata copyTo(Metadata copy) throws ValidationException {
		copy.setCreation(getCreation());
		copy.setLastModified();
		copy.setCategory(getCategory());
		copy.setDeleted(deleted);
		copy.setDescription(getDescription());
		copy.setName(getName());
		return copy;
	}

}
