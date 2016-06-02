package com.sixsq.slipstream.api;


import com.sixsq.slipstream.sscljproxy.SscljProxy;
import com.sixsq.slipstream.util.HtmlUtil;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * Gets JSON for a resource (from API server) and renders it to HTML
 * Subclassed for collection and single resource.
 *
 */
public abstract class APIBaseResource extends SscljProxy {

    protected String namespace() {
        return "api";
    }

    protected String resourceName;

    public APIBaseResource(String resourceName) {
        this.resourceName = resourceName;
    }

    @Override
    protected String uri() {
        return SSCLJ_SERVER_BASE_URL + "/" + namespace();
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

    @Override
    protected abstract String getPageRepresentation();

}