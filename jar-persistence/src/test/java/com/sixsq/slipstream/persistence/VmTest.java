package com.sixsq.slipstream.persistence;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.junit.Before;
import org.junit.Test;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;

public class VmTest {

	private static String username = "user";
	private static User user = null;

	@Before
	public void setup() throws ValidationException {
		user = new User(username);

		for(Vm vm : Vm.list(user)) {
			vm.remove();
		}
		assertThat(Vm.list(user).size(), is(0));
	}

	@Test
	public void empty() throws ConfigurationException, ValidationException {
		List<Vm> vms = new ArrayList<Vm>();
		vms = Vm.list(user);
		assertThat(vms.size(), is(0));
	}

	@Test
	public void cloudInstanceIdUserMustBeUnique() throws Exception {
		boolean exceptionOccured = false;
		boolean firstInsertAccepted = false;

		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		try {
			transaction.begin();

			String sqlInsert1 = String.format("INSERT INTO Vm (ID, CLOUD, INSTANCEID, STATE, USER_) VALUES (10, 'lokal', 'instance100', 'up', '%s');", user.getName());
			String sqlInsert2 = String.format("INSERT INTO Vm (ID, CLOUD, INSTANCEID, STATE, USER_) VALUES (10, 'lokal', 'instance100', 'down', '%s');", user.getName());

			Query query1 = em.createNativeQuery(sqlInsert1);
			Query query2 = em.createNativeQuery(sqlInsert2);

			int res = query1.executeUpdate();
			assertEquals(1, res);
			firstInsertAccepted = true;

			res = query2.executeUpdate();
			transaction.commit();
		} catch (PersistenceException pe) {
			exceptionOccured = true;
			transaction.rollback();
			boolean isCorrectException = pe.getCause().getCause().getClass() == SQLIntegrityConstraintViolationException.class;
			assertTrue("The cause of the cause sould be SQLIntegrityConstraintViolationException.", isCorrectException);
		}

		assertTrue("First insert should have worked", firstInsertAccepted);
		assertTrue("Second insert should have failed", exceptionOccured);
	}


}
