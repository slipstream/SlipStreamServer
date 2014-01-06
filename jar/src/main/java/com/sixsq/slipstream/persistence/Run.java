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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.MapKey;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Query;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang.StringUtils;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;
import org.simpleframework.xml.ElementMap;

import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.connector.Credentials;
import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.SlipStreamInternalException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.run.DeploymentFactory;
import com.sixsq.slipstream.run.RunView;
import com.sixsq.slipstream.statemachine.States;
import com.sixsq.slipstream.util.Logger;

@SuppressWarnings("serial")
@Entity
@NamedQueries({
		@NamedQuery(name = "allActiveRuns", query = "SELECT r FROM Run r WHERE r.state NOT IN (:completed) ORDER BY r.startTime DESC"),
		@NamedQuery(name = "activeRunsByUser", query = "SELECT r FROM Run r WHERE r.state NOT IN (:completed) AND r.user_ = :user ORDER BY r.startTime DESC"),
		@NamedQuery(name = "allRuns", query = "SELECT r FROM Run r ORDER BY r.startTime DESC"),
		@NamedQuery(name = "runsByUser", query = "SELECT r FROM Run r JOIN FETCH r.runtimeParameters p WHERE r.user_ = :user ORDER BY r.startTime DESC"),
		@NamedQuery(name = "runsByRefModule", query = "SELECT r FROM Run r WHERE r.user_ = :user AND r.moduleResourceUri = :referenceModule ORDER BY r.startTime DESC"),
		@NamedQuery(name = "runsByInstanceId", query = "SELECT r FROM Run r JOIN r.runtimeParameters p WHERE r.user_ = :user AND p.key_ LIKE '%:instanceid' AND p.value = :instanceid ORDER BY r.startTime DESC") })
public class Run extends Parameterized<Run, RunParameter> {

	private static final int MAX_NO_OF_ENTRIES = 20;
	public static final String ORCHESTRATOR_CLOUD_SERVICE_SEPARATOR = "-";
	public static final String NODE_NAME_PARAMETER_SEPARATOR = "--";
	// Orchestrator
	public final static String ORCHESTRATOR_NAME = "orchestrator";
	public static final String SERVICENAME_NODENAME_SEPARATOR = RuntimeParameter.NODE_PROPERTY_SEPARATOR;

	// Default machine name for image and disk creation
	public final static String MACHINE_NAME = "machine";
	public final static String MACHINE_NAME_PREFIX = MACHINE_NAME
			+ RuntimeParameter.NODE_PROPERTY_SEPARATOR;

	// The initial state of each node
	public final static String INITIAL_NODE_STATE_MESSAGE = States.Inactive
			.toString();
	public final static String INITIAL_NODE_STATE = States.Inactive.toString();

	public final static String RESOURCE_URI_PREFIX = "run/";

	public final static String TAGS_PARAMETER_DESCRIPTION = "Tags (comma separated) or annotations for this run";

	public final static String CPU_PARAMETER_NAME = ImageModule.CPU_KEY;
	public final static String CPU_PARAMETER_DESCRIPTION = "Number of CPUs (i.e. virtual cores)";

	public final static String RAM_PARAMETER_NAME = ImageModule.RAM_KEY;
	public final static String RAM_PARAMETER_DESCRIPTION = "Amount of RAM, in GB";

	public static Run abortOrReset(String abortMessage, String nodename,
			String uuid) {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();

		Run run = Run.abortOrReset(abortMessage, nodename, em, uuid);

		transaction.commit();
		em.close();

		return run;
	}

	public static Run abortOrReset(String abortMessage, String nodename,
			EntityManager em, String uuid) {
		Run run = Run.loadFromUuid(uuid, em);
		RuntimeParameter globalAbort = getGlobalAbort(run);
		String nodeAbortKey = getNodeAbortKey(nodename);
		RuntimeParameter nodeAbort = run.getRuntimeParameters().get(
				nodeAbortKey);
		if ("".equals(abortMessage)) {
			globalAbort.reset();
			if (nodeAbort != null) {
				nodeAbort.reset();
			}
		} else if (!globalAbort.isSet()) {
			globalAbort.setValue(abortMessage);
			if (nodeAbort != null) {
				nodeAbort.setValue(abortMessage);
			}
		}

		return run;
	}

	private static RuntimeParameter getGlobalAbort(Run run) {
		RuntimeParameter abort = run.getRuntimeParameters().get(
				RuntimeParameter.GLOBAL_ABORT_KEY);
		return abort;
	}

	private static String getNodeAbortKey(String nodeName) {
		return nodeName + RuntimeParameter.NODE_PROPERTY_SEPARATOR
				+ RuntimeParameter.ABORT_KEY;
	}

	public static Run loadFromUuid(String uuid) {
		String resourceUri = RESOURCE_URI_PREFIX + uuid;
		return load(resourceUri);
	}

	public static Run loadFromUuid(String uuid, EntityManager em) {
		String resourceUri = RESOURCE_URI_PREFIX + uuid;
		return load(resourceUri, em);
	}

	public static Run load(String resourceUri) {
		EntityManager em = PersistenceUtil.createEntityManager();
		Run run = em.find(Run.class, resourceUri);
		em.close();
		return run;
	}

	public static Run load(String resourceUri, EntityManager em) {
		Run run = em.find(Run.class, resourceUri);
		return run;
	}

	@SuppressWarnings("unchecked")
	public static List<RunView> viewListByInstanceId(User user,
			String instanceId) throws ConfigurationException,
			ValidationException {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("runsByInstanceId");
		q.setParameter("user", user.getName());
		q.setParameter("instanceid", instanceId);
		List<Run> runs = q.getResultList();
		List<RunView> views = convertRunsToRunViews(runs, user);
		em.close();
		return views;
	}

	private static List<RunView> convertRunsToRunViews(List<Run> runs, User user)
			throws ConfigurationException, ValidationException {
		List<RunView> views = new ArrayList<RunView>();
		RunView runView;
		for (Run r : runs) {
			// Deployment runs can span several clouds
			// this info in held in getCloudServiceNameList()
			// so if the list is not empty, use it and
			// create a RunView instance for each
			String[] cloudServiceNames = r.getCloudServiceNameList();

			runView = new RunView(r.getResourceUri(), r.getUuid(),
					r.getModuleResourceUrl(), r.getState().toString(),
					r.getStart(), r.getUser(), r.getType());
			try {
				runView.setHostname(r
						.getRuntimeParameterValueIgnoreAbort(MACHINE_NAME_PREFIX
								+ RuntimeParameter.HOSTNAME_KEY));
			} catch (NotFoundException e) {
			}
			try {
				runView.setVmstate(r
						.getRuntimeParameterValueIgnoreAbort(MACHINE_NAME_PREFIX
								+ RuntimeParameter.STATE_VM_KEY));
			} catch (NotFoundException e) {
			}
			try {
				runView.setTags(r
						.getRuntimeParameterValueIgnoreAbort(RuntimeParameter.GLOBAL_TAGS_KEY));
			} catch (NotFoundException e) {
			}

			// For each cloud service, create a RunView entry
			for (String csn : cloudServiceNames) {
				RunView rv = runView.copy();
				rv.setCloudServiceName(csn);
				views.add(rv);
			}
		}
		return views;
	}

	public static Run updateVmStatus(Run run, User user)
			throws SlipStreamException {

		return updateVmStatus(run, describeInstances(user, run));
	}

	public static Properties describeInstances(User user) 
			throws ValidationException{
		return describeInstances(user, null);
	}
	
	public static Properties describeInstances(User user, Run run)
			throws ValidationException {
		Properties describeInstancesStates = new Properties();
		
		String[] cloudServicesList = null;
		if(run != null){
			cloudServicesList = run.getCloudServiceNameList();
		}else{
			cloudServicesList = ConnectorFactory.getCloudServiceNames();
		}
		
		for (String cloudServiceName : cloudServicesList) {
			Connector connector = ConnectorFactory
					.getConnector(cloudServiceName);
			Properties props;
			try {
				props = connector.describeInstances(user);
			} catch (SlipStreamException e) {
				Logger.warning(e.getMessage());
				continue;
			}
			for (String key : props.stringPropertyNames()) {
				describeInstancesStates.put(key, props.getProperty(key));
			}
		}
		return describeInstancesStates;
	}

	public static Run updateVmStatus(Run run, Properties describeInstancesStates)
			throws SlipStreamException {
		run = populateVmStateProperties(run, describeInstancesStates);
		return run;
	}

	public static Run populateVmStateProperties(Run run,
			Properties describeInstancesStates) throws NotFoundException,
			ValidationException {

		List<String> nodes = run.getNodeNameList();
		String vmIdKey;
		String vmId;
		String vmStateKey;

		for (String nodeName : nodes) {
			String keyPrefix = nodeName
					+ RuntimeParameter.NODE_PROPERTY_SEPARATOR;
			vmIdKey = keyPrefix + RuntimeParameter.INSTANCE_ID_KEY;
			vmId = run.getRuntimeParameterValueIgnoreAbort(vmIdKey);
			vmId = vmId == null ? "" : vmId;
			vmStateKey = keyPrefix + RuntimeParameter.STATE_VM_KEY;
			String vmState = describeInstancesStates.getProperty(vmId,
					"Unknown");
			try {
				run.updateRuntimeParameter(vmStateKey, vmState);
			} catch (NotFoundException e) {
				run.assignRuntimeParameter(vmStateKey, vmState,
						RuntimeParameter.STATE_VM_DESCRIPTION);
			}
		}

		return run;
	}

	@SuppressWarnings("unchecked")
	public static List<RunView> viewListAll(User user)
			throws ConfigurationException, ValidationException {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = createNamedQuery(em, "allRuns");
		List<Run> runs = q.getResultList();
		List<RunView> views = convertRunsToRunViews(runs, user);
		em.close();
		return views;
	}

	@SuppressWarnings("unchecked")
	public static List<RunView> viewList(User user)
			throws ConfigurationException, ValidationException {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = createNamedQuery(em, "runsByUser");
		q.setParameter("user", user.getName());
		List<Run> runs = q.getResultList();
		List<RunView> views = convertRunsToRunViews(runs, user);
		em.close();
		return views;
	}

	@SuppressWarnings("unchecked")
	public static List<RunView> viewList(String moduleResourceUri, User user)
			throws ConfigurationException, ValidationException {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = createNamedQuery(em, "runsByRefModule");
		q.setParameter("user", user.getName());
		q.setParameter("referenceModule", moduleResourceUri);
		List<Run> runs = q.getResultList();
		List<RunView> views = convertRunsToRunViews(runs, user);
		em.close();
		return views;
	}

	@SuppressWarnings("unchecked")
	public static List<Run> listAllActive(EntityManager em)
			throws ConfigurationException, ValidationException {
		Query q = em.createNamedQuery("allActiveRuns");
		q.setParameter("completed", States.completed());
		List<Run> runs = q.getResultList();
		return runs;
	}

	@SuppressWarnings("unchecked")
	public static List<Run> listAllActive(EntityManager em, User user)
			throws ConfigurationException, ValidationException {
		Query q = em.createNamedQuery("activeRunsByUser");
		q.setParameter("completed", States.completed());
		q.setParameter("user", user.getName());
		List<Run> runs = q.getResultList();
		return runs;
	}
	
	private static Query createNamedQuery(EntityManager em, String query) {
		Query q = em.createNamedQuery(query);
		q.setMaxResults(MAX_NO_OF_ENTRIES);
		return q;
	}

	public static List<Run> viewListAllActive() throws ConfigurationException,
			ValidationException {
		EntityManager em = PersistenceUtil.createEntityManager();
		List<Run> runs = listAllActive(em);
		em.close();
		return runs;
	}

	@Attribute
	@Id
	private String resourceUri;

	@Attribute
	private String uuid;

	@Attribute(empty = "Orchestration")
	private RunType type = RunType.Orchestration;

	@Attribute
	private String cloudServiceName;

	/**
	 * Cloud service names (only applies to deployment type run) comma separated
	 */
	@Attribute(required = false)
	@Lob
	private String cloudServiceNames;

	@Attribute(required = false)
	@Enumerated
	private States state = States.Inactive;

	@Attribute
	private String moduleResourceUri;

	private transient Credentials credentials;

	@OneToMany(mappedBy = "container", cascade = CascadeType.ALL)
	@MapKey(name = "key_")
	@ElementMap(name = "runtimeParameters", required = false, data = true, valueType = RuntimeParameter.class)
	private Map<String, RuntimeParameter> runtimeParameters = new HashMap<String, RuntimeParameter>();

	@Attribute
	@Temporal(TemporalType.TIMESTAMP)
	private Date startTime = new Date();

	@Attribute(required = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date endTime;

	/**
	 * Comma separated list of node names - e.g. apache1.1, apache1.2, ...
	 * Including the orchestrator: orchestrator-local, ...
	 */
	@Attribute
	@Lob
	private String nodeNames = "";

	/**
	 * Comma separated list of nodes, including the associated orchestror name -
	 * e.g. orchestrator:apache1, orchestrator:testclient1, ... or
	 * orchestrator-stratuslab:apache1, orchestrator-openstack:testclient1, ...
	 */
	private String groups = "";

	@Attribute(name = "user", required = false)
	private String user_;

	@Element(required = false)
	private transient Module module;

	/**
	 * List of cloud service names used in the current run
	 */
	@ElementArray(required = false)
	public String[] getCloudServiceNameList() {
		return cloudServiceNames == null ? new String[] { cloudServiceName }
				: cloudServiceNames.split(",");
	}

	public void assignCloudServiceNames() throws ValidationException {
		if (getCategory() == ModuleCategory.Deployment) {
			cloudServiceNames = StringUtils.join(getCloudServicesList(), ",");
		} else {
			cloudServiceNames = getCloudService();
		}

	}

	@ElementArray(required = false)
	public void setCloudServiceNameList(String[] names) {
	}

	@SuppressWarnings("unused")
	private Run() throws NotFoundException {
	}

	public Run(Module module, RunType type, String cloudServiceName, User user)
			throws ValidationException {

		uuid = UUID.randomUUID().toString();
		resourceUri = RESOURCE_URI_PREFIX + uuid;

		this.category = module.getCategory();
		this.moduleResourceUri = module.getResourceUri();
		this.type = type;
		this.cloudServiceName = (CloudImageIdentifier.DEFAULT_CLOUD_SERVICE
				.equals(cloudServiceName) ? user.getDefaultCloudService()
				: cloudServiceName);
		this.user_ = user.getName();

		this.module = module;

		setStart();
	}

	public Module getModule() {
		if (module == null) {
			module = Module.load(getModuleResourceUrl());
		}
		return module;
	}

	public void setModule(Module module) throws ValidationException {
		setModule(module, false);
	}

	public void setModule(Module module, boolean populate)
			throws ValidationException {
		this.module = module;
		if (populate) {
			populateModule();
		}
	}

	@Override
	public String getName() {
		return uuid;
	}

	@Override
	public void setName(String name) {
		this.uuid = name;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getModuleResourceUrl() {
		return moduleResourceUri;
	}

	public void setModuleResourceUrl(String moduleResourceUri) {
		this.moduleResourceUri = moduleResourceUri;
	}

	public Map<String, RuntimeParameter> getRuntimeParameters() {
		return runtimeParameters;
	}

	public void setRuntimeParameters(
			Map<String, RuntimeParameter> runtimeParameters) {
		this.runtimeParameters = runtimeParameters;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

	public String getRefqname() {
		return moduleResourceUri;
	}

	public void setRefqname(String refqname) {
		this.moduleResourceUri = refqname;
	}

	public void setCredentials(Credentials credentials) {
		this.credentials = credentials;
	}

	public Credentials getCredentials() {
		return this.credentials;
	}

	/**
	 * Set value to key, ignoring the abort flag, such that no exception is
	 * thrown.
	 * 
	 * @param key
	 * @return new value
	 * @throws AbortException
	 * @throws NotFoundException
	 */
	public String getRuntimeParameterValueIgnoreAbort(String key)
			throws NotFoundException {

		assert (runtimeParameters != null);

		RuntimeParameter parameter = extractRuntimeParameter(key);
		return parameter.getValue();
	}

	private RuntimeParameter extractRuntimeParameter(String key)
			throws NotFoundException {

		if (!runtimeParameters.containsKey(key)) {
			throwNotFoundException(key);
		}
		return runtimeParameters.get(key);
	}

	public String getRuntimeParameterValue(String key) throws AbortException,
			NotFoundException {

		if (isAbort()) {
			throw new AbortException("Abort flag raised!");
		}

		return getRuntimeParameterValueIgnoreAbort(key);
	}

	public void removeRuntimeParameter(String key) throws NotFoundException {

		assert (runtimeParameters != null);

		Metadata parameter = extractRuntimeParameter(key);
		runtimeParameters.remove(parameter);
	}

	public boolean isAbort() {
		RuntimeParameter abort = null;
		try {
			abort = extractRuntimeParameter(RuntimeParameter.GLOBAL_ABORT_KEY);
		} catch (NotFoundException e) {
			return false;
		}
		return abort.isSet();
	}

	public void createRuntimeParameter(Node node, String key, String value)
			throws ValidationException {
		createRuntimeParameter(node, key, value, "", ParameterType.String);
	}

	public void createRuntimeParameter(Node node, String key, String value,
			String description, ParameterType type) throws ValidationException {

		// We only test for the first one
		String parameterName = composeParameterName(node, key, 1);
		if (getParameters().containsKey(parameterName)) {
			throw new ValidationException("Parameter " + parameterName
					+ " already exists in node " + node.getName());
		}

		for (int i = 1; i <= node.getMultiplicity(); i++) {
			assignRuntimeParameter(composeParameterName(node, key, i), value,
					description, type);
		}
	}

	private String composeParameterName(Node node, String key, int i) {
		return composeNodeName(node, i)
				+ RuntimeParameter.NODE_PROPERTY_SEPARATOR + key;
	}

	private String composeNodeName(Node node, int i) {
		return node.getName()
				+ RuntimeParameter.NODE_MULTIPLICITY_INDEX_SEPARATOR + i;
	}

	public Date getStart() {
		return (Date) startTime.clone();
	}

	public void setStart() {
		setDateNow(startTime);
	}

	public void setStart(Date start) {
		this.startTime = (Date) start.clone();
	}

	public Date getEnd() {
		return endTime == null ? null : (Date) endTime.clone();
	}

	public void setEnd(Date end) {
		this.endTime = (Date) end.clone();
	}

	public void setEnd() {
		setDateNow(endTime);
	}

	public void setDateNow(Date date) {
		date = new Date();
	}

	public void addNodeName(String node) {
		nodeNames += node + ", ";
	}

	/**
	 * Return nodenames, including a value for each index from 1 to multiplicity
	 * (e.g. apache1.1, apache1.2...)
	 * 
	 * @return comma separated nodenames
	 */
	public String getNodeNames() {
		return nodeNames;
	}

	public List<String> getNodeNameList() {
		return Arrays.asList(getNodeNames().split(", "));
	}

	@Override
	public String getResourceUri() {
		return resourceUri;
	}

	@Override
	public void setContainer(RunParameter parameter) {
		parameter.setContainer(this);
	}

	public String getUser() {
		return user_;
	}

	public void setUser(String user) {
		this.user_ = user;
	}

	public RuntimeParameter assignRuntimeParameter(String key, String value,
			String description) throws ValidationException {
		return assignRuntimeParameter(key, value, description,
				ParameterType.String);
	}

	public RuntimeParameter assignRuntimeParameter(String key, String value,
			String description, ParameterType type) throws ValidationException {
		if (runtimeParameters.containsKey(key)) {
			throw new ValidationException("Key " + key
					+ " already exists, cannot re-define");
		}
		RuntimeParameter parameter = new RuntimeParameter(this, key, value,
				description);

		parameter.setType(type);
		runtimeParameters.put(key, parameter);

		return parameter;
	}

	public RuntimeParameter assignRuntimeParameter(String key,
			String description) throws ValidationException {
		return assignRuntimeParameter(key, "", description);
	}

	public RuntimeParameter updateRuntimeParameter(String key, String value)
			throws NotFoundException, ValidationException {
		if (!runtimeParameters.containsKey(key)) {
			throwNotFoundException(key);
		}

		RuntimeParameter parameter = runtimeParameters.get(key);
		if (RuntimeParameter.GLOBAL_ABORT_KEY.equals(key)) {
			if (isAbort()) {
				return parameter;
			}
		}
		parameter.setValue(value);

		return getRuntimeParameters().get(key);
	}

	private void throwNotFoundException(String key) throws NotFoundException {
		throw new NotFoundException("Couldn't find key '" + key
				+ "' in execution instance: '" + getName() + "'");
	}

	public Run store() {
		return (Run) super.store();
	}

	public void setType(RunType type) {
		this.type = type;
	}

	public RunType getType() {
		return type;
	}

	public States getState() {
		// required to keep backward compatibility
		return state == null ? States.Unknown : state;
	}

	public void setState(States state) {
		RunStates rState = new RunStates(state, isAbort());
		this.state = rState.getState();
	}

	public int getMultiplicity(String nodeName) throws NotFoundException {
		String multiplicity = getRuntimeParameterValueIgnoreAbort(nodeName
				+ RuntimeParameter.NODE_MULTIPLICITY_INDEX_SEPARATOR
				+ RuntimeParameter.MULTIPLICITY_NODE_START_INDEX
				+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
				+ RuntimeParameter.MULTIPLICITY_PARAMETER_NAME);
		return Integer.parseInt(multiplicity);
	}

	public void setCloudServiceName(String cloudServiceName) {
		this.cloudServiceName = cloudServiceName;
	}

	public String getCloudService() {
		return cloudServiceName;
	}

	public List<String> getOrchestrators() {
		List<String> orchestrators = new ArrayList<String>();

		for (String nodename : getNodeNameList()) {
			if (nodename.startsWith(Run.ORCHESTRATOR_NAME)) {
				orchestrators.add(nodename);
			}
		}

		return orchestrators;
	}

	public Map<String, Node> getNodes() throws ValidationException {
		if (module == null) {
			module = new DeploymentFactory().overloadModule(this,
					User.loadByName(getUser()));
		}

		if (module.getCategory() != ModuleCategory.Deployment) {
			throw new SlipStreamInternalException(
					"getNodes can only be used with a Deployment module");
		}

		return ((DeploymentModule) module).getNodes();
	}

	public HashSet<String> getCloudServicesList() throws ValidationException {
		HashSet<String> cloudServicesList = new HashSet<String>();
		if (getCategory() == ModuleCategory.Deployment) {
			for (Node n : getNodes().values()) {
				String cloudServiceName = n.getCloudService();
				cloudServicesList
						.add(getEffectiveCloudServiceName(cloudServiceName));
			}
		} else {
			cloudServicesList.add(getCloudService());
		}
		return cloudServicesList;
	}

	public String getEffectiveCloudServiceName(String cloudService) {
		return ConnectorFactory.isDefaultCloudService(cloudService) ? this.cloudServiceName
				: cloudService;
	}

	public void addGroup(String group, String serviceName) {
		this.groups += serviceName + SERVICENAME_NODENAME_SEPARATOR + group
				+ ", ";
	}

	@Attribute
	@Lob
	public String getGroups() {
		getRuntimeParameters().get(RuntimeParameter.GLOBAL_NODE_GROUPS_KEY)
				.setValue(groups);
		return groups;
	}

	@Attribute
	@Lob
	public void setGroups(String groups) {
		this.groups = groups;
	}

	/**
	 * Populate a volatile module and override its parameter (e.g. cloud
	 * service, multiplicity)
	 * 
	 * @throws ValidationException
	 */
	private void populateModule() throws ValidationException {

		if (module.getCategory() == ModuleCategory.Deployment) {
			populateDeploymentModule((DeploymentModule) module);
		}
		if (module.getCategory() == ModuleCategory.Image) {
			populateImageModule((ImageModule) module);
		}
	}

	public List<String> getGroupNameList() {
		return Arrays.asList(getGroups().split(", "));
	}

	private void populateDeploymentModule(DeploymentModule deployment)
			throws ValidationException {
		for (Node node : deployment.getNodes().values()) {

			RunParameter runParameter;

			runParameter = getParameter(nodeRuntimeParameterKeyName(node,
					RuntimeParameter.MULTIPLICITY_PARAMETER_NAME));
			if (runParameter != null) {
				node.setMultiplicity(runParameter.getValue());
			}

			node.getImage().assignImageIdFromCloudService(
					node.getCloudService());
		}
	}

	public String nodeRuntimeParameterKeyName(Node node,
			String nodeParameterName) {
		return node.getName() + NODE_NAME_PARAMETER_SEPARATOR
				+ nodeParameterName;
	}

	private void populateImageModule(ImageModule image)
			throws ValidationException {
		if (type == RunType.Orchestration) {
			image.assignBaseImageIdToImageIdFromCloudService(getCloudService());
		} else {
			image.assignImageIdFromCloudService(getCloudService());
		}
	}

	public void done() {
		RunStates state = new RunStates(this);
		state.done();
		this.state = state.getState();
		getRuntimeParameters().get(RuntimeParameter.GLOBAL_STATE_KEY).setValue(
				state.toString());
		setEnd();
	}

	public static String constructOrchestratorName(String cloudService) {
		return ORCHESTRATOR_NAME + ORCHESTRATOR_CLOUD_SERVICE_SEPARATOR
				+ cloudService;
	}

	public String getCloudServiceNames() {
		return cloudServiceNames;
	}

	public void setCloudServiceNames(String cloudServiceNames) {
		this.cloudServiceNames = cloudServiceNames;
	}

}
