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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementMap;

import com.sixsq.slipstream.exceptions.ValidationException;

import flexjson.JSON;

@SuppressWarnings("serial")
public class Node extends Parameterized {

	private static final String NETWORK_KEY = ImageModule.NETWORK_KEY;
	@Attribute(required = false)
	protected ModuleCategory category;

	@Attribute
	private String name;

	@Attribute
	private int multiplicity = RuntimeParameter.MULTIPLICITY_NODE_START_INDEX;

	@Attribute(empty = "0")
	private int maxProvisioningFailures = 0;

	@Attribute
	private String cloudService = CloudImageIdentifier.DEFAULT_CLOUD_SERVICE;

	public String getCloudService() {
		return cloudService;
	}

	public void setCloudService(String cloudService) {
		this.cloudService = cloudService;
	}

	@Attribute(required = false)
	private String imageUri;

	public String getImageUri() {
		return imageUri;
	}

	public void setImageUri(String imageUri) {
		this.imageUri = imageUri;
	}

	@JSON(include = false)
	private ImageModule image;

	/**
	 * Holds the <parameter-name> and the corresponding
	 * <node-name>.<index>:<parameter-name>.
	 */
	@ElementMap(required = false)
	private Map<String, NodeParameter> parameterMappings = new ConcurrentHashMap<String, NodeParameter>();

//	@JsonExcludeField
	@JSON(include = false)
	private DeploymentModule module;

	protected Node() {
	}

	public Node(String name, String imageUri) throws ValidationException {
		this.name = name;
		this.imageUri = imageUri;
	}

	public Node(String name, ImageModule image) throws ValidationException {
		this(name, image.getResourceUri());
		this.image = image;
	}

	public DeploymentModule getModule() {
		return module;
	}

	public void setModule(DeploymentModule module) {
		this.module = module;
	}

	public void validate() throws ValidationException {
		super.validate();
		Matcher matcher = RuntimeParameter.NODE_NAME_ONLY_PATTERN.matcher(name);
		if (!matcher.matches()) {
			throwValidationException("invalid node name: " + name);
		}
		image.validate();
	}

	public int getMultiplicity() {
		return multiplicity;
	}

	public void setMultiplicity(String multiplicity) throws ValidationException {
		int parsedMultiplicity;
		try {
			parsedMultiplicity = Integer.parseInt(multiplicity);
		} catch (NumberFormatException ex) {
			throw (new ValidationException("Invalid multiplicity value"));
		}
		setMultiplicity(parsedMultiplicity);
	}

	public void setMultiplicity(int multiplicity) throws ValidationException {
		if (multiplicity < 0) {
			throw (new ValidationException("Invalid multiplicity, it must be positive"));
		}
		this.multiplicity = multiplicity;
	}

	public void setMaxProvisioningFailures(int value) {
		this.maxProvisioningFailures = value;
	}

	public int getMaxProvisioningFailures() {
		return this.maxProvisioningFailures;
	}

	@Attribute(required = false)
	public void setNetwork(String network) {
	}

	@Attribute(required = false)
	public String getNetwork() throws ValidationException {
		return extractParameterWithOverride(NETWORK_KEY);
	}

	/**
	 * Look for a value in the local parameter list, otherwise return the value
	 * from the image parameter list
	 */
	private String extractParameterWithOverride(String key) throws ValidationException {
		ImageModule image = getImage();
		if (image != null) {
			return getParameterValue(key, image.getParameterValue(key, null));
		} else {
			// The image is missing, but this will be picked-up when running
			return null;
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	public Map<String, NodeParameter> getParameterMappings() {
		return parameterMappings;
	}

	/**
	 * Assembled notes. Includes notes from inherited images.
	 */
	@ElementList(required = false, entry = "note")
	public String[] getNotes() {
		List<String> notes = new ArrayList<String>();
		ImageModule image = getImage();
		if (image != null) {
			notes.addAll(Arrays.asList(image.getNotes()));
		}
		return notes.toArray(new String[0]);
	}

	@ElementList(required = false, entry = "note")
	private void setNotes(String[] notes) {
	}

	public void setParameterMappings(Map<String, NodeParameter> parameterMappings) {
		this.parameterMappings = parameterMappings;
	}

	public void setParameterMapping(NodeParameter nodeParameter, DeploymentModule deployment)
			throws ValidationException {

		validateMapping(nodeParameter, deployment);

		getParameterMappings().put(nodeParameter.getName(), nodeParameter);
	}

	private void validateMapping(NodeParameter nodeParameter, DeploymentModule deployment) throws ValidationException {
		ModuleParameter inputParameter = (ModuleParameter) image.getParameter(nodeParameter.getName());
		if (!ParameterCategory.Input.name().equals(inputParameter.getCategory())) {
			throw new ValidationException("Input parameter " + nodeParameter.getName() + " not Input category");
		}

		if (nodeParameter.isStringValue()) {
			return;
		}

		ModuleParameter outputParameter = extractModuleParameterFromNodeString(nodeParameter.getValue(), deployment);

		if (!ParameterCategory.Output.name().equals(outputParameter.getCategory())) {
			throw new ValidationException("Output parameter " + outputParameter.getName() + " not Output category");
		}
	}

	private ModuleParameter extractModuleParameterFromNodeString(String fullyQualifiedParameterName,
			DeploymentModule deployment) {
		String[] parts = fullyQualifiedParameterName.split(":");
		String nodeName = parts[0];
		String paramName = parts[1];

		return (ModuleParameter) deployment.getNodes().get(nodeName).getImage().getParameter(paramName);
	}

	@Element(required = false)
	public ImageModule getImage() {
		if (image == null) {
			image = (ImageModule) ImageModule.load(imageUri);
		}
		return image;
	}

	@Element(required = false)
	public void setImage(ImageModule image) {
		this.image = image;
	}

	@Override
	public String getResourceUri() {
		return null;
	}

	public void setParameterMapping(NodeParameter parameter) {
		parameter.setContainer(this);
		this.getParameterMappings().put(parameter.getName(), parameter);
	}

	public Node copy() throws ValidationException {
		Node copy = new Node(getName(), getImageUri());
		copy = (Node) copyTo(copy);
		copy.setCloudService(getCloudService());
		copy.setMultiplicity(getMultiplicity());
		copy.setNetwork(getNetwork());
		return copy;
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
}
