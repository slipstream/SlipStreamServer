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

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.persistence.Transient;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.module.ModuleVersionView;
import com.sixsq.slipstream.module.ModuleView;
import com.sixsq.slipstream.util.ModuleUriUtil;

/**
 * Unit test see:
 * 
 * @see ModuleTest
 * 
 */
@SuppressWarnings("serial")
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@NamedQueries({
		@NamedQuery(name = "moduleLastVersion", query = "SELECT m FROM Module m WHERE m.version = (SELECT MAX(m.version) FROM Module m WHERE m.name = :name)"),
		@NamedQuery(name = "moduleViewLatestChildren", query = "SELECT NEW com.sixsq.slipstream.module.ModuleView(m.resourceUri, m.description, m.category, m.customVersion, m.authz) FROM Module m WHERE m.parentUri = :parent AND m.version = (SELECT MAX(c.version) FROM Module c WHERE c.name = m.name)"),
		@NamedQuery(name = "moduleViewAllVersions", query = "SELECT NEW com.sixsq.slipstream.module.ModuleVersionView(m.resourceUri, m.version, m.lastModified, m.comment, m.authz) FROM Module m WHERE m.name = :name"),
		@NamedQuery(name = "moduleViewPublished", query = "SELECT NEW com.sixsq.slipstream.module.ModuleView(m.resourceUri, m.description, m.category, m.customVersion, m.authz) FROM Module m WHERE m.published != null") })
public abstract class Module extends Parameterized<Module, ModuleParameter> {

	public final static String RESOURCE_URI_PREFIX = "module/";

	public final static int DEFAULT_VERSION = -1;

	private static Module loadByUri(String uri) {
		EntityManager em = PersistenceUtil.createEntityManager();
		return em.find(Module.class, uri);
	}

	public static Module loadLatest(String resourceUri) {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("moduleLastVersion");
		String name = ModuleUriUtil
				.extractModuleNameFromResourceUri(resourceUri);
		q.setParameter("name", name);
		Module module;
		try {
			module = (Module) q.getSingleResult();
		} catch (NoResultException ex) {
			module = null;
		}
		return module;
	}

	public static boolean exists(String resourceUri) {
		boolean exists;
		if (load(resourceUri) != null) {
			exists = true;
		} else {
			exists = false;
		}
		return exists;
	}

	public static Module load(String uri) {
		String resourceUri = uri;
		int version = ModuleUriUtil.extractVersionFromResourceUri(resourceUri);
		return (version == DEFAULT_VERSION ? loadLatest(resourceUri)
				: loadByUri(resourceUri));
	}

	@SuppressWarnings("unchecked")
	public static List<ModuleView> viewList(String resourceUri) {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("moduleViewLatestChildren");
		q.setParameter("parent", Module.constructResourceUri(ModuleUriUtil
				.extractModuleNameFromResourceUri(resourceUri)));
		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	public static List<ModuleVersionView> viewListAllVersions(String resourceUri) {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("moduleViewAllVersions");
		String name = ModuleUriUtil
				.extractModuleNameFromResourceUri(resourceUri);
		q.setParameter("name", name);
		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	public static List<ModuleView> viewPublishedList() {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("moduleViewPublished");
		return q.getResultList();
	}

	public static String constructResourceUri(String name) {
		return RESOURCE_URI_PREFIX + name;
	}

	public static String constructResourceUrl(String name, int version) {
		return constructResourceUri(name + "/" + String.valueOf(version));
	}

	@Attribute
	@Id
	private String resourceUri;

	@Element(required = false)
	@OneToOne(cascade = CascadeType.ALL)
	private Authz authz = new Authz("Unknown", this);

	@Attribute(required = false)
	private String customVersion;

	/**
	 * Module hierarchy (e.g. <parent>/<module>)
	 */
	@Attribute(required = true)
	private String parentUri;

	/**
	 * Last part of the module hierarchy (e.g. <parent>/<module>)
	 */
	@Attribute(required = true)
	private String name;

	@Attribute(required = true)
	private int version;

	@Attribute(required = false)
	@Lob
	private String tag;

	@Element(required = false)
	@Lob
	private String comment;

	@Element(required = false)
	@Lob
	private Publish published; // to the marketplace

	/**
	 * Module reference is a URL.
	 * <p/>
	 * In the case "Image", the module to use as a base image from which to
	 * extract the cloud image id (e.g. AMI). It can be empty if the ImageId
	 * field is provided.
	 */
	@Attribute(required = false)
	private String moduleReferenceUri;

	@Transient
	@ElementArray(required = false)
	protected String[] cloudNames;

	protected Module() {
		super();
	}

	public Module(String name, ModuleCategory category)
			throws ValidationException {

		this.category = category;

		setName(name);
	}

	private void validateName(String name) throws ValidationException {
		if (name == null || "".equals(name)) {
			throw new ValidationException("module name cannot be empty");
		}
		if (ModuleUriUtil.extractVersionFromResourceUri(name) != -1) {
			throw new ValidationException("Invalid name, cannot be an integer");
		}
		if (name.contains(" ")) {
			throw new ValidationException("Invalid name, cannot contain space");
		}
	}

	@Attribute
	public String getShortName() {
		return ModuleUriUtil.extractShortNameFromResourceUri(resourceUri);
	}

	@Attribute
	public void setShortName(String name) {
	}

	public String getOwner() {
		return authz.getUser();
	}

	@Override
	public String getResourceUri() {
		return resourceUri;
	}

	public Authz getAuthz() {
		return authz;
	}

	public void setAuthz(Authz authz) {
		this.authz = authz;
	}

	public int getVersion() {
		return version;
	}

	@Override
	public String getParent() {
		return parentUri;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) throws ValidationException {
		validateName(name);

		resourceUri = constructResourceUri(name);

		extractUriComponents();
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getCustomVersion() {
		return customVersion;
	}

	public void setCustomVersion(String customVersion) {
		this.customVersion = customVersion;
	}

	public String getModuleReference() {
		return moduleReferenceUri;
	}

	public void setModuleReference(Module reference) {
		this.moduleReferenceUri = reference.getResourceUri();
	}

	public void setModuleReference(String moduleReferenceUri) {
		this.moduleReferenceUri = moduleReferenceUri;
	}

	/**
	 * A virtual module is a module that doesn't require to be explicitly built,
	 * since it only defines runtime behavior
	 * 
	 * @return true if the module is virtual, false if not
	 */
	public boolean isVirtual() {
		return false;
	}

	private void extractUriComponents() throws ValidationException {

		version = ModuleUriUtil.extractVersionFromResourceUri(resourceUri);

		parentUri = ModuleUriUtil.extractParentUriFromResourceUri(resourceUri);

		name = ModuleUriUtil.extractModuleNameFromResourceUri(resourceUri);
	}

	@Override
	public void setContainer(ModuleParameter parameter) {
		parameter.setContainer(this);
	}

	public Module store() {
		return store(true);
	}

	public Module store(boolean incrementVersion) {
		setLastModified();
		if (incrementVersion) {
			setVersion();
		}
		return (Module) super.store();
	}

	protected void setVersion() {
		version = VersionCounter.getNextVersion();
		resourceUri = Module.constructResourceUri(ModuleUriUtil
				.extractModuleNameFromResourceUri(resourceUri) + "/" + version);
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getComment() {
		return comment;
	}

	public String[] getCloudNames() {
		return cloudNames;
	}

	public void setCloudNames(String[] cloudNames) {
		this.cloudNames = cloudNames;
	}

	public void publish() {
		this.published = new Publish();
	}

	public void unpublish() {
		this.published = null;
	}

	public Publish getPublished() {
		return published;
	}

	protected String computeParameterValue(String key)
			throws ValidationException {
		ModuleParameter parameter = getParameter(key);
		String value = (parameter == null ? null : parameter.getValue());
		if (value == null) {
			String reference = getModuleReference();
			if (reference != null) {
				Module parent = Module.load(getModuleReference());
				if (parent != null) {
					value = parent.computeParameterValue(key);
				}
			}
		}
		return value;
	}

	public void resetUser(String username) {
		getAuthz().setUser(username);
	}

	public abstract Module copy() throws ValidationException;

	protected Module copyTo(Module copy) throws ValidationException {
		copy = (Module) super.copyTo(copy);

		copy.setComment(getComment());
		copy.setCustomVersion(getCustomVersion());

		copy.setCloudNames((cloudNames == null ? null : cloudNames.clone()));
		copy.setModuleReference(getModuleReference());
		copy.setTag(getTag());

		copy.setAuthz(getAuthz().copy(copy));

		return copy;
	}

}
