package com.sixsq.slipstream.event;

import com.google.gson.*;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

import static com.sixsq.slipstream.event.TypePrincipal.PrincipalType.ROLE;
import static com.sixsq.slipstream.event.TypePrincipal.PrincipalType.USER;
import static com.sixsq.slipstream.event.TypePrincipalRight.Right.ALL;

public class Event {

	public static boolean isMuted = false;

	private static final String EVENT_SERVER = "http://localhost:8201/api";
	private static final String EVENT_RESOURCE_NAME = "event";

	private static final Logger logger = Logger.getLogger(Event.class.getName());

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
		state, alarm, action, system;
	}

	public Event(ACL acl, Date timestamp, String resourceRef, String state, Severity severity, EventType type){
		this.acl = acl;
		this.timestamp = timestamp;
		this.resourceURI = EVENT_URI;
		this.content = buildContent(resourceRef, state);
		this.severity = severity;
		this.type = type;
	}

	public static void postEvent(String resourceRef, Event.Severity severity, String message, String username,
								 EventType type) {
		TypePrincipal owner = new TypePrincipal(USER, username);
		List<TypePrincipalRight> rules = Arrays.asList(
				new TypePrincipalRight(USER, username, ALL),
				new TypePrincipalRight(ROLE, "ADMIN", ALL));
		ACL acl = new ACL(owner, rules);

		Event event = new Event(acl, new Date(), resourceRef, message, severity, type);

		Event.post(event);
	}

	public static void muteForTests() {
		isMuted = true;
		logger.severe("You should NOT see this message in production: events won't be posted");
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

			if(isMuted) {
				return;
			}

		ClientResource resource = null;
		Representation response = null;

		try {
			StringRepresentation stringRep = new StringRepresentation(event.toJson());
			stringRep.setMediaType(MediaType.APPLICATION_JSON);

			Context context = new Context();
			Series<Parameter> parameters = context.getParameters();
			parameters.add("socketTimeout", "1000");
			parameters.add("idleTimeout", "1000");
			parameters.add("idleCheckInterval", "1000");
			parameters.add("socketConnectTimeoutMs", "1000");

			resource = new ClientResource(context, EVENT_SERVER + "/" + EVENT_RESOURCE_NAME);
			resource.setRetryOnError(false);
			response = resource.post(stringRep, MediaType.APPLICATION_JSON);
			
		} catch (ResourceException re) {
			logger.warning(re.getMessage());
		} catch (Exception e) {
			logger.warning(e.getMessage());
		} finally {
			if (response != null) {
				try {
					response.exhaust();
				} catch (IOException e) {
					logger.warning(e.getMessage());
				}
				response.release();
			}
			if (resource != null) {
				resource.release();
			}
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
