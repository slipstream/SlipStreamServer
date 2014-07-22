package com.sixsq.slipstream.run;

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

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.commons.lang.StringUtils;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.DeploymentFactory;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.Node;
import com.sixsq.slipstream.persistence.NodeParameter;
import com.sixsq.slipstream.persistence.PersistenceUtil;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunParameter;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.Vm;
import com.sixsq.slipstream.statemachine.StateMachine;
import com.sixsq.slipstream.statemachine.States;

public class RunNodeResource extends RunBaseResource {

	private final static String NUMBER_INSTANCES_ADD_FORM_PARAM = "n";
	private final static String NUMBER_INSTANCES_ADD_DEFAULT = "1";

	private final static String INSTANCE_IDS_REMOVE_FORM_PARAM = "ids";

	private final static List<Method> IGNORE_ABORT_HTTP_METHODS = new ArrayList<Method>(
			Arrays.asList(Method.GET));

	private String nodename;

	private String nodeMultiplicityRunParam;
	private String nodeMultiplicityRuntimeParam;
	private String nodeIndicesRuntimeParam;

	@Override
	public void initializeSubResource() throws ResourceException {

		parseRequest();

		raiseConflictIfAbortIsSet();

		initNodeParameters();
	}

	private void parseRequest() {
		extractAndSetIgnoreAbort();

		nodename = getAttribute("node");
	}

	private void initNodeParameters() {
		nodeMultiplicityRunParam = nodename + Run.NODE_NAME_PARAMETER_SEPARATOR
				+ RuntimeParameter.MULTIPLICITY_PARAMETER_NAME;
		nodeMultiplicityRuntimeParam = nodename
				+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
				+ RuntimeParameter.MULTIPLICITY_PARAMETER_NAME;
		nodeIndicesRuntimeParam = nodename
				+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
				+ RuntimeParameter.IDS_PARAMETER_NAME;
	}

	@Get
	public Representation represent(Representation entity) {
		Run run = Run.loadFromUuid(getUuid());
		List<String> instanceNames = run.getNodeInstanceNames(nodename);
		return new StringRepresentation(StringUtils.join(instanceNames, ","), MediaType.TEXT_PLAIN);
	}

	@Post
	public Representation addNodeInstances(Representation entity)
			throws ResourceException {
		Representation result = null;
		try {
			result = addNodeInstancesInTransaction(entity);
		} catch (ResourceException e) {
			throw e;
		} catch (SlipStreamClientException e) {
			throwClientConflicError(e.getMessage());
		} catch (SlipStreamException e) {
			throwServerError(e.getMessage());
		} catch (Exception e) {
			throwServerError(e.getMessage());
		}
		return result;
	}

	private Representation addNodeInstancesInTransaction(Representation entity)
			throws Exception {

		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		Run run = Run.loadFromUuid(getUuid(), em);
		List<String> instanceNames = new ArrayList<String>();
		try {
			validateRun(run);

			transaction.begin();

			int noOfInst = getNumberOfInstancesToAdd(entity);
			Node node = getNode(run, nodename);
			for (int i = 0; i < noOfInst; i++) {
				instanceNames.add(createNodeInstanceOnRun(run, node));
			}
			incrementNodeMultiplicityOnRun(noOfInst, run);
			StateMachine.createStateMachine(run).tryAdvanceToProvisionning();

			if (Configuration.isQuotaEnabled()) {
				User user = User.loadByName(run.getUser());
				Quota.validate(user, run.getCloudServiceUsage(), Vm.usage(user.getName()));
			}

			transaction.commit();
		} catch (Exception ex) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			throw ex;
		} finally {
			em.close();
		}

		getResponse().setStatus(Status.SUCCESS_CREATED);
		return new StringRepresentation(StringUtils.join(instanceNames, ","), MediaType.TEXT_PLAIN);
	}

	@Delete
	public void deleteNodeInstances(Representation entity) throws Exception {
		try {
			deleteNodeInstancesInTransaction(entity);
		} catch (ResourceException e) {
			throw e;
		} catch (SlipStreamClientException e) {
			throwClientConflicError(e.getMessage());
		} catch (SlipStreamException e) {
			throwServerError(e.getMessage());
		} catch (Exception e) {
			throwServerError(e.getMessage());
		}
	}

	private void deleteNodeInstancesInTransaction(Representation entity) throws Exception {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		Run run = Run.loadFromUuid(getUuid(), em);
		try {
			validateRun(run);
			transaction.begin();

			Form form = new Form(entity);
			String ids = form.getFirstValue(INSTANCE_IDS_REMOVE_FORM_PARAM, "");
			if (ids.isEmpty()) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"Provide list of node instance IDs to be removed from the Run.");
			} else {
				String cloudServiceName = getCloudServiceName(run);
				List<String> instanceIds = Arrays.asList(ids.split("\\s*,\\s*"));
				for (String _id : instanceIds) {
					int id = Integer.parseInt(_id);
					setRemovingNodeInstance(run, getNodeInstanceName(id));

					String instanceName = getNodeInstanceName(id);
					run.removeNodeInstanceName(instanceName, cloudServiceName);
				}
				// update instance ids
				removeNodeInstanceIndices(run, instanceIds);
				decrementNodeMultiplicityOnRun(instanceIds.size(), run);
			}

			StateMachine.createStateMachine(run).tryAdvanceToProvisionning();

			transaction.commit();
		} catch (Exception ex) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			throw ex;
		} finally {
			em.close();
		}

		getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
	}

	private void raiseConflictIfAbortIsSet() {
		// Let certain methods to succeed if 'ignore abort' is set.
		if (!isIgnoreAbort() && isAbortSet()) {
			throw (new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					"Abort flag raised!"));
		}
	}

	private boolean isIgnoreAbort() {
		Method httpMethod = getMethod();
		if (getIgnoreAbort() && IGNORE_ABORT_HTTP_METHODS.contains(httpMethod)) {
			return true;
		} else {
			return false;
		}
	}

	private String getCloudServiceName(Run run) {
		Node node = getNode(run, nodename);
		return node.getCloudService();
	}

	private int getNumberOfInstancesToAdd(Representation entity) {
		Form form = new Form(entity);
		try {
			return Integer.parseInt(form.getFirstValue(NUMBER_INSTANCES_ADD_FORM_PARAM,
					NUMBER_INSTANCES_ADD_DEFAULT));
		} catch (NumberFormatException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Number of instances to add should be an integer.");
		}
	}

	private String createNodeInstanceOnRun(Run run, Node node)
			throws NotFoundException, AbortException, ValidationException {

		int newId = addNodeInstanceIndex(run, node);

		createNodeInstanceRuntimeParameters(run, node, newId);

		String instanceName = getNodeInstanceName(newId);

		//setCreatingNodeInstance(run, instanceName);

		return instanceName;
	}

	private void createNodeInstanceRuntimeParameters(Run run, Node node,
			int newId) throws ValidationException, NotFoundException {
		/*
		String cloudService = node.getCloudService();
		List<String> filter = new ArrayList<String>();
		for (ParameterCategory c : ParameterCategory.values()) {
			filter.add(c.toString());
		}
		// add runtime parameters
		DeploymentFactory.initNodeInstanceState(run, nodename, newId,
				cloudService);
		Module image = node.getImage();
		for (ModuleParameter param : image.getParameterList()) {
			String category = param.getCategory();
			if (filter.contains(category) || cloudService.equals(category)) {
				String initialValue = DeploymentFactory.extractInitialValue(
						param, node);
				run.assignRuntimeParameter(
						run.composeParameterName(node, param.getName(), newId),
						initialValue, param.getDescription(), param.getType());
			}
		}
		*/
		DeploymentFactory.initNodeInstanceRuntimeParameters(run, node, newId);

		//TODO: LS: check this part
		// add mapping parameters
		for (NodeParameter param : node.getParameterMappings().values()) {
			if (!param.isStringValue()) {
				DeploymentFactory.addParameterMapping(run, param, newId);
			}
		}
	}

	private Node getNode(Run run, String nodename) {
		DeploymentModule deployment = (DeploymentModule) run.getModule();
		for (Node node : deployment.getNodes().values()) {
			if (node.getName().equals(nodename)) {
				return node;
			}
		}
		throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Node "
				+ nodename + " doesn't exist.");
	}

	private void removeNodeInstanceIndices(Run run, List<String> ids)
			throws NotFoundException, AbortException, ValidationException {

		List<String> nodeIds = new ArrayList<String>(
				Arrays.asList(getNodeInstanceIndices(run).split("\\s*,\\s*")));

		nodeIds.removeAll(ids);
		String newNodeIds = StringUtils.join(nodeIds, ",");
		setNodeInstanceIndices(run, newNodeIds);
	}

	private void validateRun(Run run) {
		if (!run.isMutable()) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					"Can't add/remove instances. Run is not mutable.");
		}
		States currentState = run.getState();
		if (currentState != States.Ready) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					"Can't add/remove instances. Incompatible state "
							+ currentState.toString() + ". Allowed state "
							+ States.Ready.toString());
		}
	}

	private int addNodeInstanceIndex(Run run, Node node) throws NotFoundException,
			AbortException, ValidationException {

		String ids = getNodeInstanceIndices(run);

		String key = run.nodeRuntimeParameterKeyName(node, RunParameter.NODE_INCREMENT_KEY);
		RunParameter nodeInscrement = run.getParameter(key);

		int newId = Integer.parseInt(nodeInscrement.getValue("0"));
		nodeInscrement.setValue(String.valueOf(newId + 1));

		if (!ids.isEmpty()) {
			ids += ",";
		}
		ids += newId;
		setNodeInstanceIndices(run, ids);

		return newId;
	}

	private void setRemovingNodeInstance(Run run, String instanceName)
			throws NotFoundException, ValidationException {
		setScaleActionOnNodeInstance(run, instanceName, "removing");
	}

	private void setScaleActionOnNodeInstance(Run run, String instanceName,
			String action) throws NotFoundException, ValidationException {
		run.updateRuntimeParameter(RuntimeParameter.constructParamName(
				instanceName, RuntimeParameter.SCALE_STATE_KEY), action);
	}

	private String getNodeInstanceIndices(Run run) throws NotFoundException, AbortException {
		return run.getRuntimeParameterValue(nodeIndicesRuntimeParam);
	}

	private void setNodeInstanceIndices(Run run, String indices)
			throws ValidationException, NotFoundException {
		run.updateRuntimeParameter(nodeIndicesRuntimeParam, indices);
	}

	private String getNodeInstanceName(int index) {
		return RuntimeParameter.constructNodeInstanceName(nodename, index);
	}

	private void decrementNodeMultiplicityOnRun(int decrement, Run run)
			throws ValidationException, NotFoundException {
		incrementNodeMultiplicityOnRun(-1 * decrement, run);
	}

	private void incrementNodeMultiplicityOnRun(int inc, Run run)
			throws ValidationException, NotFoundException {

		// node_name--multiplicity - Run Parameter
		int newMultiplicity = getNodeGroupMulptilicity(run) + inc;
		if (newMultiplicity < 0) {
			newMultiplicity = 0;
		}
		run.getParameter(nodeMultiplicityRunParam, "General").setValue(
				Integer.toString(newMultiplicity));

		// node_name:multiplicity - Runtime Parameter
		run.updateRuntimeParameter(nodeMultiplicityRuntimeParam,
				Integer.toString(newMultiplicity));
	}

	private int getNodeGroupMulptilicity(Run run) {
		return Integer.parseInt(run.getParameter(nodeMultiplicityRunParam,
				"General").getValue());
	}

}
