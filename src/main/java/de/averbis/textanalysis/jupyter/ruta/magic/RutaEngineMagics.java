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
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.fit.factory.ConfigurationParameterFactory;
import org.apache.uima.fit.internal.ReflectionUtil;
import org.apache.uima.ruta.engine.RutaEngine;

import de.averbis.textanalysis.jupyter.ruta.IRuta;
import de.averbis.textanalysis.jupyter.ruta.UimaFitUtils;
import io.github.spencerpark.jupyter.kernel.magic.registry.LineMagic;
import io.github.spencerpark.jupyter.kernel.magic.registry.MagicsArgs;
import io.github.spencerpark.jupyter.kernel.magic.registry.MagicsArgs.KeywordSpec;
import io.github.spencerpark.jupyter.kernel.magic.registry.MagicsArgs.MagicsArgsBuilder;

public class RutaEngineMagics {

	private static final MagicsArgs PARAM_ARGS;

	static {
		MagicsArgsBuilder paramArgsBuilder = MagicsArgs.builder();
		for (Field field : ReflectionUtil.getFields(RutaEngine.class)) {
			if (ConfigurationParameterFactory.isConfigurationParameterField(field)) {
				org.apache.uima.fit.descriptor.ConfigurationParameter annotation = ReflectionUtil
						.getAnnotation(field,
								org.apache.uima.fit.descriptor.ConfigurationParameter.class);
				String name = annotation.name();
				paramArgsBuilder.keyword(name, KeywordSpec.COLLECT);
			}
		}
		PARAM_ARGS = paramArgsBuilder.onlyKnownFlags().onlyKnownKeywords().build();
	}


	public RutaEngineMagics() {

		super();

	}


	@LineMagic(aliases = { "writescript" })
	public String setWriteScriptFile(List<String> args) {

		String path = "./Script.ruta";
		if (args != null && !args.isEmpty()) {
			path = args.get(0);
		}
		IRuta.getInstance().setWriteScriptFile(new File(path));

		return null;
	}


	@LineMagic(aliases = { "configParams" })
	public String setConfigurationParameters(List<String> args) throws Exception {

		Map<String, Object> params = new HashMap<>();
		Map<String, List<String>> map = PARAM_ARGS.parse(args);
		for (Entry<String, List<String>> entry : map.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				params.put(entry.getKey(), UimaFitUtils.getParameterValue(entry, RutaEngine.class));
			}
		}
		IRuta.getInstance().setConfigurationParameters(params);

		return null;
	}


	@LineMagic(aliases = { "saveTypeSystem" })
	public String setSaveTypeSystemFile(List<String> args) {

		File file = null;
		if (args != null && !args.isEmpty() && !StringUtils.isBlank(args.get(0))) {
			file = new File(args.get(0));
		}
		IRuta.getInstance().setSaveTypeSystemFile(file);
		return null;
	}

}
