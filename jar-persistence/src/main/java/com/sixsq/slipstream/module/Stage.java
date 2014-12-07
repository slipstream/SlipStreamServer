package com.sixsq.slipstream.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementMap;

import com.sixsq.slipstream.module.Transformation.Stages;
import com.sixsq.slipstream.persistence.Package;

class Stage {

	@Attribute
	private String moduleUri;

	@ElementMap(data = true, attribute = true, name = "recipes", key = "name", entry = "recipe")
	private Map<Stages, String> recipes = new HashMap<Stages, String>();

	@ElementList(inline = true, required = false)
	private List<Package> packages = new ArrayList<Package>();

	// this is only valid in the context of a specific cloud
	@Attribute(required = false)
	private boolean built = false;

	public Stage() {
		super();
	}

	public Stage(String moduleUri, Map<Stages, String> recipes, List<Package> packages) {
		this();
		this.moduleUri = moduleUri;
		this.recipes = recipes;
		this.packages = packages;
	}

	public List<Package> getPackages() {
		return packages;
	}

	public String getModuleUri() {
		return moduleUri;
	}

	public Map<Stages, String> getRecipes() {
		return recipes;
	}

	public String getRecipe(Stages stage) {
		return recipes.get(stage);
	}

	public boolean isBuilt() {
		return built;
	}

	public void setBuilt(boolean built) {
		this.built = built;
	}
}