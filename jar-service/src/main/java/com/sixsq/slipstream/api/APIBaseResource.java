package com.sixsq.slipstream.api;


import com.sixsq.slipstream.resource.BaseResource;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.SscljProxy;
import org.apache.commons.lang.StringUtils;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Get;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Gets JSON for a resource (from API server) and renders it to HTML
 * Subclassed for collection and single resource.
 *
 */
public abstract class APIBaseResource extends BaseResource {

    protected String namespace() {
        return "api";
    }

    protected String resourceName;

    APIBaseResource(String resourceName) {
        this.resourceName = resourceName;
    }

    protected String resourceUri() {
        return namespace();
    }

    protected String getSsclj() throws IOException {
        return SscljProxy.get(resourceUri(), getNameRoles(), true).getEntity().getText();
    }

    @Get("html")
    public Representation toHtml() {

        try {
            String html = HtmlUtil.toHtmlFromJson(getSsclj(), getPageRepresentation(), getUser(), getRequest());
            return new StringRepresentation(html, MediaType.TEXT_HTML);
        } catch (Exception e) {
            String message = "Unable to contact API Server: " + e.getMessage();
            throw (new ResourceException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, message));
        }
    }

    protected String getNameRoles() {

        List<String> nameRoles = new ArrayList<>();

        nameRoles.add(getUser().getName());
        if (getUser().isSuper()) {
            nameRoles.add("ADMIN");
        }
        if (getUser().getRoles() != null) {
            nameRoles.addAll(Arrays.asList(StringUtils.split(getUser().getRoles(), ",")));
        }

        return StringUtils.join(nameRoles, " ");
    }

    @Override
    protected abstract String getPageRepresentation();

}