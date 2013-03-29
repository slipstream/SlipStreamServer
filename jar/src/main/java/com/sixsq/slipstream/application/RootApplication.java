package com.sixsq.slipstream.application;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2013 SixSq Sarl (sixsq.com)
 * =====
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -=================================================================-
 */


import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.resource.Directory;
import org.restlet.routing.Filter;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.routing.TemplateRoute;
import org.restlet.routing.Variable;
import org.restlet.security.Authenticator;
import org.restlet.security.Authorizer;

import com.sixsq.slipstream.action.ActionRouter;
import com.sixsq.slipstream.authn.BasicAuthenticator;
import com.sixsq.slipstream.authn.CookieAuthenticator;
import com.sixsq.slipstream.authn.LoginResource;
import com.sixsq.slipstream.authn.LogoutResource;
import com.sixsq.slipstream.authn.RegistrationResource;
import com.sixsq.slipstream.authz.ReportsAuthorizer;
import com.sixsq.slipstream.authz.SuperEnroler;
import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.configuration.ServiceConfigurationResource;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamInternalException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.filter.Decorator;
import com.sixsq.slipstream.filter.TrimmedMediaTypesFilter;
import com.sixsq.slipstream.initialstartup.BaseImages;
import com.sixsq.slipstream.initialstartup.Tutorials;
import com.sixsq.slipstream.initialstartup.Users;
import com.sixsq.slipstream.module.ModuleRouter;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.resource.ResultsDirectory;
import com.sixsq.slipstream.resource.WelcomeResource;
import com.sixsq.slipstream.run.DashboardRouter;
import com.sixsq.slipstream.run.RunRouter;
import com.sixsq.slipstream.staticcontent.StaticContentDecorator;
import com.sixsq.slipstream.user.UserRouter;
import com.sixsq.slipstream.util.RequestUtil;

public class RootApplication extends Application {

    public RootApplication() throws ValidationException {
        super();

        try {

            createStartupMetadata();

            initializeStatusServiceToHandleErrors();

            verifyMinimumDatabaseInfo();

        } catch (ConfigurationException e) {
            throw new SlipStreamInternalException(e.getMessage(), e);
        } catch (NotFoundException e) {
            throw new SlipStreamInternalException(e.getMessage(), e);
        }

        getMetadataService().setDefaultMediaType(MediaType.TEXT_HTML);
        getMetadataService().addExtension("tgz",
                MediaType.APPLICATION_COMPRESS, true);
        getMetadataService().addExtension("multipart", MediaType.MULTIPART_ALL);
    }

    private void createStartupMetadata() throws ValidationException,
            NotFoundException, ConfigurationException {

        try {
            Users.create();
        } catch (Exception ex) {
            getLogger().warning(
                    "Error creating default users... already existing?");
        }
        try {
            BaseImages.create();
        } catch (Exception ex) {
            getLogger().warning(
                    "Error creating base images... already existing?");
        }
        try {
            Tutorials.create();
        } catch (Exception ex) {
            getLogger()
                    .warning("Error creating tutorials... already existing?");
        }
    }

    private void initializeStatusServiceToHandleErrors()
            throws ConfigurationException {

        CommonStatusService statusService = new CommonStatusService();

        setStatusService(statusService);

    }

    private static void verifyMinimumDatabaseInfo()
            throws ConfigurationException {

        User user = User.loadByName("super");
        if (user == null) {
            throw new ConfigurationException("super user is missing");
        }

        // get the instance, which will load the configuration and perform
        // self validity check
        Configuration.getInstance();

    }

    @Override
    public Restlet createInboundRoot() {

        RootRouter router = new RootRouter(getContext());

        attachAction(router);
        attachModule(router);
        attachUser(router);
        attachDashboard(router);
        try {
            attachRun(router);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            throw (new SlipStreamRuntimeException(e));
        }
        attachWelcome(router);
        attachLogin(router);
        attachLogout(router);
        attachRegister(router);
        attachConfiguration(router);
        attachSupport(router);
        attachDocumentation(router);
        try {
            attachReports(router);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            throw (new SlipStreamRuntimeException(e));
        }

        Directory directoryStaticContent = attachStaticContent();
        router.attachDefault(directoryStaticContent);

        Directory directoryDownloads = attachDownloadsDirectory();
        router.attach("/downloads", directoryDownloads);
        
        enableTunnelService();

        // Some browsers need to have their media types preferences trimmed.
        // Create a filter and put this in front of the application router.
        return new TrimmedMediaTypesFilter(getContext(), router);
    }

    /**
     * During dev, set static content to local dir (e.g.
     * "file:///Users/meb/Documents/workspace/SlipStream/SlipStreamServer/src/main/webapp/static-content/"
     * )
     */
    private Directory attachStaticContent() {
        String staticContentLocation = System.getProperty(
                "static.content.location", "war:///static-content");
        Directory directory = new Directory(getContext(), staticContentLocation);
        directory.setModifiable(false);
        directory.setListingAllowed(true);
        return directory;
    }

    private Directory attachDownloadsDirectory() {
    	String staticContentLocation = System.getProperty(
    			"downloads.directory.location", "file:///opt/slipstream/downloads");
    	Directory directory = new Directory(getContext(), staticContentLocation);
    	directory.setModifiable(false);
    	return directory;
    }

    private void attachReports(RootRouter router) throws ConfigurationException {
        String reportsLocation = Configuration.getInstance().getProperty(
                "slipstream.reports.location");

//        router.getContext().getClientDispatcher().getProtocols()
//                .add(Protocol.FILE);
        ResultsDirectory directory = new ResultsDirectory(getContext(),
                "file://" + reportsLocation);

        Filter decorator = new Decorator();
        decorator.setNext(directory);
        Authorizer authorizer = new ReportsAuthorizer();
        authorizer.setNext(decorator);
        Authenticator authenticator = new CookieAuthenticator(getContext());
        authenticator.setNext(authorizer);
        authenticator.setEnroler(new SuperEnroler());

        router.attach("/reports", authenticator);

//        router.getContext().getClientDispatcher().getProtocols()
//                .add(Protocol.FILE);
    }

    private void attachConfiguration(RootRouter router) {
        TemplateRoute route;
        Authenticator authenticator = new CookieAuthenticator(getContext());
        authenticator.setNext(ServiceConfigurationResource.class);
        authenticator.setEnroler(new SuperEnroler());
        route = router.attach(ServiceConfigurationResource.CONFIGURATION_PATH, authenticator);
        route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
    }

    private void attachRegister(RootRouter router) {
        TemplateRoute route;
        route = router.attach("/register", RegistrationResource.class);
        route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
    }

    private void attachLogout(RootRouter router) {
        TemplateRoute route;
        route = router.attach("/logout", LogoutResource.class);
        route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
    }

    private void attachLogin(RootRouter router) {
        TemplateRoute route;
        route = router.attach(LoginResource.getResourceRoot()
                + "?embedded={embedded}", LoginResource.class);
        route.setMatchingQuery(true);
        route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
        route.getTemplate().getVariables()
                .put("embedded", new Variable(Variable.TYPE_URI_QUERY));

        route = router.attach(LoginResource.getResourceRoot(),
                LoginResource.class);
        route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
    }

    private void attachRun(RootRouter router) throws ConfigurationException {
        TemplateRoute route;

        Authenticator basicAuthenticator = new BasicAuthenticator(getContext());
        basicAuthenticator.setEnroler(new SuperEnroler());

        Authenticator cookieAuthenticator = new CookieAuthenticator(
                getContext());
        cookieAuthenticator.setOptional(true);

        cookieAuthenticator.setNext(basicAuthenticator);
        cookieAuthenticator.setEnroler(new SuperEnroler());

        basicAuthenticator.setNext(new RunRouter(getContext()));

        route = router.attach(convertToRouterRoot(Run.RESOURCE_URI_PREFIX),
                cookieAuthenticator);
        route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
    }

    private void attachDashboard(RootRouter router) throws ConfigurationException {
        TemplateRoute route;

        Authenticator basicAuthenticator = new BasicAuthenticator(getContext());
        basicAuthenticator.setEnroler(new SuperEnroler());

        Authenticator cookieAuthenticator = new CookieAuthenticator(
                getContext());
        cookieAuthenticator.setOptional(true);

        cookieAuthenticator.setNext(basicAuthenticator);
        cookieAuthenticator.setEnroler(new SuperEnroler());

        basicAuthenticator.setNext(new DashboardRouter(getContext()));

        route = router.attach(convertToRouterRoot("dashboard"),
                cookieAuthenticator);
        route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
    }

    private void attachUser(RootRouter router) {
        TemplateRoute route;
        
        Authenticator basicAuthenticator = new BasicAuthenticator(getContext());
        basicAuthenticator.setEnroler(new SuperEnroler());

        Authenticator cookieAuthenticator = new CookieAuthenticator(
                getContext());
        cookieAuthenticator.setOptional(true);

        cookieAuthenticator.setNext(basicAuthenticator);

        basicAuthenticator.setNext(new UserRouter(getContext()));
        route = router.attach(convertToRouterRoot(User.RESOURCE_URL_PREFIX),
        		cookieAuthenticator);
        route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
    }

    private void attachModule(RootRouter router) {
        TemplateRoute route;

        Authenticator basicAuthenticator = new BasicAuthenticator(getContext());
        basicAuthenticator.setEnroler(new SuperEnroler());

        Authenticator cookieAuthenticator = new CookieAuthenticator(
                getContext());
        cookieAuthenticator.setOptional(true);

        cookieAuthenticator.setNext(basicAuthenticator);

        basicAuthenticator.setNext(new ModuleRouter(getContext()));

        route = router.attach(convertToRouterRoot(Module.RESOURCE_URI_PREFIX),
                cookieAuthenticator);
        route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
    }

    private String convertToRouterRoot(String prefix) {
        return "/"
                + (prefix.endsWith("/") ? prefix.substring(0,
                        prefix.length() - 1) : prefix);
    }

    private void attachAction(RootRouter router) {
        TemplateRoute route;
        route = router.attach("/action/", new ActionRouter());
        route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
    }

    private void attachDocumentation(RootRouter router) {
        StaticContentDecorator restlet = new StaticContentDecorator(
                "documentation");
        router.attach("/documentation", restlet);
        router.attach("/documentation/", restlet);
    }

    private void attachSupport(RootRouter router) {
        StaticContentDecorator restlet = new StaticContentDecorator("support");
        router.attach("/support", restlet);
        router.attach("/support/", restlet);
    }

    private void attachWelcome(RootRouter router) {
        router.attach("/", WelcomeResource.class); 
    }

    private void enableTunnelService() {
        getTunnelService().setEnabled(true);
    }

    public void addConfigurationToRequest(Request request)
            throws ConfigurationException {

        RequestUtil.addConfigurationToRequest(request);

    }

    public class RootRouter extends Router {

        public RootRouter(Context context) {
            super(context);
        }

        @Override
        public void doHandle(Restlet next, Request request, Response response) {

            try {
                addConfigurationToRequest(request);
            } catch (ConfigurationException e) {
                throw new SlipStreamInternalException(e.getMessage(), e);
            }

            super.doHandle(next, request, response);
        }

    }
}