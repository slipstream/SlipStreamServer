package com.sixsq.slipstream.persistence;

public interface Guarded {

	public Authz getAuthz();

	public void setAuthz(Authz authz);

	public Guarded getGuardedParent();

}
