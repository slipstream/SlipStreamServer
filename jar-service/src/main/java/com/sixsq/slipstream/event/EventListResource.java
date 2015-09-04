package com.sixsq.slipstream.event;

import com.sixsq.slipstream.resource.BaseResource;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.RequestUtil;

import org.restlet.Context;
import org.restlet.data.Parameter;
import org.restlet.engine.header.Header;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;

import java.util.*;
import java.util.logging.Logger;

public class EventListResource extends BaseResource {

    private static final String SSCLJ_SERVER = "http://localhost:8201/api";
    private static final String EVENT_RESOURCE_NAME = "event";

    private static final Logger logger = Logger.getLogger(EventListResource.class.getName());


    @Get("html")
    public Representation toHtml() {
        String html = HtmlUtil.toHtmlFromJson(getEventsJSON(), getPageRepresentation(), getUser(), getRequest());
        return new StringRepresentation(html, MediaType.TEXT_HTML);
    }

    @Override
    protected String getPageRepresentation() {
        return "events";
    }

    private String getEventsJSON() {
        try {
            Context context = new Context();
            Series<Parameter> parameters = context.getParameters();
            parameters.add("socketTimeout", "1000");
            parameters.add("idleTimeout", "1000");
            parameters.add("idleCheckInterval", "1000");
            parameters.add("socketConnectTimeoutMs", "1000");

            String uri = SSCLJ_SERVER + "/" + EVENT_RESOURCE_NAME;

            logger.info("Will query Event resource with uri = '" + uri + "'");

            ClientResource resource = new ClientResource(context, uri);

            resource.setRetryOnError(false);
            Series<Header> headers = (Series<Header>) resource.getRequestAttributes().get("org.restlet.http.headers");
            if (headers == null) {
                headers = new Series<Header>(Header.class);
                resource.getRequestAttributes().put("org.restlet.http.headers", headers);
            }
            headers.add("slipstream-authn-info", getUser().getName());

            return resource.get().getText();

        } catch (Exception e) {
            logger.warning("Unable to getEventsJSON :" + e.getMessage());
            return null;
        }
    }


}
