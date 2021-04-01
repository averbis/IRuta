/*
 * Copyright (c) 2021, Averbis GmbH. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.averbis.textanalysis.jupyter.ruta;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

import io.github.spencerpark.jupyter.channels.JupyterConnection;
import io.github.spencerpark.jupyter.channels.JupyterSocket;
import io.github.spencerpark.jupyter.kernel.KernelConnectionProperties;

/**
 *
 * @author entwicklerteam
 */
public class IRuta {

	private static RutaKernel rutaKernel;


	/**
	 *
	 * @return the current instance of the ruta kernel
	 */
	public static RutaKernel getInstance() {

		if (rutaKernel == null) {
			rutaKernel = new RutaKernel();
		}

		return rutaKernel;
	}


	public static void main(String[] args) throws Exception {

		if (args.length < 1) {
			throw new IllegalArgumentException("Missing connection file argument");
		}

		Path connectionFile = Paths.get(args[0]);

		if (!Files.isRegularFile(connectionFile)) {
			throw new IllegalArgumentException(
					"Connection file '" + connectionFile + "' isn't a file.");
		}

		String contents = new String(Files.readAllBytes(connectionFile));

		JupyterSocket.JUPYTER_LOGGER.setLevel(Level.WARNING);

		KernelConnectionProperties connProps = KernelConnectionProperties.parse(contents);
		JupyterConnection connection = new JupyterConnection(connProps);

		RutaKernel kernel = getInstance();
		kernel.becomeHandlerForConnection(connection);

		connection.connect();
		connection.waitUntilClose();
	}

}
