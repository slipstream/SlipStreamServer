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

import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.resource.BaseResource;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.SerializationUtil;
import com.sixsq.slipstream.vm.VmsQueryParameters;


public class VmsResource extends BaseResource {

	@Get("xml")
	public Representation toXml() {

		String metadata = SerializationUtil.toXmlString(getVms());
		return new StringRepresentation(metadata, MediaType.APPLICATION_XML);

	}

	@Get("json")
	public Representation toJson() {

		String metadata = SerializationUtil.toJsonString(getVms());
		return new StringRepresentation(metadata, MediaType.APPLICATION_JSON);

	}

	@Get("html")
	public Representation toHtml() {

		String html = HtmlUtil.toHtml(getVms(), getPageRepresentation(), getUser(), getRequest());

		return new StringRepresentation(html, MediaType.TEXT_HTML);
	}

	private Vms getVms() {
		Vms vms = new Vms();
		VmsQueryParameters parameters = new VmsQueryParameters(getUser(), getOffset(), getLimit(), getCloud(),
				getRunUuid(), getRunOwner(), getUserFilter());

		try {
			vms.populate(parameters);
		} catch (SlipStreamClientException e) {
		} catch (SlipStreamException e) {
		}

		return vms;
	}

	@Override
	protected String getPageRepresentation() {
		return "vms";
	}

}
