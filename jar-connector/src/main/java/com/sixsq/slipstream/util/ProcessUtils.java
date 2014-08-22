package com.sixsq.slipstream.util;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.logging.Logger;

import com.sixsq.slipstream.exceptions.ProcessException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamInternalException;

public class ProcessUtils {


	public static String execGetOutput(String[] command)
			throws IOException, SlipStreamClientException {
		return execGetOutputAsArray(command, true)[0];
	}

	public static String execGetOutput(String[] command, Map<String, String> environment)
			throws IOException, SlipStreamClientException {
		return execGetOutputAsArray(command, true, environment)[0];
	}

	public static String execGetOutput(String[] command, boolean stderrToStdout)
			throws IOException, SlipStreamClientException {
		return execGetOutputAsArray(command, stderrToStdout)[0];
	}

	public static String execGetOutput(String[] command, boolean stderrToStdout, Map<String, String> environment)
			throws IOException, SlipStreamClientException {
		return execGetOutputAsArray(command, stderrToStdout, environment)[0];
	}

	public static String[] execGetOutputAsArray(String[] command, boolean stderrToStdout)
			throws IOException, SlipStreamClientException {
		return execGetOutputAsArray(command, stderrToStdout, null);
	}

	public static String[] execGetOutputAsArray(String[] command, boolean stderrToStdout,
			Map<String, String> environment) throws IOException, SlipStreamClientException {

		StringBuilder commandMessage = new StringBuilder();
		for (String part : command) {
            commandMessage.append(part).append(" ");
		}
		getLogger().info("Calling: " + commandMessage.toString());

		ProcessBuilder pb = new ProcessBuilder(command);
		pb.redirectErrorStream(stderrToStdout);

		if (environment != null) {
			pb.environment().putAll(environment);
		}

		Process p = pb.start();

		StringBuffer outputBuf = new StringBuffer();
		BufferedReader stdOutErr = new BufferedReader(new InputStreamReader(
				p.getInputStream()));

		StringBuffer errBuf = new StringBuffer();
		BufferedReader stdErrReader = new BufferedReader(new InputStreamReader(
				p.getErrorStream()));

		String line;
		while ((line = stdOutErr.readLine()) != null) {
			outputBuf.append(line);
			outputBuf.append("\n");
			getLogger().info(line);
		}

		while ((line = stdErrReader.readLine()) != null) {
			errBuf.append(line);
			errBuf.append("\n");
			getLogger().info(line);
		}

		// Check for failure
		try {
			if (p.waitFor() != 0) {
				String error = "Error executing: " + commandMessage
						+ ". With exit code = " + p.exitValue()
						+ " and stdout: " + outputBuf
						+ " and stderr: " + errBuf;
				getLogger().severe(error);
				String message = (stderrToStdout)? outputBuf.toString() : errBuf.toString();
				throw (new ProcessException(message, outputBuf.toString()));
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw (new SlipStreamInternalException(e));
		} finally {
			stdOutErr.close();
			stdErrReader.close();
		}

		return new String[]{outputBuf.toString(), errBuf.toString()};
	}

	protected static Logger getLogger() {
		return Logger.getLogger("SlipStream");
	}
}
