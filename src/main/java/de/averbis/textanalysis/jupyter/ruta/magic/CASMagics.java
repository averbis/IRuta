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
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SerialFormat;

import de.averbis.textanalysis.jupyter.ruta.IRuta;
import io.github.spencerpark.jupyter.kernel.magic.registry.CellMagic;
import io.github.spencerpark.jupyter.kernel.magic.registry.LineMagic;
import io.github.spencerpark.jupyter.kernel.magic.registry.MagicsArgs;

public class CASMagics {

	private static final String DOCUMENT_NAME = "documentName";
	private static final MagicsArgs DOCUMENT_TEXT_ARGS = MagicsArgs.builder()
			.optional(DOCUMENT_NAME)
			.optional(CAS.FEATURE_BASE_NAME_LANGUAGE)
			.onlyKnownFlags().onlyKnownKeywords()
			.build();


	public CASMagics() {

		super();
	}


	@CellMagic(aliases = { "documentText" })
	public String setDocumentText(List<String> args, String body) {

		setOptionalArgs(args);
		String text = body.substring(body.indexOf('\n') + 1);
		IRuta.getInstance().setDocumentText(text);
		// all consumed
		return null;
	}


	@LineMagic(aliases = { "documentText" })
	public String setDocumentText(List<String> args) {

		String text = null;
		if (args.size() == 1) {
			text = args.get(0);
		} else if (args.size() == 2) {
			IRuta.getInstance().setDocumentLanguage(args.get(0));
			text = args.get(1);
		} else if (args.size() == 3) {
			IRuta.getInstance().setDocumentName(args.get(0));
			IRuta.getInstance().setDocumentLanguage(args.get(1));
			text = args.get(2);
		}
		IRuta.getInstance().setDocumentText(text);

		return text;
	}


	@LineMagic(aliases = { DOCUMENT_NAME })
	public String setDocumentName(List<String> args) {

		String name = null;
		if (args != null && !args.isEmpty()) {
			name = args.get(0);
		}
		IRuta.getInstance().setDocumentName(name);

		return null;
	}


	private void setOptionalArgs(List<String> args) {

		Map<String, List<String>> map = DOCUMENT_TEXT_ARGS.parse(args);
		if (map == null) {
			return;
		}
		List<String> documentLanguageList = map.get(CAS.FEATURE_BASE_NAME_LANGUAGE);
		if (documentLanguageList != null && !documentLanguageList.isEmpty()) {
			IRuta.getInstance().setDocumentLanguage(documentLanguageList.get(0));
		}
		List<String> documentNameList = map.get(DOCUMENT_NAME);
		if (documentNameList != null && !documentNameList.isEmpty()) {
			IRuta.getInstance().setDocumentName(documentNameList.get(0));
		}
	}


	@LineMagic(aliases = { "resetDocument" })
	public String resetCas() {

		IRuta.getInstance().resetCas();

		return null;
	}


	@LineMagic(aliases = { "casSerialFormat" })
	public String setCasSerialFormat(List<String> args) {

		SerialFormat serialFormat = null;
		if (args != null && !args.isEmpty()) {
			serialFormat = SerialFormat.valueOf(args.get(0));
		}
		IRuta.getInstance().setCasSerialFormat(serialFormat);

		return null;
	}


	@LineMagic(aliases = { "loadCas", "loadDocument" })
	public String setLoadCasFile(List<String> args) {

		File file = null;
		if (args != null && !args.isEmpty()) {
			file = new File(args.get(0));
		}
		IRuta.getInstance().setLoadCasFile(file);

		return null;
	}


	@LineMagic()
	public String loadTypeSystem(List<String> args) throws Exception {

		File file = null;
		if (args != null && !args.isEmpty()) {
			file = new File(args.get(0));
		}
		IRuta.getInstance().addTypeSystem(file);

		return null;
	}


	@LineMagic(aliases = { "saveCas", "saveDocument" })
	public String setSaveCasFile(List<String> args) {

		File file = null;
		if (args != null && !args.isEmpty()) {
			file = new File(args.get(0));
		}
		IRuta.getInstance().setSaveCasFile(file);

		return null;
	}

}
