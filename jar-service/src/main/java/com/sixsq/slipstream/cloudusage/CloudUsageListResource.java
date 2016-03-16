package com.sixsq.slipstream.cloudusage;

import com.sixsq.slipstream.api.APICollectionResource;
import com.sixsq.slipstream.util.RequestUtil;

public class CloudUsageListResource extends APICollectionResource {

    public CloudUsageListResource() {
        super("usage");
    }

    protected String cimiFilter() {

        String cloud = (String) getRequest().getAttributes().get("cloud");

        String cimiFilter = String.format("&$filter=grouping='cloud'andcloud='%s'", cloud);

        String cimiFilterFromReq = RequestUtil.getCIMIFilter(getRequest());
        if (cimiFilterFromReq != null && !cimiFilterFromReq.isEmpty()) {
            cimiFilter += ("and" + cimiFilterFromReq);
        }

        return cimiFilter;
    }

    @Override
    protected String getPageRepresentation() {
        return "cloud-usages";
    }

}
