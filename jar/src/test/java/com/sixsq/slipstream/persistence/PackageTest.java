package com.sixsq.slipstream.persistence;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sixsq.slipstream.exceptions.ValidationException;

public class PackageTest {
	@Test
	public void equals() throws ValidationException {
		assertTrue(new Package("name").equals(new Package("name")));
		assertFalse(new Package("name").equals(new Package("other_name")));

		Package p = new Package("name");
		p.setKey("key");

		assertFalse(p.equals(null));

		Package p2 = new Package("name");
		p.setKey("key2");
		
		assertFalse(p.equals(p2));

		p.setKey("repo");
		p2.setKey("repo");
		p.setRepository("repo");
		p2.setRepository("repo");

		assertTrue(p.equals(p2));
		
	}
}
