package com.sixsq.slipstream.configuration;

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

import com.sixsq.slipstream.event.Event;
import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.*;
import com.sixsq.slipstream.ssclj.app.CIMITestServer;
import com.sixsq.slipstream.statemachine.States;
import org.hibernate.LazyInitializationException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;


public class ConfigurationTest {

	@BeforeClass
	public static void setupClass() {
		CIMITestServer.start();
		Event.muteForTests();
	}

	@AfterClass
	public static void teardownClass() {
		CIMITestServer.stop();
	}

	@Test
	public void updatesOverCIMI() {

	}

}
