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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.restlet.data.Form;

import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;

public class NodeParameterTest {

	@Test
	public void validateValue() throws SlipStreamClientException {
		NodeParameter.validate(new NodeParameter(RuntimeParameter.GLOBAL_STATE_KEY, "'value_with_quote'"));
		NodeParameter.validate(new NodeParameter(RuntimeParameter.GLOBAL_STATE_KEY, "\"value_with_quote\""));
		try {
			NodeParameter.validate(new NodeParameter(RuntimeParameter.GLOBAL_STATE_KEY, "value_no_quote"));
			fail();
		} catch(ValidationException ex) {
			// OK
		}
		
		NodeParameter.validate(new NodeParameter(RuntimeParameter.GLOBAL_STATE_KEY, Run.MACHINE_NAME_PREFIX + "a_value"));
	}
	
	@Test
	public void nodeParametersParsing() throws ValidationException {
		Form form = new Form();
		form.add("parameter--node--n1--p1", "'value1'");
		form.add("parameter--node--n1--p2", "'value2'");
		form.add("parameter--node--n2--pa", "'valuea'");
		form.add("parameter--node--n2--pb", "'valueb'");
		Map<String, List<NodeParameter>> parsed = NodeParameter.parseNodeNameOverride(form);
		assertThat(parsed.size(), is(2));
		assertThat(parsed.get("n1").size(), is(2));
		assertThat(parsed.get("n1").get(0).getName(), is("p1"));
		assertThat(parsed.get("n1").get(0).getValue(), is("'value1'"));
	}

}
