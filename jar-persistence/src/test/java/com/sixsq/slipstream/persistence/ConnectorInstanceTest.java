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

public class ConnectorInstanceTest {

	@Test
	public void equals() throws ValidationException {
		ConnectorInstance ci1 = new ConnectorInstance("name", null);
		ConnectorInstance ci2 = new ConnectorInstance("name", null);
		assertTrue(ci1.equals(ci2));

		ci1 = new ConnectorInstance("name", "owner");
		ci2 = new ConnectorInstance("name", "owner");
		assertTrue(ci1.equals(ci2));
	}

	@Test
	public void equalsWithNullName() throws ValidationException {
		ConnectorInstance ci1 = new ConnectorInstance(null, null);
		ConnectorInstance ci2 = new ConnectorInstance(null, null);
		assertTrue(ci1.equals(ci2));
	}

	@Test
	public void notEquals() throws ValidationException {
		ConnectorInstance ci1 = new ConnectorInstance("name", null);
		ConnectorInstance ci2 = new ConnectorInstance("another", null);
		assertFalse(ci1.equals(ci2));
		assertFalse(ci1.equals(null));
		assertFalse(ci1.equals(new ConnectorInstance(null, null)));
	}
}
