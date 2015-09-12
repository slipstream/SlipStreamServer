package com.sixsq.slipstream.api;

public class APIResource extends APIBaseResource {

    public APIResource(String resourceName) {
        super(resourceName);
    }

    @Override
    protected String getPageRepresentation() {
        return resourceName;
    }

    @Override
    protected String uri() {
        String resourceId = (String) getRequest().getAttributes().get("resource-id");
        return SSCLJ_SERVER + "/" + this.resourceName + "/" + resourceId;
    }

}
