package com.sixsq.slipstream.util.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Inspired by: https://sites.google.com/site/gson/gson-user-guide#TOC-Primitives-Examples
 *
 */
public class JsonSerializationUtil {


	  public static String toJson(Object o) {
	    Gson gson = new GsonBuilder()
	        .setExclusionStrategies(new JsonExclusionStrategy())
	        .serializeNulls()
	        .create();
	    return gson.toJson(o);
	  }

	  public static Object fromJson(String contents, Class<?> resultClass) {
		  Gson gson = new Gson();
		  return gson.fromJson(contents, resultClass);
	  }
}
