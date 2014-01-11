package com.sixsq.slipstream.persistence;

import org.simpleframework.xml.Element;

import com.sixsq.slipstream.module.ModuleView.ModuleViewList;

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

	public void setServiceCatalogues(ServiceCatalogs serviceCatalogues){
		this.serviceCatalogues = serviceCatalogues;
	}

}
