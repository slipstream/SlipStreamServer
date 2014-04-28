package com.sixsq.slipstream.module;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.restlet.data.Form;

import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Node;
import com.sixsq.slipstream.persistence.NodeParameter;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.User;

/**
 * Unit test see
 * 
 * @see DeploymentFormProcesorTest
 * 
 */
public class DeploymentFormProcessor extends ModuleFormProcessor {

	public DeploymentFormProcessor(User user) {
		super(user);
	}

	@Override
	public void parseForm() throws ValidationException, NotFoundException {

		super.parseForm();

		parseNodeMapping();

		setPostProcessingScript();

		validate();

	}

	private void parseNodeMapping() throws ValidationException {

		DeploymentModule module = castToModule();
		module.setNodes(new HashMap<String, Node>());

		Set<String> formitems = getForm().getNames();

		// will contain the mapping between nodeindex and mappings
		// (parameter mapping)
		HashMap<String, List<String>> nodeMappingList = new HashMap<String, List<String>>();
		// will contain the mapping between nodeindex and node shortname
		HashMap<String, String> indexToNode = new HashMap<String, String>();

		// items should be encoded as:
		// node--[nodeindex]--mappingstable--[mappingindex]--[value]
		// Or node--[index]--shortname
		// We start by extracting the node indices and the mappings
		for (String item : formitems.toArray(new String[0])) {
			if (item.startsWith("node--")) {
				String nodeindex = item.split("--")[1]; // get the nodeindex
				// Extract the node indexes
				if (item.endsWith("--shortname")) {
					String shortName = getForm().getFirstValue(item);
					indexToNode.put(nodeindex, shortName);
					List<String> mapping = nodeMappingList.get(nodeindex);
					if (mapping == null) {
						mapping = new ArrayList<String>();
					}
					nodeMappingList.put(nodeindex, mapping);
					continue;
				}
				if (item.contains("--mappingtable--")) {
					List<String> mapping = nodeMappingList.get(nodeindex);
					if (mapping == null) {
						mapping = new ArrayList<String>();
					}
					String mappingIndex = item.split("--")[3];
					mapping.add(mappingIndex);
					nodeMappingList.put(nodeindex, mapping);
				}
			}
		}

		// Iterate over the local structure and extract the corresponding
		// data from the form
		for (Entry<String, List<String>> nodeentry : nodeMappingList.entrySet()) {
			String nodeindex = nodeentry.getKey();
			String shortname = getForm().getFirstValue(
					"node--" + nodeindex + "--shortname");

			if (shortname == null) {
				throw (new ValidationException("Missing node shortname"));
			}

			Node node = createNode(nodeindex, shortname);

			if (module.getNodes().containsKey(shortname)) {
				throw (new ValidationException(
						"Node short names must be unique. '"
								+ shortname.replace("<", "&lt;")
								+ "' is already defined"));
			}

			for (String mappingindex : nodeentry.getValue()) {
				String input = getForm().getFirstValue(
						"node--" + nodeindex + "--mappingtable--"
								+ mappingindex + "--input");
				if (input == null) {
					throw (new ValidationException("Node " + node.getName()
							+ " contains an empty input parameter"));
				}
				String output = getForm().getFirstValue(
						"node--" + nodeindex + "--mappingtable--"
								+ mappingindex + "--output");
				if (output == null) {
					throw (new ValidationException("Node " + node.getName()
							+ " is missing a linked parameter for " + input));
				}
				if (output.startsWith(shortname
						+ RuntimeParameter.NODE_PROPERTY_SEPARATOR)) {
					throw (new ValidationException(
							"Output parameters cannot refer to their own node"));
				}
				node.setParameterMapping(new NodeParameter(input, output, ""));
			}

			setMultiplicity(getForm(), nodeindex, node);

			setCloudService(getForm(), nodeindex, node);

			module.getNodes().put(shortname, node);
		}
	}

	private Node createNode(String nodeindex, String shortname)
			throws ValidationException {
		String imagelink = getForm().getFirstValue(
				"node--" + nodeindex + "--imagelink");

		return new Node(shortname, imagelink);
	}

	private void setPostProcessingScript() throws ValidationException {

		Form form = getForm();
		DeploymentModule module = castToModule();
		Set<String> formitems = getForm().getNames();

		// Look for the post-processing entry
		for (String item : formitems.toArray(new String[0])) {
			if (item.equals("post-processing")) {
				ModuleParameter parameter = new ModuleParameter(item,
						form.getFirstValue(item), "Post-Processing script");
				module.setParameter(parameter);
			}
		}
	}

	private DeploymentModule castToModule() {
		return (DeploymentModule) getParametrized();
	}

	private void setCloudService(Form form, String nodeindex, Node node) {
		String cloudServiceValue = form.getFirstValue("node--" + nodeindex
				+ "--cloudservice--value", "default");
		node.setCloudService(cloudServiceValue);
	}

	private void setMultiplicity(Form form, String nodeindex, Node node)
			throws ValidationException {

		String multiplicityValue = form.getFirstValue("node--" + nodeindex
				+ "--multiplicity--value", "1");
		node.setMultiplicity(parseMultiplicity(multiplicityValue));
	}

	private int parseMultiplicity(String multiplicityValue)
			throws ValidationException {
		int multiplicity = 0;
		try {
			multiplicity = Integer.parseInt(multiplicityValue);
		} catch (NumberFormatException e) {
			throw new ValidationException(
					"Field multiplicity must be a valid integer, got: "
							+ multiplicityValue);
		}
		return multiplicity;
	}

	@Override
	protected Module getOrCreateParameterized(String name)
			throws ValidationException {
		Module loaded = load(name);
		return loaded == null ? new DeploymentModule(name) : loaded;
	}

	protected void validate() throws ValidationException {
		castToModule().validate();
	}
}
