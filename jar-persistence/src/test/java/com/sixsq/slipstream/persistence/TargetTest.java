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

import com.sixsq.slipstream.exceptions.ValidationException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TargetTest {

	@Test
	public void sameWithNull() throws ValidationException {

		Target n = new Target(null, null, null);
		Target o = new Target(null, null, null);
		assertTrue(n.equals(o));
	}

	@Test
	public void sameName() throws ValidationException {

		Target n = new Target("name", null, null);
		Target o = new Target("name", null, null);
		assertTrue(n.equals(o));
	}

	@Test
	public void sameScript() throws ValidationException {

		Target n = new Target("name", "script", null);
		Target o = new Target("name", "script", null);
		assertTrue(n.equals(o));
	}

	@Test
	public void sameInheritedScripts() throws ValidationException {

		List<String> is = new ArrayList<String>();
		is.add("script 1");
		is.add("script 2");

		Target n = new Target("name", "script", is);
		Target o = new Target("name", "script", is);
		assertTrue(n.equals(o));
	}

	@Test
	public void notSameName() throws ValidationException {

		Target n = new Target("name", null, null);
		Target o = new Target("other", null, null);
		assertFalse(n.equals(o));

		n = new Target(null, null, null);
		o = new Target("other", null, null);
		assertFalse(n.equals(o));

		n = new Target("name", null, null);
		o = new Target(null, null, null);
		assertFalse(n.equals(o));
	}

	@Test
	public void notSameScript() throws ValidationException {

		Target n = new Target("name", "script", null);
		Target o = new Target("name", "other", null);
		assertFalse(n.equals(o));

		n = new Target("name", null, null);
		o = new Target("name", "other", null);
		assertFalse(n.equals(o));

		n = new Target("name", "script", null);
		o = new Target("name", null, null);
		assertFalse(n.equals(o));
	}

	@Test
	public void notSameInheritedScripts() throws ValidationException {

		List<String> is = new ArrayList<String>();
		is.add("script 1");
		is.add("script 2");

		List<String> isOther = new ArrayList<String>();
		isOther.add("script 1");
		isOther.add("other 2");

		Target n = new Target("name", "script", is);
		Target o = new Target("name", "script", isOther);
		assertFalse(n.equals(o));

		n = new Target("name", "script", is);
		o = new Target("name", "script", null);
		assertFalse(n.equals(o));

		n = new Target("name", "script", null);
		o = new Target("name", "script", isOther);
		assertFalse(n.equals(o));

		n = new Target("name", "script", is);
		o = new Target("name", "script", null);
		assertFalse(n.equals(o));

		n = new Target("name", "script", is);
		o = new Target("name", "script", new ArrayList<String>());
		assertFalse(n.equals(o));

		n = new Target("name", "script", new ArrayList<String>());
		o = new Target("name", "script", isOther);
		assertFalse(n.equals(o));
	}

	@Test
	public void targetSet() {
		Target t = new Target("t", "something");
		assertTrue(t.isTargetSet());

		t = new Target("t", "something", Arrays.asList("something else"));
		assertTrue(t.isTargetSet());
	}

	@Test
	public void targetNotSet() {
		Target t = new Target("t");
		assertFalse(t.isTargetSet());

		t = new Target("t", null);
		assertFalse(t.isTargetSet());

		t = new Target("t", "");
		assertFalse(t.isTargetSet());

		t = new Target("t", "", Arrays.asList("something else"));
		assertFalse(t.isTargetSet());
	}
	@Test
	public void inheritedTargetSet() {
		Target t = new Target("t", "something", Arrays.asList("something else"));
		assertTrue(t.isInheritedTargetSet());
	}

	@Test
	public void inheritedTargetNotSet() {
		Target t = new Target("t");
		assertFalse(t.isInheritedTargetSet());

		t = new Target("t", null);
		assertFalse(t.isInheritedTargetSet());

		t = new Target("t", "");
		assertFalse(t.isInheritedTargetSet());

		t = new Target("t", "", new ArrayList<String>());
		assertFalse(t.isTargetSet());
	}
}
