package com.sixsq.slipstream.acl;

/**
 * Structure representing a rule (a user or role has a certain right).
 *  
 */
public class TypePrincipalRight extends TypePrincipal {

	public static enum Right {
		ALL, MODIFY, VIEW;
	}
	
	@SuppressWarnings("unused")
	private Right right;

	public Right getRight() {
		return right;
	}

	public void setRight(Right right) {
		this.right = right;
	}

	public TypePrincipalRight(PrincipalType type, String principal, Right right) {
		super(type, principal);
		this.right = right;
	}
	
}
