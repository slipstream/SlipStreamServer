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

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Attribute;

import com.sixsq.slipstream.statemachine.States;

public class RunStates {

	public static final States SUCCESS = States.Done;
	public static final States FAILED = States.Aborted;
	public static final States FAILING = States.Aborting;
	
	@Attribute
	private States state;

	@Attribute
	private boolean isAbort = false;

	public RunStates(Run run) {
		this(extractState(run), run.isAbort());
	}

	static private States extractState(Run run) {
		return run.getState();
	}

	public RunStates(States state, boolean isAbort) {
		this.isAbort = isAbort;
		this.state = state;
		init();
	}

	private void init() {
		boolean isFinal = canTerminate(state);
		if (isAbort) {
			if (isFinal) {
				state = States.Aborted;
			} else {
				state = States.Aborting;
			}
		} else if (state == States.Terminal) {
			state = SUCCESS;
		}
	}

	private boolean canTerminate(States state) {
		return States.canTerminate().contains(state);
	}
	
	public States getState() {
		return state;
	}
	
	@Override
	public String toString() {
		return state.toString();
	}

	public void done() {
		state = canTerminate(state) ? States.Terminal
				: States.Cancelled;
		init();
	}
}
