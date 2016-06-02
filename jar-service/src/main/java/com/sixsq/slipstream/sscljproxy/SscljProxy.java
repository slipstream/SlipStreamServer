package com.sixsq.slipstream.sscljproxy;

import com.sixsq.slipstream.resource.BaseResource;
import org.apache.commons.lang.StringUtils;
import org.restlet.Context;
import org.restlet.data.Parameter;
import org.restlet.engine.header.Header;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
public abstract class SscljProxy extends BaseResource {

    private static final Logger logger = Logger.getLogger(SscljProxy.class.getName());

    protected static final String SSCLJ_SERVER_BASE_URL = "http://localhost:8201";

    protected abstract String namespace();

    private ClientResource resource() {
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

        return resource;
    }

    protected String getSsclj() throws IOException {
        return resource().get().getText();
    }

    protected String putSsclj() throws IOException {
        return resource().put(null).getText();
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

    protected abstract String uri();
}
