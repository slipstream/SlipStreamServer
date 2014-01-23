package com.sixsq.slipstream.action;

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

import javax.persistence.EntityManager;

import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.representation.Representation;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.persistence.PersistenceUtil;

public abstract class OneShotActionPerformer {

	private OneShotAction action;

	public static OneShotActionPerformer getPerformer(OneShotAction action) {
		
		OneShotActionPerformer performer = null;
		
		switch (Performers.valueOf(action.getClass().getSimpleName())) {
		case ResetPasswordAction:
			performer = new ResetPasswordActionPerformer(action);
			break;
		case UserEmailValidationAction:
			performer = new UserEmailValidationActionPerformer(action);
			break;
		}
		
		return performer;
	}
	
	public OneShotActionPerformer(OneShotAction action) {
		this.action = action;
	}

	public abstract Representation doAction(Request request)
			throws SlipStreamRuntimeException, ConfigurationException;

	public Form getForm() {
		return getAction().getForm();
	}

	protected void setForm(Form form) {
		getAction().setForm(form);
	}

	public static OneShotAction load(String uuid) {
		EntityManager em = PersistenceUtil.createEntityManager();
		return em.find(OneShotAction.class, uuid);
	}

	public OneShotAction getAction() {
		return action;
	}

	enum Performers {
		ResetPasswordAction,
		UserEmailValidationAction
	}
}
