package com.sixsq.slipstream.event;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Event {

	// TODO ???
	private static final String EVENT_URI = "http://sixsq.com/slipstream/1/Event";
	
	private static final String ISO_8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	
	@SuppressWarnings("unused")
	private ACL acl;
	
	@SuppressWarnings("unused")
	private Date timestamp;
	
	@SuppressWarnings("unused")
	private String resourceURI;
	
	@SuppressWarnings("unused")
	private String resourceRef;
	
	@SuppressWarnings("unused")
	private String state;
	
	@SuppressWarnings("unused")
	private Severity severity;
	
	@SuppressWarnings("unused")
	private Map<String, Object> content;
	
	@SuppressWarnings("unused")
	private EventType type;
	
	public static enum Severity {
		critical, high, medium, low;
	}
	
	public static enum EventType {
		state, alarm;
	}
	
	public Event(ACL acl, Date timestamp, String resourceRef, String state, Severity severity, EventType type){	
		this.acl = acl;
		this.timestamp = timestamp;	
		this.resourceURI = EVENT_URI;		
		this.content = buildContent(resourceRef, state); 				
		this.severity = severity;
		this.type = type;
	}

	private Map<String, Object> buildContent(String resourceRef, String state) {
		Map<String, Object> result = new HashMap<String, Object>();	
		Map<String, String> resource = new HashMap<String, String>();
		resource.put("href", resourceRef);		
		result.put("resource", resource);		
		result.put("state", state);
		return result;
	}
	
	public String toJson(){

		Gson gson = new GsonBuilder()
		   .setDateFormat(ISO_8601_PATTERN)
		   .setPrettyPrinting()
		   .create();
		
		return gson.toJson(this);
	}
}
