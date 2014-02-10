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

import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.Publish;
import com.sixsq.slipstream.util.RequestUtil;

public class ModulePublishResource extends ModuleResource {

	@Get
	public Representation toHtml() {
		if(getParameterized().getPublished() == null) {
			throw(new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Module not published"));
		}
		return new StringRepresentation(getParameterized().getPublished().getPublicationDate().toString());
	}
	
	@Delete
	public void unpublish() {
		Module module = (Module) getParameterized();
		module.unpublish();
		module.store(false);
		getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
	}

	@Put("form")
	@Override
	public void updateOrCreateFromForm(Representation entity)
			throws ResourceException {

		if (!isExisting()) {
			throwNotFoundResource();
		}
		
		Module module = (Module)getParameterized();
		Publish published = module.getPublished();
		if(published != null) {
			throw(new ResourceException(Status.CLIENT_ERROR_CONFLICT, "The module was already published: " + published.getPublicationDate().toString()));
		}
		module.publish();
		module.store(false);
		
		String absolutePath = RequestUtil.constructAbsolutePath("/");
		getResponse().setLocationRef(absolutePath);
	}
}
