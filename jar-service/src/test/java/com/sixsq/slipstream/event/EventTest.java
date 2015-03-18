package com.sixsq.slipstream.event;

import static com.sixsq.slipstream.event.Event.EventType.state;
import static com.sixsq.slipstream.event.Event.Severity.medium;
import static com.sixsq.slipstream.event.TypePrincipal.PrincipalType.USER;
import static com.sixsq.slipstream.event.TypePrincipalRight.Right.ALL;

import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.joda.time.DateTime;
import org.junit.Test;

public class EventTest {

	@Test
	public void createEvent() {
		
		TypePrincipal owner = new TypePrincipal(USER, "joe");
		List<TypePrincipalRight> rules = Arrays.asList(new TypePrincipalRight(USER, "jack", ALL));				
		ACL acl= new ACL(owner, rules);
		
		DateTime eventTimestamp = new DateTime(2015, 2, 15, 16, 34);
		
		Event event = new Event(acl, eventTimestamp.toDate(), "ref1",
				"started", medium, state);
		
		Assert.assertNotNull(event.toJson());
	}

}
