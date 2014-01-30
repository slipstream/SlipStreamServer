package com.sixsq.slipstream.stats;

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
import java.util.Date;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import com.sixsq.slipstream.persistence.RunType;

/**
 * Unit test:
 * 
 * @see MeasurementTest
 * 
 */
@Root(name="vm")
@SuppressWarnings("serial")
public class Measurement implements Serializable {

	@Attribute(name = "instance_id",required=false)
	private String instanceId;

	@Attribute(name = "run_id")
	private String run;

	@Attribute(name = "index", required = false)
	private int index;

	@Attribute(name="node", required=false)
	private String nodeName;

	@Attribute(name = "name")
	private String module;

	@Attribute(name = "image_id")
	private String image;

	@Attribute(name = "user_id")
	private String user;
	
	@Attribute
	private RunType type;

	@Attribute
	private String cloud;

	@Attribute(required=false)
	private int cpu;

	@Attribute(required=false)
	private int ram; // in MB

	@Attribute(required=false, name = "disk")
	private int storage; // in GB

	@Attribute(name = "created_at")
	private Date startTime;

	@Attribute(name = "deleted_at", required = false)
	private Date endTime;
	
	@Attribute(required=false)
	private String state = "Unknown";

	@Attribute(required=false)
	private String vmstate = "Unknown";

	public String getRun() {
		return run;
	}

	public void setRun(String run) {
		this.run = run;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public RunType getType() {
		return type;
	}

	public void setType(RunType type) {
		this.type = type;
	}

	public String getCloud() {
		return cloud;
	}

	public void setCloud(String cloud) {
		this.cloud = cloud;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String id) {
		this.instanceId = id;
	}

	public int getCpu() {
		return cpu;
	}

	public void setCpu(int cpu) {
		this.cpu = cpu;
	}

	public int getRam() {
		return ram;
	}

	public void setRam(int ram) {
		this.ram = ram;
	}

	public int getStorage() {
		return storage;
	}

	public void setStorage(int storage) {
		this.storage = storage;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setCreation(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEnd(Date endTime) {
		this.endTime = endTime;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getVmState() {
		return vmstate;
	}

	public void setVmState(String vmstate) {
		this.vmstate = vmstate;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

}
