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

import static com.sixsq.slipstream.event.TypePrincipal.PrincipalType.ROLE;
import static com.sixsq.slipstream.event.TypePrincipal.PrincipalType.USER;
import static com.sixsq.slipstream.event.TypePrincipalRight.Right.ALL;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Query;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.CollectionType;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;
import org.simpleframework.xml.ElementMap;

import com.sixsq.slipstream.credentials.Credentials;
import com.sixsq.slipstream.event.ACL;
import com.sixsq.slipstream.event.Event;
import com.sixsq.slipstream.event.Event.EventType;
import com.sixsq.slipstream.event.TypePrincipal;
import com.sixsq.slipstream.event.TypePrincipalRight;
import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.run.RunView;
import com.sixsq.slipstream.statemachine.States;

@SuppressWarnings("serial")
@Entity(name="Run")
@NamedQueries({
		@NamedQuery(name = "allRuns", query = "SELECT r FROM Run r ORDER BY r.startTime DESC"),
		@NamedQuery(name = "runWithRuntimeParameters", query = "SELECT r FROM Run r JOIN FETCH r.runtimeParameters p WHERE r.uuid = :uuid"),
		@NamedQuery(name = "oldInStatesRuns", query = "SELECT r FROM Run r WHERE r.user_ = :user AND r.lastStateChangeTime < :before AND r.state IN (:states)"),
		@NamedQuery(name = "runByInstanceId", query = "SELECT r FROM Run r JOIN FETCH r.runtimeParameters p WHERE r.user_ = :user AND p.name_ = :instanceidkey AND p.value = :instanceidvalue ORDER BY r.startTime DESC") })
public class Run extends Parameterized<Run, RunParameter> {

	private static final int DEFAULT_TIMEOUT = 60; // In minutes

	public static final int DEFAULT_LIMIT = 20;
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
	public final static String INITIAL_NODE_STATE_MESSAGE = States.Initializing
			.toString();
	public final static String INITIAL_NODE_STATE = States.Initializing.toString();

	public final static String RESOURCE_URI_PREFIX = "run/";

	public final static String TAGS_PARAMETER_DESCRIPTION = "Tags (comma separated) or annotations for this run";

	public final static String CPU_PARAMETER_NAME = ImageModule.CPU_KEY;
	public final static String CPU_PARAMETER_DESCRIPTION = "Number of CPUs (i.e. virtual cores)";

	public final static String RAM_PARAMETER_NAME = ImageModule.RAM_KEY;
	public final static String RAM_PARAMETER_DESCRIPTION = "Amount of RAM, in GB";

	public final static String GARBAGE_COLLECTED_PARAMETER_NAME = "garbage_collected";
	public final static String GARBAGE_COLLECTED_PARAMETER_DESCRIPTION = "true if the Run was already garbage collected";

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

		run.postEventAbort(nodename, abortMessage);

		RuntimeParameter globalAbort = getGlobalAbort(run);
		String nodeAbortKey = getNodeAbortKey(nodename);
		RuntimeParameter nodeAbort = run.getRuntimeParameters().get(
				nodeAbortKey);
		if ("".equals(abortMessage)) {
			globalAbort.reset();
			if (nodeAbort != null) {
				nodeAbort.reset();
			}
			resetRecoveryMode(run);
		} else if (!globalAbort.isSet()) {
			setGlobalAbortState(abortMessage, globalAbort);
			if (nodeAbort != null) {
				nodeAbort.setValue(abortMessage);
			}
			if (run.state == States.Provisioning) {
				setRecoveryMode(run);
			}
		}

		return run;
	}

	private static void setGlobalAbortState(String abortMessage,
			RuntimeParameter globalAbort) {
		globalAbort.setValue(abortMessage);
		globalAbort.store();
		Run run = globalAbort.getContainer();
		run.setState(run.getState());
	}

	public static Run abort(String abortMessage, String uuid) {
		EntityManager em = PersistenceUtil.createEntityManager();
		Run run = Run.loadFromUuid(uuid, em);
		RuntimeParameter globalAbort = getGlobalAbort(run);
		if (!globalAbort.isSet()) {
			setGlobalAbortState(abortMessage, globalAbort);
		}
		if (run.state == States.Provisioning) {
			setRecoveryMode(run);
		}
		em.close();
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

	private static RuntimeParameter getRecoveryModeParameter(Run run) {
		return run.getRuntimeParameters().get(
				RuntimeParameter.GLOBAL_RECOVERY_MODE_KEY);
	}

	public static void setRecoveryMode(Run run) {
		RuntimeParameter recoveryModeParam = getRecoveryModeParameter(run);
		recoveryModeParam.setValue("true");
		recoveryModeParam.store();

		run.postEventRecoveryMode(recoveryModeParam.getValue());
	}

	public static void resetRecoveryMode(Run run) {
		RuntimeParameter recoveryModeParam = getRecoveryModeParameter(run);
		recoveryModeParam.setValue("false");
		recoveryModeParam.store();

		run.postEventRecoveryMode(recoveryModeParam.getValue());
	}

	public static boolean isInRecoveryMode(Run run) {
		RuntimeParameter recoveryModeParam = getRecoveryModeParameter(run);
		boolean result = false;
		if (recoveryModeParam != null) {
			String recoveryMode = recoveryModeParam.getValue();
			result = ! ("".equals(recoveryMode) || "false".equalsIgnoreCase(recoveryMode));
		}
		return result;
	}

	private static RunParameter getGarbageCollectedParameter(Run run) {
		return run.getParameter(Run.GARBAGE_COLLECTED_PARAMETER_NAME);
	}

	public static void setGarbageCollected(Run run) throws ValidationException {
		RunParameter garbageCollected = getGarbageCollectedParameter(run);

		if (garbageCollected == null) {
			run.setParameter(new RunParameter(Run.GARBAGE_COLLECTED_PARAMETER_NAME, "true",
					Run.GARBAGE_COLLECTED_PARAMETER_DESCRIPTION));
			run.postEventGarbageCollected();
		} else if (!garbageCollected.isTrue()) {
			garbageCollected.setValue("true");
			run.postEventGarbageCollected();
		}
	}

	public static boolean isGarbageCollected(Run run) {
		RunParameter garbageCollected = getGarbageCollectedParameter(run);
		boolean result = false;
		if (garbageCollected != null) {
			String recoveryMode = garbageCollected.getValue();
			result = Boolean.parseBoolean(recoveryMode);
		}
		return result;
	}

	public static Run updateRunState(Run run, States newState, boolean retry) {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		try {
			run = Run.loadFromUuid(run.getUuid(), em);
			run.setState(newState);
			transaction.commit();
			em.close();
		} catch (Exception e) {
			String error = "error setting run state: " + newState;
			if (retry) {
				Logger.getLogger("restlet").warning(error + " retrying...");
			} else {
				Logger.getLogger("restlet").severe(error);
			}
			// retry once
			if (retry) {
				updateRunState(run, newState, false);
			}
		}
		return run;
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

	private static List<RunView> convertRunsToRunViews(List<Run> runs)
			throws ConfigurationException, ValidationException {
		List<RunView> views = new ArrayList<RunView>();
		RunView runView;
		for (Run r : runs) {
			runView = convertRunToRunView(r);
			views.add(runView);
		}
		return views;
	}

	private static RunView convertRunToRunView(Run run) {

		if (run == null) {
			return null;
		}

		RuntimeParameter globalAbort = Run.getGlobalAbort(run);
		String abortMessage = (globalAbort != null)? globalAbort.getValue() : "";

		RunView runView;
		runView = new RunView(run.getResourceUri(), run.getUuid(), run.getModuleResourceUrl(),
				run.getState().toString(), run.getStart(), run.getUser(), run.getType(), run.getCloudServiceNames(),
				abortMessage);

		try {
			runView.setTags(run.getRuntimeParameterValueIgnoreAbort(RuntimeParameter.GLOBAL_TAGS_KEY));
		} catch (NotFoundException e) {
		}
		return runView;
	}

	private static Predicate andPredicate(CriteriaBuilder builder, Predicate currentPredicate, Predicate newPredicate){
		return (currentPredicate != null) ? builder.and(currentPredicate, newPredicate) : newPredicate;
	}

	public static List<RunView> viewList(User user, String moduleResourceUri)
			throws ConfigurationException, ValidationException {
		return viewList(user, moduleResourceUri, null, null, null);
	}

	public static List<RunView> viewList(User user, String moduleResourceUri, Integer offset,
			Integer limit, String cloudServiceName) throws ConfigurationException, ValidationException {
		List<RunView> views = null;
		EntityManager em = PersistenceUtil.createEntityManager();
		try {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Run> critQuery = builder.createQuery(Run.class);
			Root<Run> rootQuery = critQuery.from(Run.class);
			critQuery.select(rootQuery);
			Predicate where = viewListCommonQueryOptions(builder, rootQuery, user, moduleResourceUri, cloudServiceName);
			if (where != null){
				critQuery.where(where);
			}
			critQuery.orderBy(builder.desc(rootQuery.get("startTime")));
			TypedQuery<Run> query = em.createQuery(critQuery);
			if (offset != null) {
				query.setFirstResult(offset);
			}
			query.setMaxResults((limit != null)? limit : DEFAULT_LIMIT);
			List<Run> runs = query.getResultList();
			views = convertRunsToRunViews(runs);
		} finally {
			em.close();
		}
		return views;
	}

	public static int viewListCount(User user, String moduleResourceUri, String cloudServiceName)
			throws ConfigurationException, ValidationException {
		int count = 0;
		EntityManager em = PersistenceUtil.createEntityManager();
		try {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Long> critQuery = builder.createQuery(Long.class);
			Root<Run> rootQuery = critQuery.from(Run.class);
			critQuery.select(builder.count(rootQuery));
			Predicate where = viewListCommonQueryOptions(builder, rootQuery, user, moduleResourceUri, cloudServiceName);
			if (where != null){
				critQuery.where(where);
			}
			TypedQuery<Long> query = em.createQuery(critQuery);
			count = (int)(long) query.getSingleResult();
		} finally {
			em.close();
		}
		return count;
	}

	private static Predicate viewListCommonQueryOptions(CriteriaBuilder builder, Root<Run> rootQuery, User user,
			String moduleResourceUri, String cloudServiceName) {
		Predicate where = null;
		if (!user.isSuper()) {
			where = andPredicate(builder, where, builder.equal(rootQuery.get("user_"), user.getName()));
		}
		if (moduleResourceUri != null && !"".equals(moduleResourceUri)) {
			where = andPredicate(builder, where, builder.equal(rootQuery.get("moduleResourceUri"), moduleResourceUri));
		}
		if (cloudServiceName != null && !"".equals(cloudServiceName)) {
			// TODO: Replace the 'like' by an 'equals'
			where = andPredicate(builder, where, builder.like(rootQuery.<String>get("cloudServiceNames"), "%" + cloudServiceName + "%"));
		}
		return where;
	}

	public static List<Run> listAll()
			throws ConfigurationException, ValidationException {
 		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = createNamedQuery(em, "allRuns");
 		@SuppressWarnings("unchecked")
		List<Run> runs = q.getResultList();
 		em.close();
 		return runs;
 	}

 	public static List<Run> listOldTransient(User user) throws ConfigurationException,
 			ValidationException {
 		return listOldTransient(user, 0);
 	}

	@SuppressWarnings("unchecked")
	public static List<Run> listOldTransient(User user, int timeout) throws ConfigurationException,
			ValidationException {
		if (timeout <= 0) {
			timeout = DEFAULT_TIMEOUT;
		}
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, -timeout);
		Date back = calendar.getTime();

		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = createNamedQuery(em, "oldInStatesRuns");
		q.setParameter("user", user.getName());
		q.setParameter("before", back);
		q.setParameter("states", States.transition());
		List<Run> runs = q.getResultList();
		em.close();
		return runs;
	}

	public static Run loadRunWithRuntimeParameters(String uuid)
			throws ConfigurationException, ValidationException {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = createNamedQuery(em, "runWithRuntimeParameters");
		q.setParameter("uuid", uuid);
		Run run = (Run) q.getSingleResult();
		em.close();
		return run;
	}

	private static Query createNamedQuery(EntityManager em, String query) {
		Query q = em.createNamedQuery(query);
		q.setMaxResults(DEFAULT_LIMIT);
		return q;
	}

	@Attribute
	@Id
	private String resourceUri;

	@Attribute
	private String uuid;

	@Attribute(empty = "Orchestration")
	private RunType type = RunType.Orchestration;

	/**
	 * Cloud service names comma separated
	 */
	@Attribute(required = true)
	@Column(length=65536)
	private String cloudServiceNames;

	@Attribute(required = false)
	@Enumerated
	private States state = States.Initializing;

	@Attribute
	private String moduleResourceUri;

	private transient Credentials credentials;

	@OneToMany(mappedBy = "container", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@MapKey(name = "key_")
	@CollectionType(type = "com.sixsq.slipstream.persistence.ConcurrentHashMapType")
	@ElementMap(name = "runtimeParameters", required = false, data = true, valueType = RuntimeParameter.class)
	private Map<String, RuntimeParameter> runtimeParameters = new ConcurrentHashMap<String, RuntimeParameter>();

	@Attribute
	@Temporal(TemporalType.TIMESTAMP)
	private Date startTime = new Date();

	@Attribute(required = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date endTime;

	@Attribute(required = false)
	@Temporal(TemporalType.TIMESTAMP)
	protected Date lastStateChangeTime = new Date();

	/**
	 * Comma separated list of node names - e.g. apache1.1, apache1.2, ...
	 * Including the orchestrator: orchestrator-local, ...
	 *
	 * FIXME: Should be changed to instanceNames (or nodeInstanceNames).
	 *        NB: orchestrator is part of this list as well.
	 */
	@Column(length=65536)
	@Attribute
	private String nodeNames = "";
	private static final String NODE_NAMES_SEPARATOR = ",";

	/**
	 * Comma separated list of nodes, including the associated orchestror name -
	 * e.g. orchestrator:apache1, orchestrator:testclient1, ... or
	 * orchestrator-stratuslab:apache1, orchestrator-openstack:testclient1, ...
	 */
	private String groups = "";

	@Attribute(name = "user", required = false)
	private String user_;

	@Element(required = false)
	@Transient
	private Module module;

	@Attribute(required = false)
	@Column(nullable=false, columnDefinition="boolean default false")
	private boolean mutable;

	@Transient
	private Map<String, Integer> cloudServiceUsage = new HashMap<String, Integer>();

	/**
	 * List of cloud service names used in the current run
	 */
	@ElementArray(required = false)
	public String[] getCloudServiceNamesList() {
		if (cloudServiceNames == null) {
			return new String[] {};
		}
		Set<String> uniqueCloudServiceNames = new HashSet<String>(Arrays.asList(cloudServiceNames.split(",")));
		return uniqueCloudServiceNames.toArray(new String[uniqueCloudServiceNames.size()]);
	}

	@ElementArray(required = false)
	private void setCloudServiceNamesList(String[] cloudServiceNames) {
		Set<String> uniqueCloudServiceNames = new HashSet<String>(Arrays.asList(cloudServiceNames));
		this.cloudServiceNames = StringUtils.join(
		        uniqueCloudServiceNames.toArray(new String[uniqueCloudServiceNames.size()]), ",");
	}

	@SuppressWarnings("unused")
	private Run() throws NotFoundException {
	}

	public Run(Module module, RunType type, Set<String> cloudServiceNames, User user)
			throws ValidationException {

		uuid = UUID.randomUUID().toString();
		resourceUri = RESOURCE_URI_PREFIX + uuid;

		this.category = module.getCategory();
		this.moduleResourceUri = module.getResourceUri();
		this.type = type;
		this.cloudServiceNames = StringUtils.join(cloudServiceNames, ",");
		this.user_ = user.getName();

		this.module = module;

		setStart();
		postEventStateTransition(this.state, true);
	}

	@Override
	@ElementMap(name = "parameters", required = false, valueType = RunParameter.class)
	protected void setParameters(Map<String, RunParameter> parameters) {
		this.parameters = parameters;
	}

	@Override
	@ElementMap(name = "parameters", required = false, valueType = RunParameter.class)
	public Map<String, RunParameter> getParameters() {
		return parameters;
	}

	public Module getModule(boolean load) {
		if (module == null && load) {
			module = Module.load(getModuleResourceUrl());
		}
		return module;
	}

	public Module getModule() {
		return getModule(true);
	}


	public void setModule(Module module) throws ValidationException {
		this.module = module;
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

	public String getRuntimeParameterValue(String key) throws AbortException, NotFoundException {

		if (isAbort()) {
			throw new AbortException("Abort flag raised!");
		}

		return getRuntimeParameterValueIgnoreAbort(key);
	}

	public void removeRuntimeParameter(String key) throws NotFoundException {
		assert (runtimeParameters != null);
		runtimeParameters.remove(key);
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

	public void createRuntimeParameter(Node node, int nodeInstanceId, String key, String value)
			throws ValidationException {
		createRuntimeParameter(node, nodeInstanceId, key, value, "", ParameterType.String);
	}

	public void createRuntimeParameter(Node node, int nodeInstanceId, String key, String value, String description)
			throws ValidationException {
		createRuntimeParameter(node, nodeInstanceId, key, value, description, ParameterType.String);
	}

	public void createRuntimeParameter(Node node, int nodeInstanceId, String key, String value,
			String description, ParameterType type) throws ValidationException {

		String parameterName = composeNodeInstanceParameterName(node, nodeInstanceId, key);
		assignRuntimeParameter(parameterName, value, description, type);
	}

	public static String composeNodeInstanceParameterName(Node node, int nodeInstanceId, String key) {
		return composeNodeInstanceName(node, nodeInstanceId) + RuntimeParameter.NODE_PROPERTY_SEPARATOR + key;
	}

	public static String composeNodeInstanceName(Node node, int nodeInstanceId) {
		return RuntimeParameter.constructNodeInstanceName(node.getName(), nodeInstanceId);
	}

	public Date getStart() {
		return (Date) startTime.clone();
	}

	public void setStart() {
		startTime = now();
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
		if (endTime == null) {
			endTime = now();
		}
	}

	public Date now() {
		return new Date();
	}

	public void addNodeInstanceName(Node node, int nodeInstanceId) {
		String nodeInstanceName = composeNodeInstanceName(node, nodeInstanceId);
		String cloudServiceName = getCloudServiceNameForNode(node.getName());

		addNodeInstanceName(nodeInstanceName, cloudServiceName);
	}

	public void addNodeInstanceName(String nodeInstanceName, String cloudServiceName) {
		List<String> nodeNamesList = new ArrayList<String>(getNodeInstanceNamesList());
		nodeNamesList.remove("");
		if (!nodeNamesList.contains(nodeInstanceName)) {
			nodeNamesList.add(nodeInstanceName);
			nodeNames = StringUtils.join(nodeNamesList, NODE_NAMES_SEPARATOR);

			Integer nb = cloudServiceUsage.get(cloudServiceName);
			if (nb == null){
				nb = 0;
			}
			cloudServiceUsage.put(cloudServiceName, nb + 1);
		}
	}

	public void removeNodeInstanceName(String nodeInstanceName, String cloudServiceName) {
		// removeNodeInstanceName(nodeInstanceName);
		Integer nb = cloudServiceUsage.get(cloudServiceName);
		if (nb != null && nb > 0){
			cloudServiceUsage.put(cloudServiceName, nb - 1);
		}
	}

	public void removeNodeInstanceName(String nodeInstanceName) {
		List<String> nodeNamesList = new ArrayList<String>(getNodeInstanceNamesList());
		while (nodeNamesList.contains(nodeInstanceName)) {
			nodeNamesList.remove(nodeInstanceName);
		}
		nodeNames = StringUtils.join(nodeNamesList, NODE_NAMES_SEPARATOR);
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

	/**
	 * Builds a list of node instance names (e.g. node.1, node.2, machine)
	 * @return node instance name
	 */
	public List<String> getNodeInstanceNamesList() {
		String[] rawNodeNames = getNodeNames().split(NODE_NAMES_SEPARATOR);
		List<String> nodeNames = new ArrayList<String>(rawNodeNames.length);

		for (int i=0; i < rawNodeNames.length; i++) {
			String nodeName = rawNodeNames[i].trim();
			if (!nodeName.isEmpty()) {
				nodeNames.add(nodeName);
			}
		}

		return nodeNames;
	}

	/**
	 * Builds a list of node instance names (e.g. nodeA, nodeB, machine)
	 * @return node names
	 */
	public List<String> getNodeNamesList() {
		List<String> groupNames = getGroupNameList();
		List<String> nodeNames = new ArrayList<String>();

		for (String groupName : groupNames) {
			String nodeName = "";
			try {
				nodeName = groupName.split(SERVICENAME_NODENAME_SEPARATOR)[1];
			} catch (IndexOutOfBoundsException ex) {
				//
			}
			if (!nodeName.isEmpty()) {
				nodeNames.add(nodeName);
			}
		}

		return nodeNames;
	}

	public List<String> getNodeInstanceNames(String nodeName) {
		Pattern INSTANCENAME = Pattern.compile("^(" + nodeName + "\\.\\d+)$");
		List<String> requested = new ArrayList<String>();
		for (String instanceName : getNodeInstanceNamesList()) {
			if (INSTANCENAME.matcher(instanceName).matches())
				requested.add(instanceName);
		}
		return requested;
	}

	public Map<String, Integer> getCloudServiceUsage() {
		return cloudServiceUsage;
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
		if (key == null) {
			throw new ValidationException("Key cannot be null");
		}

		if (runtimeParameters.containsKey(key)) {
			throw new ValidationException("Key " + key + " already exists, cannot re-define");
		}

		RuntimeParameter parameter = new RuntimeParameter(this, key, value, description);

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

	public void updateRuntimeParameters(Map<String, RuntimeParameter> runtimeParameters) {
		for (String key : runtimeParameters.keySet()) {
			this.runtimeParameters.put(key, runtimeParameters.get(key));
		}
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
		postEventStateTransition(state);
		this.state = state;
	}

	private void postEventStateTransition(States newState) {
		postEventStateTransition(newState, false);
	}

	private void postEventGarbageCollected() {
		postEvent(Event.Severity.medium, "Garbage collected");
	}

	public void postEventTerminate() {
		postEvent(Event.Severity.medium, "Terminated");
	}

	public void postEventScaleUp(String nodename, List<String> nodeInstanceNames, int nbInstancesToAdd) {
		String message = "Scaling up '" + nodename + "' with " + nbInstancesToAdd + " new instances: " + nodeInstanceNames;
		postEvent(Event.Severity.medium, message);
	}

	public void postEventScaleDown(String nodename, List<String> nodeInstanceIds) {

		int nbInstancesToDelete = 0;
		if (nodeInstanceIds != null) {
			nbInstancesToDelete = nodeInstanceIds.size();
		}

		String message = "Scaling down '" + nodename + "' by deleting " + nbInstancesToDelete +" instances: " + nodeInstanceIds;
		postEvent(Event.Severity.medium, message);
	}

	private void postEventStateTransition(States newState, boolean forcePost) {
		boolean stateWillChange = this.state != newState;
		boolean shouldPost = forcePost || stateWillChange;
		if (shouldPost) {
			postEvent(Event.Severity.medium, newState.toString());
		}
	}

	private void postEventAbort(String origin, String abortMessage) {
		String message = "Abort from '" + origin + "', message:" + abortMessage;
		postEvent(Event.Severity.high, message);
	}

	private void postEventRecoveryMode(String newValue) {
		String message = "Recovery mode set to '" + newValue + "'";
		postEvent(Event.Severity.high, message);
	}

	private void postEvent(Event.Severity severity, String message) {
		TypePrincipal owner = new TypePrincipal(USER, getUser());
		List<TypePrincipalRight> rules = Arrays.asList(
				new TypePrincipalRight(USER, getUser(), ALL),
				new TypePrincipalRight(ROLE, "ADMIN", ALL));
		ACL acl = new ACL(owner, rules);

		String resourceRef = RESOURCE_URI_PREFIX + uuid;
		Event event = new Event(acl, now(), resourceRef, message, severity, EventType.state);

		Event.post(event);
	}

	public Date getLastStateChange() {
		return this.lastStateChangeTime;
	}

	public void setLastStateChange() {
		setLastStateChange(now());
	}

	public void setLastStateChange(Date date){
		this.lastStateChangeTime = date;
	}

	public List<String> getOrchestrators() {
		List<String> orchestrators = new ArrayList<String>();

		for (String nodename : getNodeInstanceNamesList()) {
			if (nodename.startsWith(Run.ORCHESTRATOR_NAME)) {
				orchestrators.add(nodename);
			}
		}

		return orchestrators;
	}

	public void addGroup(String group, String serviceName) {
		if (!this.groups.isEmpty()) {
			this.groups += Run.NODE_NAMES_SEPARATOR;
		}
		this.groups += serviceName + SERVICENAME_NODENAME_SEPARATOR + group;
	}

	@Attribute
	@Column(length=1024)
	public String getGroups() {
		getRuntimeParameters().get(RuntimeParameter.GLOBAL_NODE_GROUPS_KEY).setValue(groups);
		return groups;
	}

	@Attribute
	@Column(length=1024)
	public void setGroups(String groups) {
		this.groups = groups;
	}

	public List<String> getGroupNameList() {
		return Arrays.asList(getGroups().split(","));
	}

	public String nodeRuntimeParameterKeyName(Node node, String nodeParameterName) {
		return node.getName() + NODE_NAME_PARAMETER_SEPARATOR + nodeParameterName;
	}

	public static String constructOrchestratorName(String cloudService) {
		return ORCHESTRATOR_NAME + ORCHESTRATOR_CLOUD_SERVICE_SEPARATOR + cloudService;
	}

	public String getCloudServiceNames() {
		return cloudServiceNames;
	}

	public void setCloudServiceNames(String cloudServiceNames) {
		this.cloudServiceNames = cloudServiceNames;
	}

	public String getCloudServiceNameForNode(String nodeName) {
		String key = RuntimeParameter.constructParamName(nodeName, RuntimeParameter.CLOUD_SERVICE_NAME);
		return getParameter(key).getValue();
	}

	public boolean isMutable() {
		return mutable;
	}

	public void setMutable() {
		this.mutable = true;
	}

	public void setImmutable() {
		this.mutable = false;
	}

//	public void remove() {
//		List<VmRuntimeParameterMapping> ms = VmRuntimeParameterMapping.getMappings(getUuid());
//		for(VmRuntimeParameterMapping m : ms) {
//			try {
//				m.remove();
//			} catch (IllegalArgumentException e) {
//
//			}
//		}
//		super.remove();
//	}

}
