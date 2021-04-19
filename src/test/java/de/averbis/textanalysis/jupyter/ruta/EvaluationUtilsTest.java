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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.ruta.engine.Ruta;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author entwicklerteam
 */
public class EvaluationUtilsTest {

	@Test
	public void test() throws Exception {

		JCas jcas = JCasFactory.createJCas();
		jcas.setDocumentText("This is A test.");

		Ruta.apply(jcas.getCas(), "(CW SW){->Line};");

		List<String> types = Arrays.asList("Line");
		EvaluationUtils.createGoldView(jcas, types);

		Ruta.apply(jcas.getCas(), "(CW SW){->Line} PERIOD; CW{->Line};");

		Map<String, EvaluationResult> map = EvaluationUtils.evaluate("test", types, jcas);

		Assert.assertEquals(1, map.size());

		EvaluationResult result = map.values().iterator().next();

		Assert.assertEquals(1, result.getTP());
		Assert.assertEquals(2, result.getFP());
		Assert.assertEquals(1, result.getFN());
		Assert.assertEquals(0.333, result.getPrecision(), 0.001);
		Assert.assertEquals(0.5, result.getRecall(), 0.001);
		Assert.assertEquals(0.4, result.getF1Score(), 0.001);

	}

}
