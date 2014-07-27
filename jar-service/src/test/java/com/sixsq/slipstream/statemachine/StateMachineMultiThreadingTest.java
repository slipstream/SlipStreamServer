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
import static org.junit.Assert.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.representation.StringRepresentation;

import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.RunFactory;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Node;
import com.sixsq.slipstream.persistence.Package;
import com.sixsq.slipstream.persistence.PersistenceUtil;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.run.RuntimeParameterResourceTestBase;
import com.sixsq.slipstream.util.CommonTestUtil;

public class StateMachineMultiThreadingTest extends
		RuntimeParameterResourceTestBase {

	private static String ORCHESTRATOR_NAME = Run.ORCHESTRATOR_NAME + "-local";
	private static int errors = 0;

	public synchronized static void incrementError() {
		errors++;
	}

	@BeforeClass
	public static void classSetup() throws ValidationException {
		RuntimeParameterResourceTestBase.classSetup();
	}

	@AfterClass
	public static void classTearDown() throws ValidationException {
		RuntimeParameterResourceTestBase.classTearDown();
	}

	protected static String createParameterName(String key, String nodename) {
		return nodename + RuntimeParameter.NODE_PROPERTY_SEPARATOR + key;
	}

	protected static String createQualifiedNodeName(String nodename, int index) {
		return nodename + RuntimeParameter.NODE_MULTIPLICITY_INDEX_SEPARATOR
				+ index;
	}

	public class ChangeStateRunnable implements Runnable {

		private String nodename;
		private Run run;
		public boolean success;

		public ChangeStateRunnable(Run run, String nodename) {
			this.nodename = nodename;
			this.run = run;
		}

		@Override
		public void run() {
			Map<String, Object> attributes = new HashMap<String, Object>();
			attributes.put("uuid", run.getUuid());
			attributes.put("key",
					createParameterName(RuntimeParameter.COMPLETE_KEY));

			Request request = createPostRequest(attributes,
					new StringRepresentation(""));

			Response response = executeRequest(request);

			success = response.getStatus().isSuccess();

			if (!success) {
				StateMachineMultiThreadingTest.incrementError();
			}
		}

		private String createParameterName(String key) {
			return StateMachineMultiThreadingTest.createParameterName(key,
					nodename);
		}

	}

	private static final int MULTIPLICITY = 50;
	private static final String NODE_NAME = "node1";
	private static final boolean MULTI_THREADED = true;

	private States initialState = States.Executing;
	private States finalState = States.SendingReports;

	@Test
	public void testMultipleStateTransitions() throws FileNotFoundException,
			IOException, SlipStreamException, InterruptedException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException,
			ClassNotFoundException {

		errors = 0;

		CommonTestUtil
				.resetAndLoadConnector(com.sixsq.slipstream.connector.local.LocalConnector.class);

		EntityManager em = PersistenceUtil.createEntityManager();

		Run run = initRun(em);

		em.close();

		List<Thread> threads = execute(run, MULTI_THREADED);

		waitAllThreadsComplete(threads);

		try {
			assertTransitionReached(Run.load(run.getResourceUri()), finalState);
			assertThat(errors, is(0));
		} finally {
			run.remove();
		}
	}

	private Run initRun(EntityManager em) throws FileNotFoundException,
			IOException, SlipStreamException {

		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		Run run = createRunDeployment("testMultipleStateTransitions");
		run.getRuntimeParameters().get(RuntimeParameter.GLOBAL_STATE_KEY)
				.setValue(initialState.name());
		run = em.merge(run);
		transaction.commit();
		return run;
	}

	private List<Thread> execute(Run run, boolean multithreaded) {
		List<Thread> threads = new ArrayList<Thread>();

		if (multithreaded) {
			for (int i = 1; i <= MULTIPLICITY; i++) {
				Thread thread = new Thread(new ChangeStateRunnable(run,
						createQualifiedNodeName(NODE_NAME, i)));
				thread.start();
				threads.add(thread);
			}
			Thread thread = new Thread(new ChangeStateRunnable(run,
					ORCHESTRATOR_NAME));
			thread.start();
			threads.add(thread);
		} else {
			ChangeStateRunnable r;
			for (int i = 1; i <= MULTIPLICITY; i++) {
				r = new ChangeStateRunnable(run, createQualifiedNodeName(
						NODE_NAME, i));
				r.run();
			}
			r = new ChangeStateRunnable(run, ORCHESTRATOR_NAME);
			r.run();

		}

		return threads;

	}

	private void waitAllThreadsComplete(List<Thread> threads)
			throws InterruptedException {
		for (Thread thread : threads) {
			thread.join();
		}
	}

	private Run createRunDeployment(String moduleName)
			throws FileNotFoundException, IOException, SlipStreamException {
		image = new ImageModule(moduleName);
		image.setModuleReference(baseImage);
		image.getPackages().add(new Package("package1"));
		image.setModuleReference(baseImage);
		image.setImageId("123", cloudServiceName);

		Node node = new Node(NODE_NAME, image);
		node.setMultiplicity(MULTIPLICITY);
		node.setCloudService(cloudServiceName);

		deployment = new DeploymentModule("createAndStoreRunDeployment");
		deployment.setNode(node);

		deployment.store();

		Run run = RunFactory.getRun(deployment, RunType.Orchestration, user);
		return run;
	}

	private void assertTransitionReached(Run run, States expectedState)
			throws NotFoundException, AbortException {

		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();

		run = em.find(Run.class, run.getResourceUri());

		String actualState = run
				.getRuntimeParameterValue(RuntimeParameter.GLOBAL_STATE_KEY);

		for (int i = 1; i <= MULTIPLICITY; i++) {
			String key = createParameterName(RuntimeParameter.COMPLETE_KEY,
					createQualifiedNodeName(NODE_NAME, i));
			String complete = run.getRuntimeParameterValue(key);
			assertThat(complete, is("false"));
		}

		String key = createParameterName(RuntimeParameter.COMPLETE_KEY,
				ORCHESTRATOR_NAME);
		String complete = run.getRuntimeParameterValue(key);
		System.out.println("Node: " + ORCHESTRATOR_NAME + " is " + complete);

		assertThat(States.valueOf(actualState), is(expectedState));

		transaction.commit();
		em.close();

	}
}
