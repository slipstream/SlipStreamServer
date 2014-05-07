package com.sixsq.slipstream.persistence;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2013 SixSq Sarl (sixsq.com)
 * =====
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -=================================================================-
 */


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToOne;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

import com.sixsq.slipstream.exceptions.NotFoundException;

/**
 * For unit tests
 *
 * @see AuthzTest
 *
 */
@Entity
@SuppressWarnings("serial")
public class Authz implements Serializable {

	@Id
	@GeneratedValue
	Long id;

	@OneToOne
	private Module guardedModule;

	@Attribute
	private String owner;

	@Attribute
	private boolean ownerGet = true;
	@Attribute
	private boolean ownerPut = true;
	@Attribute
	private boolean ownerPost = true;
	@Attribute
	private boolean ownerDelete = true;
	@Attribute
	private boolean ownerCreateChildren = true;

	@Attribute
	private boolean groupGet = false;
	@Attribute
	private boolean groupPut = false;
	@Attribute
	private boolean groupPost = false;
	@Attribute
	private boolean groupDelete = false;
	@Attribute
	private boolean groupCreateChildren = false;

	@Attribute
	private boolean publicGet = false;
	@Attribute
	private boolean publicPut = false;
	@Attribute
	private boolean publicPost = false;
	@Attribute
	private boolean publicDelete = false;
	@Attribute
	private boolean publicCreateChildren = false;

	@Lob
	@Column(length=1024)
	private String groupMembers_ = ", ";

	@Attribute
	private boolean inheritedGroupMembers = true;

	private Authz() {
	}

	public Authz(String owner, Module guardedModule) {
		this.owner = owner;
		this.guardedModule = guardedModule;
		this.guardedModule.setAuthz(this);
	}

	public Metadata getGuardedModule() {
		return guardedModule;
	}

	public String getUser() {
		return owner;
	}

	public void setUser(String user) {
		owner = user;
	}

	public void clear() {
		ownerGet = true;
		ownerPut = true;
		ownerPost = true;
		ownerDelete = true;

		groupGet = false;
		groupPut = false;
		groupPost = false;
		groupDelete = false;

		publicGet = false;
		publicPut = false;
		publicPost = false;
		publicDelete = false;
	}

	public boolean isOwnerGet() {
		return ownerGet;
	}

	public void setOwnerGet(boolean ownerGet) {
		if (!ownerGet) {
			setPublicPut(false);
			setPublicPost(false);
			setPublicDelete(false);
			setPublicGet(false);
		}
		this.ownerGet = true; // we don't allow get for owner to be false
	}

	public boolean isOwnerPut() {
		return ownerPut;
	}

	public void setOwnerPut(boolean ownerPut) {
		if (!ownerPut) {
			setPublicPut(false);
		} else {
			setOwnerGet(true);
		}
		this.ownerPut = ownerPut;
	}

	public boolean isOwnerPost() {
		return ownerPost;
	}

	public void setOwnerPost(boolean ownerPost) {
		if (!ownerPost) {
			setPublicPost(false);
		} else {
			setOwnerGet(true);
		}
		this.ownerPost = ownerPost;
	}

	public boolean isOwnerDelete() {
		return ownerDelete;
	}

	public void setOwnerDelete(boolean ownerDelete) {
		if (!ownerDelete) {
			setPublicDelete(false);
		} else {
			setOwnerGet(true);
		}
		this.ownerDelete = ownerDelete;
	}

	public void setOwnerCreateChildren(boolean ownerCreateChildren) {
		this.ownerCreateChildren = ownerCreateChildren;
	}

	public boolean isOwnerCreateChildren() {
		return ownerCreateChildren;
	}

	public boolean isGroupGet() {
		return groupGet;
	}

	public void setGroupGet(boolean groupGet) {
		if (!groupGet) {
			setPublicGet(false);
			setGroupPut(false);
			setGroupPost(false);
			setGroupDelete(false);
		}
		this.groupGet = groupGet;
	}

	public boolean isGroupPut() {
		return groupPut;
	}

	public void setGroupPut(boolean groupPut) {
		if (!groupPut) {
			setPublicPut(false);
		} else {
			setOwnerPut(true);
			setGroupGet(true);
		}
		this.groupPut = groupPut;
	}

	public boolean isGroupPost() {
		return groupPost;
	}

	public void setGroupPost(boolean groupPost) {
		if (!groupPost) {
			setPublicPost(false);
		} else {
			setOwnerPost(true);
			setGroupGet(true);
		}
		this.groupPost = groupPost;
	}

	public boolean isGroupDelete() {
		return groupDelete;
	}

	public void setGroupDelete(boolean groupDelete) {
		if (!groupDelete) {
			setPublicDelete(false);
		} else {
			setOwnerDelete(true);
			setGroupGet(true);
		}
		this.groupDelete = groupDelete;
	}

	public void setGroupCreateChildren(boolean groupCreateChildren) {
		if (groupCreateChildren) {
			setOwnerCreateChildren(true);
		}
		this.groupCreateChildren = groupCreateChildren;
	}

	public boolean isGroupCreateChildren() {
		return groupCreateChildren;
	}

	public boolean isPublicGet() {
		return publicGet;
	}

	public void setPublicGet(boolean publicGet) {
		if (!publicGet) {
			setPublicPut(false);
			setPublicPost(false);
			setPublicDelete(false);
		} else {
			setGroupGet(true);
		}
		this.publicGet = publicGet;
	}

	public boolean isPublicPut() {
		return publicPut;
	}

	public void setPublicPut(boolean publicPut) {
		if (publicPut) {
			setGroupPut(true);
			setPublicGet(true);
		}
		this.publicPut = publicPut;
	}

	public boolean isPublicPost() {
		return publicPost;
	}

	public void setPublicPost(boolean publicPost) {
		if (publicPost) {
			setGroupPost(true);
			setPublicGet(true);
		}
		this.publicPost = publicPost;
	}

	public boolean isPublicDelete() {
		return publicDelete;
	}

	public void setPublicDelete(boolean publicDelete) {
		if (publicDelete) {
			setGroupDelete(true);
			setPublicGet(true);
		}
		this.publicDelete = publicDelete;
	}

	public void setPublicCreateChildren(boolean publicCreateChildren) {
		if (publicCreateChildren) {
			setGroupCreateChildren(true);
		}
		this.publicCreateChildren = publicCreateChildren;
	}

	public boolean isPublicCreateChildren() {
		return publicCreateChildren;
	}

	public boolean canGet(User user) {
		if (user == null || publicGet) {
			return publicGet;
		}
		if (user.getName().equals(owner)) {
			return ownerGet;
		}
		if (user.isSuper()) {
			return true;
		}
		if(groupGet) {
			return isUserInInheritedGroup(user);
		}
		return publicGet;
	}

	public boolean canPut(User user) {
		if (user == null || publicPut) {
			return publicPut;
		}
		if (user.getName().equals(owner)) {
			return ownerPut;
		}
		if (user.isSuper()) {
			return true;
		}
		if(groupPut) {
			return isUserInInheritedGroup(user);
		}
		return publicPut;
	}

	public boolean canPost(User user) {
		if (user == null || publicPost) {
			return publicPost;
		}
		if (user.getName().equals(owner)) {
			return ownerPost;
		}
		if (user.isSuper()) {
			return true;
		}
		if(groupPost) {
			return isUserInInheritedGroup(user);
		}
		return publicPost;
	}

	public boolean canDelete(User user) {
		if (user == null || publicDelete) {
			return publicDelete;
		}
		if (user.getName().equals(owner)) {
			return ownerDelete;
		}
		if (user.isSuper()) {
			return true;
		}
		if(groupDelete) {
			return isUserInInheritedGroup(user);
		}
		return publicDelete;
	}

	public boolean canCreateChildren(User user) {
		if (user == null || publicCreateChildren) {
			return publicCreateChildren;
		}
		if (user.getName().equals(owner)) {
			return ownerCreateChildren;
		}
		if (user.isSuper()) {
			return true;
		}
		if(groupCreateChildren) {
			if (getGroupMembers().contains(user.getName())) {
				return true;
			} else {
				if(inheritedGroupMembers) {
					return isInInheritedGroup(user);
				}
			}
		}
		return publicCreateChildren;
	}

	private boolean isUserInInheritedGroup(User user) {
		if (getGroupMembers().contains(user.getName())) {
			return true;
		} else {
			if(inheritedGroupMembers) {
				return isInInheritedGroup(user);
			} else {
				return false;
			}
		}
	}

	private boolean isInInheritedGroup(User user) {
		return lastInherited().getGroupMembers().contains(user.getName());
	}

	private Authz lastInherited() {
		return inheritedAuthz(this);
	}

	private Authz inheritedAuthz(Authz authz) {
		if(authz.isInheritedGroupMembers()) {
			return inheritedAuthz(authz.getParentAuthz());
		}
		return authz;
	}

	private Authz getParentAuthz() {
		Authz defaultAuthz = new Authz();
		defaultAuthz.setInheritedGroupMembers(false);

		if(getGuardedModule() == null) {
			return defaultAuthz ;
		}
		String parentUri = getGuardedModule().getParent();
		Module parent = Module.load(parentUri);
		if(parent == null) {
			return defaultAuthz ;
		}
		return parent.getAuthz();
	}

	public void addGroupMember(String groupMember) {
		if (getGroupMembers().contains(groupMember)) {
			return;
		}
		if (groupContainsMembers()) {
			this.groupMembers_ += ", " + groupMember;
		} else {
			this.groupMembers_ = groupMember;
		}
	}

	private boolean groupContainsMembers() {
		return !"".equals(groupMembers_);
	}

	public void setGroupMembers(String groupMembers) {
		setGroupMembers(Arrays.asList(groupMembers.split(",")));
	}

	@ElementList
	public void setGroupMembers(List<String> group) {
		StringBuilder _group = new StringBuilder(", ");
		List<String> processingGroup = new ArrayList<String>();
		for (String member : group) {
			if (processingGroup.contains(member)) {
				continue;
			}
			processingGroup.add(member);
            _group.append(member.trim())
                  .append(", ");
		}
		this.groupMembers_ = _group.toString();
	}

	@ElementList
	public List<String> getGroupMembers() {
		if(inheritedGroupMembers) {
			return getParentAuthz().getGroupMembers();
		}
		if ("".equals(groupMembers_)) {
			return new ArrayList<String>();
		}
		return splitGroupMembers(groupMembers_);
	}

	protected List<String> splitGroupMembers(String groupMembers) {
		List<String> parsedMembers = new ArrayList<String>();
		for (String member : groupMembers.split(",")) {
			member = member.trim();
			if (!"".equals(member)) {
				parsedMembers.add(member);
			}
		}
		return parsedMembers;
	}

	public void setInheritedGroupMembers(boolean inheritedGroupMembers) {
		this.inheritedGroupMembers = inheritedGroupMembers;
	}

	public boolean isInheritedGroupMembers() {
		return inheritedGroupMembers;
	}

	public static Authz loadByGuardedModuleResourceUrl(String resourceUrl)
			throws NotFoundException {

		// FIXME: Add real implementation of the method.
		throw (new NotFoundException());
	}

	public Authz store() {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		Authz merged = em.merge(this);
		transaction.commit();
		em.close();
		return merged;
	}

	public Authz copy(Module module) {

		Authz copy = new Authz(getUser(), module);

		copy.setGroupCreateChildren(isGroupCreateChildren());
		copy.setGroupDelete(isGroupDelete());
		copy.setGroupGet(isGroupGet());
		copy.setGroupMembers(getGroupMembers());
		copy.setGroupPost(isGroupPost());
		copy.setGroupPut(isGroupPut());
		copy.setInheritedGroupMembers(isInheritedGroupMembers());
		copy.setOwnerCreateChildren(isOwnerCreateChildren());
		copy.setOwnerDelete(isOwnerDelete());
		copy.setOwnerGet(isOwnerGet());
		copy.setOwnerPost(isOwnerPost());
		copy.setOwnerPut(isGroupPut());
		copy.setPublicCreateChildren(isPublicCreateChildren());
		copy.setPublicDelete(isPublicDelete());
		copy.setPublicGet(isPublicGet());
		copy.setPublicPost(isPublicPost());
		copy.setPublicPut(isPublicPut());

		return copy;
	}

}
