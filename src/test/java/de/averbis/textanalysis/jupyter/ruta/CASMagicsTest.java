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
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.github.spencerpark.jupyter.kernel.display.DisplayData;
import io.github.spencerpark.jupyter.kernel.display.mime.MIMEType;

/**
 *
 * @author entwicklerteam
 */
public class CASMagicsTest {

	private RutaKernel kernel = IRuta.getInstance();


	@Before
	public void resetKernel() {

		kernel.clear();
	}


	@Test
	public void resetCas() throws Exception {

		String cell = "%documentText \"text1\"\n"
				+ "%resetCas\n";
		DisplayData eval = kernel.eval(cell);
		Assert.assertNull(eval.getData(MIMEType.TEXT_HTML));
	}


	@Test
	public void documentCellMagicFull() throws Exception {

		String name = "text.txt";
		String language = "de";
		String text = "This is a test.";
		String cell = "%%documentText " + name + " " + language + "\n" + text;

		kernel.eval(cell);

		Assert.assertEquals(name, kernel.getDocumentName());
		Assert.assertEquals(language, kernel.getDocumentLanguage());
		Assert.assertEquals(text, kernel.getDocumentText());
	}


	@Test
	public void documentCellMagicNoLangugage() throws Exception {

		String name = "text.txt";
		String text = "This is a test.";
		String cell = "%%documentText " + name + "\n" + text;

		kernel.eval(cell);

		Assert.assertEquals(name, kernel.getDocumentName());
		Assert.assertNull(kernel.getDocumentLanguage());
		Assert.assertEquals(text, kernel.getDocumentText());
	}


	@Test
	public void documentCellMagicTextOnly() throws Exception {

		String text = "This is a test.";
		String cell = "%%documentText\n" + text;

		kernel.eval(cell);

		Assert.assertNull(kernel.getDocumentName());
		Assert.assertNull(kernel.getDocumentLanguage());
		Assert.assertEquals(text, kernel.getDocumentText());
	}


	@Test
	public void documentName() throws Exception {

		String name = "text.txt";
		String cell = "%documentName " + name;

		kernel.eval(cell);

		Assert.assertEquals(name, kernel.getDocumentName());
	}


	@Test
	public void setCasSerialFormat() throws Exception {

		SerialFormat format = SerialFormat.COMPRESSED_FILTERED_TSI;
		String cell = "%casSerialFormat " + format + "\n"
				+ "%resetCas\n";
		kernel.eval(cell);
		Assert.assertEquals(format, kernel.getCasSerialFormat());
	}


	@Test
	public void addTypeSystem() throws Exception {

		String tsPath = "target/test-output/test-ts.xml";
		TypeSystemDescription tsd = new TypeSystemDescription_impl();
		String external = "External";
		tsd.addType(external, "", CAS.TYPE_NAME_ANNOTATION);
		File tsFile = new File(tsPath);
		tsFile.getParentFile().mkdirs();

		try (FileOutputStream fos = new FileOutputStream(tsFile)) {
			tsd.toXML(fos);
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		}

		kernel.eval("%loadTypeSystem " + tsPath);
		TypeSystemDescription typeSystemDescription = kernel.getTypeSystemDescription();
		Assert.assertNotNull(typeSystemDescription.getType(external));
	}
}
