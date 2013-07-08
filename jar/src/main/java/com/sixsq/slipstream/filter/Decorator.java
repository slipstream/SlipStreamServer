package com.sixsq.slipstream.filter;

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

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.routing.Filter;

import com.sixsq.slipstream.exceptions.SlipStreamException;

public class Decorator extends Filter {

	@Override
	protected int doHandle(Request request, Response response) {

		super.doHandle(request, response);

		if(response.getStatus().isSuccess() && request.getMethod().equals(Method.GET)) {
			try {
				response.setEntity(toHtml(request, response));
			} catch (SlipStreamException e) {
				// ok it failed generating html... do we care?
			}
		}
		
		return CONTINUE;
	}

	public Representation toHtml(Request request, Response response) throws SlipStreamException {

		String xhtmlRepresentation = convertHtmlToXhtml(response.getEntityAsText());

		String html = slipstream.ui.views.Representation.toHtml(xhtmlRepresentation,
				getPageRepresentation(), null);
		
		return new StringRepresentation(html, MediaType.TEXT_HTML);
	}

	protected String getPageRepresentation() {
		return "reports";
	}
		
	private String convertHtmlToXhtml(String htmlRepresentation) {
		return htmlRepresentation.replace("<br>", "<br/>");
	}
}
