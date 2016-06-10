package com.sixsq.slipstream.api;

import com.sixsq.slipstream.util.RequestUtil;

public class APICollectionResource extends APIBaseResource {

    public APICollectionResource(String resourceName) {
        super(resourceName);
    }

    @Override
    protected String uri() {
        return super.uri() + "/" + this.resourceName + cimiParams();
    }

    @Override
    protected String getPageRepresentation() {
        return resourceName + "s";
    }

    private String cimiParams() {
        int limit = RequestUtil.getLimit(getRequest());
        int offset = RequestUtil.getOffset(getRequest());
        int first = offset + 1;
        int last = offset + limit;

        return "?$first=" + first + "&$last=" + last + cimiFilter();
    }

    protected String cimiFilter() {
        String cimiFilter = RequestUtil.getCIMIFilter(getRequest());
        if (cimiFilter != null && !cimiFilter.isEmpty()) {
            return "&$filter=" + cimiFilter;
        } else {
            return "";
        }
    }

}
