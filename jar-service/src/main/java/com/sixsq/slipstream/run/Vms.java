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

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.Vm;

@Root
public class Vms {

	@Attribute(required=false)
	private int offset;

	@Attribute(required=false)
	private int limit;

	@Attribute(required=false)
	private int count;

	@Attribute(required=false)
	private int totalCount;

	@Attribute(required=false)
	private String cloud;

	@ElementList(inline = true)
	private List<Vm> vms = new ArrayList<Vm>();

	public List<Vm> getVms() {
		return vms;
	}

	public void populate(User user, int offset, int limit, String cloudServiceName) throws SlipStreamException {
		populateVms(user, offset, limit, cloudServiceName);
	}

	private void populateVms(User user, int offset, int limit, String cloudServiceName) throws SlipStreamException {
		this.offset = offset;
		this.limit = limit;
		this.cloud = cloudServiceName;

		totalCount = Vm.listCount(user.getName(), cloudServiceName);
		vms = Vm.list(user.getName(), offset, limit, cloudServiceName);
		count = vms.size();
	}
}
