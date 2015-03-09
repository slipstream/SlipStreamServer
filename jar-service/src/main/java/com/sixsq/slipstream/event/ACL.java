package com.sixsq.slipstream.event;

import java.util.List;

public class ACL {
	
	private TypePrincipal owner;
	private List<TypePrincipalRight> rules;
	
	public ACL(TypePrincipal owner, List<TypePrincipalRight> rules) {
		this.owner = owner;
		this.rules = rules;
	}	
	
}
