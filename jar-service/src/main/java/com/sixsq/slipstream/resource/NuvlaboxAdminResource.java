package com.sixsq.slipstream.resource;

import com.sixsq.slipstream.authz.SuperEnroler;
import com.sixsq.slipstream.util.HtmlUtil;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.data.Status;

public class NuvlaboxAdminResource extends BaseResource {

    public static final String RESOURCE_URI = "nuvlabox-admin";

    @Get("html")
    public Representation toHtml() {
        String html = HtmlUtil.toHtml("", getPageRepresentation(), getUser(), getRequest());
        return new StringRepresentation(html, MediaType.TEXT_HTML);
    }

    @Override
    protected String getPageRepresentation() {
        return RESOURCE_URI;
    }

    @Override
    protected void authorize() {
        if (!getClientInfo().getRoles().contains(SuperEnroler.Super)) {
            throw (new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                    "Only administrators can access this resource"));
        }
    }
}