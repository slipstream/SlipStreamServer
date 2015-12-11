package com.sixsq.slipstream.run;

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

import java.util.Date;
import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.User;

@Root(name = "item")
public class RunView {

	@Attribute
	public final String resourceUri;

	@Attribute
	public final String uuid;

	@Attribute
	public final String moduleResourceUri;

	@Attribute(required = false)
	public String status;

	@Attribute(required = false)
	public String abort;

	@Attribute
	public final Date startTime;

	@Attribute(required = false)
	private String hostname;

	@Attribute(required = false)
	private String cloudServiceNames;

	@Attribute(required = false)
	private String username;

	@Attribute(required = false)
	private RunType type;

	@Attribute(required = false)
	public String tags;

	@Attribute(required=false)
	private String serviceUrl;

	public RunView(String resourceUrl, String uuid, String moduleResourceUri,
			String status, Date startTime, String username, RunType type, String cloudServiceNames, String abort) {
		this.resourceUri = resourceUrl;
		this.uuid = uuid;
		this.moduleResourceUri = moduleResourceUri;
		this.status = status;
		this.username = username;
		this.type = type;
		this.cloudServiceNames = cloudServiceNames;
		this.abort = abort;

        if (startTime != null) {
            this.startTime = (Date) startTime.clone();
        } else {
            this.startTime = null;
        }
	}

	public static List<RunView> fetchListView(User user, int offset, int limit)
			throws ConfigurationException, ValidationException {
		return fetchListView(user, null, offset, limit, null, false);
	}

	public static List<RunView> fetchListView(User user, String moduleResourceUri, int offset, int limit,
			String cloudServiceName, boolean activeOnly) throws ConfigurationException, ValidationException {
		return Run.viewList(user, moduleResourceUri, offset, limit, cloudServiceName, activeOnly);
	}

	public static int fetchListViewCount(User user)
			throws ConfigurationException, ValidationException {
		return fetchListViewCount(user, null, null, false);
	}

	public static int fetchListViewCount(User user, String moduleResourceUri, String cloudServiceName, boolean activeOnly)
			throws ConfigurationException, ValidationException {
		return Run.viewListCount(user, moduleResourceUri, cloudServiceName, activeOnly);
	}

	public RunView copy() {
		RunView copy = new RunView(resourceUri, uuid, moduleResourceUri,
				status, startTime, username, type, cloudServiceNames, abort);
		copy.setHostname(hostname);
		copy.setTags(tags);
		return copy;
	}

	public String getServiceUrl() {
		return serviceUrl;
	}

	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	public String getCloudServiceNames() {
		return cloudServiceNames;
	}

	public void setCloudServiceNames(String cloudServiceNames) {
		this.cloudServiceNames = cloudServiceNames;
	}

	public String getUsername() {
		return username;
	}

	public RunType getType() {
		return type;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getHostname() {
		return hostname;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public String getTags() {
		return tags;
	}

	public boolean isAbort() {
		return Parameter.hasValueSet(abort);
	}

	public void setAbort(String abort) {
		this.abort = abort;
	}

}
