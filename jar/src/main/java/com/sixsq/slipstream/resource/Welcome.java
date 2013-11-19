package com.sixsq.slipstream.resource;

import org.simpleframework.xml.Element;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.module.ModuleView.ModuleViewList;
import com.sixsq.slipstream.persistence.ServiceCatalogs;

public class Welcome {

	@Element(required = false)
	ModuleViewList modules;

	@Element(required = false)
	ServiceCatalogs serviceCatalogues;

	public ModuleViewList getModules() {
		return modules;
	}

	public void setModules(ModuleViewList modules) {
		this.modules = modules;
	}

	public ServiceCatalogs getServiceCatalogues() {
		return serviceCatalogues;
	}

	public void setServiceCatalogues(ServiceCatalogs serviceCatalogues)
			throws ConfigurationException, ValidationException {
		if (ServiceCatalogs.isEnabled()) {
			this.serviceCatalogues = serviceCatalogues;
		}
	}

}
