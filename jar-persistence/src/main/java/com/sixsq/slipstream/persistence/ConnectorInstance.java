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
import java.util.Objects;

import javax.persistence.*;

import flexjson.JSON;
import org.simpleframework.xml.Attribute;

@Entity
@SuppressWarnings("serial")
public class ConnectorInstance implements Serializable {

	@Id
	@GeneratedValue
	Long id;

	@ManyToOne
	@JSON(include=false)
	private Run run;

	@Attribute
	private String name;

	@Attribute(required=false)
	private String owner;

	public ConnectorInstance(String name, String owner) {
		this.name = name;
		this.owner= owner;
	}

	public ConnectorInstance() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Run getRun() {
		return run;
	}

	public void setRun(Run run) {
		this.run = run;
	}

	public ConnectorInstance store() {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		try {
			transaction.begin();
			em.merge(this);
			transaction.commit();
		} finally {
			if(transaction.isActive()) {
				transaction.rollback();
			}
			em.close();
		}
		return this;
	}

	public boolean equals(Object obj) {
		if(!(obj instanceof ConnectorInstance)) {
			return false;
		}
		ConnectorInstance other = (ConnectorInstance) obj;
		boolean sameName = (this.getName() == null && other.getName() == null) || this.name.equals(other.getName());
		boolean sameOwner = ((this.owner == null) && (other.owner == null)) || this.owner.equals(other.owner);
		return (sameName && sameOwner);
	}

	public int hashCode(){
		return Objects.hashCode(name) ^ Objects.hashCode(owner);
	}

}
