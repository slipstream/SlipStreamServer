package com.sixsq.slipstream.run;

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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.restlet.data.Form;
import org.restlet.resource.ResourceException;

public class RunNodeResourceTest {
	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Test
	public void getNumberOfInstancesToAdd() {
		assert Integer.parseInt(RunNodeResource.NUMBER_INSTANCES_ADD_DEFAULT) ==
				RunNodeResource.getNumberOfInstancesToAdd(new Form(""));
		assert Integer.parseInt(RunNodeResource.NUMBER_INSTANCES_ADD_DEFAULT) ==
				RunNodeResource.getNumberOfInstancesToAdd(new Form("n=1"));
		assert 0 == RunNodeResource.getNumberOfInstancesToAdd(new Form("n=0"));
		assert 0 == RunNodeResource.getNumberOfInstancesToAdd(new Form("n=0&foo=bar"));
		assert 0 == RunNodeResource.getNumberOfInstancesToAdd(new Form("foo=bar&n=0"));

		exception.expect(ResourceException.class);
		RunNodeResource.getNumberOfInstancesToAdd(new Form("n="));

		exception.expect(ResourceException.class);
		RunNodeResource.getNumberOfInstancesToAdd(new Form("foo=bar"));
	}
}

