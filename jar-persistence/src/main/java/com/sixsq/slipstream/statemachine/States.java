package com.sixsq.slipstream.statemachine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

public enum States {
    Inactive,
    Initializing,
    Running,
    SendingFinalReport,
    Disconnected,
    Finalizing,
    Terminal, 
    Unknown,
    Done,
    Cancelled,
    Aborting,
    Aborted,
    Failing,
    Failed,
    Detached;
    
    public static List<States> inactive() {
    	List<States> list = new ArrayList<States>(completed());
    	list.add(States.Inactive);
    	return list;
    }
    
    public static List<States> completed() {
    	return Arrays.asList(States.Cancelled, 
    						 States.Terminal,
    						 States.Aborted,
    						 States.Failed,
    						 States.Unknown,
    						 States.Done);
    }

    public static List<States> transition() {
    	return Arrays.asList(Initializing,
    						 Running,
    						 SendingFinalReport,
    						 Finalizing,
    						 Unknown,
    						 Aborting,
    						 Failing);
    }
}
