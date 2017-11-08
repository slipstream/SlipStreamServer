package com.sixsq.slipstream.event;

import static com.sixsq.slipstream.persistence.UserCloudCredentialsTest.createUser;

import java.net.URL;
import java.net.URLClassLoader;

public class IntegrationTestHelper {

	public static void main(String[] args) {
		try {

			ClassLoader cl = ClassLoader.getSystemClassLoader();
	        URL[] urls = ((URLClassLoader)cl).getURLs();
	        for(URL url: urls){
	        	System.out.println(url.getFile());
	        }

			createUser("user1", "123456").store();
			createUser("user2", "456789").store();
		} catch (Throwable th) {
			th.printStackTrace();
		}

		System.out.println("Users created");
		System.exit(0);
	}

}
