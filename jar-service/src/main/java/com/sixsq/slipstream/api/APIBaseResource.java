package com.sixsq.slipstream.api;


import com.sixsq.slipstream.resource.BaseResource;
import com.sixsq.slipstream.util.HtmlUtil;
import org.apache.commons.lang.StringUtils;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Gets JSON for a resource (from API server) and renders it to HTML
 * Subclassed for collection and single resource.
 *
 */
public abstract class APIBaseResource extends BaseResource {

    protected static final String SSCLJ_SERVER = "http://localhost:8201/api";
    private static final Logger logger = Logger.getLogger(APIBaseResource.class.getName());
    protected String resourceName;

    public APIBaseResource(String resourceName) {
        this.resourceName = resourceName;
    }

    @Get("html")
    public Representation toHtml() {

        try {
            String html = HtmlUtil.toHtmlFromJson(getAPIResourceAsJSON(), getPageRepresentation(), getUser(), getRequest());
            return new StringRepresentation(html, MediaType.TEXT_HTML);
        } catch (Exception e) {
            String message = "Unable to contact API Server: " + e.getMessage();
            throw (new ResourceException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, message));
        }
    }

    @Override
    protected abstract String getPageRepresentation();

    private String getAPIResourceAsJSON() throws IOException {

        Context context = new Context();
        Series<Parameter> parameters = context.getParameters();
        parameters.add("socketTimeout", "1000");
        parameters.add("idleTimeout", "1000");
        parameters.add("idleCheckInterval", "1000");
        parameters.add("socketConnectTimeoutMs", "1000");

        String uri = uri();
        logger.info("Will query resource with uri = '" + uri + "'");
        ClientResource resource = new ClientResource(context, uri);

        resource.setRetryOnError(false);
        Series<Header> headers = (Series<Header>) resource.getRequestAttributes().get("org.restlet.http.headers");
        if (headers == null) {
            headers = new Series<Header>(Header.class);
            resource.getRequestAttributes().put("org.restlet.http.headers", headers);
        }
        headers.add("slipstream-authn-info", getNameRoles());

        return resource.get().getText();
    }

    private String getNameRoles() {

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

    protected abstract String uri();

}