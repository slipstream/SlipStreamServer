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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.sixsq.slipstream.exceptions.ValidationException;

@Entity
@SuppressWarnings("serial")
public class UserParameter extends Parameter<User> {

	public static UserParameter convert(Parameter<ServiceConfiguration> source) {
		return new UserParameter(source.getName(), source.getValue(), source.getDescription());
	}

	@Id
	@GeneratedValue
	Long id;

	@SuppressWarnings("unused")
	private UserParameter() {
	}

	public UserParameter(String name, String value, String description) {
		super(name, value, description);
	}

	public UserParameter(String name) {
		super(name);
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	protected void setId(Long id) {
		this.id = id;
	}

	@Override
	public UserParameter copy() throws ValidationException {
		return (UserParameter) copyTo(new UserParameter(getName(),
				getValue(), getDescription()));
	}
	
}
