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

	public static String DEFAULT_CLOUD_SERVICE_PARAMETER_NAME = "default.cloud.service";

	public static String KEY_ON_ERROR_RUN_FOREVER = "On Error Run Forever";
	public static String KEY_ON_SUCCESS_RUN_FOREVER = "On Success Run Forever";
	
	public static UserParameter convert(Parameter<ServiceConfiguration> source)
			throws ValidationException {
		UserParameter target = new UserParameter(source.getName(),
				source.getValue(), source.getDescription());
		target.setCategory(source.getCategory());
		return target;
	}

	@Id
	@GeneratedValue
	Long id;

	@SuppressWarnings("unused")
	private UserParameter() {
	}

	public UserParameter(String name, String value, String description)
			throws ValidationException {
		super(name, value, description);
	}

	public UserParameter(String name) throws ValidationException {
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
		return (UserParameter) copyTo(new UserParameter(getName(), getValue(),
				getDescription()));
	}

}
