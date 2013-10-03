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

public class RunStatus {

	public static final String SUCCESS = "Done";
	public static final String FAILED = "Aborted";
	public static final String FAILING = "Aborting";

	public static final List<States> finalStates = new ArrayList<States>();
	
	static {
		finalStates.add(States.Aborted);
		finalStates.add(States.Done);
		finalStates.add(States.Terminal);
		finalStates.add(States.Cancelled);
	}
	
	@Attribute
	private String status;

	@Attribute
	private boolean isAbort = false;

	public RunStatus(Run run) {
		this(extractState(run), run.isAbort());
	}

	static private States extractState(Run run) {
		RuntimeParameter rp = run.getRuntimeParameters().get(
				RuntimeParameter.GLOBAL_STATE_KEY);
		States state = States.valueOf(rp.getValue());
		return state;
	}

	public RunStatus(States state, boolean isAbort) {
		this.isAbort = isAbort;
		status = state.toString();
		init();
	}

	private void init() {
		States state = States.valueOf(status);
		boolean isFinal = isFinal(state);
		if (isAbort) {
			if (isFinal) {
				status = FAILED;
			} else {
				status = FAILING;
			}
		} else if (state == States.Terminal) {
			status = SUCCESS;
		}
	}

	private boolean isFinal(States state) {
		return finalStates.contains(state);
	}

	@Override
	public String toString() {
		return status;
	}

	public void done() {
		status = (isFinal(States.valueOf(status)) ? States.Terminal
				: States.Cancelled).toString();
		init();
	}
}
