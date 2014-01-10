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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.util.SerializationUtil;

public class ServiceCatalogTest {

	final static String CLOUDX = "cloudx";
	final static String CLOUDY = "cloudy";

	@Before
	public void setup() {
	}

	@After
	public void tearDown() {
		
		for(ServiceCatalog sc : ServiceCatalog.listall()) {
			sc.remove();
		}
	}

	@Test
	public void createAndStore() throws ValidationException {

		ServiceCatalog sc = ServiceCatalog.loadByCloud(CLOUDY);

		assertNull(sc);
		
		sc = new ServiceCatalog(CLOUDY);
		sc = sc.store();
		assertNotNull(sc);

		sc = ServiceCatalog.loadByCloud(CLOUDY);
		assertNotNull(sc);
		assertThat(sc.getCloud(), is(CLOUDY));
		assertThat(sc.getName(), is(CLOUDY));
	}

	@Test
	public void createAndStoreWithParameters() throws ValidationException {

		ServiceCatalog sc = new ServiceCatalog(CLOUDY);
		sc.setParameter(new ServiceCatalogParameter("key1", "value1", "description1"));
		sc.setParameter(new ServiceCatalogParameter("key2", "value2", "description2"));

		sc = sc.store();
		
		sc = ServiceCatalog.loadByCloud(CLOUDY);

		assertThat(sc.getParameters().size(), is(2));
		assertThat(sc.getParameters().get("key1").getValue(), is("value1"));
		assertThat(sc.getParameters().get("key2").getDescription(), is("description2"));
	}

	@Test
	public void storeRetrieveAndDelete() throws SlipStreamClientException {


		ServiceCatalog sc = new ServiceCatalog(CLOUDX);
		sc = sc.store();
		assertNotNull(sc);

		sc = ServiceCatalog.loadByCloud(CLOUDX);
		assertNotNull(sc);

		sc.remove();

		sc = ServiceCatalog.loadByCloud(CLOUDX);
		assertNull(sc);
	}

	@Test
	public void byCloud() throws SlipStreamClientException {

		ServiceCatalog sc = new ServiceCatalog(CLOUDX);
		sc = sc.store();
		sc = new ServiceCatalog(CLOUDY);
		sc = sc.store();

		List<ServiceCatalog> scs = ServiceCatalog.list(CLOUDX);
		
		assertThat(scs.size(), is(1));
		assertThat(scs.get(0).getCloud(), is(CLOUDX));
	}


	@Test
	public void checkXmlSerialization() {

		ServiceCatalog sc = new ServiceCatalog(CLOUDX);

		SerializationUtil.toXmlString(sc);
	}
}
