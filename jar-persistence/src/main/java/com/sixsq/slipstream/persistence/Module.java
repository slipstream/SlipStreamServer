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
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.persistence.Transient;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;
import org.simpleframework.xml.ElementMap;

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.module.ModuleVersionView;
import com.sixsq.slipstream.module.ModuleView;
import com.sixsq.slipstream.run.RunView.RunViewList;
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
		@NamedQuery(name = "moduleLastVersion", query = "SELECT m FROM Module m WHERE m.version = (SELECT MAX(n.version) FROM Module n WHERE n.name = :name AND n.deleted != TRUE)"),
		@NamedQuery(name = "moduleViewLatestChildren", query = "SELECT NEW com.sixsq.slipstream.module.ModuleView(m.resourceUri, m.description, m.category, m.customVersion, m.authz) FROM Module m WHERE m.parentUri = :parent AND m.version = (SELECT MAX(c.version) FROM Module c WHERE c.name = m.name AND c.deleted != TRUE)"),
		@NamedQuery(name = "moduleViewAllVersions", query = "SELECT NEW com.sixsq.slipstream.module.ModuleVersionView(m.resourceUri, m.version, m.lastModified, m.commit, m.authz, m.category) FROM Module m WHERE m.name = :name AND m.deleted != TRUE"),
		@NamedQuery(name = "moduleAll", query = "SELECT m FROM Module m WHERE m.deleted != TRUE"),
		@NamedQuery(name = "moduleViewPublished", query = "SELECT NEW com.sixsq.slipstream.module.ModuleViewPublished(m.resourceUri, m.description, m.category, m.customVersion, m.authz, m.logoLink) FROM Module m WHERE m.published != null AND m.deleted != TRUE") })
public abstract class Module extends Parameterized<Module, ModuleParameter> implements Guarded {

	public final static String RESOURCE_URI_PREFIX = "module/";

	public final static int DEFAULT_VERSION = -1;

	private static Module loadByUri(String uri) {
		EntityManager em = PersistenceUtil.createEntityManager();
		Module m = em.find(Module.class, uri);
		Module latestVersion = loadLatest(uri);
		em.close();
		if (latestVersion != null && m != null) {
			m.setIsLatestVersion(latestVersion.version);
		}
		return m;
	}

	public static Module loadLatest(String resourceUri) {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("moduleLastVersion");
		String name = ModuleUriUtil.extractModuleNameFromResourceUri(resourceUri);
		q.setParameter("name", name);
		Module module;
		try {
			module = (Module) q.getSingleResult();
			module.setIsLatestVersion(module.version);
		} catch (NoResultException ex) {
			module = null;
		}
		em.close();
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

	public static Module loadByName(String name) {
		return load(constructResourceUri(name));
	}

	public static Module load(String uri) {
		String resourceUri = uri;
		int version = ModuleUriUtil.extractVersionFromResourceUri(resourceUri);
		Module module = (version == DEFAULT_VERSION ? loadLatest(resourceUri) : loadByUri(resourceUri));
		if (module != null) {
			// set authz to bind them
			module.getAuthz().setGuarded(module);
		}
		return module;
	}

	@SuppressWarnings("unchecked")
	public static List<Module> listAll() {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("moduleAll");
		List<Module> list = q.getResultList();
		em.close();
		return list;
	}

	@SuppressWarnings("unchecked")
	public static List<ModuleView> viewList(String resourceUri) {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("moduleViewLatestChildren");
		q.setParameter("parent",
				Module.constructResourceUri(ModuleUriUtil.extractModuleNameFromResourceUri(resourceUri)));
		List<ModuleView> list = q.getResultList();
		em.close();
		return list;
	}

	@SuppressWarnings("unchecked")
	public static List<ModuleVersionView> viewListAllVersions(String resourceUri) {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("moduleViewAllVersions");
		String name = ModuleUriUtil.extractModuleNameFromResourceUri(resourceUri);
		q.setParameter("name", name);
		List<ModuleVersionView> list = q.getResultList();
		em.close();
		return list;
	}

	@SuppressWarnings("unchecked")
	public static List<ModuleView> viewPublishedList() {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("moduleViewPublished");
		List<ModuleView> list = q.getResultList();
		em.close();
		return list;
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
	@Embedded
	private Authz authz = new Authz("Unknown", this);

	@Transient
	private Module parent = null;

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

	@Transient
	@Attribute(required = false)
	private boolean isLatestVersion;

	@Attribute(required = false)
	@Column(length = 1024)
	private String tag;

	@Element(required = false)
	@OneToOne(optional = true, cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	private Commit commit;

	@Element(required = false)
	@OneToOne(optional = true, cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	private Publish published; // to the app store

	/**
	 * Module reference is a URL.
	 * <p/>
	 * In the case "Image", the module to use as a base image from which to
	 * extract the cloud image id (e.g. AMI). It can be empty if the ImageId
	 * field is provided.
	 */
	@Attribute(required = false)
	private String moduleReferenceUri;

	/**
	 * Contains all available cloud services. Used for HTML UI.
	 */
	@Transient
	@ElementArray(required = false)
	protected String[] cloudNames;

	@Transient
	@Element(required = false)
	private RunViewList runs;

	@Attribute(required = false)
	private String logoLink;

	protected Module() {
		super();
	}

	@Override
	@ElementMap(name = "parameters", required = false, valueType = ModuleParameter.class)
	protected void setParameters(Map<String, ModuleParameter> parameters) {
		this.parameters = parameters;
	}

	@Override
	@ElementMap(name = "parameters", required = false, valueType = ModuleParameter.class)
	public Map<String, ModuleParameter> getParameters() {
		return parameters;
	}

	public Module(String name, ModuleCategory category)
			throws ValidationException {

		this.category = category;

		setName(name);
	}

	public Guarded getGuardedParent() {
		if (parent == null) {
			if (parentUri != null) {
				parent = Module.load(parentUri);
			}
		}
		return parent;
	}

	public void clearGuardedParent() {
		parent = null;
	}

	public RunViewList getRuns() {
		return runs;
	}

	public void setRuns(RunViewList runs) {
		this.runs = runs;
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

	public void setModuleReference(Module reference) throws ValidationException {
		setModuleReference(reference.getResourceUri());
	}

	public void setModuleReference(String moduleReferenceUri) throws ValidationException {
		if (moduleReferenceUri != null){
			String moduleReferenceUriVersionLess = ModuleUriUtil.extractVersionLessResourceUri(moduleReferenceUri);
			String moduleUriVersionLess = ModuleUriUtil.extractVersionLessResourceUri(getResourceUri());
			if (moduleUriVersionLess.equals(moduleReferenceUriVersionLess)) {
				throw new ValidationException("Module reference cannot be itself");
			}
		}
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
		resourceUri = Module.constructResourceUri(ModuleUriUtil.extractModuleNameFromResourceUri(resourceUri) + "/"
				+ version);
	}

	protected void setIsLatestVersion(int lastVersion) {
		this.isLatestVersion = version == lastVersion;
	}

	public void setCommit(String author, String commit) {
		this.commit = new Commit(author, commit, this);
	}

	public void setCommit(Commit commit) {
		this.commit = commit;
	}

	public Commit getCommit() {
		return commit;
	}

	public String[] getCloudNames() {
		return cloudNames;
	}

	public void setCloudNames(String[] cloudNames) {
		this.cloudNames = cloudNames;
	}

	public String getLogoLink() {
		return logoLink;
	}

	public void setLogoLink(String logoLink) {
		this.logoLink = logoLink;
	}

	public void publish() {
		published = new Publish(this);
	}

	public void unpublish() {
		published = null;
	}

	public Publish getPublished() {
		return published;
	}

	protected String computeParameterValue(String key) throws ValidationException {
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

	public abstract Module copy() throws ValidationException;

	protected Module copyTo(Module copy) throws ValidationException {
		copy = (Module) super.copyTo(copy);

		if (getCommit() != null) {
			copy.setCommit(getCommit().copy());
		}
		copy.setCustomVersion(getCustomVersion());

		copy.setCloudNames((cloudNames == null ? null : cloudNames.clone()));
		copy.setModuleReference(getModuleReference());
		copy.setTag(getTag());

		copy.setAuthz(getAuthz().copy(copy));

		return copy;
	}
}
