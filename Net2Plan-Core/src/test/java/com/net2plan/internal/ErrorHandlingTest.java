package com.net2plan.internal;

import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.junit.Test;

import javax.swing.*;

import static com.net2plan.internal.Constants.*;
import static org.assertj.core.api.Assertions.*;

public class ErrorHandlingTest
{
	/**
	 *  When we change the user interface from CLI to GUI,
	 *  the console frame must be initialized so that
	 *  we can change where the errors are being printed.
	 */
	@Test
	public void showConsoleAfterCLITest()
	{
		SystemUtils.configureEnvironment(ErrorHandling.class, UserInterface.CLI);

		assertThat(ErrorHandling.consoleDialog).isNull();
		assertThat(ErrorHandling.log).isNull();

		SystemUtils.configureEnvironment(ErrorHandling.class, UserInterface.GUI);

		assertThat(ErrorHandling.consoleDialog).isNotNull();
		assertThat(ErrorHandling.log).isNotNull();
	}
}