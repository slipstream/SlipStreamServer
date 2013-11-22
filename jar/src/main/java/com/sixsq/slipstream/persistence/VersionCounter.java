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

@SuppressWarnings("serial")
@Entity
public class VersionCounter implements Serializable {

	private static int ID = 1;
	
	public static int getNextVersion() {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		VersionCounter counter = em.find(VersionCounter.class, ID);
		if(counter == null) {
			counter = new VersionCounter();
		} else {
			counter.increment();
		}
		em.merge(counter);
		transaction.commit();
		em.close();
		return counter.counter;
	}
	
	@Id
	private int id = ID;
	
	private int counter = 1;

	private void increment() {
		counter++;
	}
}
