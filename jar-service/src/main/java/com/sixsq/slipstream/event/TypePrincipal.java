package com.sixsq.slipstream.event;

public class TypePrincipal {

	public static enum PrincipalType {
		USER, ROLE;
	}
	
	private PrincipalType type;
	private String principal;
	
	public TypePrincipal(PrincipalType type, String principal) {
		this.type = type;
		this.principal = principal;
	}
	
}
