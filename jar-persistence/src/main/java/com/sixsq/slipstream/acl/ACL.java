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

	public void addRule(TypePrincipalRight rule){

		TypePrincipal.PrincipalType type = rule.getType();
		String principal =rule.getPrincipal();

		//ignore new rule if type and principal already exists
		boolean duplicate = false;
		for(TypePrincipalRight r : rules){
			if ((r.getType() == type) && (r.getPrincipal() == principal)){
				duplicate = true;
				break;
			}
		}

		if (!duplicate){
			rules.add(rule);
		}
	}

	public ACL(TypePrincipal owner, List<TypePrincipalRight> rules) {
		this.owner = owner;
		this.rules = rules;
	}
}
