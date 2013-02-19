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

import static com.sixsq.slipstream.messages.MessageUtils.MSG_INVALID_ACTION_URL;
import static org.restlet.data.Status.CLIENT_ERROR_NOT_FOUND;

import org.restlet.Request;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.sixsq.slipstream.messages.MessageUtils;

public class InvalidActionResource extends ServerResource {

	@Get("txt|html|xml")
	public Representation toError() {

		Request request = getRequest();
		Reference resourceRef = request.getResourceRef();

		String msg = MessageUtils.format(MSG_INVALID_ACTION_URL, resourceRef);

		throw new ResourceException(CLIENT_ERROR_NOT_FOUND, msg);
	}
}
