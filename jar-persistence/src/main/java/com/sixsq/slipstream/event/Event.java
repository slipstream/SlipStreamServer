package com.sixsq.slipstream.event;

import com.sixsq.slipstream.util.SscljProxy;
import java.util.*;
import java.util.logging.Logger;

import static com.sixsq.slipstream.event.TypePrincipal.PrincipalType.ROLE;
import static com.sixsq.slipstream.event.TypePrincipal.PrincipalType.USER;
import static com.sixsq.slipstream.event.TypePrincipalRight.Right.ALL;

public class Event {

	public static boolean isMuted = false;

	private static final String EVENT_RESOURCE = "api/event";

	private static final Logger logger = Logger.getLogger(Event.class.getName());

	private static final String EVENT_URI = "http://sixsq.com/slipstream/1/Event";

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
		return SscljProxy.toJson(this);
	}

	public static void post(Event event) {

		if (isMuted) {
			return;
		}

		SscljProxy.post(EVENT_RESOURCE, event);
	}

}
