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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import io.github.spencerpark.jupyter.kernel.display.DisplayData;
import io.github.spencerpark.jupyter.kernel.display.mime.MIMEType;

/**
 *
 * @author entwicklerteam
 */
public class DisplayModeMagicsTest {

	private RutaKernel kernel = IRuta.getInstance();


	@Before
	public void resetKernel() {

		kernel.clear();
	}


	@Test
	public void setDisplayModeNone() throws Exception {

		String cell = "%documentText \"text1\"\n"
				+ "%displayMode NONE";
		DisplayData eval = kernel.eval(cell);
		Assert.assertNull(eval);
	}


	@Test(expected = IllegalArgumentException.class)
	@Ignore("Exception is catched, will not yet arrive here.")
	public void setDisplayModeInvalid() throws Exception {

		String cell = "%documentText \"text1\"\n"
				+ "%displayMode ABCXYZ";
		kernel.eval(cell);
	}


	@Test
	public void setDisplayModeCsv() throws Exception {

		String cell = "%documentText doc1 en \"a b c\"\n"
				+ "%csvConfig SW\n"
				+ "%displayMode CSV\n";
		DisplayData eval = kernel.eval(cell);
		MIMEType mimeType = MIMEType.TEXT_HTML;
		Assert.assertEquals("<html><table>\n" +
				"<tr><th>Document</th><th>SW</th></tr>\n" +
				"<tr><td>doc1</td><td>a</td></tr>\n" +
				"<tr><td>doc1</td><td>b</td></tr>\n" +
				"<tr><td>doc1</td><td>c</td></tr>\n" +
				"</table></html>", eval.getData(mimeType));
	}


	@Test
	public void setDisplayModeCsvComplexFeaturesWithHighlighting() throws Exception {

		String cell = "%documentText doc1 en \"Some words with 1 2 3 numbers.\"\n"
				+ "DECLARE Target (Annotation numberSpan, STRING start);\n"
				+ "DECLARE NumberSpan (Annotation firstNum);\n"
				+ "COLOR(SW, \"pink\");\n"
				+ "COLOR(NUM, \"lightgreen\");\n"
				+ "(n1:NUM{-PARTOF(NumberSpan)} NUM*){-> s:NumberSpan, s.firstNum=n1};\n"
				+ "Document{-> t:Target,t.numberSpan=NumberSpan,t.start=\"here\"};\n"
				+ "%csvConfig Target start numberSpan.firstNum\n"
				+ "%displayMode CSV\n";
		DisplayData eval = kernel.eval(cell);
		MIMEType mimeType = MIMEType.TEXT_HTML;
		String actual = (String) eval.getData(mimeType);
		// FileUtils.writeStringToFile(new
		// File("setDisplayModeCsvComplexFeaturesWithHighlighting.html"),
		// actual, StandardCharsets.UTF_8);

		String expected = FileUtils.readFileToString(
				new File(
						"src/test/resources/test-html-output/setDisplayModeCsvComplexFeaturesWithHighlighting.html"),
				StandardCharsets.UTF_8);

		Assert.assertEquals(expected, actual);
	}


	@Test
	public void setDynamicHtmlAllowedTypes() throws Exception {

		kernel.eval("%documentText \"This is a test\"\n"
				+ "%displayMode DYNAMIC_HTML");
		DisplayData eval1 = kernel.eval("%dynamicHtmlAllowedTypes SW SPACE");
		assertThat(kernel.getDynamicHtmlAllowedTypeNames())
				.contains("SW", "SPACE");
		String html1 = (String) eval1.getData(MIMEType.TEXT_HTML);
		Assert.assertTrue(html1.contains("SW"));
		Assert.assertFalse(html1.contains("CW"));

		DisplayData eval2 = kernel.eval("%dynamicHtmlAllowedTypes");
		assertThat(kernel.getDynamicHtmlAllowedTypeNames()).isNull();
		String html2 = (String) eval2.getData(MIMEType.TEXT_HTML);
		Assert.assertTrue(html2.contains("CW"));

	}

}
