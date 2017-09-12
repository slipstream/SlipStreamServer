package com.sixsq.slipstream.acl;

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


	public PrincipalType getType() {
		return type;
	}

	public void setType(PrincipalType type) {
		this.type = type;
	}

	public String getPrincipal() {
		return principal;
	}

	public void setPrincipal(String principal) {
		this.principal = principal;
	}

	public TypePrincipal(PrincipalType type, String principal) {
		this.type = type;
		this.principal = principal;
	}
	
}
