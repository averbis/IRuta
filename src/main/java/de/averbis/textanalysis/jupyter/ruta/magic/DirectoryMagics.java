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
package de.averbis.textanalysis.jupyter.ruta.magic;

import java.io.File;
import java.util.List;

import de.averbis.textanalysis.jupyter.ruta.IRuta;
import io.github.spencerpark.jupyter.kernel.magic.registry.LineMagic;

public class DirectoryMagics {

	public DirectoryMagics() {

		super();
	}


	@LineMagic(aliases = { "inputDir" })
	public String setInputDir(List<String> args) {

		File dir = null;
		if (args != null && !args.isEmpty()) {
			dir = new File(args.get(0));
		}
		IRuta.getInstance().setInputDir(dir);
		if (dir != null) {
			IRuta.getInstance().setDocumentText(null);
		}

		return null;
	}


	@LineMagic(aliases = { "outputDir" })
	public String setOutputDir(List<String> args) {

		File dir = null;
		if (args != null && !args.isEmpty()) {
			dir = new File(args.get(0));
		}
		IRuta.getInstance().setOutputDir(dir);

		return null;
	}


	@LineMagic(aliases = { "scriptDir" })
	public String setScriptPaths(List<String> args) {

		IRuta.getInstance().setScriptPaths(args.toArray(new String[0]));
		return null;
	}


	@LineMagic(aliases = { "descriptorDir", "typeSystemDir" })
	public String setDescriptorPaths(List<String> args) {

		IRuta.getInstance().setDescriptorPaths(args.toArray(new String[0]));
		return null;
	}


	@LineMagic(aliases = { "resourceDir" })
	public String setResourcePaths(List<String> args) {

		IRuta.getInstance().setResourcePaths(args.toArray(new String[0]));
		return null;
	}

}
