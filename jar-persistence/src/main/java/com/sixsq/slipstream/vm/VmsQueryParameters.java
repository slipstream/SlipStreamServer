package com.sixsq.slipstream.vm;

import com.sixsq.slipstream.persistence.User;


public class VmsQueryParameters {

	public final User user;
	public final Integer offset;
	public final Integer limit;
	public final String cloud;
	public final String runUuid;
	public final String runOwner;
	public final String userFilter;

	public VmsQueryParameters(User user, Integer offset, Integer limit, String cloud, String runUuid, String runOwner,
			String userFilter) {
		this.user = user;
		this.offset = offset;
		this.limit = limit;
		this.cloud = cloud;
		this.runUuid = runUuid;
		this.runOwner = runOwner;
		this.userFilter = userFilter;
	}

}
