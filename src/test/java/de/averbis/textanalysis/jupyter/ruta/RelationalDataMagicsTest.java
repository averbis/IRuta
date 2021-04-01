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
public class RelationalDataMagicsTest {

	private RutaKernel kernel = IRuta.getInstance();


	@Before
	public void resetKernel() {

		kernel.clear();
	}


	@Test
	public void setSaveCSV() throws Exception {

		String path = "./target/test-output/setSaveCSV.csv";
		String cell = "%documentText \"text1\"\n"
				+ "%saveCSV " + path;
		kernel.eval(cell);
		Assert.assertEquals("setSaveCSV.csv", kernel.getSaveCSVFile().getName());
	}

}
