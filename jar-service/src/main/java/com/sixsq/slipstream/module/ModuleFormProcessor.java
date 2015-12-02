package com.sixsq.slipstream.module;

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

import java.util.*;

import com.sixsq.slipstream.persistence.*;
import org.restlet.data.Form;

import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.user.FormProcessor;

public abstract class ModuleFormProcessor extends
		FormProcessor<Module, ModuleParameter> {

	private List<String> illegalNames = new ArrayList<String>(
			(Arrays.asList(("new"))));

	public ModuleFormProcessor(User user) {
		super(user);
	}

	static public ModuleFormProcessor createFormProcessorInstance(
			ModuleCategory category, User user) {

		ModuleFormProcessor processor = null;

		switch (category) {
		case Project:
			processor = new ProjectFormProcessor(user);
			break;
		case Image:
			processor = new ImageFormProcessor(user);
			break;
		case BlockStore:
			break;
		case Deployment:
			processor = new DeploymentFormProcessor(user);
			break;
		default:
			String msg = "Unknown category: " + category.toString();
			throw new IllegalArgumentException(msg);
		}

		return processor;

	}

	@Override
	protected void parseForm() throws ValidationException, NotFoundException {
		super.parseForm();

		String name = parseName();
		setParametrized(getOrCreateParameterized(name));
		getParametrized().setDescription(parseDescription());
		getParametrized().setCommit(parseCommit());
		getParametrized().setLogoLink(parseLogoLink());
		getParametrized().setNote(parseNote());
	}

	private String parseName() throws ValidationException {
		String parent = getForm().getFirstValue("parentmodulename", "");
		String name = getForm().getFirstValue("name");

		validateName(name);

		return ("".equals(parent)) ? name : parent + "/" + name;
	}

	private String parseDescription() throws ValidationException {
		return getForm().getFirstValue("description");
	}

	private String parseNote() throws ValidationException {
		return getForm().getFirstValue("note");
	}

	private Commit parseCommit() throws ValidationException {
		return new Commit(getUser().getName(), getForm().getFirstValue(
				"comment"), getParametrized());
	}

	private String parseLogoLink() throws ValidationException {
		return getForm().getFirstValue("logoLink");
	}

	private void validateName(String name) throws ValidationException {
		for (String illegal : illegalNames) {
			if (illegal.equals(name)) {
				throw (new ValidationException("Illegal name: " + name));
			}
		}
		return;
	}

	protected void parseAuthz() {

		// Save authz section
		Module module = getParametrized();
		String owner = module.getAuthz().getUser();
		if (owner == null || owner.isEmpty()) {
			owner = getUser().getName();
		}
		Authz authz = new Authz(owner, module);
		authz.clear();

		Form form = getForm();

		// ownerGet: can't be changed because owner would lose access
		authz.setOwnerPost(getBooleanValue(form, "ownerPost"));
		authz.setOwnerDelete(getBooleanValue(form, "ownerDelete"));

		authz.setGroupGet(getBooleanValue(form, "groupGet"));
		authz.setGroupPut(getBooleanValue(form, "groupPut"));
		authz.setGroupPost(getBooleanValue(form, "groupPost"));
		authz.setGroupDelete(getBooleanValue(form, "groupDelete"));

		authz.setPublicGet(getBooleanValue(form, "publicGet"));
		authz.setPublicPut(getBooleanValue(form, "publicPut"));
		authz.setPublicPost(getBooleanValue(form, "publicPost"));
		authz.setPublicDelete(getBooleanValue(form, "publicDelete"));

		authz.setGroupMembers(form.getFirstValue("groupmembers", ""));
		authz.setInheritedGroupMembers(getBooleanValue(form, "inheritedGroupMembers"));

		if (module.getCategory() == ModuleCategory.Project) {
			authz.setOwnerCreateChildren(getBooleanValue(form, "ownerCreateChildren"));
			authz.setGroupCreateChildren(getBooleanValue(form, "groupCreateChildren"));
			authz.setPublicCreateChildren(getBooleanValue(form, "publicCreateChildren"));
		}

		getParametrized().setAuthz(authz);

	}

	protected boolean getBooleanValue(Form form, String parameter) {

		Object value = form.getFirstValue(parameter);
		if (value != null && "on".equals(value.toString())) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected ModuleParameter createParameter(String name, String value,
			String description) throws SlipStreamClientException {
		return new ModuleParameter(name, value, description);
	}

	public void adjustModule(Module older) throws ValidationException {
		if (older != null) {
			getParametrized().setCreation(older.getCreation());
			getParametrized().getAuthz().setUser(older.getOwner());
		}
	}

	protected Module load(String name) {
		return Module.loadByName(name);
	}
}
