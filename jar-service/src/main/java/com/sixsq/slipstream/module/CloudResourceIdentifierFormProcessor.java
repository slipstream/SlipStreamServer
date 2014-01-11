//package com.sixsq.slipstream.module;

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
//
//import org.restlet.data.Form;
//
//import com.sixsq.slipstream.exceptions.BadlyFormedElementException;
//import com.sixsq.slipstream.exceptions.SlipStreamClientException;
//import com.sixsq.slipstream.exceptions.ValidationException;
//import com.sixsq.slipstream.persistence.CloudResourceIdentifier;
//
//public class CloudResourceIdentifierFormProcessor {
//
//	private Form form;
//	private CloudResourceIdentifier cloudResourceIdentifier;
//	
//	public void processForm(Form form) throws BadlyFormedElementException,
//			SlipStreamClientException {
//
//		this.setForm(form);
//
//		parseForm();
//	}
//
//	private void parseForm() throws ValidationException {
//		String cloudMachineIdentifer = getForm().getFirstValue("cloudmachineidentifer");
//
//		validateCloudMachineIdentifer(cloudMachineIdentifer);
//		
//		
//	}
//
//	private void validateCloudMachineIdentifer(String name) throws ValidationException {
//		return;
//	}
//
//	public void setForm(Form form) {
//		this.form = form;
//	}
//
//	public Form getForm() {
//		return form;
//	}
//
//	public CloudResourceIdentifier getCloudResourceIdentifier() {
//		return cloudResourceIdentifier;
//	}
//	
//}
