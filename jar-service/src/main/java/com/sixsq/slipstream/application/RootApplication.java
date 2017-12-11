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



import com.sixsq.slipstream.action.ActionRouter;
import com.sixsq.slipstream.authn.CookieAuthenticator;
import com.sixsq.slipstream.authn.LoginResource;
import com.sixsq.slipstream.authn.LogoutResource;
import com.sixsq.slipstream.authn.RegistrationResource;
import com.sixsq.slipstream.authn.ResetPasswordResource;
import com.sixsq.slipstream.authz.SuperEnroler;
import com.sixsq.slipstream.cloudusage.CloudUsageRouter;
import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.DiscoverableConnectorServiceLoader;
import com.sixsq.slipstream.dashboard.DashboardRouter;
import com.sixsq.slipstream.es.CljElasticsearchHelper;
import com.sixsq.slipstream.event.Event;
import com.sixsq.slipstream.event.EventRouter;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.Util;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.filter.TrimmedMediaTypesFilter;
import com.sixsq.slipstream.initialstartup.CloudIds;
import com.sixsq.slipstream.initialstartup.Modules;
import com.sixsq.slipstream.initialstartup.Users;
import com.sixsq.slipstream.metrics.GraphiteRouter;
import com.sixsq.slipstream.module.ModuleListRouter;
import com.sixsq.slipstream.metrics.Metrics;
import com.sixsq.slipstream.module.ModuleRouter;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.resource.AppStoreResource;
import com.sixsq.slipstream.resource.ModulesChooserResource;
import com.sixsq.slipstream.resource.ReportRouter;
import com.sixsq.slipstream.resource.RootRedirectResource;
import com.sixsq.slipstream.resource.configuration.ServiceConfigurationResource;
import com.sixsq.slipstream.resource.NuvlaboxAdminRouter;
import com.sixsq.slipstream.run.RunRouter;
import com.sixsq.slipstream.run.VmsRouter;
import com.sixsq.slipstream.ui.UIResourceRouter;
import com.sixsq.slipstream.usage.UsageRouter;
import com.sixsq.slipstream.user.UserRouter;
import com.sixsq.slipstream.util.ConfigurationUtil;
import com.sixsq.slipstream.util.Logger;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.resource.Directory;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Filter;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.routing.TemplateRoute;
import org.restlet.security.Authenticator;
import org.restlet.service.MetadataService;
import slipstream.async.Collector;
import slipstream.async.GarbageCollector;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.ServiceLoader;

public class RootApplication extends Application {

	@SuppressWarnings("serial")
	private class Authenticators extends ArrayList<Authenticator> {

		public Authenticator getFirst() {
			return this.get(0);
		}

		public Authenticator getLast() {
			return this.get(this.size() -1);
		}

	}

	private class AuthenticatorsTemplateRoute {
		private TemplateRoute templateRoute;
		private Authenticators authenticators;

		public AuthenticatorsTemplateRoute(TemplateRoute templateRoute, Authenticators authenticators){
			this.templateRoute = templateRoute;
			this.authenticators = authenticators;
		}

		public Authenticators getAuthenticators() {
			return authenticators;
		}

		public TemplateRoute getTemplateRoute() {
			return templateRoute;
		}
	}

	public RootApplication() throws ValidationException {
		super();

		try {
			createShutdownHook();

			CljElasticsearchHelper.init();

			createStartupMetadata();
			loadOptionalConfiguration();

			initializeStatusServiceToHandleErrors();

			verifyMinimumDatabaseInfo();

			initializeMetadataService();

			// Load the configuration early
			Configuration.getInstance();

			Collector.start();
			GarbageCollector.start();

			Metrics.addJvmMetrics();
			if (Configuration.isMetricsGraphiteEnabled()) {
				Metrics.addGraphiteReporter();
			}
			if (Configuration.isMetricsLoggerEnabled()) {
				Metrics.addSlf4jReporter();
			}

			logServerStarted();

		} catch (ConfigurationException e) {
			Util.throwConfigurationException(e);
		}
	}

	private void createShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				String message = "Server shutdown";
				Event.postEvent("system", Event.Severity.high, message, "system", Event.EventType.system);
			}
		}));
	}

	private void loadOptionalConfiguration() {
		// Note: Connectors have already been loaded by the Configuration class

		// Load modules
        Modules.load();

		// Load cloud-ids
        CloudIds.load();

		// Load users
		Users.load();
	}

	private void initializeMetadataService() {
		MetadataService ms = getMetadataService();
		ms.addCommonExtensions();
		ms.setDefaultCharacterSet(CharacterSet.UTF_8);
		ms.addExtension("tgz", MediaType.APPLICATION_COMPRESS, true);
		ms.addExtension("multipart", MediaType.MULTIPART_ALL);
	}

	private void logServerStarted() {
		String message = "Server started";
		Logger.debug(message);
		Logger.info(message);
		Logger.warning(message);
		Logger.severe(message);
		Event.postEvent("system", Event.Severity.high, message, "system", Event.EventType.system);
	}

	protected void loadConnectors() {
		ServiceLoader<Connector> connectorLoader = ServiceLoader.load(Connector.class);

		for (Connector c : connectorLoader) {
			getLogger().info("Connector name: " + c.getCloudServiceName());
		}
	}

	private void createStartupMetadata() {
        try {
            Users.create();
        } catch (ValidationException e) {
            getLogger().warning("Error creating default users... already existing?");
        } catch (NotFoundException e) {
            getLogger().warning("Error creating default users... already existing?");
        } catch (NoSuchAlgorithmException e) {
            getLogger().warning("Error creating default users... already existing?");
        } catch (UnsupportedEncodingException e) {
            getLogger().warning("Error creating default users... already existing?");
        }
    }

	private void initializeStatusServiceToHandleErrors() {
		CommonStatusService statusService = new CommonStatusService();
		setStatusService(statusService);
	}

	private static void verifyMinimumDatabaseInfo() throws ConfigurationException, ValidationException {
		User user = Users.loadSuper();
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
			attachModulesChooser(router);
			attachMetering(router);
			attachAction(router);
			attachModule(router);
			attachUser(router);
			attachDashboard(router);
			attachVms(router);
			attachRun(router);
			attachTeapot(router);
			attachRootRedirect(router);
			attachLogin(router);
			attachLogout(router);
			attachConfiguration(router);
			attachReports(router);
			attachEvent(router);
			attachUsage(router);
			attachCloudUsage(router);
			attachNuvlaboxAdmin(router);

			attachAppStore(router);
			attachUIResource(router);
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
		String staticContentLocation = System.getProperty("static.content.location", "war:///static-content");
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

	private void attachTeapot(RootRouter router) throws ConfigurationException, ValidationException {
		router.attach("/teapot", new Filter() {
			protected int beforeHandle(Request request, Response response) {
				response.setStatus(new Status(418, "I'm a teapot!", "I'm a teapot!",
						"http://tools.ietf.org/html/rfc2324"));
				return Filter.STOP;
			}
		});
	}

	private void attachConfiguration(RootRouter router) {
		String rootUri = ServiceConfigurationResource.CONFIGURATION_PATH.replaceAll("^/",	"");
		guardAndAttach(router, ServiceConfigurationResource.class, rootUri);
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

	private void attachRun(RootRouter router) {
		guardAndAttach(router, new RunRouter(getContext()), "run");
	}

	private void attachDashboard(RootRouter router) {
		guardAndAttach(router, new DashboardRouter(getContext()), "dashboard");
	}

	private void attachVms(RootRouter router) {
		guardAndAttach(router, new VmsRouter(getContext()), "vms");
	}

	private Authenticators getAuthenticators(Application application) {
		Authenticators authenticators = new Authenticators();

		Authenticator cookieAuthenticator = new CookieAuthenticator(getContext());
		cookieAuthenticator.setOptional(false);

		cookieAuthenticator.setEnroler(new SuperEnroler(application));

		authenticators.add(cookieAuthenticator);

		return authenticators;
	}

	private TemplateRoute attach(Router rootRouter, String rootUri, Authenticator authenticator) {
		TemplateRoute route = rootRouter.attach(convertToRouterRoot(rootUri), authenticator);
		route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
		return route;
	}

	private AuthenticatorsTemplateRoute guardAndAttach(Router rootRouter, Class<? extends ServerResource> router,
			String rootUri) {
		Authenticators authenticators = getAuthenticators(rootRouter.getApplication());
		authenticators.getLast().setNext(router);
		TemplateRoute route = attach(rootRouter, rootUri, authenticators.getFirst());
		return new AuthenticatorsTemplateRoute(route, authenticators);
	}

	private AuthenticatorsTemplateRoute guardAndAttach(Router rootRouter, Router router, String rootUri) {
		Authenticators authenticators = getAuthenticators(rootRouter.getApplication());
		authenticators.getLast().setNext(router);

		TemplateRoute route = attach(rootRouter, rootUri, authenticators.getFirst());

		return new AuthenticatorsTemplateRoute(route, authenticators);
	}

	private void attachRootRedirect(RootRouter rootRouter) {
		rootRouter.attach("", RootRedirectResource.class);
		rootRouter.attach("/", RootRedirectResource.class);
	}

	private void attachUser(RootRouter router) {
		guardAndAttach(router, new UserRouter(getContext()), User.RESOURCE_URL_PREFIX);
	}

	private void attachModule(RootRouter router) {
		guardAndAttach(router, new ModuleRouter(getContext()), Module.RESOURCE_URI_PREFIX);
	}

	private String convertToRouterRoot(String prefix) {
		return "/" + (prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix);
	}

	private void attachAction(RootRouter router) {
		TemplateRoute route;
		route = router.attach("/action/", new ActionRouter());
		route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
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

	private void attachEvent(RootRouter router) throws ValidationException {
		guardAndAttach(router, new EventRouter(getContext()), "event");
	}

	private void attachUsage(RootRouter router) throws ValidationException {
		guardAndAttach(router, new UsageRouter(getContext()), "usage");
	}

	private void attachCloudUsage(RootRouter router) throws ValidationException {
		guardAndAttach(router, new CloudUsageRouter(getContext()), "cloud-usage");
	}

	private void attachAppStore(RootRouter router) throws ValidationException {
		guardAndAttach(router, new ModuleListRouter(getContext(), AppStoreResource.class), "appstore");
	}

	private void attachUIResource(RootRouter router) throws ValidationException {
		guardAndAttach(router, new UIResourceRouter(getContext()), "ui");
	}

	private void attachModulesChooser(RootRouter router) throws ValidationException {
		guardAndAttach(router, new ModuleListRouter(getContext(), ModulesChooserResource.class), "chooser");
	}

	private void attachNuvlaboxAdmin(RootRouter router) throws ValidationException {
		guardAndAttach(router, new NuvlaboxAdminRouter(getContext()), "nuvlabox-admin");
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
