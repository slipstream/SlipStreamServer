package com.sixsq.slipstream.event;

/**
 * Structure representing a given user or a given role.
 *  
 */
public class TypePrincipal {

	public static final String ADMIN = "ADMIN";
	
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
