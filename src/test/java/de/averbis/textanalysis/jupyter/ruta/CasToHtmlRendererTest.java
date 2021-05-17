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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import java.io.File;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.junit.Test;

import de.averbis.textanalysis.jupyter.ruta.html.CasToHtmlRenderer;

public class CasToHtmlRendererTest {

	@Test
	public void test() throws Exception {

		CAS cas = CasFactory.createCas();

		cas.setDocumentText("This is a test.");

		cas.addFsToIndexes(cas.createAnnotation(cas.getAnnotationType(), 0, 4));

		CasToHtmlRenderer htmlDocument = new CasToHtmlRenderer();
		htmlDocument.setRenderId("testRenderId");
		String actual = htmlDocument.render(true, cas.getJCas()).replace("\r\n", "\n");

		String expected = contentOf(
				new File("src/test/resources/annotations-ref.html"), UTF_8).replace("\r\n", "\n");

		assertThat(actual).isEqualTo(expected);
	}

}
