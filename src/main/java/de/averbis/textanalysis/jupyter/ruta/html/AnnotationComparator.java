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
package de.averbis.textanalysis.jupyter.ruta.html;

import java.util.Comparator;

import org.apache.uima.jcas.tcas.Annotation;

/**
 * @author entwicklerteam
 */
public class AnnotationComparator implements Comparator<Annotation> {

	@Override
	public int compare(Annotation a1, Annotation a2) {

		if (a1 == a2) {
			return 0;
		}

		if (a1 == null) {
			return 1;
		}

		if (a2 == null) {
			return -1;
		}

		int compareBegin = Integer.valueOf(a1.getBegin()).compareTo(Integer.valueOf(a2.getBegin()));
		if (compareBegin != 0) {
			return compareBegin;
		}

		int compareEnd = Integer.valueOf(a1.getEnd()).compareTo(Integer.valueOf(a2.getEnd()));
		if (compareEnd != 0) {
			return compareEnd;
		}

		int compareType = a1.getType().getName().compareTo(a2.getType().getName());
		if (compareType != 0) {
			return compareType;
		}

		int compareAddress = Integer.valueOf(a1.getAddress())
				.compareTo(Integer.valueOf(a2.getAddress()));
		if (compareAddress != 0) {
			return compareAddress;
		}

		return 0;
	}

}
