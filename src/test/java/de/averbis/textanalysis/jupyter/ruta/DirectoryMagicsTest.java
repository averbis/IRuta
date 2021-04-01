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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author entwicklerteam
 */
public class DirectoryMagicsTest {

	private RutaKernel kernel = IRuta.getInstance();


	@Before
	public void resetKernel() {

		kernel.clear();
	}


	@Test
	public void setInputDir() throws Exception {

		String cell = "%inputDir test-input";
		kernel.eval(cell);
		Assert.assertEquals("test-input", kernel.getInputDir().getPath());
	}


	@Test
	public void setInputDirReset() throws Exception {

		kernel.eval("%inputDir test-input");
		kernel.eval("%inputDir");
		Assert.assertEquals(null, kernel.getInputDir());
	}


	@Test
	public void testThatSetInputDirResetsDocumentText() throws Exception {

		String commandThatSetsTheDocumentText = "%documentText \"Example Text\"";
		kernel.eval(commandThatSetsTheDocumentText);
		Assert.assertEquals("Example Text", kernel.getDocumentText());

		String commandThatChangesTheInputDir = "%inputDir src/test/resources/test-input";
		kernel.eval(commandThatChangesTheInputDir);

		Assert.assertNull(kernel.getDocumentText());
	}


	@Test
	public void setOutputDir() throws Exception {

		String cell = "%outputDir test-output";
		kernel.eval(cell);
		Assert.assertEquals("test-output", kernel.getOutputDir().getPath());
	}


	@Test
	public void setOutputDirReset() throws Exception {

		kernel.eval("%outputDir test-output");
		kernel.eval("%outputDir");
		Assert.assertEquals(null, kernel.getOutputDir());
	}


	@Test
	public void setScriptPaths() throws Exception {

		String cell = "%setScriptPaths test-script";
		kernel.eval(cell);
		Assert.assertEquals("test-script", kernel.getScriptPaths()[0]);
	}


	@Test
	public void setDescriptorPaths() throws Exception {

		String cell = "%setDescriptorPaths test-descriptor";
		kernel.eval(cell);
		Assert.assertEquals("test-descriptor", kernel.getDescriptorPaths()[0]);
	}


	@Test
	public void setResourcePaths() throws Exception {

		String cell = "%setResourcePaths test-resources";
		kernel.eval(cell);
		Assert.assertEquals("test-resources", kernel.getResourcePaths()[0]);
	}
}
