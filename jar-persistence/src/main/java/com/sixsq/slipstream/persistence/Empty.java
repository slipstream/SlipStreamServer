package com.sixsq.slipstream.persistence;

import com.sixsq.slipstream.exceptions.ValidationException;
import org.simpleframework.xml.Attribute;

@SuppressWarnings("serial")
public class Empty extends Metadata {

	public Empty() {
		super("Empty");
	}

	@Override
	public String getId() {
		return "";
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public void setName(String name) throws ValidationException {
	}
}
