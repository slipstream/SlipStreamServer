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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.simpleframework.xml.*;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.util.SerializationUtil;

import flexjson.JSON;

/**
 * Unit test see:
 *
 * @see ImageModuleTest
 *
 */
@Entity
@Root(name = "Image")
@SuppressWarnings("serial")
public class ImageModule extends Module {

	public static final String INSTANCE_TYPE_KEY = "instance.type";
	public static final String INSTANCE_TYPE_INHERITED = "inherited";

	public static final String RAM_KEY = "ram";
	public static final String CPU_KEY = "cpu";
	public static final String SMP_KEY = "smp";
	public static final String NETWORK_KEY = "network";
	public static final String LOGINPASSWORD_KEY = "login.password";

	public static final String EXTRADISK_PARAM_PREFIX = "extra.disk";
	private static final String EXTRADISK_NAME_VOLATILE = "volatile";
	public static final String EXTRADISK_VOLATILE_PARAM = EXTRADISK_PARAM_PREFIX + "." + EXTRADISK_NAME_VOLATILE;
	private static final String VOLATILE_DISK_VALUE_REGEX = "^[0-9]*$";
	private static final String VOLATILE_DISK_VALUE_REGEXERROR = "Integer value expected for volatile extra disk";

	@Transient
	@ElementMap(required = false)
	private Map<String, Target> targets = new HashMap<String, Target>();

	@Transient
	@ElementList(required = false)
	private Set<Package> packages = new HashSet<Package>();

	@Transient
	@Attribute
	private Boolean isBase = false;

	@Transient
	private String loginUser = "root";

	@Transient
	private String platform = "other";

	@OneToMany(mappedBy = "container", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@ElementList(required = false, data = true)
	private Set<CloudImageIdentifier> cloudImageIdentifiers = new HashSet<CloudImageIdentifier>();

	@Transient
	private ImageModule moduleReference;

	protected ImageModule() {
		super(ModuleCategory.Image);
	}

	public ImageModule(String name) throws ValidationException, ConfigurationException {
		super(name, ModuleCategory.Image);
		setDefaultParameters();
	}

	public Module fromJson(String json) throws SlipStreamClientException {
		return (Module) SerializationUtil.fromJson(json, ImageModule.class, createDeserializer());
	}

	/**
	 * Validate for an image run (as opposed to a build or as part of a
	 * deployment).
	 *
	 * @param cloudService
	 * @throws ValidationException
	 */
	public void validateForRun(String cloudService) throws ValidationException {

		validateHasImageId(cloudService);

		validateExtraDisksParameters();
	}

	private void validateExtraDisksParameters() throws ValidationException {
		validateExtraVolatileDiskValue();
	}

	private void validateExtraVolatileDiskValue() throws ValidationException {
		String paramValue = getParameterValue(EXTRADISK_VOLATILE_PARAM, "");
		if (!paramValue.matches(VOLATILE_DISK_VALUE_REGEX))
			throw (new ValidationException(VOLATILE_DISK_VALUE_REGEXERROR));
	}

	private void validateHasImageId(String cloudService) throws ValidationException {

		if (isBase) {
			validateBaseImage(cloudService, true);
			return;
		}
		if (isVirtual()) {
			extractBaseImageId(cloudService);
			return;
		}
	}

	public boolean hasToRunBuildRecipes(String cloudService) {
		return !isVirtual() && getImageId(cloudService) == null;
	}

	private void validateBaseImage(String cloudService, boolean throwOnError) throws ValidationException {
		CloudImageIdentifier cloudImageIdentifier = getCloudImageIdentifier(cloudService);

		if (cloudImageIdentifier == null || !Parameter.hasValueSet(cloudImageIdentifier.getCloudMachineIdentifer())) {
			throw (new ValidationException("Base image must have an image id for cloud service: " + cloudService));
		}
	}

	public String getImageId(String cloudService) {
		CloudImageIdentifier cloudImageId = null;
		for (CloudImageIdentifier c : getCloudImageIdentifiers()) {
			if (cloudService.equals(c.getCloudServiceName())) {
				cloudImageId = c;
				break;
			}
		}
		String id = null;
		if (cloudImageId != null) {
			id = cloudImageId.getCloudMachineIdentifer();
		}
		return id;
	}

	/**
	 * Finds the base image id
	 *
	 * @param cloudService
	 * @return image id
	 * @throws ValidationException
	 */
	public String extractBaseImageId(String cloudService) throws ValidationException {

		String imageId = getCloudImageId(cloudService);
		if (isSet(imageId)) {
			return imageId;
		}

		if (isBase()) {
			throw (new ValidationException("Missing image id for base image: " + getName() + " on cloud service: "
					+ cloudService));
		}

		ImageModule referenceModule = getModuleReference();
		if (referenceModule == null) {
			throw (new ValidationException("Missing reference module"));
		}

		imageId = referenceModule.extractBaseImageId(cloudService);
		if (!isSet(imageId)) {
			throw (new ValidationException("Missing image id in reference module: " + referenceModule.getName()
					+ " for cloud service: " + cloudService));
		}
		return imageId;
	}

	@JSON(include = false)
	private ImageModule getModuleReference() {
		if (moduleReference == null && getModuleReferenceUri() != null) {
			moduleReference = (ImageModule) Module.load(getModuleReferenceUri());
		}
		return moduleReference;
	}

	private void setDefaultParameters() throws ValidationException, ConfigurationException {

		addMandatoryParameter(RuntimeParameter.HOSTNAME_KEY, RuntimeParameter.HOSTNAME_DESCRIPTION,
				ParameterCategory.Output);
		addMandatoryParameter(RuntimeParameter.INSTANCE_ID_KEY, RuntimeParameter.INSTANCE_ID_DESCRIPTION,
				ParameterCategory.Output);

		updateNetwork();
		updateExtraDisks();
	}

	private void updateNetwork() throws ValidationException {
		addMandatoryEnumParameter(NETWORK_KEY, "Network type", ParameterCategory.Cloud, NetworkType.getValues());
	}

	private void updateExtraDisks() throws ValidationException, ConfigurationException {
		addVolatileDiskParameter();
	}

	private void addVolatileDiskParameter() throws ValidationException {
		addMandatoryParameter(EXTRADISK_VOLATILE_PARAM, "Volatile extra disk in GB", ParameterCategory.Cloud);
	}

	private void addMandatoryParameter(String name, String description, ParameterCategory category)
			throws ValidationException {
		addMandatoryParameter(name, description, category, ParameterType.String);
	}

	private void addMandatoryEnumParameter(String name, String description, ParameterCategory category,
			List<String> enumValues) throws ValidationException {
		addMandatoryParameter(name, description, category, ParameterType.Enum, enumValues);
	}

	private void addMandatoryParameter(String name, String description, ParameterCategory category, ParameterType type)
			throws ValidationException {

		addMandatoryParameter(name, description, category, type, null);
	}

	private void addMandatoryParameter(String name, String description, ParameterCategory category, ParameterType type,
			List<String> enumValues) throws ValidationException {

		ModuleParameter parameter = new ModuleParameter(name, null, description, category);

		parameter.setMandatory(true);
		parameter.setType(type);

		if (enumValues != null) {
			parameter.setEnumValues(enumValues);
			parameter.setValue(enumValues.get(0));
		}

		setParameter(parameter);
	}

	public Boolean isBase() {
		return isBase == null ? false : isBase;
	}

	// Flexjson is not smart enough to use the isBase accessor
	public Boolean getIsBase() {
		return isBase();
	}

	public void setIsBase(Boolean isBase) throws ValidationException {
		this.isBase = isBase;
	}

	public Map<String, Target> getTargets() {
		return targets;
	}

	public void setTargets(Map<String, Target> targets) {
		this.targets.clear();
		for (Target t : targets.values()) {
			setTarget(t);
		}
	}

	public void setTarget(Target target) {
		target.setModule(this);
        targets.put(target.getName(), target);
	}

	public Set<Package> getPackages() {
		return packages;
	}

	public void setPackages(Set<Package> packages) {
		this.packages.clear();
		for (Package p : packages) {
			setPackage(p);
		}
	}

	public void setPackage(Package package_) {
		package_.setModule(this);
		packages.add(package_);
	}

	@Attribute
	public String getLoginUser() throws ValidationException {
		if (isBase()) {
			return loginUser;
		}
		if (getModuleReference() != null) {
			return getModuleReference().getLoginUser();
		} else {
			return "";
		}
	}

	@Attribute
	public void setLoginUser(String loginUser) {
		this.loginUser = loginUser;
	}

	@Attribute
	public void setPlatform(String platform) {
		this.platform = platform;
	}

	@Attribute
	public String getPlatform() throws ValidationException {
		if (isBase()) {
			return platform;
		}
		String platform = "";
		if (getModuleReference() != null) {
			platform = getModuleReference().getPlatform();
		}
		return platform;
	}

	@Override
	public boolean isVirtual() {
		if (isPreRecipeEmpty() && isRecipeEmpty() && isPackagesEmpty()) {
			return true;
		} else {
			return false;
		}
	}

	@JSON(include = false)
	private boolean isRecipeEmpty() {
		return !isTargetSet("recipe");
	}

	@JSON(include = false)
	private boolean isPreRecipeEmpty() {
		return !isTargetSet("prerecipe");
	}

	@JSON(include = false)
	private boolean isTargetSet(String name) {
		Target target = targets.get(name);
		return target != null && target.isTargetSet();
	}

	@JSON(include = false)
	private boolean isPackagesEmpty() {
		return getPackages().isEmpty() ? true : false;
	}

	public ImageModule store() {
		setVersion();
		if (targets != null) {
			for (Target t : targets.values()) {
				t.setModule(this);
			}
		}
		if (packages != null) {
			for (Package p : packages) {
				p.setModule(this);
			}
		}
		for (CloudImageIdentifier id : getCloudImageIdentifiers()) {
			id.setId(getId() + "/" + id.getCloudServiceName());
		}
		return (ImageModule) store(false);
	}

	public static ImageModule load(String uri) {
		return (ImageModule) Module.load(uri);
	}

	public void setImageId(String imageId, String cloudService) {
		if (!isSet(imageId)) {
			return;
		}
		if (!isSet(cloudService)) {
			return;
		}
		CloudImageIdentifier cloudImageIdentifier = getCloudImageIdentifier(cloudService);
		if (cloudImageIdentifier == null) {
			getCloudImageIdentifiers().add(new CloudImageIdentifier(this, cloudService, imageId));
		} else {
			cloudImageIdentifier.setCloudMachineIdentifer(imageId);
		}
	}

	protected boolean isSet(String value) {
		return Parameter.hasValueSet(value);
	}

	/**
	 * Assembled notes. Includes notes from inherited images.
	 */
	@Transient
	@ElementArray(required = false, entry = "note")
	public String[] getNotes() {
		List<String> notes = new ArrayList<String>();
		String moduleReference = getModuleReferenceUri();
		if (moduleReference != null) {
			ImageModule parent = load(moduleReference);
			if (parent != null) {
			    notes.addAll(Arrays.asList(parent.getNotes()));
			}
		}
		if (getNote() != null) {
			notes.add(getNote());
		}
		return notes.toArray(new String[0]);
	}

	/**
	 * Empty setter needed for serializer on a read only property
	 *
	 */
	@Transient
	@ElementArray(required = false, entry = "note")
	private void setNotes(String[] notes) {
	}

	public String getInheritedDefaultParameterValue(String parameterName) throws ValidationException {
		String defaultValue = "";
		ModuleParameter p = null;
		if (getModuleReference() != null) {
			p = (ModuleParameter) getModuleReference().getParameter(parameterName);
		}
		if (p != null) {
			defaultValue = p.getDefaultValue();
		}
		return defaultValue;
	}

	public ImageModule copy() throws ValidationException {

		ImageModule copy = (ImageModule) copyTo(new ImageModule(getName()));

		for (CloudImageIdentifier cii : getCloudImageIdentifiers()) {
			cii.copyTo(copy);
		}

		copy.setIsBase(isBase());
		copy.setLoginUser(getLoginUser());

		for (Package package_ : getPackages()) {
			copy.getPackages().add(package_.copy());
		}

		copy.setPlatform(getPlatform());

		for (Target target : getTargets().values()) {
			copy.getTargets().put(target.getName(), target.copy());
		}

		return copy;
	}

	public void setCloudImageIdentifiers(Set<CloudImageIdentifier> cloudImageIdentifiers) {
		this.cloudImageIdentifiers = cloudImageIdentifiers;
	}

	public Set<CloudImageIdentifier> getCloudImageIdentifiers() {
		return cloudImageIdentifiers;
	}

	public CloudImageIdentifier getCloudImageIdentifier(String cloudService) {
		// TODO: turn cloudImageIdentifiers into a map?
		CloudImageIdentifier cloudImageIdentifier = null;
		for (CloudImageIdentifier c : cloudImageIdentifiers) {
			if (cloudService.equals(c.getCloudServiceName())) {
				cloudImageIdentifier = c;
				break;
			}
		}
		return cloudImageIdentifier;
	}

	public String getCloudImageId(String cloudService) {
		CloudImageIdentifier cloudImageIdentifer = getCloudImageIdentifier(cloudService);
		return cloudImageIdentifer == null ? "" : cloudImageIdentifer.getCloudMachineIdentifer();
	}

	public void postDeserialization() {
		super.postDeserialization();
		for (CloudImageIdentifier c : getCloudImageIdentifiers()) {
			c.setContainer(this);
		}
	}

}
