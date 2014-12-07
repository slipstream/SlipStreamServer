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
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Text;

import flexjson.JSON;

@SuppressWarnings("serial")
@Entity
public class Publish implements Serializable {

	@Id
	@GeneratedValue
	Long id;

	@OneToOne
	@JSON(include = false)
	private Module module;

	@Attribute(required = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date publicationDate;

	@Attribute(required = false)
	private String publisher;

	@Text(required = false)
	private String comment = "";

	private Publish() {
		publicationDate = new Date();
	}

	public Publish(Module module) {
		this();
		this.module = module;
	}

	public Date getPublicationDate() {
		return publicationDate;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getComment() {
		return comment;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	public String getPublisher() {
		return publisher;
	}

	public void setModule(Module module) {
		this.module = module;
	}

}
