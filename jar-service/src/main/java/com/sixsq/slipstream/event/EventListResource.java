package com.sixsq.slipstream.event;

import com.sixsq.slipstream.resource.BaseResource;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

public class EventListResource extends BaseResource {

    @Get("html")
    public Representation toHtml() {

        // TODO retrieve events list from API server
        // String html = HtmlUtil.toHtml(TODO, getPageRepresentation(), getUser(), getRequest());

        // TODO
        return new StringRepresentation("<h2>hello</h2>", MediaType.TEXT_HTML);
    }

    @Override
    protected String getPageRepresentation() {
        return "events";
    }
}
