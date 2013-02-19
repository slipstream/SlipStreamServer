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

import static com.sixsq.slipstream.persistence.OneShotAction.State.ABORTED;
import static com.sixsq.slipstream.persistence.OneShotAction.State.ACTIVE;
import static com.sixsq.slipstream.persistence.OneShotAction.State.DONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;

import org.junit.Test;

public abstract class OneShotActionTest {

	public abstract OneShotAction getConcreteTestInstance();

	@Test
	public void verifyAutomaticFieldsAreSet() {

		OneShotAction action = getConcreteTestInstance();

		assertNotNull(action.getUuid());
		assertFalse("".equals(action.getUuid()));

		assertEquals(ACTIVE, action.getState());

		assertNotNull(action.getForm());

		assertNotNull(action.getLastModified());
	}

	@Test
	public void verifyConfirmedStateIsInvariant() {

		OneShotAction action = getConcreteTestInstance();

		action.setState(DONE);
		action.setState(ACTIVE);

		assertEquals(DONE, action.getState());
	}

	@Test
	public void verifyCancelledStateIsInvariant() {

		OneShotAction action = getConcreteTestInstance();

		action.setState(ABORTED);
		action.setState(ACTIVE);

		assertEquals(ABORTED, action.getState());
	}

	@Test
	public void verifyUpdatingLastModifiedChangesTime()
			throws InterruptedException {

		OneShotAction action = getConcreteTestInstance();

		Timestamp initialTime = action.getLastModified();
		Thread.sleep(2000);
		action.setLastModified();
		Timestamp laterTime = action.getLastModified();

		assertTrue(initialTime.before(laterTime));
	}

}
