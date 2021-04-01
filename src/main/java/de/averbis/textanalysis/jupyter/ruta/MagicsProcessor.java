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

import io.github.spencerpark.jupyter.kernel.magic.CellMagicArgs;
import io.github.spencerpark.jupyter.kernel.magic.CellMagicParseContext;
import io.github.spencerpark.jupyter.kernel.magic.LineMagicArgs;
import io.github.spencerpark.jupyter.kernel.magic.LineMagicParseContext;
import io.github.spencerpark.jupyter.kernel.magic.MagicParser;
import io.github.spencerpark.jupyter.kernel.magic.registry.Magics;

/**
 *
 * @author entwicklerteam
 */
public class MagicsProcessor {

	public static final String LINE_MAGIC_PREFIX = "%";
	public static final String CELL_MAGIC_PREFIX = "%%";

	private MagicParser magicParser;
	private Magics magics;


	public MagicsProcessor(Magics kernelMagics) {

		super();

		magics = kernelMagics;
		magicParser = new MagicParser("^" + LINE_MAGIC_PREFIX, CELL_MAGIC_PREFIX);
	}


	public String process(String script) throws Exception {

		CellMagicParseContext cellContext = magicParser.parseCellMagic(script);
		if (cellContext != null) {
			CellMagicArgs magicCall = cellContext.getMagicCall();
			return magics.applyCellMagic(magicCall.getName(), magicCall.getArgs(), script);
		}
		return magicParser.transformLineMagics(script, lineContext -> applyLineMagic(lineContext));
	}


	private String applyLineMagic(LineMagicParseContext lineMagicParseContext)
			throws MagicsProcessorException {

		LineMagicArgs magicCall = lineMagicParseContext.getMagicCall();
		try {
			magics.applyLineMagic(magicCall.getName(), magicCall.getArgs());
		} catch (Exception e) {
			throw new MagicsProcessorException("Unable to apply [" + magicCall.getName() + "]", e);
		}

		// How should we change the line in order to avoid RUTA parse problems?
		// we just comment it for now
		return "//" + lineMagicParseContext.getEntireLine();
	}
}
