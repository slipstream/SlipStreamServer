package com.sixsq.slipstream.event;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.sixsq.slipstream.acl.ACL;
import com.sixsq.slipstream.acl.TypePrincipal;
import com.sixsq.slipstream.acl.TypePrincipalRight;
import org.junit.Assert;
import org.junit.Test;

import com.sixsq.slipstream.event.Event.EventType;
import com.sixsq.slipstream.acl.TypePrincipal.PrincipalType;
import com.sixsq.slipstream.acl.TypePrincipalRight.Right;


public class EventTest {

	@Test
	public void eventCanBeCreatedAndJsonified() {
		
		TypePrincipal owner = new TypePrincipal(PrincipalType.USER, "joe");
		List<TypePrincipalRight> rules = Arrays.asList(new TypePrincipalRight(
				PrincipalType.ROLE, "ANON", Right.ALL));
		ACL acl = new ACL(owner, rules);

		Event event = new Event(acl, new Date(), "run/12313", "Initializing", Event.Severity.medium,
				EventType.state);
		
		Assert.assertNotNull(event.toJson());
	}

}
