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


public abstract class State {

	protected ExtrinsicState extrinsicState;
	protected States nextState;

	protected State(ExtrinsicState extrinsicState) {
		this.extrinsicState = extrinsicState;
		this.extrinsicState.state.setValue(getState().toString());
	}

	public ExtrinsicState getExtrinsicState() {
		return extrinsicState;
	}

	public boolean synchronizedForEveryone() {
		return false;
	}

	public boolean synchronizedForOrchestrators() {
		return false;
	}

	public abstract States getState();

	public boolean isFinal() {
		return false;
	}

	@Override
	public String toString() {
		return getState().toString();
	}

	public boolean isStateCompleted() {
		return extrinsicState.isCompleted();
	}

	public boolean isOrchestrator() {
		return extrinsicState.isOrchestrator();
	}

	public boolean isRemoved() {
		return extrinsicState.isRemoved();
	}

	public void setStateCompleted(boolean completed) {
		extrinsicState.setCompleted(completed);
	}

	public boolean isFailing() {
		return extrinsicState.isFailing();
	}

	public void setFailing(boolean failing) {
		extrinsicState.setFailing(failing);
	}

}
