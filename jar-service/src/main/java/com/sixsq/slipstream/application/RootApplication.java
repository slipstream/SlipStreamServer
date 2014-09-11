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

import java.util.ServiceLoader;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.routing.TemplateRoute;
import org.restlet.routing.Variable;
import org.restlet.security.Authenticator;
import org.restlet.service.MetadataService;

import slipstream.async.Collector;
import slipstream.async.GarbageCollector;

import com.sixsq.slipstream.action.ActionRouter;
import com.sixsq.slipstream.authn.BasicAuthenticator;
import com.sixsq.slipstream.authn.CookieAuthenticator;
import com.sixsq.slipstream.authn.LoginResource;
import com.sixsq.slipstream.authn.LogoutResource;
import com.sixsq.slipstream.authn.RegistrationResource;
import com.sixsq.slipstream.authn.ResetPasswordResource;
import com.sixsq.slipstream.authz.SuperEnroler;
import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.DiscoverableConnectorServiceLoader;
import com.sixsq.slipstream.dashboard.DashboardRouter;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.Util;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.filter.TrimmedMediaTypesFilter;
import com.sixsq.slipstream.initialstartup.Users;
import com.sixsq.slipstream.metrics.GraphiteRouter;
import com.sixsq.slipstream.module.ModuleRouter;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.resource.ConnectorClassRouter;
import com.sixsq.slipstream.resource.ConnectorInstanceRouter;
import com.sixsq.slipstream.resource.DocumentationResource;
import com.sixsq.slipstream.resource.ReportRouter;
import com.sixsq.slipstream.resource.ServiceCatalogRouter;
import com.sixsq.slipstream.resource.WelcomeResource;
import com.sixsq.slipstream.resource.configuration.ServiceConfigurationResource;
import com.sixsq.slipstream.run.RunRouter;
import com.sixsq.slipstream.run.VmsRouter;
import com.sixsq.slipstream.user.UserRouter;
import com.sixsq.slipstream.util.ConfigurationUtil;
import com.sixsq.slipstream.util.Logger;

public class RootApplication extends Application {

	public RootApplication() throws ValidationException {
		super();

		try {

			createStartupMetadata();

			initializeStatusServiceToHandleErrors();

			verifyMinimumDatabaseInfo();

		} catch (ConfigurationException e) {
			Util.throwConfigurationException(e);
		} catch (NotFoundException e) {
			Util.throwConfigurationException(e);
		}

		MetadataService ms = getMetadataService();
		ms.addCommonExtensions();
		ms.setDefaultCharacterSet(CharacterSet.UTF_8);
		ms.addExtension("tgz", MediaType.APPLICATION_COMPRESS, true);
		ms.addExtension("multipart", MediaType.MULTIPART_ALL);

		Collector.start();
		GarbageCollector.start();

		logServerStarted();
	}

	private void logServerStarted() {
		String message = "Server started";
		Logger.info(message);
		Logger.warning(message);
		Logger.severe(message);
	}

	protected void loadConnectors() {
		ServiceLoader<Connector> connectorLoader = ServiceLoader.load(Connector.class);

		for (Connector c : connectorLoader) {
			getLogger().info("Connector name: " + c.getCloudServiceName());
		}
	}

	private void createStartupMetadata() throws ValidationException, NotFoundException, ConfigurationException {

		try {
			Users.create();
		} catch (Exception ex) {
			getLogger().warning("Error creating default users... already existing?");
		}
	}

	private void initializeStatusServiceToHandleErrors() throws ConfigurationException {

		CommonStatusService statusService = new CommonStatusService();

		setStatusService(statusService);

	}

	private static void verifyMinimumDatabaseInfo() throws ConfigurationException, ValidationException {

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

		enableTunnelService();

		RootRouter router = new RootRouter(getContext());

		try {
			attachMetering(router);
			attachAction(router);
			attachModule(router);
			attachUser(router);
			attachDashboard(router);
			attachVms(router);
			attachRun(router);
			attachWelcome(router);
			attachLogin(router);
			attachLogout(router);
			attachConfiguration(router);
			attachServiceCatalog(router); // needs to be after configuration
			attachDocumentation(router);
			attachReports(router);
			attachConnectorInstance(router);
			attachConnectorClass(router);
		} catch (ConfigurationException e) {
			Util.throwConfigurationException(e);
		} catch (ValidationException e) {
			Util.throwConfigurationException(e);
		}

		Directory directoryStaticContent = attachStaticContent();
		router.attachDefault(directoryStaticContent);

		Directory directoryDownloads = attachDownloadsDirectory();
		router.attach("/downloads", directoryDownloads);

		// Some browsers need to have their media types preferences trimmed.
		// Create a filter and put this in front of the application router.
		return new TrimmedMediaTypesFilter(getContext(), router);
	}

    @Override
    public void start() throws Exception {
        super.start();
        DiscoverableConnectorServiceLoader.initializeAll();
    }

    @Override
    public void stop() throws Exception {
        DiscoverableConnectorServiceLoader.shutdownAll();
        super.stop();
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
				"downloads.directory.location",
				"file:///opt/slipstream/downloads");
		Directory directory = new Directory(getContext(), staticContentLocation);
		directory.setModifiable(false);
		return directory;
	}

	private void attachReports(RootRouter router) throws ConfigurationException, ValidationException {
		router.attach("/reports", new ReportRouter(getContext(), router.getApplication()));
	}

	private void attachConfiguration(RootRouter router) {
		TemplateRoute route;
		Authenticator authenticator = new CookieAuthenticator(getContext());
		authenticator.setNext(ServiceConfigurationResource.class);
		authenticator.setEnroler(new SuperEnroler(router.getApplication()));
		route = router.attach(ServiceConfigurationResource.CONFIGURATION_PATH, authenticator);
		route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
	}

	private void attachLogout(RootRouter router) {
		TemplateRoute route;
		route = router.attach("/logout", LogoutResource.class);
		route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
	}

	private void attachLogin(RootRouter router) {
		TemplateRoute route = router.attach(LoginResource.getResourceRoot(), LoginResource.class);
		route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);

		router.attach(RegistrationResource.getResourceRoot(), RegistrationResource.class);

		router.attach(ResetPasswordResource.getResourceRoot(), ResetPasswordResource.class);
	}

	private void attachRun(RootRouter router) throws ConfigurationException {
		guardAndAttach(router, new RunRouter(getContext()), "run");
	}

	private void attachDashboard(RootRouter router)
			throws ConfigurationException {
		guardAndAttach(router, new DashboardRouter(getContext()), "dashboard");
	}

	private void attachServiceCatalog(RootRouter router) throws ConfigurationException, ValidationException {
		guardAndAttach(router, new ServiceCatalogRouter(getContext()), "service_catalog");
	}

	private void attachVms(RootRouter router) throws ConfigurationException {
		guardAndAttach(router, new VmsRouter(getContext()), "vms");
	}

	private void guardAndAttach(Router rootRouter, Router router, String rootUri) throws ConfigurationException {
		Authenticator basicAuthenticator = new BasicAuthenticator(getContext());
		basicAuthenticator.setEnroler(new SuperEnroler(router.getApplication()));

		Authenticator cookieAuthenticator = new CookieAuthenticator(getContext());
		cookieAuthenticator.setOptional(true);

		cookieAuthenticator.setNext(basicAuthenticator);
		cookieAuthenticator.setEnroler(new SuperEnroler(router.getApplication()));

		basicAuthenticator.setNext(router);

		TemplateRoute route = rootRouter.attach(convertToRouterRoot(rootUri), cookieAuthenticator);
		route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
	}

	private void attachUser(RootRouter router) {
		TemplateRoute route;

		Authenticator basicAuthenticator = new BasicAuthenticator(getContext());
		basicAuthenticator.setEnroler(new SuperEnroler(router.getApplication()));

		Authenticator cookieAuthenticator = new CookieAuthenticator(getContext());
		cookieAuthenticator.setOptional(true);

		cookieAuthenticator.setNext(basicAuthenticator);

		basicAuthenticator.setNext(new UserRouter(getContext()));
		route = router.attach(convertToRouterRoot(User.RESOURCE_URL_PREFIX), cookieAuthenticator);
		route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
	}

	private void attachModule(RootRouter router) {

		Authenticator basicAuthenticator = new BasicAuthenticator(getContext());
		basicAuthenticator.setEnroler(new SuperEnroler(router.getApplication()));

		Authenticator cookieAuthenticator = new CookieAuthenticator(getContext());
		cookieAuthenticator.setOptional(true);

		cookieAuthenticator.setNext(basicAuthenticator);

		basicAuthenticator.setNext(new ModuleRouter(getContext()));

		TemplateRoute route = router.attach(convertToRouterRoot(Module.RESOURCE_URI_PREFIX), cookieAuthenticator);
		route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
	}

	private String convertToRouterRoot(String prefix) {
		return "/" + (prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix);
	}

	private void attachAction(RootRouter router) {
		TemplateRoute route;
		route = router.attach("/action/", new ActionRouter());
		route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
	}

	private void attachDocumentation(RootRouter router) {
		router.attach("/documentation", DocumentationResource.class);
	}

	private void attachWelcome(RootRouter router) {
		Authenticator basicAuthenticator = new BasicAuthenticator(getContext());
		basicAuthenticator.setEnroler(new SuperEnroler(router.getApplication()));

		Authenticator cookieAuthenticator = new CookieAuthenticator(getContext());
		cookieAuthenticator.setOptional(true);

		cookieAuthenticator.setNext(basicAuthenticator);

		basicAuthenticator.setNext(WelcomeResource.class);

		TemplateRoute route = router.attach("/?chooser={chooser}", cookieAuthenticator);
		route.setMatchingQuery(true);
		route.getTemplate().getVariables().put("chooser", new Variable(Variable.TYPE_URI_QUERY));

		router.attach("/", cookieAuthenticator);
	}

	private void attachConnectorInstance(RootRouter router) throws ConfigurationException, ValidationException {
		guardAndAttach(router, new ConnectorInstanceRouter(getContext()), "conninst");
	}

	private void attachConnectorClass(RootRouter router) throws ConfigurationException, ValidationException {
		guardAndAttach(router, new ConnectorClassRouter(getContext()), "connclass");
	}

	private void enableTunnelService() {
		getTunnelService().setExtensionsTunnel(true);
		getTunnelService().setEnabled(true);
	}

	public void addConfigurationToRequest(Request request) throws ConfigurationException, ValidationException {

		ConfigurationUtil.addConfigurationToRequest(request);

	}

	private void attachMetering(RootRouter router) throws ConfigurationException, ValidationException {

		guardAndAttach(router, new GraphiteRouter(getContext()), GraphiteRouter.ROOT_URI);
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
				Util.throwConfigurationException(e);
			} catch (ValidationException e) {
				Util.throwClientValidationError(e.getMessage());
			}

			super.doHandle(next, request, response);
		}

	}
}
