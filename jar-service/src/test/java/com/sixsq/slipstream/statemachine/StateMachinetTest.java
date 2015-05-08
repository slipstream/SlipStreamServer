package com.sixsq.slipstream.statemachine;

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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sixsq.slipstream.connector.local.LocalConnector;
import com.sixsq.slipstream.event.Event;
import com.sixsq.slipstream.exceptions.CannotAdvanceFromTerminalStateException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.InvalidStateException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.RunFactory;
import com.sixsq.slipstream.persistence.CloudImageIdentifier;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Node;
import com.sixsq.slipstream.persistence.Package;
import com.sixsq.slipstream.persistence.PersistenceUtil;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;
import com.sixsq.slipstream.util.CommonTestUtil;

public class StateMachinetTest {

	private ExtrinsicState globalExtrinsicState;
	private Map<String, ExtrinsicState> nodeExtrinsicStates = new HashMap<String, ExtrinsicState>();
	private Run run = null;
	private static User user = null;
	private final String cloudName = "dummy";
	private final String orchName = "orchestrator-" + cloudName;

	protected static String cloudServiceName = new LocalConnector()
			.getCloudServiceName();

	@BeforeClass
	public static void beforeClass() throws ConfigurationException,
			ValidationException {

		Event.muteForTests();

		user = CommonTestUtil.createTestUser();

		CommonTestUtil.addSshKeys(user);
	}

	@AfterClass
	public static void tearDownAfterClass() throws ValidationException {
		CommonTestUtil.deleteUser(user);
		removeRuns();
		List<Run> runs = Run.listAll();
		assertEquals(0, runs.size());
	}

	private static void removeRuns() throws ValidationException {
		for (Run run : Run.listAll()) {
			run.remove();
		}
	}

	private void generateDeploymentAndRun(String[] nodeNames) throws SlipStreamException {

		// Run can be created only on a deployment module with nodes.
		DeploymentModule module = generateDummyDeploymentModuleWithNode(nodeNames);
		run = RunFactory.getRun(module, RunType.Orchestration, user);
		run = run.store();
	}

	private DeploymentModule generateDummyDeploymentModuleWithNode(String[] nodeNames) throws ValidationException,
	        ConfigurationException {

		DeploymentModule module = new DeploymentModule("setUp");


		ImageModule image = new ImageModule("foo_image");
		image.setIsBase(true);
		image.setImageId("123", cloudName);
		image.store();

		if (nodeNames.length > 0) {
			for (String nodename : nodeNames) {
				Node node = new Node(nodename, image);
				node.setCloudService(cloudName);
				node = (Node) node.store();
				module.setNode(node);
			}
		} else {
			// Add fake node if none were provided.  Run needs nodes on the deployment module.
    		Node node = new Node("fake", image);
    		node.setCloudService(cloudName);
    		node = (Node) node.store();
    		module.setNode(node);
		}

		return module;
	}

	private StateMachine createStateMachine(String[] nodeInstanceNames) throws SlipStreamException {
		generateDeploymentAndRun(nodeInstanceNamesToNodeNames(nodeInstanceNames));
		StateMachine sc = createStateContext(nodeInstanceNames);
		return sc;
	}

	private String[] nodeInstanceNamesToNodeNames(String[] nodeInstanceNames) {
		String[] nodeNames = new String[nodeInstanceNames.length];
		for (int i = 0; i < nodeNames.length; i++) {
			nodeNames[i] = nodeInstanceNames[i].replaceAll("\\.[0-9]$", "");
        }
		return nodeNames;
	}

	@After
	public void tearDown() {
		globalExtrinsicState = null;
		if (run != null) {
			run.remove();
			run = null;
		}
	}

	@Test
	public void initialEmptyState() throws IllegalArgumentException,
			SecurityException, ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, SlipStreamException {
		StateMachine sc = createStateMachine(new String[] {});

		assertEquals(States.Initializing, sc.getState());
	}

	@Test
	public void initialState() throws InvalidStateException,
			SlipStreamException {
		StateMachine sc = createStateMachine(new String[] { "n1.1" });

		assertEquals(States.Initializing, sc.getState());
	}

	@Test
	public void globalProvisioningState() throws SlipStreamException {
		StateMachine sc = createStateMachine(new String[] { "n1.1" });
		sc.start();

		assertEquals(States.Provisioning, sc.getState());
	}

	@Test
	public void fullNominalWorkflow() throws SlipStreamException {
		StateMachine sc = createStateMachine(new String[] { "n1.1", "n2.1" });
		sc.start();

		assertState(sc, States.Provisioning);

		EntityManager em = sc.beginTransation();
		sc.updateState("n1.1");
		sc.commitTransaction(em);

		assertState(sc, States.Provisioning);

		sc.updateState("n2.1");
		sc.updateState(orchName);

		assertState(sc, States.Executing);

		sc.updateState("n1.1");

		assertState(sc, States.Executing);

		sc.updateState("n2.1");
		sc.updateState(orchName);

		assertState(sc, States.SendingReports);

		sc.updateState("n1.1");

		assertState(sc, States.SendingReports);

		sc.updateState("n2.1");
		sc.updateState(orchName);

		assertState(sc, States.Ready);

		sc.updateState("n1.1");

		assertState(sc, States.Ready);

		sc.updateState("n2.1");
		sc.updateState(orchName);

		assertState(sc, States.Finalizing);

		sc.updateState("n1.1");
		sc.updateState(orchName);

		em = PersistenceUtil.createEntityManager();
		run = em.find(Run.class, run.getResourceUri());
		assertThat(run.getRuntimeParameterValue("ss:state"),
				is(States.Done.toString()));
		em.close();

		assertState(sc, States.Done);

	}

	private void assertState(StateMachine sc, States state) {
		assertEquals(state, sc.getState());
	}

	@Test(expected = SlipStreamClientException.class)
	public void inexistantNodeName() throws SlipStreamException {
		StateMachine sc = createStateMachine(new String[] { "n1.1", "n2.1" });

		sc.updateState("doesn-t-exist");
	}

	@Test
	public void invalidNodeName() throws SlipStreamException {
		createStateMachine(new String[] { "111_starting_with_int.1" });
	}

	@Test
	public void doneIsFinal() throws SlipStreamException {

		createStateMachine(new String[] { "n1.1"});

		ExtrinsicState extrinsicState = getNodeExtrinsicState("n1.1");

		State done = new DoneState(extrinsicState);

		assertTrue(done.isFinal());
	}

	@Test
	public void failureDuringProvisioning() throws IllegalArgumentException,
			SecurityException, ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, SlipStreamException {
		StateMachine sc = createStateMachine(new String[] { "n1.1", "n2.1" });
		sc.start();

		assertEquals(States.Provisioning, sc.getState());

		sc.updateState("n1.1");
		assertEquals(States.Provisioning, sc.getState());

		sc.fail("n2.1");

		assertEquals(States.SendingReports, sc.getState());
		sc.updateState("n2.1");
	}

	@Test
	public void failureDuringExecuting() throws InvalidStateException,
			SlipStreamException {
		String failingNodeName = "n1.1";
		StateMachine sc = createStateMachine(new String[] { failingNodeName, "n2.1" });
		assertEquals(States.Initializing, sc.getState());
		sc.start();

		assertEquals(States.Provisioning, sc.getState());
		sc.updateState(failingNodeName);
		assertEquals(States.Provisioning, sc.getState());
		sc.updateState("n2.1");
		sc.updateState(orchName);
		assertEquals(States.Executing, sc.getState());

		sc.failCurrentState(failingNodeName);
		assertEquals(true, sc.isFailing());

		assertEquals(States.Executing, sc.getState());
		assertTrue(sc.isFailing());

		sc.updateState("n2.1");
		sc.updateState(orchName);

		assertEquals(States.Executing, sc.getState());
		assertTrue(sc.isFailing());

		sc.updateState(failingNodeName);

		assertEquals(States.SendingReports, sc.getState());
		sc.updateState(failingNodeName);

		assertEquals(States.SendingReports, sc.getState());
		sc.updateState("n2.1");
		sc.updateState(orchName);

		assertEquals(States.Ready, sc.getState());
		sc.updateState(failingNodeName);

		assertEquals(States.Ready, sc.getState());
		sc.updateState("n2.1");
		sc.updateState(orchName);

		assertEquals(States.Finalizing, sc.getState());
		sc.updateState("n2.1");
		sc.updateState(orchName);

		assertEquals(States.Aborted, sc.getState());
	}

	@Test
	public void onErrorKeepRunningInBuildImage() throws InvalidStateException,
			SlipStreamException {

		User user = CommonTestUtil.createUser("user1");
		user.setKeepRunning(UserParameter.KEEP_RUNNING_ALWAYS);
		user.store();

		ImageModule parent = new ImageModule("test/parent");
		Set<CloudImageIdentifier> cloudImageIdentifiers = new HashSet<CloudImageIdentifier>();
		cloudImageIdentifiers.add(new CloudImageIdentifier(parent, cloudServiceName, "image-id"));
		parent.setCloudImageIdentifiers(cloudImageIdentifiers);
		parent.store();

		ImageModule module = new ImageModule("test/image-module");
		module.setModuleReference(parent);
		module.setPackage(new Package("hello"));

		Run run = RunFactory.getRun(module, RunType.Machine, user);
		run.store();

		StateMachine sc = StateMachine.getStateMachine(run);
		assertEquals(States.Initializing, sc.getState());
		sc.start();

		assertEquals(States.Provisioning, sc.getState());
		sc.tryAdvanceState(true);

		assertEquals(States.Executing, sc.getState());
		run = Run.abort("Error in build image", run.getUuid());
		sc = StateMachine.getStateMachine(run);
		assertTrue(sc.isFailing());
		sc.tryAdvanceState(true);

		assertEquals(States.SendingReports, sc.getState());
		sc.tryAdvanceState(true);

		assertEquals(States.Ready, sc.getState());
		sc.tryAdvanceState(true);

		assertEquals(States.Finalizing, sc.getState());
		sc.tryAdvanceState(true);

		assertEquals(States.Aborted, sc.getState());

		run.remove();
		parent.remove();
		user.remove();
	}

	@Test
	public void onSuccessKeepRunningInBuildImage() throws InvalidStateException,
			SlipStreamException {

		User user = CommonTestUtil.createUser("user1");
		user.setKeepRunning(UserParameter.KEEP_RUNNING_ALWAYS);
		user.store();

		ImageModule parent = new ImageModule("test/parent");
		Set<CloudImageIdentifier> cloudImageIdentifiers = new HashSet<CloudImageIdentifier>();
		cloudImageIdentifiers.add(new CloudImageIdentifier(parent, cloudServiceName, "image-id"));
		parent.setCloudImageIdentifiers(cloudImageIdentifiers);
		parent.store();

		ImageModule module = new ImageModule("test/image-module");
		module.setModuleReference(parent);
		module.setPackage(new Package("hello"));

		Run run = RunFactory.getRun(module, RunType.Machine, user);
		run.store();

		StateMachine sc = StateMachine.getStateMachine(run);
		assertEquals(States.Initializing, sc.getState());
		sc.start();

		assertEquals(States.Provisioning, sc.getState());
		sc.tryAdvanceState(true);

		assertEquals(States.Executing, sc.getState());
		assertTrue(!sc.isFailing());
		sc.tryAdvanceState(true);

		assertEquals(States.SendingReports, sc.getState());
		sc.tryAdvanceState(true);

		assertEquals(States.Ready, sc.getState());
		sc.tryAdvanceState(true);

		assertEquals(States.Finalizing, sc.getState());
		sc.tryAdvanceState(true);

		assertEquals(States.Done, sc.getState());

		run.remove();
		parent.remove();
		user.remove();
	}

	@Test(expected = CannotAdvanceFromTerminalStateException.class)
	public void cannotAdvanceFromTerminalState()
			throws IllegalArgumentException, SecurityException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, SlipStreamException {

		StateMachine sc = createStateMachine(new String[] { "n1.1" });
		sc.start();

		EntityManager em = sc.beginTransation();
		sc.setState(States.Done, true);
		sc.commitTransaction(em);

		assertThat(sc.getState(), is(States.Done));

		em = PersistenceUtil.createEntityManager();
		run = em.find(Run.class, run.getResourceUri());
		assertThat(run.getRuntimeParameterValue("ss:state"),
				is(States.Done.toString()));
		em.close();

		sc.updateState("n1.1");
	}

	private StateMachine createStateContext(String[] nodes)
			throws InvalidStateException, SlipStreamException {

		Map<String, State> nodeStates = new HashMap<String, State>();
		for (String nodeName : nodes) {
			State state = createState(nodeName);
			nodeStates.put(nodeName, state);
		}

		if (globalExtrinsicState == null) {
			globalExtrinsicState = StateMachine.createGlobalExtrinsicState(run);
		}

		State globalState = StateFactory.createInstance(
				globalExtrinsicState.getState(), globalExtrinsicState);
		StateMachine sc = new StateMachine(nodeStates, globalState, run);
		return sc;
	}

	private State createState(String nodeName) throws SlipStreamException,
			InvalidStateException {
		ExtrinsicState extrinsicState = getNodeExtrinsicState(nodeName);
		return StateFactory.createInstance(extrinsicState.getState(),
				extrinsicState);
	}

	private ExtrinsicState getNodeExtrinsicState(String nodeName)
			throws SlipStreamException {

		if (!nodeExtrinsicStates.containsKey(nodeName)) {
			ExtrinsicState extrinsicState = StateMachine
					.createNodeExtrinsicState(run, nodeName);
			nodeExtrinsicStates.put(nodeName, extrinsicState);
		}

		return nodeExtrinsicStates.get(nodeName);
	}

}
