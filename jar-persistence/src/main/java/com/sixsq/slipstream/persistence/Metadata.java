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

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceException;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.simpleframework.xml.Attribute;

import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamDatabaseException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.util.SerializationUtil;

import flexjson.JSON;
import flexjson.JSONDeserializer;

@MappedSuperclass
@SuppressWarnings("serial")
public abstract class Metadata implements Serializable {

	@SuppressWarnings("unused")
	@Deprecated
	private int jpaVersion;

	protected Date creation = new Date();

	@Attribute(required = false)
	@Temporal(TemporalType.TIMESTAMP)
	protected Date lastModified;

	@Attribute(required = false)
	@Column(length = 1024)
	protected String description;

	@Attribute(required = false)
	protected boolean deleted;

	@Column(length = 1048576)
	@JSON(include = false)
	private String json;

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	String getJson() {
		return json;
	}

	void setJson(String json) {
		this.json = json;
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

	public Metadata store() throws SlipStreamDatabaseException {
		Metadata obj = null;
		json = toJsonForPersistence();
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = null;
		try {
			transaction = em.getTransaction();
			transaction.begin();
			obj = em.merge(this);
			transaction.commit();
		} catch (PersistenceException e) {
			if(transaction.isActive()) {
				transaction.rollback();
			}
			throw new SlipStreamDatabaseException(e.getMessage());
		} finally {
			em.close();
		}
		return obj.substituteFromJson();
	}

	protected String toJsonForPersistence() {
		return SerializationUtil.toJsonString(this);
	}

	public static Metadata load(String resourceUri, Class<? extends Metadata> type, EntityManager em) {
		Metadata meta = (Metadata) em.find(type, resourceUri);
		if(meta != null) {
			meta = meta.substituteFromJson();
		}
		return meta;
	}

	public static Metadata load(String resourceUri, Class<? extends Metadata> type) {
		EntityManager em = PersistenceUtil.createEntityManager();
		Metadata meta = (Metadata) load(resourceUri, type, em);
		em.close();
		return meta;
	}

	protected Metadata substituteFromJson() {
		return substituteFromJson(createDeserializer());
	}

	protected JSONDeserializer<Object> createDeserializer() {
		return new JSONDeserializer<Object>();
	}
	
	protected Metadata substituteFromJson(JSONDeserializer<Object> deserializer) {
		// cache the json since it will be set to null by the deserializer,
		// since it's volatile
		String json = getJson();
		if(json == null) {
			return this;
		}
		Metadata meta = null;
		try {
			meta = (Metadata) SerializationUtil.fromJson(getJson(), getClass(), deserializer);
		} catch (SlipStreamClientException e) {
			throw new SlipStreamRuntimeException(e.getMessage(), e);
		}
		if(meta != null) {
			meta.setJson(json);
		}
		return meta;
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

	protected void throwValidationException(String error) throws ValidationException {
		throw new ValidationException(error);
	}

	protected Metadata copyTo(Metadata copy) throws ValidationException {
		copy.setCreation(getCreation());
		copy.setLastModified();
		copy.setDeleted(deleted);
		copy.setDescription(getDescription());
		copy.setName(getName());
		return copy;
	}

	/**
	 * Fix deserialization (e.g. from xml) lists and maps containers.
	 */
	public void postDeserialization() {
	}

}
