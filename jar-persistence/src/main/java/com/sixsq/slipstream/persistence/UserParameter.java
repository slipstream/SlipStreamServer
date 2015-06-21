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
import org.simpleframework.xml.Root;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Arrays;
import java.util.List;

@Entity
@Root(name = "parameter")
@SuppressWarnings("serial")
public class UserParameter extends Parameter<User> {

	public static final String DEFAULT_CLOUD_SERVICE_PARAMETER_NAME = "default.cloud.service";

	public static final String KEY_TIMEOUT = "Timeout";

	public static final String KEY_ON_ERROR_RUN_FOREVER = "On Error Run Forever";
	public static final String KEY_ON_SUCCESS_RUN_FOREVER = "On Success Run Forever";
	public static final String KEY_KEEP_RUNNING = "keep-running";

	public static final String KEEP_RUNNING_NEVER = "never";
	public static final String KEEP_RUNNING_ALWAYS = "always";
	public static final String KEEP_RUNNING_ON_ERROR = "on-error";
	public static final String KEEP_RUNNING_ON_SUCCESS = "on-success";

	public static final String KEEP_RUNNING_DEFAULT = KEEP_RUNNING_ON_SUCCESS;

	public static final String KEY_MAIL_USAGE = "mail-usage";
	public static final String MAIL_USAGE_NEVER = "never";
	public static final String MAIL_USAGE_DAILY = "daily";
	public static final String MAIL_USAGE_WEEKLY = "weekly";
	public static final String MAIL_USAGE_MONTHLY = "monthly";
	public static final String MAIL_USAGE_DEFAULT = MAIL_USAGE_NEVER;


	public static final String SSHKEY_PARAMETER_NAME = "ssh.public.key";

	public static List<String> getKeepRunningOptions() {
		String[] options = {KEEP_RUNNING_ALWAYS, KEEP_RUNNING_ON_SUCCESS, KEEP_RUNNING_ON_ERROR, KEEP_RUNNING_NEVER};
		return Arrays.asList(options);
	}

	public static List<String> getMailUsageOptions(){
		return Arrays.asList(MAIL_USAGE_NEVER, MAIL_USAGE_DAILY);
	}

	public static String convertOldFormatToKeepRunning(boolean onSuccess, boolean onError) {
		String keepRunning = null;
		if (!onError && !onSuccess) keepRunning = UserParameter.KEEP_RUNNING_NEVER;
		else if (onError && onSuccess) keepRunning = UserParameter.KEEP_RUNNING_ALWAYS;
		else if (onError && !onSuccess) keepRunning = UserParameter.KEEP_RUNNING_ON_ERROR;
		else if (!onError && onSuccess) keepRunning = UserParameter.KEEP_RUNNING_ON_SUCCESS;
		return keepRunning;
	}

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
