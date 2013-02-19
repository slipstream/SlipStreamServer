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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

import org.simpleframework.xml.ElementMap;

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.run.RunFactory;

@Entity
@SuppressWarnings("serial")
public class DeploymentModule extends Module {

	@ElementMap(required = false)
	@OneToMany(cascade = CascadeType.ALL)
	private Map<String, Node> nodes = new HashMap<String, Node>();

	@SuppressWarnings("unused")
	private DeploymentModule() {
		super();
	}

	public DeploymentModule(String name) throws ValidationException {
		super(name, ModuleCategory.Deployment);
	}

	public Map<String, Node> getNodes() {
		return nodes;
	}

	public void setNodes(HashMap<String, Node> nodes) {
		this.nodes = nodes;
	}

	/**
	 * Validates the integrity of the object. For example, checks that the
	 * mapping for deployment instances is complete and no input parameter is
	 * left unresolved.
	 * 
	 * @throws ValidationException
	 */
	public void validate() throws ValidationException {

		super.validate();

		HashMap<String, ImageModule> imagemap = buildImageNameImageMap();

		validateThatAllParametersExist(imagemap);

		validateAllInputsHaveMappingOutputs(imagemap);

		for (Node n : getNodes().values()) {
			n.validate();
		}
	}

	private HashMap<String, ImageModule> buildImageNameImageMap() {

		List<ImageModule> images = new LinkedList<ImageModule>();
		for (Node node : nodes.values()) {
			images.add(node.getImage());
		}

		// Build a map of: "image qname" : "image"
		HashMap<String, ImageModule> imagemap = new HashMap<String, ImageModule>();
		for (ImageModule image : images) {
			imagemap.put(image.getName(), image);
		}

		return imagemap;
	}

	private void validateThatAllParametersExist(
			HashMap<String, ImageModule> imagemap) throws ValidationException {

		// Iterate over each node
		for (Entry<String, Node> nodeEntry : getNodes().entrySet()) {
			Node node = nodeEntry.getValue();

			validateInputParametersExistsInNodeImage(node);

			validateOutputParametersExistsInOtherNodes(imagemap, node);

			imageInputParameterPresentIfNoDefault(node);
		}

	}

	private void validateOutputParametersExistsInOtherNodes(
			HashMap<String, ImageModule> imagemap, Node node)
			throws ValidationException {

		// Check output params
		for (NodeParameter nodeParameter : node.getParameterMappings().values()) {

			if (nodeParameter.isStringValue()) {
				continue;
			}

			String nodeName = RuntimeParameter
					.extractNodeNamePart(nodeParameter.getValue());
			String paramName = RuntimeParameter
					.extractParamNamePart(nodeParameter.getValue());

			// Check that the node referring to by the oparam exists
			if (!this.getNodes().containsKey(nodeName)) {
				throw (new ValidationException(
						"Node: "
								+ node.getName()
								+ " defines an output parameter: "
								+ nodeParameter.getValue()
								+ " which referes to a node not defined in the deployment"));
			}
			if (!this.getNodes().get(nodeName).getImage().getParameters()
					.containsKey(paramName)) {
				throw (new ValidationException(
						"Failed to find output parameter: "
								+ nodeParameter.getValue()
								+ " as declared in: " + node.getName()));
			}
		}
	}

	private void validateInputParametersExistsInNodeImage(Node node)
			throws ValidationException {
		boolean foundit = false;
		for (String iparam : node.getParameterMappings().keySet()) {
			foundit = false;
			for (ModuleParameter param : node.getImage().getParameterList()) {
				if (param.getName().equals(iparam)) {
					foundit = true;
					break;
				}
			}
			if (!foundit) {
				throw (new ValidationException("Input parameter: " + iparam
						+ " doesn't exist in image: " + node.getName()));
			}
		}
	}

	private void validateAllInputsHaveMappingOutputs(
			HashMap<String, ImageModule> imagemap) throws ValidationException {
		// Iterate over each node and build a map of mappings across all images
		HashMap<String, NodeParameter> mapping = new HashMap<String, NodeParameter>();
		for (Node node : getNodes().values()) {
			if (node.getParameterMappings() != null) {
				mapping.putAll(node.getParameterMappings());
			}
		}
		for (Module image : imagemap.values()) {
			for (Parameter<Module> param : image.getParameters(
					ParameterCategory.Input.name()).values()) {
				if (!param.hasValueSet()) {
					if (!mapping.containsKey(param.getName())) {
						throw (new ValidationException(
								"Missing mapping for input parameter: "
										+ param.getName()));
					}
				}
			}
		}

	}

	private void imageInputParameterPresentIfNoDefault(Node node)
			throws ValidationException {
		for (Parameter<Module> moduleParameter : node.getImage()
				.getParameters(ParameterCategory.Input.toString()).values()) {
			if (!moduleParameter.hasValueSet()) {
				if (!node.getParameterMappings().containsKey(
						moduleParameter.getName())) {
					throw (new ValidationException("Missing input parameter "
							+ moduleParameter.getName() + " in node "
							+ node.getName()));
				}
			}
		}
	}

	public static DeploymentModule load(String uri) {
		return (DeploymentModule) Module.load(uri);
	}

	public DeploymentModule copy() throws ValidationException {

		DeploymentModule copy = (DeploymentModule) copyTo(new DeploymentModule(
				getName()));

		for (Node node : getNodes().values()) {
			copy.getNodes().put(node.getName(), node.copy());
		}

		return copy;
	}

	public static DeploymentModule populateFromRun(Run run, Module module, User user)
			throws ValidationException {

		DeploymentModule deployment = (DeploymentModule) module;

		for (Node node : deployment.getNodes().values()) {

			String nodeRuntimeParameterKeyName = run
					.nodeRuntimeParameterKeyName(node,
							RuntimeParameter.MULTIPLICITY_PARAMETER_NAME);

			if (run.getParameters().containsKey(nodeRuntimeParameterKeyName)) {
				node.setMultiplicity(run.getParameter(
						nodeRuntimeParameterKeyName).getValue());
			}

			nodeRuntimeParameterKeyName = run.nodeRuntimeParameterKeyName(node,
					RuntimeParameter.CLOUD_SERVICE_NAME);

			if (run.getParameters().containsKey(nodeRuntimeParameterKeyName)) {
				String cloudService = run.getParameter(
						nodeRuntimeParameterKeyName).getValue();
				node.setCloudService(cloudService);
				node.getImage().assignBaseImageIdToImageIdFromCloudService(
						cloudService);
			}
			
			RunFactory.resolveImageIdIfAppropriate(node.getImage(), user);
		}
		
		return deployment;
	}

}
