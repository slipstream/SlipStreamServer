package com.sixsq.slipstream.util;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.exceptions.CannotAdvanceFromTerminalStateException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.InvalidStateException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.metrics.Metrics;
import com.sixsq.slipstream.metrics.MetricsTimer;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.PersistenceUtil;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;
//import com.sixsq.slipstream.statemachine.StateMachine;
import com.sixsq.slipstream.statemachine.States;

public class Terminator {

	/* I WILL BE BACK */

	private static MetricsTimer purgeTimer = Metrics.newTimer(Terminator.class, "purge");

	public static int purge() throws ConfigurationException, ValidationException {
		int runPurged =0;

		purgeTimer.start();
		try {
			List<User> users = User.list();
			for (User u: users) {
				u = User.loadByName(u.getName());
				int timeout = u.getTimeout();

				List<Run> old = Run.listOldTransient(u, timeout); //TODO kb
				for (Run r : old) {
					EntityManager em = PersistenceUtil.createEntityManager();
					try {
						r = Run.load(r.getResourceUri(), em);
						purgeRun(r);
					} catch (SlipStreamException e) {
						Logger.getLogger("garbage-collector").log(Level.SEVERE, e.getMessage(), e.getCause());
					} finally {
						em.close();
					}
				}
				runPurged += old.size();
			}
		} finally {
			purgeTimer.stop();
		}

		return runPurged;
	}

	public static void purgeRun(Run run) throws SlipStreamException {
		Run.abort("The run has timed out", run.getUuid());

		boolean isGarbageCollected = Run.isGarbageCollected(run); //TODO kb
		Run.setGarbageCollected(run);
		run = run.store();

		String onErrorKeepRunning = run.getParameterValue(Parameter
				.constructKey(ParameterCategory.General.toString(),
						UserParameter.KEY_ON_ERROR_RUN_FOREVER), "false");

		if (! Boolean.parseBoolean(onErrorKeepRunning) &&
				! run.isMutable() &&
				(run.getState() == States.Initializing || isGarbageCollected)) {
			terminate(run.getResourceUri());
			run.postEventGarbageCollectorTerminated();
		} else if (!isGarbageCollected) {
			run.postEventGarbageCollectorTimedOut();
		}

	}

	public static void terminate(String runResourceUri) throws SlipStreamException {

		EntityManager em = PersistenceUtil.createEntityManager();

		Run run = Run.load(runResourceUri, em);
		User user = User.loadByName(run.getUser());

		//StateMachine sc = StateMachine.createStateMachine(run);

		//if (sc.canCancel()) {
		//	sc.tryAdvanceToCancelled();
		//	terminateInstances(run, user);
		//} else {
		//	if (sc.getState() == States.Ready) {
		//		sc.tryAdvanceToFinalizing();
		//	}
		//	terminateInstances(run, user);
		//	sc.tryAdvanceState(true);
		//}

		terminateInstances(run, user);

		em.close();
	}

	private static void terminateInstances(Run run, User user) throws SlipStreamException {
		user.addSystemParametersIntoUser(Configuration.getInstance().getParameters());

		for (String cloudServiceName : run.getCloudServiceNamesList()) {
			Connector connector = ConnectorFactory.getConnector(cloudServiceName);
			connector.terminate(run, user);
		}
	}

}
