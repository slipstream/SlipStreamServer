package com.sixsq.slipstream.usage;

import com.sixsq.slipstream.api.APICollectionResource;
import com.sixsq.slipstream.util.RequestUtil;

public class UsageListResource extends APICollectionResource {

    public UsageListResource() {
        super("usage");
    }

    @Override
    protected String getSsclj() {
        return "{}";
    }

    @Override
    protected String cimiFilter() {
        return "";
    }
}
