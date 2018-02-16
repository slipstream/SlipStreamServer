package com.sixsq.slipstream.run;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.connector.UserParametersFactoryBase;
import com.sixsq.slipstream.connector.local.LocalConnector;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.RunFactory;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Node;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;
import com.sixsq.slipstream.statemachine.States;
import com.sixsq.slipstream.util.CommonTestUtil;

public class RunTestBase {

	private static final String USER_DEFAULT = "user";
	protected static User user = null;
	protected static ImageModule image = null;
	protected static ImageModule imageref = null;
	protected static ImageModule imagebase = null;
	protected static ImageModule imagenoref = null;
	protected static ImageModule imagebuildme = null;
	protected static LocalConnector localConnector = new LocalConnector();
	protected static final String cloudServiceName = localConnector.getCloudServiceName();
	protected static final Set<String> cloudServiceNames = new HashSet<String>(Arrays.asList(cloudServiceName));

	public static DeploymentModule deployment = null;
	protected static ImageModule imageForDeployment1 = null;
	protected static ImageModule imageForDeployment2 = null;

	private static final Logger logger = Logger.getLogger(RunTestBase.class.getName());

	protected static void setupImages() throws ValidationException {
		imagebase = new ImageModule("test/imagebase");
		imagebase.setImageId("base_image_id", cloudServiceName);
		imagebase.setIsBase(true);
		imagebase = imagebase.store();

		imageref = new ImageModule("test/imageref");
		imageref.setModuleReference(imagebase);
		imageref.setImageId("ref_image_id", cloudServiceName);
		imageref = imageref.store();

		image = new ImageModule("test/image");
		image.setModuleReference(imageref.getResourceUri());
		image.setRecipe("a recipe");
		image.setImageId("image_id", cloudServiceName);
		image = image.store();

		imagebuildme = new ImageModule("test/imagebuildme");
		imagebuildme.setModuleReference(imageref.getResourceUri());
		imagebuildme.setRecipe("a recipe");
		imagebuildme = imagebuildme.store();

		imagenoref = new ImageModule("test/imagenoref");
		imagenoref.setRecipe("a recipe");
		imagenoref = imagenoref.store();

		imagenoref = new ImageModule("test/imagenoref");
		imagenoref.setRecipe("a recipe");
		imagenoref = imagenoref.store();

		createUser();

		CommonTestUtil.addSshKeys(user);
	}

	protected static void createUser() throws ConfigurationException,
			ValidationException {

		user = CommonTestUtil.createUser("RunTestBaseUser", "password");

		// Add dummy credentials
		user.setParameter(new UserParameter(
				UserParametersFactoryBase.KEY_PARAMETER_NAME, "key", ""));
		user.setParameter(new UserParameter(
				UserParametersFactoryBase.SECRET_PARAMETER_NAME, "secret", ""));
		user.setParameter(new UserParameter(
				UserParametersFactoryBase.DEFAULT_CLOUD_SERVICE_PARAMETER_NAME, cloudServiceName, ""));
	}

	protected static void tearDownImagesAndUser() {
		try {
			imagebase.remove();
			imageref.remove();
			image.remove();
			user.remove();
		} catch (Exception e) {
			e.printStackTrace();
			logger.warning("Failed removing images and user in RunTestBase.tearDownImagesAndUser().");
		}
	}

	protected static void setupDeployments() throws ValidationException,
			NotFoundException {

		imageForDeployment1 = new ImageModule("test/imagefordeployment1");
		imageForDeployment1
				.setParameter(new ModuleParameter("pi1", "pi1 init value",
						"pi1 parameter desc", ParameterCategory.Input));
		imageForDeployment1.setParameter(new ModuleParameter("po1",
				"po1 init value", "po1 parameter desc",
				ParameterCategory.Output));

		imageForDeployment1.setIsBase(true);
		imageForDeployment1.setImageId("123", cloudServiceName);
		imageForDeployment1 = imageForDeployment1.store();

		imageForDeployment2 = new ImageModule("test/imagefordeployment2");
		imageForDeployment2
				.setParameter(new ModuleParameter("pi2", "pi2 init value",
						"pi2 parameter desc", ParameterCategory.Input));
		imageForDeployment2.setParameter(new ModuleParameter("po2",
				"po2 init value", "po2 parameter desc",
				ParameterCategory.Output));
		imageForDeployment2.setImageId("123", cloudServiceName);
		imageForDeployment2 = imageForDeployment2.store();

		deployment = new DeploymentModule("test/deployment");

		Node node;

		node = new Node("node1", ImageModule.constructResourceUri(imageForDeployment1.getName()));
		node.setCloudService(cloudServiceName);
		deployment.setNode(node);

		node = new Node("node2", ImageModule.constructResourceUri(imageForDeployment2.getName()));
		node.setCloudService(cloudServiceName);
		deployment.setNode(node);

		deployment = deployment.store();
	}

	protected void removeDeployments() {
		deployment.remove();
		imageForDeployment1.remove();
		imageForDeployment2.remove();
	}

	public RunTestBase() {
		super();
	}

	protected static Run createAndStoreRun(Module module) throws SlipStreamException {

		return createAndStoreRun(module, USER_DEFAULT);
	}

	protected static Run createAndStoreRun(Module module, RunType type)
			throws SlipStreamException {

		return createAndStoreRun(module, USER_DEFAULT, type);
	}

	public static Run createAndStoreRun(Module module, String user)
			throws SlipStreamException {

		return createAndStoreRun(module, user, RunType.Orchestration);
	}

	protected static Run createAndStoreRun(Module module, String user, RunType type)
			throws SlipStreamException {
		return createAndStoreRun(module, user, type, States.Initializing);
	}

	protected static Run createAndStoreRun(Module module, String user, RunType type, States state)
			throws SlipStreamException {

		Run run = RunFactory.getRun(module, type, RunTestBase.user);
		run.setUser(user);
		run.setState(state);
		run = run.store();
		run = ConnectorFactory.getConnector(cloudServiceName).launch(run, RunTestBase.user);
		return run;
	}

}