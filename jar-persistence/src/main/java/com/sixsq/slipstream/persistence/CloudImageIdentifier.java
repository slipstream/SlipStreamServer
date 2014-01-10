package com.sixsq.slipstream.persistence;

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

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.simpleframework.xml.Attribute;

@SuppressWarnings("serial")
@Entity
public class CloudImageIdentifier implements Serializable {

	public static final String CLOUD_SERVICE_ID_SEPARATOR = ":";
	public static final String DEFAULT_CLOUD_SERVICE = "default";

	@Id
	@Attribute
	private String resourceUri;

	@Attribute
	private String cloudServiceName;

	@Attribute(required = false)
	private String region;

	@Attribute(required = false)
	private String cloudImageIdentifier;

	@ManyToOne
	private ImageModule container;

	public CloudImageIdentifier() {
	}

	public CloudImageIdentifier(ImageModule module, String cloudServiceName) {
		this.setCloudServiceName(cloudServiceName);
		this.resourceUri = module.getResourceUri() + "/" + cloudServiceName;
		setContainer(module);
	}

	public CloudImageIdentifier(ImageModule module, String cloudServiceName,
			String cloudImageIdentifer) {
		this(module, cloudServiceName);
		this.cloudImageIdentifier = cloudImageIdentifer;
	}

	public CloudImageIdentifier(ImageModule module, String cloudServiceName,
			String region, String cloudImageIdentifer) {
		this(module, cloudServiceName, cloudImageIdentifer);
		this.region = region;
		if (Parameter.hasValueSet(region)) {
			resourceUri += CloudImageIdentifier.CLOUD_SERVICE_ID_SEPARATOR
					+ region;
		}
	}

	public static CloudImageIdentifier load(String uri) {
		EntityManager em = PersistenceUtil.createEntityManager();
		return em.find(CloudImageIdentifier.class, uri);
	}

	public void store() {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		em.merge(this);
		transaction.commit();
		em.close();
	}

	public void setResourceUri(String resourceUri) {
		this.resourceUri = resourceUri;
	}

	public String getResourceUri() {
		return resourceUri;
	}

	public void setCloudServiceName(String cloudServiceName) {
		this.cloudServiceName = cloudServiceName;
	}

	public String getCloudServiceName() {
		return cloudServiceName;
	}

	public void setCloudMachineIdentifer(String cloudMachineIdentifer) {
		this.cloudImageIdentifier = cloudMachineIdentifer;
	}

	public String getCloudMachineIdentifer() {
		return cloudImageIdentifier;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getRegion() {
		return region;
	}

	public ImageModule copyTo(ImageModule image) {
		CloudImageIdentifier copy = new CloudImageIdentifier(image,
				getCloudServiceName(), getRegion(), getCloudMachineIdentifer());
		image.getCloudImageIdentifiers().add(copy);
		return image;
	}

	public void setContainer(ImageModule container) {
		this.container = container;
	}

	public ImageModule getContainer() {
		return container;
	}
}
