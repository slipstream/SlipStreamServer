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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.sixsq.slipstream.statemachine.States;

public class RunStatusTest {

	@Test
	public void verifyRunningSuccess() {

		assertEquals(States.Running,
				new RunStates(States.Running, false).getState());
	}

	@Test
	public void verifyRunningWhileAborting() {

		assertEquals(RunStates.FAILING,
				new RunStates(States.Running, true).getState());
	}

	@Test
	public void verifyFinalStateWithSuccess() {

		assertEquals(RunStates.SUCCESS,
				new RunStates(States.Terminal, false).getState());
	}

	@Test
	public void verifyFinalStateAndAborted() {

		assertEquals(RunStates.FAILED,
				new RunStates(States.Terminal, true).getState());
	}

	@Test
	public void verifyDoneFromNotFinal() {

		boolean isAborted = false;
		RunStates rs = new RunStates(States.Inactive, isAborted);
		rs.done();
		assertEquals(States.Cancelled.toString(), rs.toString());
	}

	@Test
	public void verifyDoneFromNotFinalAborted() {

		boolean isAborted = true;
		RunStates rs = new RunStates(States.Running, isAborted);
		rs.done();
		assertEquals(States.Aborted.toString(), rs.toString());
	}

	@Test
	public void verifyDoneFromFinal() {

		boolean isAborted = false;
		RunStates rs = new RunStates(States.Terminal, isAborted);
		rs.done();
		assertEquals(States.Done.toString(), rs.toString());

		rs = new RunStates(States.Detached, isAborted);
		rs.done();
		assertEquals(States.Done.toString(), rs.toString());
	}

	@Test
	public void verifyDoneFromFinalAborted() {

		boolean isAborted = true;
		RunStates rs = new RunStates(States.Terminal, isAborted);
		rs.done();
		assertEquals(States.Aborted.toString(), rs.toString());
	}

}
