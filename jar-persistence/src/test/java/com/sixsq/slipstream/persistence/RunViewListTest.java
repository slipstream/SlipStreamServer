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

import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.run.RunView;
import com.sixsq.slipstream.run.RunViewList;
import com.sixsq.slipstream.statemachine.States;
import org.hibernate.LazyInitializationException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;


public class RunViewListTest {

	static private User user = null;

	private static final Set<String> cloudServiceNames = new HashSet<String>(Arrays.asList("test"));
	public static final String USER = "user";
	private Run run = null;

	@BeforeClass
	public static void setupClass() throws ValidationException {

		// clean previous runs
		List<Run> runs = Run.listAll();
		runs.forEach(r -> r.remove());

		user = new User(USER);
	}

	@Before
	public void setup() throws ValidationException {
		Module image = new ImageModule();
		run = new Run(image, RunType.Run, cloudServiceNames, user);
		run = run.store();
	}

	@After
	public void tearDown() {
		run.remove();
	}

	@Test
	public void buildListWithServiceUrl() throws ValidationException, NotFoundException, AbortException {

		run.assignRuntimeParameter(RuntimeParameter.GLOBAL_URL_SERVICE_KEY, "value", "description");
		run.store();

		List<RunView> runs = RunView.fetchListView(user, 0, 20);

		assertThat(runs.size(), is(1));

		RunView v = runs.get(0);

		assertThat(v.getServiceUrl(), is("value"));
	}

	@Test
	public void buildListWithCleanRun() throws ValidationException, NotFoundException, AbortException {
		Module image = new ImageModule();

		List<RunView> runs = RunView.fetchListView(user, 0, 20);

		assertThat(runs.size(), is(1));

		RunView v = runs.get(0);

		assertNull(v.getServiceUrl());
	}


}
