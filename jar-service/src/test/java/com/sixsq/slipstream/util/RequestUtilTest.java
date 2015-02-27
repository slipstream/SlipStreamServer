package com.sixsq.slipstream.util;

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

import java.util.Map;

import org.junit.Test;
import org.restlet.Request;
import org.restlet.data.Method;
import org.restlet.data.Reference;

public class RequestUtilTest extends ResourceTestBase {

	@Test
	public void constructEditOptions() {

		Request r = createRequest("/something", "edit=true");
		Map<String, Object> options = RequestUtil.constructOptions(r);
		assertThat((String) options.get(RequestUtil.TYPE_KEY), is("edit"));

	}

	@Test
	public void constructNewOptionsFromQuery() {

		Request r = createRequest("/something", "new=true");
		Map<String, Object> options = RequestUtil.constructOptions(r);
		assertThat((String) options.get(RequestUtil.TYPE_KEY), is("new"));

	}

	@Test
	public void constructNewOptionsFromPath() {

		Request r = createRequest("/something/new", null);
		Map<String, Object> options = RequestUtil.constructOptions(r);
		assertThat((String) options.get(RequestUtil.TYPE_KEY), is("new"));

	}

	@Test
	public void constructChooserOptions() {

		Request r = createRequest("/something", "chooser=true");
		Map<String, Object> options = RequestUtil.constructOptions(r);
		assertThat((String) options.get(RequestUtil.TYPE_KEY), is("chooser"));

	}

	@Test
	public void constructDefaultTypeOptions() {

		Request r = createRequest("/something", null);
		Map<String, Object> options = RequestUtil.constructOptions(r);
		assertThat((String) options.get(RequestUtil.TYPE_KEY), is("view"));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void constructUrlOptions() {

		Request r = createRequest("/something", "query=value");
		Map<String, Object> options = RequestUtil.constructOptions(r);
		assertThat((String) ((Map<String, Object>) options.get(RequestUtil.REQUEST_KEY)).get(RequestUtil.URL_KEY),
				is("/something"));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void constructQueryParametersOptions() {
	
		Request r = createRequest("/something", "query1=value1&query2=value2");
		Map<String, Object> options = RequestUtil.constructOptions(r);
		Map<String, String> queryParameters = (Map<String,String>)((Map<String,Object>)options.get(RequestUtil.REQUEST_KEY)).get(RequestUtil.QUERY_PARAMETERS_KEY);
		assertThat(queryParameters.size(), is(2));
		assertThat(queryParameters.get("query1"), is("value1"));
	}

	private Request createRequest(String path, String query) {

		String url = path + (query != null ? ("?" + query) : "");
		Request r = new Request(Method.GET, url);
		r.setOriginalRef(new Reference(path));
		return r;
	}
}
