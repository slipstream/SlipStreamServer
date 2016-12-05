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
import java.util.*;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.simpleframework.xml.*;

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
public class ImageModule extends TargetContainerModule {

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

	private static final String CPU_PARAM = "cpu.nb";
	private static final String RAM_PARAM = "ram.GB";
	private static final String DISK_PARAM = "disk.GB";

	private static class BuildState implements Serializable {
		@Attribute
		public final String moduleUri;

		@Attribute
		public final String builtOn;

		public BuildState(String moduleUri, List<String> clouds) {
			this.moduleUri = moduleUri;
			this.builtOn = clouds.stream().collect(Collectors.joining(","));
		}
	}

	@ElementList(required = false)
	@OneToMany(mappedBy = "module", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private Set<Package> packages = new HashSet<Package>();

	@Transient
	private Set<Package> packagesExpanded;

	@Transient
	protected Map<String, ModuleParameter> inputParametersExpanded;

	@Transient
	protected Map<String, ModuleParameter> outputParametersExpanded;

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

	@OneToMany(mappedBy = "container", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@ElementList(required = false, data = true)
	private Set<CloudImageIdentifier> cloudImageIdentifiers = new HashSet<CloudImageIdentifier>();

	@Transient
	private Set<BuildState> buildStates;

	@Transient
	private volatile ImageModule parentModule;

	protected ImageModule() {
		super();
	}

	public ImageModule(String name) throws ValidationException, ConfigurationException {

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

	public List<String> getCloudNamesWhereBuilt() {
		List<String> cloudNames = new LinkedList<>();

		if (!isVirtual() && !isBase()) {
			String cloudId = null;
			for (CloudImageIdentifier c : getCloudImageIdentifiers()) {
				cloudId = c.getCloudMachineIdentifer();
				if (cloudId != null) {
					cloudNames.add(c.getCloudServiceName());
				}
			}
		}

		return cloudNames;
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

		ImageModule parentModule = getParentModule();
		if (parentModule == null) {
			throw (new ValidationException("Missing reference module"));
		}

		imageId = parentModule.extractBaseImageId(cloudService);
		if (!isSet(imageId)) {
			throw (new ValidationException("Missing image id in reference module: " + parentModule.getName()
					+ " for cloud service: " + cloudService));
		}
		return imageId;
	}

	/**
	 * This method will look into the parent image if the parameter doesn't exist, not if it's empty.
	 * @return the value of the parameter.
	 * @throws ValidationException if the parameter doesn't exist in the image hierarchy.
	 */
	public ModuleParameter extractParameter(String parameterName) throws ValidationException {

		ModuleParameter parameter = getParameter(parameterName);
		if (parameter == null) {
			throw (new ValidationException("Parameter " + parameterName + " not found."));
		}
		return parameter;
	}

	/**
	 * Override the method in {@link Parameterized} to add recursivity.
	 * This method will look into the parent image if the parameter doesn't exist, not if it's empty.
	 * @return the value of the parameter or null if it doesn't exist in the image hierarchy.
	 */
	@Override
	public ModuleParameter getParameter(String name) {
		ModuleParameter parameter = super.getParameter(name);
		if (parameter != null) {
			return parameter;
		}

		ImageModule parentModule = getParentModule();
		if (parentModule == null) {
			return null;
		}

		return parentModule.getParameter(name);
	}

	@ElementMap(required = false)
	public Map<String, ModuleParameter> getInputParametersExpanded() {
		if (inputParametersExpanded == null) {
			inputParametersExpanded = new HashMap<>();
			findAndAddInheritedApplicationParameters(inputParametersExpanded, this, ParameterCategory.Input);
		}
		return inputParametersExpanded;
	}

	@ElementMap(required = false)
	public void setInputParametersExpanded(Map<String, ModuleParameter> parameters) {

	}

	@ElementMap(required = false)
	public Map<String, ModuleParameter> getOutputParametersExpanded() {
		if (outputParametersExpanded == null) {
			outputParametersExpanded = new HashMap<>();
			findAndAddInheritedApplicationParameters(outputParametersExpanded, this, ParameterCategory.Output);
		}
		return outputParametersExpanded;
	}

	@ElementMap(required = false)
	public void setOutputParametersExpanded(Map<String, ModuleParameter> parameters) {

	}

	private void findAndAddInheritedApplicationParameters(Map<String, ModuleParameter> params, ImageModule image,
														  ParameterCategory type) {

		for (Map.Entry<String, ModuleParameter> entry : image.getParameters().entrySet()) {
			String parameterName = entry.getKey();
			ModuleParameter parameter = entry.getValue();

			String category = parameter.getCategory();

			if (type.toString().equals(category)) {
				params.putIfAbsent(parameterName, parameter);
			}
		}

		ImageModule parent = image.getParentModule();
		if (parent != null) {
			findAndAddInheritedApplicationParameters(params, parent, type);
		}
	}

	/**
	 * @return parent module. If the module doesn't have a parent, returns null
	 */
	public ImageModule getParentModule() {
		if (parentModule != null) {
			return parentModule;
		}
		if (getModuleReference() == null) {
			return null;
		}
		parentModule = (ImageModule) Module.load(getModuleReference());
		return parentModule;
	}

	private void setDefaultParameters() throws ValidationException, ConfigurationException {

		addMandatoryParameter(RuntimeParameter.HOSTNAME_KEY, RuntimeParameter.HOSTNAME_DESCRIPTION,
				ParameterCategory.Output);
		addMandatoryParameter(RuntimeParameter.INSTANCE_ID_KEY, RuntimeParameter.INSTANCE_ID_DESCRIPTION,
				ParameterCategory.Output);

		updateCPU();
		updateRAM();
		updateDisk();

		updateExtraDisks();
		updateNetwork();
	}

	private void updateNetwork() throws ValidationException {
		addMandatoryEnumParameter(NETWORK_KEY, "Network type", ParameterCategory.Cloud, NetworkType.getValues());
	}

	private void updateExtraDisks() throws ValidationException, ConfigurationException {
		addVolatileDiskParameter();
	}

	private void updateCPU() throws ValidationException {
        addCPUParameter();
    }

	private void updateRAM() throws ValidationException {
		addRAMParameter();
	}

	private void updateDisk() throws ValidationException {
		addDiskParameter();
	}

	private void addCPUParameter() throws ValidationException {
        addMandatoryParameter(CPU_PARAM, "Number of CPUs", ParameterCategory.Cloud);
    }

	private void addRAMParameter() throws ValidationException {
		addMandatoryParameter(RAM_PARAM, "RAM in GB", ParameterCategory.Cloud);
	}

	private void addDiskParameter() throws ValidationException {
		addMandatoryParameter(DISK_PARAM, "Disk in GB", ParameterCategory.Cloud);
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

	public void setIsBase(Boolean isBase) throws ValidationException {
		this.isBase = isBase;
	}

	public String getPreRecipe() {
		return prerecipe;
	}

	public void setPreRecipe(String prerecipe) {
		this.prerecipe = prerecipe;
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

	public String getRecipe() {
		return recipe;
	}

	public void setRecipe(String recipe) {
		this.recipe = recipe;
	}

	@Attribute
	public String getLoginUser() throws ValidationException {
		if (isBase()) {
			return loginUser;
		}
		if (getModuleReference() == null) {
			return "";
		}
		if (getParentModule() != null) {
			return getParentModule().getLoginUser();
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
		if (getModuleReference() == null) {
			return "";
		}
		if (getParentModule() != null) {
			return getParentModule().getPlatform();
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

	public ImageModule store() {
		setVersion();
		setModuleToTargets();

		if (packages != null) {
			for (Package p : packages) {
				p.setModule(this);
			}
		}

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
		String moduleReference = getModuleReference();
		if (moduleReference != null) {
			ImageModule parent = load(moduleReference);
			if (parent != null) {
			    notes.addAll(Arrays.asList(parent.getNotes()));
			}
		}
		if (getNote() != null) {
			notes.add(getNote());
		}
		return notes.toArray(new String[notes.size()]);
	}

	/**
	 * Empty setter needed for serializer on a read only property
	 *
	 */
	@Transient
	@ElementArray(required = false, entry = "note")
	private void setNotes(String[] notes) {
	}

	@ElementList(required = false, entry = "packageExpanded")
	public Set<Package> getPackagesExpanded() {

		if(packagesExpanded == null) {
			packagesExpanded = new HashSet<>();
			findAndAddPackages(this);
		}

		return packagesExpanded;
	}

	@ElementList(required = false, entry = "packageExpanded")
	public void setPackagesExpanded(Set<Package> packagesExpanded) {
	}

	private void findAndAddPackages(ImageModule image) {
		ImageModule parent = image.getParentModule();
		if (parent != null)
			findAndAddPackages(parent);

		packagesExpanded.addAll(image.getPackages());
	}

	@ElementList(required = false)
	public Set<BuildState> getBuildStates() {

		if (buildStates == null) {
			buildStates = new HashSet<>();
			findAndAddBuildStates(this);
		}

		return buildStates;
	}

	@ElementList(required = false)
	public void setBuildStates(Set<BuildState> buildStates) { }

	private void findAndAddBuildStates(ImageModule image) {
		ImageModule parent = image.getParentModule();
		if (parent != null)
			findAndAddBuildStates(parent);

		buildStates.add(new BuildState(image.getResourceUri(), image.getCloudNamesWhereBuilt()));
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
		copy.setPlacementPolicy(getPlacementPolicy());

		for (Target target : getTargets()) {
			copy.getTargets().add(target.copy());
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

	@Override
	protected void expandTargets() {
		super.expandTargets();

		targetsExpanded.add(new TargetExpanded(this, TargetExpanded.BuildRecipe.PRE_RECIPE));
		targetsExpanded.add(new TargetExpanded(this, TargetExpanded.BuildRecipe.RECIPE));
	}

	private boolean nullOrEmpty(String s) {
		return s == null || s.isEmpty();
	}

	private String andPolicies(String policy1, String policy2) {
		if (nullOrEmpty(policy1) && nullOrEmpty(policy2)) {
			return null;
		} else if (nullOrEmpty(policy1)) {
			return policy2;
		} else if (nullOrEmpty(policy2)) {
			return policy1;
		} else {
			return "(" + policy1 + ") and (" + policy2 + ")";
		}
	}

	@Override
	public Map<String, String> placementPoliciesPerComponent() {

		Map<String, String> result = new HashMap<>();

		String resultPolicy = null;
		if (getModuleReference() == null) {
			resultPolicy = getPlacementPolicy();
		}
		if (getParentModule() != null) {
			String policy = getPlacementPolicy();
			String parentPolicy = null;
			Map<String, String> parentPlacementPolicies = getParentModule().placementPoliciesPerComponent();
			if (parentPlacementPolicies != null && !parentPlacementPolicies.isEmpty()) {
				parentPolicy = (String) parentPlacementPolicies.values().toArray()[0];
			}

			resultPolicy = andPolicies(parentPolicy, policy);
		}

		result.put(getResourceUri(), resultPolicy);
		return result;
	}
}
