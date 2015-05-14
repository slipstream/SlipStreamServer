package com.sixsq.slipstream.util;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;

import com.sixsq.slipstream.persistence.*;
import com.sixsq.slipstream.statemachine.State;
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
import com.sixsq.slipstream.statemachine.StateMachine;
import com.sixsq.slipstream.statemachine.States;

public class Terminator {

	/* I WILL BE BACK */

	public static int purge() throws ConfigurationException, ValidationException {
		int runPurged = 0;

		List<User> users = User.list();
		for (User u : users) {
			u = User.loadByName(u.getName());
			int timeout = u.getTimeout();

			List<Run> old = Run.listOldTransient(u, timeout);
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

		return runPurged;
	}

	public static void purgeRun(Run run) throws SlipStreamException {

		boolean isGarbageCollected = Run.isGarbageCollected(run);
		Run.setGarbageCollected(run);
		run = run.store();

		String onErrorKeepRunning = run.getParameterValue(
				Parameter.constructKey(ParameterCategory.General.toString(), UserParameter.KEY_ON_ERROR_RUN_FOREVER),
				"false");

		// user wants to run forever?
		boolean terminateOnError = !Boolean.parseBoolean(onErrorKeepRunning);
		// for mutable run we never transition to cancel on error
		boolean notMutable = !run.isMutable();
		// state from which we should terminate
		boolean shouldTerminate = (run.getState() == States.Initializing) || (run.getState() == States.Provisioning)
				|| (run.getState() == States.Executing)|| (run.getState() == States.Ready) || isGarbageCollected;

		if (terminateOnError && notMutable && shouldTerminate) {
			terminate(run.getResourceUri());
		}

		if(run.getState() != States.Done) {
			Run.abort("The run has timed out", run.getUuid());
		}
	}

	public static void terminate(String runResourceUri) throws ValidationException,
			CannotAdvanceFromTerminalStateException, InvalidStateException {

		EntityManager em = PersistenceUtil.createEntityManager();
		em.getTransaction().begin();

		try {
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
			run = run.store();

		} finally {
			if(em.getTransaction().isActive()) {
				em.getTransaction().commit();
			}
			em.close();
		}
	}

	private static void terminateInstances(Run run, User user) throws ValidationException {
		user.addSystemParametersIntoUser(Configuration.getInstance().getParameters());

		for (ConnectorInstance cloudServiceName : run.getCloudServices()) {
			Connector connector = ConnectorFactory.getConnector(cloudServiceName.getName());
			try {
				connector.terminate(run, user);
			} catch (SlipStreamException e) {
				throw new ResourceException(Status.CLIENT_ERROR_CONFLICT, "Failed terminating VMs", e);
			}
		}
	}

}
