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

import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RuntimeParameter;

public class ExtrinsicState {

	RuntimeParameter completed;
	RuntimeParameter failing;
	RuntimeParameter orchestrator;
	RuntimeParameter state;
	RuntimeParameter scaleState;

	public ExtrinsicState(RuntimeParameter completed, RuntimeParameter failing, RuntimeParameter state) {
		this(completed, failing, null, state, null);
	}

	public ExtrinsicState(RuntimeParameter completed, RuntimeParameter failing, RuntimeParameter isOrchestrator,
			RuntimeParameter state, RuntimeParameter scaleState) {
		this.completed = completed;
		this.failing = failing;
		this.orchestrator = isOrchestrator;
		this.state = state;
		this.scaleState = scaleState;
	}

	public Run getRun() {
		return state.getContainer();
	}

	public boolean isFailing() {
		return failing.isSet();
	}

	public boolean isOrchestrator() {
		boolean result = false;
		if (orchestrator != null){
			result = getParameterValueAsBoolean(orchestrator);
		}
		return result;
	}

	public boolean isRemoved() {
		boolean result = false;
		if (scaleState != null){
			String value = scaleState.getValue();
			result = RuntimeParameter.ScaleStates.removed.name().equals(value) ||
					RuntimeParameter.ScaleStates.gone.name().equals(value);
		}
		return result;
	}

	public void setFailing(boolean failing) {
		setValue(this.failing, failing);
	}

	public boolean isCompleted() {
		return getParameterValueAsBoolean(completed);
	}

	public void setCompleted(boolean completed) {
		setValue(this.completed, completed);
	}

	public States getState() {
		return States.valueOf(state.getValue());
	}

	public void setState(States state) {
		this.state.setValue(state.toString());
	}

	private void setValue(RuntimeParameter targetParameter,
			boolean newValue) {
		targetParameter.setValue(Boolean.toString(newValue));
	}

	private boolean getParameterValueAsBoolean(RuntimeParameter parameter) {
		return Boolean.parseBoolean(parameter.getValue());
	}

}
