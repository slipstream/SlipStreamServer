package com.sixsq.slipstream.acl;

import java.util.List;

public class ACL {
	
	@SuppressWarnings("unused")
	private TypePrincipal owner;
	
	@SuppressWarnings("unused")
	private List<TypePrincipalRight> rules;

	public TypePrincipal getOwner() {
		return owner;
	}

	public void setOwner(TypePrincipal owner) {
		this.owner = owner;
	}

	public List<TypePrincipalRight> getRules() {
		return rules;
	}

	public void setRules(List<TypePrincipalRight> rules) {
		this.rules = rules;
	}

	public ACL(TypePrincipal owner, List<TypePrincipalRight> rules) {
		this.owner = owner;
		this.rules = rules;
	}	
	
}
