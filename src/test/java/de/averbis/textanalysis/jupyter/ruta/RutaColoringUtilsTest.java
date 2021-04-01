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

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.ruta.engine.Ruta;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author entwicklerteam
 */
public class RutaColoringUtilsTest {

	@Test
	public void test() throws Exception {

		JCas jcas = JCasFactory.createJCas();
		jcas.setDocumentText("This is a test.");

		String script = "DECLARE Result;\n";
		script += "CW SW{->TruePositive};\n";
		script += "Document{->COLOR(TruePositive, \"green\", \"black\", true)};\n";
		Ruta.apply(jcas.getCas(), script);

		String actualHtml = RutaColoringUtils.createHighlightingHtml(jcas);
		String expectedHtml = FileUtils.readFileToString(
				new File("src/test/resources/test-html-output/test.html"),
				StandardCharsets.UTF_8);

		Assert.assertEquals(expectedHtml, actualHtml);

	}


	@Test
	public void testOverlapping() throws Exception {

		JCas jcas = JCasFactory.createJCas();
		jcas.setDocumentText("a b c");

		String script = "SW+{->TruePositive};\n";
		script += "SW SW{->FalsePositive} SW;\n";
		script += "COLOR(FalsePositive, \"pink\");\n";
		script += "COLOR(TruePositive, \"lightgreen\");\n";
		Ruta.apply(jcas.getCas(), script);

		String actualHtml = RutaColoringUtils.createHighlightingHtml(jcas);
		String expectedHtml = FileUtils.readFileToString(
				new File("src/test/resources/test-html-output/testOverlapping.html"),
				StandardCharsets.UTF_8);

		Assert.assertEquals(expectedHtml, actualHtml);

	}

}
