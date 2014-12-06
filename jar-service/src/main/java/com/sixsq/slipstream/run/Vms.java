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

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.Vm;

@Root
public class Vms {

	@ElementList(inline = true)
	private List<Vm> vms = new ArrayList<Vm>();

	public List<Vm> getVms() {
		return vms;
	}

	public void populate(User user) throws SlipStreamException {
		vms = Vm.list(user.getName());
	}
}
