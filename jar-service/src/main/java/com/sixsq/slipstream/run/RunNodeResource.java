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

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class RunNodeResource extends RunBaseResource {

	public final static String NUMBER_INSTANCES_ADD_FORM_PARAM = "n";
	public final static String NUMBER_INSTANCES_ADD_DEFAULT = "1";

	private final static String INSTANCE_IDS_REMOVE_FORM_PARAM = "ids";
	private final static String DELETE_INSTANCE_IDS_ONLY_FORM_PARAM = "delete-ids-only";

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
		String multiplicityParamName = DeploymentFactory.constructParamName(nodename, RuntimeParameter.MULTIPLICITY_PARAMETER_NAME);
		nodeMultiplicityRunParam = multiplicityParamName;
		nodeMultiplicityRuntimeParam = multiplicityParamName;
		nodeIndicesRuntimeParam = DeploymentFactory.constructParamName(nodename, RuntimeParameter.IDS_PARAMETER_NAME);
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
			throwClientConflicError(e.getMessage(), e);
		} catch (SlipStreamException e) {
			throwServerError(e.getMessage(), e);
		} catch (Exception e) {
			throwServerError(e.getMessage(), e);
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

			int noOfInst = getNumberOfInstancesToAdd(new Form(entity));

			Node node = getNode(run, nodename);
			for (int i = 0; i < noOfInst; i++) {
				instanceNames.add(createNodeInstanceOnRun(run, node));
			}

			run.postEventScaleUp(nodename, instanceNames, noOfInst);

			incrementNodeMultiplicityOnRun(noOfInst, run);
			StateMachine.createStateMachine(run).tryAdvanceToProvisionning(); //TODO kb

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
			throwClientConflicError(e.getMessage(), e);
		} catch (SlipStreamException e) {
			throwServerError(e.getMessage(), e);
		} catch (Exception e) {
			throwServerError(e.getMessage(), e);
		}
	}

	private void deleteNodeInstancesInTransaction(Representation entity) throws Exception {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();

		Run run = Run.loadFromUuid(getUuid(), em);
		try {
			Form form = new Form(entity);
			boolean deleteOnly = "true".equals(form.getFirstValue(DELETE_INSTANCE_IDS_ONLY_FORM_PARAM, "").trim()) ? true
			        : false;
			if (!deleteOnly) {
				validateRun(run);
			}
			transaction.begin();

			String ids = form.getFirstValue(INSTANCE_IDS_REMOVE_FORM_PARAM, "");
			if (ids.isEmpty()) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"Provide list of node instance IDs to be removed from the Run.");
			} else {
				String cloudServiceName = "";
				try{
					cloudServiceName = run.getCloudServiceNameForNode(nodename);
				} catch (NullPointerException ex) {
					throwClientBadRequest("Invalid nodename: " + nodename);
				}
				List<String> instanceIds = Arrays.asList(ids.split("\\s*,\\s*"));
				for (String _id : instanceIds) {
					String instanceName = "";
					try {
						instanceName = getNodeInstanceName(Integer.parseInt(_id));
					} catch (NumberFormatException ex) {
						throwClientBadRequest("Invalid instance name: " + _id);
					}
					setRemovingNodeInstance(run, instanceName);
					run.removeNodeInstanceName(instanceName, cloudServiceName);
				}
				run.postEventScaleDown(nodename, instanceIds);
				// update instance ids
				removeNodeInstanceIndices(run, instanceIds);
				decrementNodeMultiplicityOnRun(instanceIds.size(), run);
			}

			if (!deleteOnly) {
				StateMachine.createStateMachine(run).tryAdvanceToProvisionning(); //TODO kb
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

	/*
	Empty request form adds NUMBER_INSTANCES_ADD_DEFAULT node instances.

	Requesting zero instances is allowed by simply NUMBER_INSTANCES_ADD_FORM_PARAM=0.

	Throws ResourceException on
	- non-empty request form without NUMBER_INSTANCES_ADD_FORM_PARAM form parameter;
	- failure to parse NUMBER_INSTANCES_ADD_FORM_PARAM value.
	**/
	protected static int getNumberOfInstancesToAdd(Form form) {
		Set<String> formParams = form.getNames();
		try {
			if (formParams.contains(NUMBER_INSTANCES_ADD_FORM_PARAM)) {
				return Integer.parseInt(form.getFirstValue(NUMBER_INSTANCES_ADD_FORM_PARAM));
			} else if (formParams.isEmpty()) {
				return Integer.parseInt(NUMBER_INSTANCES_ADD_DEFAULT);
			} else {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"No form parameter " + NUMBER_INSTANCES_ADD_FORM_PARAM +
								"=# found in the scale-up request.");
			}
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

		return instanceName;
	}

	private void createNodeInstanceRuntimeParameters(Run run, Node node,
			int newId) throws ValidationException, NotFoundException {

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
		Node node = deployment.getNode(nodename);
		if (node != null) {
			return node;
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Node " + nodename + " doesn't exist.");
		}
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
							+ currentState.toString() + ". This can only be performed on state "
							+ States.Ready.toString());
		}
	}

	private int addNodeInstanceIndex(Run run, Node node) throws NotFoundException,
			AbortException, ValidationException {

		String ids = getNodeInstanceIndices(run);

		String key = DeploymentFactory.constructNodeParamName(node, RunParameter.NODE_INCREMENT_KEY);
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

		// node_name:multiplicity - Runtime Parameter
		run.updateRuntimeParameter(nodeMultiplicityRuntimeParam,
				Integer.toString(newMultiplicity));
	}

	private int getNodeGroupMulptilicity(Run run) throws NumberFormatException, NotFoundException {
		return Integer.parseInt(run.getRuntimeParameterValueIgnoreAbort(nodeMultiplicityRunParam));
	}

}
