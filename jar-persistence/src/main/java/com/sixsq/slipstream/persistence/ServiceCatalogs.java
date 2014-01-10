package com.sixsq.slipstream.persistence;

import java.util.List;
import java.util.Map;

import org.simpleframework.xml.ElementList;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;

public class ServiceCatalogs {

	@ElementList(inline = true, name = "service_catalog")
	List<ServiceCatalog> list;

	public ServiceCatalogs() {
		load();
	}

	private void load() {
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

	public void store() {
		for (ServiceCatalog s : getList()) {
			s = s.store();
		}
	}

	private ServiceCatalog getServiceCatalog(String cloud) {
		ServiceCatalog sc = null;
		for (ServiceCatalog s : getList()) {
			if (s.getCloud().equals(cloud)) {
				sc = s;
				break;
			}
		}
		return sc;
	}

	public void update(ServiceCatalog updated) throws ValidationException {
		ServiceCatalog sc = getServiceCatalog(updated.getCloud());

		Map<String, ServiceCatalogParameter> existingParameters = sc
				.getParameters();
		sc.clearParameters();

		for (ServiceCatalogParameter p : updated.getParameters().values()) {

			ServiceCatalogParameter existing = existingParameters.get(p
					.getName());

			if (existing == null) {
				sc.setParameter(p);
			} else {
				existing.setValue(p.getValue());
				sc.setParameter(existing);
			}
		}

		sc.populateDefinedParameters();
	}

}
