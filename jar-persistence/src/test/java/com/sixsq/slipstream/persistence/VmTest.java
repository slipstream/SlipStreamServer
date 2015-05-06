package com.sixsq.slipstream.persistence;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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
		try {
			EntityManager em = PersistenceUtil.createEntityManager();
			EntityTransaction transaction = em.getTransaction();
			transaction.begin();

			String sqlInsert1 = String.format("INSERT INTO Vm VALUES (10, 'lokal', 'instance100', null, null, null, 'up', '%s', null, null, null, null, null)", user);
			String sqlInsert2 = String.format("INSERT INTO Vm VALUES (20, 'lokal', 'instance100', null, null, null, 'down', '%s', null, null, null, null, null)", user);

			Query query1 = em.createNativeQuery(sqlInsert1);
			Query query2 = em.createNativeQuery(sqlInsert2);

			assertEquals(1, query1.executeUpdate());
			firstInsertAccepted = true;

			query2.executeUpdate();
			transaction.commit();
		} catch (PersistenceException pe) {
			exceptionOccured = true;
		}

		assertTrue("First insert should have worked", firstInsertAccepted);
		assertTrue("Second insert should have failed", exceptionOccured);
	}


}
