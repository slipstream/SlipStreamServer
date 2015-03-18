package com.sixsq.slipstream.event;

public class TypePrincipal {

	public static enum PrincipalType {
		USER, ROLE;
	}
	
	@SuppressWarnings("unused")
	private PrincipalType type;
	
	@SuppressWarnings("unused")
	private String principal;
	
	public TypePrincipal(PrincipalType type, String principal) {
		this.type = type;
		this.principal = principal;
	}
	
}
