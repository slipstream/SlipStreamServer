package com.sixsq.slipstream.api;

import com.sixsq.slipstream.util.RequestUtil;

import java.io.UnsupportedEncodingException;

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
        return SSCLJ_SERVER + "/" + this.resourceName + cimiParams();
    }

    private String cimiParams() {
        int limit = RequestUtil.getLimit(getRequest());
        int offset = RequestUtil.getOffset(getRequest());
        int first = offset + 1;
        int last = offset + limit;

        String cimiFilter = "?$first=" + first + "&$last=" + last + cimiFilter();

        getLogger().info("cimiParams = " + cimiFilter);

        return cimiFilter;
    }

    private String cimiFilter() {
        String cimiFilter = RequestUtil.getCIMIFilter(getRequest());

        getLogger().info("cimiFilter = " + cimiFilter);

        if (cimiFilter != null && !cimiFilter.isEmpty()) {
            try {
                return "&" + java.net.URLEncoder.encode("$filter=" + cimiFilter, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                getLogger().warning("Unable to retrieve CIMI filter : " + uee);
                return "";
            }
        } else {
            return "";
        }
    }

}
