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

import org.simpleframework.xml.Attribute;

import com.sixsq.slipstream.statemachine.States;

public class RunStatus {

	public static final String SUCCESS = "Done";
	public static final String FAILED = "Aborted";
	public static final String FAILING = "Aborting";

	@Attribute
	private String status;
	
    public RunStatus(States state, boolean isAbort) {
    	init(state, isAbort);
    }
    
    private void init(States state, boolean isAbort) {
    	boolean isFinal = (state == States.Terminal);
    	if(!isAbort) {
    		if(isFinal) {
        		this.status = SUCCESS;
    		} else {
    			this.status = state.toString();
    		}
    	} else {
    		if(isFinal) {
    			this.status = FAILED;
    		} else {
    			this.status = FAILING;
    		}
    	}
    }

    public RunStatus(Run run) {
    	States state = States.valueOf(run.getRuntimeParameters().get(RuntimeParameter.GLOBAL_STATE_KEY).toString());
    	init(state, run.isAbort());
    }

    @Override
    public String toString() {
    	return status;
    }
}
