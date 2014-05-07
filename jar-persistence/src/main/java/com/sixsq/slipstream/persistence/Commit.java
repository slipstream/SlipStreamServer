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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToOne;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

@Entity
@SuppressWarnings("serial")
public class Commit implements Serializable {

	@Id
	@GeneratedValue
	Long id;

	@OneToOne
	private Module guardedModule;

	@Attribute(required=false)
	private String author;

	@Element(required=false)
	@Lob
	@Column(length=1024)
	private String comment;

	public Commit() {
		author = "";
		comment = "";
	}

	public Commit(String author, String comment) {
		this.author = author;
		this.comment = comment;
	}

	public Metadata getGuardedModule() {
		return guardedModule;
	}

	public Commit store() {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		em.merge(this);
		transaction.commit();
		em.close();
		return this;
	}

	public Commit copy() {
		return new Commit(getAuthor(), getComment());
	}

	private String getAuthor() {
		return author;
	}

	private String getComment() {
		return comment;
	}

}
