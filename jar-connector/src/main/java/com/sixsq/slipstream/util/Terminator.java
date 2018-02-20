package com.sixsq.slipstream.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.exceptions.ConfigurationException;
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
import com.sixsq.slipstream.statemachine.StateMachine;
import com.sixsq.slipstream.statemachine.States;

public class Terminator {

	/* I WILL BE BACK */

	private static MetricsTimer purgeTimer = Metrics.newTimer(Terminator.class, "purge");

	private static final Logger logger = Logger.getLogger(Terminator.class.getName());

	private static final String threadInfoFormatStart = "START purge run: %s, username: %s, thread name: %s, thread id: %d";

	private static final String threadInfoFormatEnd = "END purge run: %s, username: %s, thread name: %s, thread id: %d";

	public static int purge() throws ConfigurationException, ValidationException {
		// This method has been rewritten to minimize the number of users queried and
		// the number of times a user object is loaded.

		int runPurged = 0;
		purgeTimer.start();

		try {
			List<Run> runs = Run.listAllTransient();

			HashMap<String, List<Run>> batchedRuns = new HashMap<>();
			for (Run r: runs) {
				String username = r.getUser();
				if (batchedRuns.containsKey(username)) {
					List<Run> userRuns = batchedRuns.get(username);
					userRuns.add(r);
				} else {
					List<Run> userRuns = new ArrayList<>();
					userRuns.add(r);
					batchedRuns.put(username, userRuns);
				}
			}

			for (String username: batchedRuns.keySet()) {
				User user = User.loadByName(username);
				if (user != null) {
					int timeout = user.getTimeout();

					Calendar calendar = Calendar.getInstance();
					calendar.add(Calendar.MINUTE, -timeout);
					Date timeoutDate = calendar.getTime();

					for (Run r : batchedRuns.get(username)) {
						Date lastStateChange = r.getLastStateChange();
						if (lastStateChange.before(timeoutDate)) {
							EntityManager em = PersistenceUtil.createEntityManager();
							logger.log(Level.INFO,
									String.format(threadInfoFormatStart, r.getUuid(), username, Thread.currentThread().getName(),
											Thread.currentThread().getId()));
							try {
								r = Run.load(r.getResourceUri(), em);
								purgeRun(r, user);
								runPurged += 1;
							} catch (SlipStreamException e) {
								logger.log(Level.SEVERE, e.getMessage(), e.getCause());
							} finally {
								logger.log(Level.INFO, String.format(threadInfoFormatEnd, r.getUuid(), username,
										Thread.currentThread().getName(), Thread.currentThread().getId()));
								em.close();
							}
						}
					}
				} else {
					logger.log(Level.SEVERE, "could not load user " + username + " when trying to purge runs");
				}
			}

		} finally {
			purgeTimer.stop();
		}

		return runPurged;
	}

	/*
	    This method is expected to be run from within an hibernate context.
	 */
	public static void purgeRun(Run run, User user) throws SlipStreamException {
		Run.abort("The run has timed out", run.getUuid());

		boolean isGarbageCollected = Run.isGarbageCollected(run);
		Run.setGarbageCollected(run);

		String onErrorKeepRunning = run.getParameterValue(Parameter
				.constructKey(ParameterCategory.General.toString(),
						UserParameter.KEY_ON_ERROR_RUN_FOREVER), "false");
		
		if (! Boolean.parseBoolean(onErrorKeepRunning) &&
				! run.isMutable() &&
				(run.getState() == States.Initializing || isGarbageCollected)) {
			terminateInsideTransaction(run, user);
			run.postEventGarbageCollectorTerminated();
		} else if (!isGarbageCollected) {
			run.postEventGarbageCollectorTimedOut();
		}

		// This method will open a transaction, store the modified run, close the transaction,
		// and return the run that was created by the EntityManager use to store the modified
		// run.  Because that EntityManager closed its Session, further lazy loading of
		// parameters WILL FAIL.  Do this after the run termination and ignore the returned
		// run to avoid problems!
		run.store();

	}

	/*
	    This version of the terminate method is expected to run from within a
	    hibernate transactions.  You have been warned.
	 */
	public static void terminateInsideTransaction(Run run, User user) throws SlipStreamException {

		StateMachine sc = StateMachine.createStateMachine(run);

		if (sc.canCancel()) {
			sc.tryAdvanceToCancelled();
			terminateInstances(run, user);
		} else {
			if (sc.getState() == States.Ready) {
				sc.tryAdvanceToFinalizing();
			}
			terminateInstances(run, user);
			sc.tryAdvanceState(true);
		}
	}

	public static void terminate(String runResourceUri) throws SlipStreamException {

		EntityManager em = PersistenceUtil.createEntityManager();

		Run run = Run.load(runResourceUri, em);
		User user = User.loadByName(run.getUser());

		StateMachine sc = StateMachine.createStateMachine(run);

		if (sc.canCancel()) {
			sc.tryAdvanceToCancelled();
			terminateInstances(run, user);
		} else {
			if (sc.getState() == States.Ready) {
				sc.tryAdvanceToFinalizing();
			}
			terminateInstances(run, user);
			sc.tryAdvanceState(true);
		}

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
