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

import java.util.List;

import de.averbis.textanalysis.jupyter.ruta.DisplayMode;
import de.averbis.textanalysis.jupyter.ruta.IRuta;
import io.github.spencerpark.jupyter.kernel.magic.registry.LineMagic;

public class DisplayModeMagics {

	public DisplayModeMagics() {

		super();
	}


	@LineMagic(aliases = { "displayMode" })
	public String setDisplayMode(List<String> args) {

		if (args == null || args.isEmpty()) {
			throw new IllegalArgumentException(
					"Magic %setDisplayMode requires a valid mode as argument.");
		}
		DisplayMode displayMode = DisplayMode.valueOf(args.get(0));
		IRuta.getInstance().setDisplayMode(displayMode);

		return null;
	}


	@LineMagic(aliases = { "dynamicHtmlAllowedTypes" })
	public String setDynamicHtmlAllowedTypes(List<String> args) {

		if (args.isEmpty()) {
			IRuta.getInstance().setDynamicHtmlAllowedTypeNames(null);
		} else {
			IRuta.getInstance().setDynamicHtmlAllowedTypeNames(args);
		}

		return null;
	}

}
