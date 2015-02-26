package com.sixsq.slipstream.event;

public class TypePrincipalRight extends TypePrincipal {

	public static enum Right {
		ALL, MODIFY, VIEW;
	}
	
	private Right right;
	
	public TypePrincipalRight(PrincipalType type, String principal, Right right) {
		super(type, principal);
		this.right = right;
	}
	
}
