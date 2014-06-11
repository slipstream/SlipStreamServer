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

import com.sixsq.slipstream.exceptions.InvalidStateException;

public class StateFactory {

    public static State createInstance(ExtrinsicState extrinsicState)
            throws InvalidStateException {
        return createInstance(extrinsicState.getState(), extrinsicState);
    }

    public static State createInstance(States state,
            ExtrinsicState extrinsicState) throws InvalidStateException {
        State newState = null;
        switch (state) {
        case Initializing:
            newState = new InitializingState(extrinsicState);
            break;
        case Provisioning:
            newState = new ProvisioningState(extrinsicState);
            break;
        case Executing:
            newState = new ExecutingState(extrinsicState);
            break;
        case SendingReports:
            newState = new SendingReportsState(extrinsicState);
            break;
        case Ready:
        	newState = new ReadyState(extrinsicState);
        	break;
        case Finalizing:
            newState = new FinalizingState(extrinsicState);
            break;
        case Done:
        	newState = new DoneState(extrinsicState);
        	break;
        case Cancelled:
        	newState = new CancelledState(extrinsicState);
        	break;
        case Aborted:
        	newState = new AbortedState(extrinsicState);
        	break;
        default:
            throw (new InvalidStateException("Unknown state: " + state));
        }

        // TODO: LS: I think it's useless. Check.
        newState.getExtrinsicState().setState(state);

        return newState;
    }

}
