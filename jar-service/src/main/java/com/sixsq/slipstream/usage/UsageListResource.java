package com.sixsq.slipstream.usage;

import com.sixsq.slipstream.api.APICollectionResource;
import com.sixsq.slipstream.util.RequestUtil;

public class UsageListResource extends APICollectionResource {

    public UsageListResource() {
        super("usage-summary");
    }

    protected String cimiFilter() {
        String cimiFilter = String.format("&$filter=user='%s'", getUser().getName());

        String cimiFilterFromReq = RequestUtil.getCIMIFilter(getRequest());
        if (cimiFilterFromReq != null && !cimiFilterFromReq.isEmpty()) {
            cimiFilter += ("and" + cimiFilterFromReq);
        }
        return cimiFilter;
    }
}
