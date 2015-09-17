package com.sixsq.slipstream.api;

import com.sixsq.slipstream.util.RequestUtil;

public class APICollectionResource extends APIBaseResource {

    public APICollectionResource(String resourceName) {
        super(resourceName);
    }

    @Override
    protected String getPageRepresentation() {
        return resourceName + "s";
    }

    @Override
    protected String uri() {
        return SSCLJ_SERVER + "/" + this.resourceName + cimiFilter();
    }

    private String cimiFilter() {
        int limit = RequestUtil.getLimit(getRequest());
        int offset = RequestUtil.getOffset(getRequest());
        int first = offset + 1;
        int last = offset + limit;
        return "?$first=" + first + "&$last=" + last;
    }

}
