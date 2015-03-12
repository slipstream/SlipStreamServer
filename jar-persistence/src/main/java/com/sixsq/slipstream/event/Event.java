package com.sixsq.slipstream.event;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class Event {
	
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
		   .registerTypeAdapter(Date.class, new DateTypeAdapter())
		   .setPrettyPrinting()
		   .create();
		
		return gson.toJson(this);
	}
	
	public static void post(Event event) {

		try {
			StringRepresentation stringRep = new StringRepresentation(event.toJson());
			stringRep.setMediaType(MediaType.APPLICATION_JSON);

			ClientResource resource = new ClientResource("http://localhost:8080/Event");
			resource.post(stringRep, MediaType.APPLICATION_JSON);
		} catch (ResourceException re) {
			Logger.getLogger("restlet").severe(re.getMessage());
		} catch (Exception e) {
			Logger.getLogger("restlet").severe(e.getMessage());
		}
	}
	
	// See https://code.google.com/p/google-gson/issues/detail?id=281
	private static class DateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
		private final DateFormat dateFormat;

		private DateTypeAdapter() {
			dateFormat = new SimpleDateFormat(ISO_8601_PATTERN, Locale.US);
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		}

		public synchronized JsonElement serialize(Date date, Type type,
				JsonSerializationContext jsonSerializationContext) {						
			return new JsonPrimitive(dateFormat.format(date));
		}

		public synchronized Date deserialize(JsonElement jsonElement, Type type,
				JsonDeserializationContext jsonDeserializationContext) {
			try {
				return dateFormat.parse(jsonElement.getAsString());
			} catch (ParseException e) {
				throw new JsonParseException(e);
			}
		}
	}
	
}
