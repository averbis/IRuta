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
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.XMLInputSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author entwicklerteam
 */
public class RutaEngineMagicsTest {

	private RutaKernel kernel = IRuta.getInstance();


	@Before
	public void resetKernel() {

		kernel.clear();
	}


	@Test
	public void writeScriptResetsAfterExecution() throws Exception {

		String outputScriptPath = "./test-output/MyScript.ruta";
		String cell = "%writescript " + outputScriptPath;
		kernel.eval(cell);
		Assert.assertTrue("The writeScriptFile was not resetted. ",
				kernel.getWriteScriptFile() == null);
	}


	@Test
	public void setConfigurationParameters() throws Exception {

		String cell = "%configParams --strictImports=true --rulesScriptName=MyScript --varNames=var1 --varNames=var2 --varValues=value1 --varValues=value2";
		kernel.eval(cell);

		Map<String, Object> configurationParameters = kernel.getConfigurationParameters();
		Assert.assertTrue(configurationParameters.size() == 4);
		Assert.assertEquals(configurationParameters.get("strictImports"), Boolean.TRUE);
		Assert.assertEquals(configurationParameters.get("rulesScriptName"), "MyScript");
		Assert.assertArrayEquals((Object[]) configurationParameters.get("varNames"),
				new String[] { "var1", "var2" });
		Assert.assertArrayEquals((Object[]) configurationParameters.get("varValues"),
				new String[] { "value1", "value2" });

	}


	@Test
	@Ignore("ReindexUpdateMode after ruta-core 3.0.1")
	public void setConfigurationParametersWithDataBinders() throws Exception {

		String cell = "%params --strictImports=true --rulesScriptName=MyScript --reindexUpdateMode=NONE";
		kernel.eval(cell);

		// ReindexUpdateMode after ruta-core 3.0.1
		assertThat(kernel.getConfigurationParameters())
				.containsOnlyKeys("strictImports", "rulesScriptName", "reindexUpdateMode")
				.extracting("strictImports", "rulesScriptName", "reindexUpdateMode")
				.contains(Boolean.TRUE, "MyScript", "NONE");
	}


	@Test
	public void saveTypeSystem() throws Exception {

		String path = "./target/test-output/storeTypeSystem.xml";
		kernel.eval("DECLARE Test1;");
		kernel.eval("DECLARE Test2;");
		kernel.eval("%saveTypeSystem " + path + "\nDECLARE Test3;");

		File actual = new File(path);
		assertThat(actual).exists();

		TypeSystemDescription typeSystemDescription = UIMAFramework.getXMLParser()
				.parseTypeSystemDescription(new XMLInputSource(actual));

		Assert.assertNotNull(typeSystemDescription.getType("Test1"));
		Assert.assertNotNull(typeSystemDescription.getType("Test2"));
		Assert.assertNotNull(typeSystemDescription.getType("Test3"));

		Assert.assertTrue("The storeTypeSystem file was not resetted. ",
				kernel.getSaveTypeSystemFile() == null);
	}

}
