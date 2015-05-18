package com.sixsq.slipstream.connector;

import java.util.List;

public class ACL {
	
	@SuppressWarnings("unused")
	private TypePrincipal owner;
	
	@SuppressWarnings("unused")
	private List<TypePrincipalRight> rules;
	
	public ACL(TypePrincipal owner, List<TypePrincipalRight> rules) {
		this.owner = owner;
		this.rules = rules;
	}	
	
}
