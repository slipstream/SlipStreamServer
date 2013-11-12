package com.sixsq.slipstream.persistence;

import java.util.List;

import org.simpleframework.xml.ElementList;

import com.sixsq.slipstream.exceptions.ValidationException;

public class ServiceCatalogs {

	@ElementList(inline = true, name = "service_catalog")
	List<ServiceCatalog> list;

	public ServiceCatalogs() {
		list = ServiceCatalog.listall();
	}
	
	public List<ServiceCatalog> getList() {
		return list;
	}

	public List<ServiceCatalog> retrieveServiceCatalogs()
			throws ValidationException {
		return ServiceCatalog.listall();
	}

	public void updateForEditing(User user) throws ValidationException {
		if (user.isSuper()) {
			for (ServiceCatalog sc : list) {
				sc.populateDefinedParameters();
			}
		}
	}
}
