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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.similarity.LevenshteinDistance;

/**
 *
 * @author entwicklerteam
 */
public class CompletionCandidateComparator implements Comparator<String> {

	private final String context;

	private Map<String, Integer> cache = new HashMap<>();


	public CompletionCandidateComparator(String context) {

		super();
		this.context = context;
	}


	@Override
	public int compare(String candidate1, String candidate2) {

		int sim1 = getDistance(candidate1);
		int sim2 = getDistance(candidate2);

		return Integer.compare(sim1, sim2);
	}


	private int getDistance(String candidate) {

		Integer c = cache.get(candidate);

		if (c != null) {
			return c.intValue();
		}

		LevenshteinDistance distance = new LevenshteinDistance();
		Integer d = distance.apply(candidate, context);
		int result = Integer.MAX_VALUE;
		if (d != null) {
			result = d.intValue();
		}
		cache.put(candidate, result);
		return result;
	}

}
