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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;

/**
 * Unit test see:
 * 
 * @see ImageModuleTest
 * 
 */
@Entity
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
	public static final String EXTRADISK_VOLATILE_PARAM = EXTRADISK_PARAM_PREFIX
			+ "." + EXTRADISK_NAME_VOLATILE;
	private static final String VOLATILE_DISK_VALUE_REGEX = "^[0-9]*$";
	private static final String VOLATILE_DISK_VALUE_REGEXERROR = "Integer value expected for volatile extra disk";

	@ElementList(required = false)
	@OneToMany(mappedBy = "module", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private Set<Target> targets = new HashSet<Target>();

	@ElementList(required = false)
	@OneToMany(mappedBy = "module", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private Set<Package> packages = new HashSet<Package>();

	@Element(required = false, data = true)
	@Column(length = 65536)
	private String prerecipe = "";

	@Element(required = false, data = true)
	@Column(length = 65536)
	private String recipe = "";

	@Attribute
	private Boolean isBase = false;

	private String loginUser = "root";

	private String platform = "other";

	/**
	 * Only set by resource, once a cloud service name is known. This is a hack!
	 */
	@Attribute(required = false)
	@Transient
	private String imageId;

	@OneToMany(mappedBy = "container", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@ElementList(required = false, data = true)
	private List<CloudImageIdentifier> cloudImageIdentifiers = new ArrayList<CloudImageIdentifier>();

	@Transient
	private volatile ImageModule parentModule;

	protected ImageModule() {
		super();
	}

	public ImageModule(String name) throws ValidationException,
			ConfigurationException {

		super(name, ModuleCategory.Image);

		setDefaultParameters();
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

	private void validateHasImageId(String cloudService)
			throws ValidationException {

		if (isBase) {
			validateBaseImage(cloudService, true);
			return;
		}
		if (isVirtual()) {
			extractBaseImageId(cloudService);
			return;
		}
		if (getImageId(cloudService) == null) {
			throw new ValidationException(getName()
					+ " missing an image id for cloud: " + cloudService
					+ ". Did you build it?");
		}
	}

	private void validateBaseImage(String cloudService, boolean throwOnError)
			throws ValidationException {
		CloudImageIdentifier cloudImageIdentifier = getCloudImageIdentifier(cloudService);

		if (cloudImageIdentifier == null
				|| !Parameter.hasValueSet(cloudImageIdentifier
						.getCloudMachineIdentifer())) {
			throw (new ValidationException(
					"Base image must have an image id for cloud service: "
							+ cloudService));
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
	 * Assign to the imageId attribute the value corresponding to the cloud
	 * service. This saves the client from having to dig in the module to
	 * retrieve the right image id.
	 * 
	 * @param cloudService
	 * @throws ValidationException
	 */
	public void assignImageIdFromCloudService(String cloudService)
			throws ValidationException {
		imageId = extractBaseImageId(cloudService);
	}

	/**
	 * Assign to the imageId attribute the base image id corresponding to the
	 * cloud service. This should be used when building an image, since we need
	 * to start the process from the reference image.
	 * 
	 * @param cloudService
	 * @throws ValidationException
	 */
	public void assignBaseImageIdToImageIdFromCloudService(String cloudService)
			throws ValidationException {
		imageId = getBaseImageId(cloudService);
	}

	private String getBaseImageId(String cloudService)
			throws ValidationException {

		try {
			return extractBaseImageId(cloudService);
		} catch (ValidationException e) {
			return "";
		}
	}

	/**
	 * Finds the base image id
	 * 
	 * @param cloudService
	 * @return image id
	 * @throws ValidationException
	 */
	public String extractBaseImageId(String cloudService)
			throws ValidationException {

		String imageId = getCloudImageId(cloudService);
		if (isSet(imageId)) {
			return imageId;
		}

		if (isBase()) {
			throw (new ValidationException("Missing image id for base image: "
					+ getName() + " on cloud service: " + cloudService));
		}

		ImageModule parentModule = getParentModule();
		if (parentModule == null) {
			throw (new ValidationException("Missing reference module"));
		}

		imageId = parentModule.extractBaseImageId(cloudService);
		if (!isSet(imageId)) {
			throw (new ValidationException(
					"Missing image id in reference module: "
							+ parentModule.getName() + " for cloud service: "
							+ cloudService));
		}
		return imageId;
	}

	/**
	 * @return parent module. If the module doesn't have a parent, returns null
	 * @throws ValidationException
	 */
	public ImageModule getParentModule() throws ValidationException {
		if (parentModule != null) {
			return parentModule;
		}
		if (getModuleReference() == null) {
			return null;
		}
		parentModule = (ImageModule) Module.load(getModuleReference());
		return parentModule;
	}

	private void setDefaultParameters() throws ValidationException,
			ConfigurationException {

		addMandatoryParameter(RuntimeParameter.HOSTNAME_KEY,
				RuntimeParameter.HOSTNAME_DESCRIPTION, ParameterCategory.Output);
		addMandatoryParameter(RuntimeParameter.INSTANCE_ID_KEY,
				RuntimeParameter.INSTANCE_ID_DESCRIPTION,
				ParameterCategory.Output);

		updateNetwork();
		updateExtraDisks();
	}

	private void updateNetwork() throws ValidationException {
		addMandatoryEnumParameter(NETWORK_KEY, "Network type",
				ParameterCategory.Cloud, NetworkType.getValues());
	}

	private void updateExtraDisks() throws ValidationException,
			ConfigurationException {
		addVolatileDiskParameter();
	}

	private void addVolatileDiskParameter() throws ValidationException {
		addMandatoryParameter(EXTRADISK_VOLATILE_PARAM,
				"Volatile extra disk in GB", ParameterCategory.Cloud);
	}

	private void addMandatoryParameter(String name, String description,
			ParameterCategory category) throws ValidationException {
		addMandatoryParameter(name, description, category, ParameterType.String);
	}

	private void addMandatoryEnumParameter(String name, String description,
			ParameterCategory category, List<String> enumValues)
			throws ValidationException {
		addMandatoryParameter(name, description, category, ParameterType.Enum,
				enumValues);
	}

	private void addMandatoryParameter(String name, String description,
			ParameterCategory category, ParameterType type)
			throws ValidationException {

		addMandatoryParameter(name, description, category, type, null);
	}

	private void addMandatoryParameter(String name, String description,
			ParameterCategory category, ParameterType type,
			List<String> enumValues) throws ValidationException {

		ModuleParameter parameter = new ModuleParameter(name, null,
				description, category);

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

	public void setIsBase(Boolean isBase) throws ValidationException {
		this.isBase = isBase;
	}

	public String getPreRecipe() {
		return prerecipe;
	}

	public void setPreRecipe(String prerecipe) {
		this.prerecipe = prerecipe;
	}

	public Set<Target> getTargets() {
		return targets;
	}

	public void setTargets(Set<Target> targets) {
		if (this.targets != null) {
			this.targets.clear();
			for (Target t : targets) {
				this.targets.add(t);
			}
		} else {
			this.targets = targets;
		}
	}

	public Set<Package> getPackages() {
		return packages;
	}

	public void setPackages(Set<Package> packages) {
		if (this.packages != null) {
			this.packages.clear();
			for (Package t : packages) {
				this.packages.add(t);
			}
		} else {
			this.packages = packages;
		}
	}

	public String getRecipe() {
		return recipe;
	}

	public void setRecipe(String recipe) {
		this.recipe = recipe;
	}

	@Attribute
	public String getLoginUser() {
		if (isBase()) {
			return loginUser;
		}
		if (getModuleReference() == null) {
			return "";
		}
		ImageModule parentModule = (ImageModule) Module
				.load(getModuleReference());
		if (parentModule != null) {
			return parentModule.getLoginUser();
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
	public String getPlatform() {
		if (isBase()) {
			return platform;
		}
		if (getModuleReference() == null) {
			return "";
		}
		ImageModule parentModule = (ImageModule) Module
				.load(getModuleReference());
		if (parentModule != null) {
			return parentModule.getPlatform();
		} else {
			return "";
		}
	}

	@Override
	public boolean isVirtual() {
		if (isPreRecipeEmpty() && isRecipeEmpty() && isPackagesEmpty()) {
			return true;
		} else {
			return false;
		}
	}

	private boolean isRecipeEmpty() {
		return !Parameter.hasValueSet(getRecipe());
	}

	private boolean isPreRecipeEmpty() {
		return !Parameter.hasValueSet(getPreRecipe());
	}

	private boolean isPackagesEmpty() {
		return getPackages().isEmpty() ? true : false;
	}

	public void setPackage(Package package_) {
		package_.setModule(this);
		packages.add(package_);
	}

	public ImageModule store() {
		setVersion();
		for (CloudImageIdentifier id : getCloudImageIdentifiers()) {
			id.setResourceUri(getResourceUri() + "/" + id.getCloudServiceName());
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
			getCloudImageIdentifiers().add(
					new CloudImageIdentifier(this, cloudService, imageId));
		} else {
			cloudImageIdentifier.setCloudMachineIdentifer(imageId);
		}
	}

	protected boolean isSet(String value) {
		return Parameter.hasValueSet(value);
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

		copy.setPreRecipe(getPreRecipe());
		copy.setRecipe(getRecipe());

		for (Target target : getTargets()) {
			copy.getTargets().add(target.copy());
		}

		return copy;
	}

	public void setCloudImageIdentifiers(
			List<CloudImageIdentifier> cloudImageIdentifiers) {
		this.cloudImageIdentifiers = cloudImageIdentifiers;
	}

	public List<CloudImageIdentifier> getCloudImageIdentifiers() {
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
		return cloudImageIdentifer == null ? "" : cloudImageIdentifer
				.getCloudMachineIdentifer();
	}

	/**
	 * Set image id for normal image run
	 * 
	 * @param run
	 * @return
	 * @throws ValidationException
	 */
	public static Module populateImageIdFromRun(Run run)
			throws ValidationException {

		ImageModule module = ImageModule.load(run.getModuleResourceUrl());

		module.assignBaseImageIdToImageIdFromCloudService(run.getCloudService());

		return module;
	}

	/**
	 * Set base image id for image build
	 * 
	 * @param run
	 * @return
	 * @throws ValidationException
	 */
	public static Module populateBaseImageIdFromRun(Run run, Module module)
			throws ValidationException {

		((ImageModule) module).assignBaseImageIdToImageIdFromCloudService(run
				.getCloudService());

		return module;
	}

	public static Module populateForImageRun(Run run, Module module)
			throws ValidationException {

		((ImageModule) module).assignImageIdFromCloudService(run
				.getCloudService());

		return module;
	}

	public void postDeserialization() {
		super.postDeserialization();
		for (CloudImageIdentifier c : getCloudImageIdentifiers()) {
			c.setContainer(this);
		}
	}

}
