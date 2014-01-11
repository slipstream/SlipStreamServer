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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Module;

public class ModuleUriUtilTest {

	private final static String testName = "alpha/beta/gamma";
	private final static String testUriNoVersion = Module.RESOURCE_URI_PREFIX + testName;
	private final static String testUri = testUriNoVersion + "/123";
	private final static String testUriDefaultVersion = testUriNoVersion + "/" + Module.DEFAULT_VERSION;
	private final static String testUriRoot = Module.RESOURCE_URI_PREFIX;
	private final static String testUriRootNoTrailingSlash = Module.RESOURCE_URI_PREFIX.replace("/", "");
	private final static String testUriWithTrailingSlash = testUriNoVersion + "/";

	@Test
	public void extractShortNameFromResourceUri() {

		assertEquals("gamma", ModuleUriUtil
				.extractShortNameFromResourceUri(testUri));
		assertEquals("gamma", ModuleUriUtil
				.extractShortNameFromResourceUri(testUriNoVersion));
	}

	@Test
	public void extractVersionFromResourceUrl() {

		assertEquals(123, ModuleUriUtil
				.extractVersionFromResourceUri(testUri));
	}

	@Test
	public void extractVersionFromResourceUriWithNoVersion() {

		assertEquals(-1, ModuleUriUtil
				.extractVersionFromResourceUri(testUriNoVersion));
		assertEquals(-1, ModuleUriUtil
				.extractVersionFromResourceUri(testUriRoot));
	}

	@Test
	public void extractModuleUriFromResourceUri() {

		assertEquals(testUriNoVersion, ModuleUriUtil
				.extractModuleUriFromResourceUri(testUriNoVersion));
		assertEquals(testUri, ModuleUriUtil
				.extractModuleUriFromResourceUri(testUri));
		assertEquals("module/", ModuleUriUtil
				.extractModuleUriFromResourceUri(testUriRoot));
		assertEquals("module", ModuleUriUtil
				.extractModuleUriFromResourceUri(testUriRootNoTrailingSlash));
	}

	@Test
	public void extractModuleNameFromResourceUri() {

		assertEquals(testName, ModuleUriUtil
				.extractModuleNameFromResourceUri(testUriNoVersion));
		assertEquals(testName, ModuleUriUtil
				.extractModuleNameFromResourceUri(testUri));
		assertEquals(testName, ModuleUriUtil
				.extractModuleNameFromResourceUri(testUriDefaultVersion));
		assertEquals(testName, ModuleUriUtil
				.extractModuleNameFromResourceUri(testUriWithTrailingSlash));

		assertEquals("", ModuleUriUtil
				.extractModuleNameFromResourceUri(testUriRoot));
		assertEquals("", ModuleUriUtil
				.extractModuleNameFromResourceUri(testUriRootNoTrailingSlash));
	}

	@Test
	public void extractParentUrlFromResourceUrl() throws ValidationException {

		assertEquals(Module.RESOURCE_URI_PREFIX + "alpha/beta", ModuleUriUtil
				.extractParentUriFromResourceUri(testUri));
		assertEquals(Module.RESOURCE_URI_PREFIX + "alpha/beta", ModuleUriUtil
				.extractParentUriFromResourceUri(testUriNoVersion));
	}

	@Test
	public void extractVersionLessResourceUri() throws ValidationException {

		assertEquals(testUriNoVersion, ModuleUriUtil
				.extractVersionLessResourceUri(testUri));
		assertEquals(testUriNoVersion, ModuleUriUtil
				.extractVersionLessResourceUri(testUriNoVersion));
	}

}
