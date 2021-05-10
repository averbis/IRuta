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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.ruta.RutaBasicUtils;
import org.apache.uima.ruta.type.EvalAnnotation;
import org.apache.uima.ruta.type.FalseNegative;
import org.apache.uima.ruta.type.FalsePositive;
import org.apache.uima.ruta.type.TruePositive;
import org.apache.uima.util.CasCopier;

import de.averbis.textanalysis.jupyter.ruta.EvaluationResult.Outcome;

/**
 *
 * @author entwicklerteam
 */
public class EvaluationUtils {

	public static final String GOLD = "kernelGold";


	private EvaluationUtils() {

		super();
	}


	public static void createGoldView(JCas jcas, List<String> evaluationTypeNames) {

		CAS cas = jcas.getCas();
		CAS goldCas = cas.createView(GOLD);
		goldCas.setDocumentText(cas.getDocumentText());
		CasCopier cc = new CasCopier(cas, goldCas);
		TypeSystem typeSystem = cas.getTypeSystem();

		for (String typeName : evaluationTypeNames) {

			Type type = CsvUtils.getTypeByName(typeName, typeSystem);
			if (type == null) {
				throw new IllegalArgumentException(
						"Evaluation type '" + typeName + "' not defined in type system.");
			}
			Collection<AnnotationFS> toRemove = new ArrayList<>();
			Collection<AnnotationFS> select = CasUtil.select(cas, type);
			for (AnnotationFS each : select) {

				AnnotationFS goldCopy = cc.copyFs(each);
				goldCas.addFsToIndexes(goldCopy);
				toRemove.add(each);
			}

			toRemove.forEach(a -> cas.removeFsFromIndexes(a));
		}
	}


	public static void evaluate(List<String> evaluationTypeNames, JCas jcas) {

		TypeSystem typeSystem = jcas.getTypeSystem();

		for (String typeName : evaluationTypeNames) {
			Type type = CsvUtils.getTypeByName(typeName, typeSystem);
			if (type == null) {
				throw new IllegalArgumentException(
						"Evaluation type '" + typeName + "' not defined in type system.");
			}

			evaluate(typeName, type, jcas);
		}

	}


	public static Map<String, EvaluationResult> createEvaluationResult(JCas jcas) {

		Map<String, EvaluationResult> result = new TreeMap<>();

		addToGroupedMap(result, JCasUtil.select(jcas, TruePositive.class), Outcome.TP);
		addToGroupedMap(result, JCasUtil.select(jcas, FalsePositive.class), Outcome.FP);
		addToGroupedMap(result, JCasUtil.select(jcas, FalseNegative.class), Outcome.FN);

		return result;
	}


	private static void addToGroupedMap(Map<String, EvaluationResult> result,
			Collection<? extends EvalAnnotation> annotations, Outcome outcome) {

		for (EvalAnnotation evalAnnotation : annotations) {
			String group = evalAnnotation.getGroup();
			EvaluationResult evaluationResult = result.get(group);
			if (evaluationResult == null) {
				evaluationResult = new EvaluationResult(group);
				result.put(group, evaluationResult);
			}
			evaluationResult.increment(outcome);
		}
	}


	public static void evaluate(String name, Type type, JCas jcas) {

		CAS cas = jcas.getCas();
		CAS goldCas = cas.getView(GOLD);
		if (goldCas == null) {
			throw new IllegalArgumentException("No gold view available for evaluation.");
		}
		Type goldType = goldCas.getTypeSystem().getType(type.getName());

		Collection<AnnotationFS> annotations = CasUtil.select(cas, type);
		Collection<AnnotationFS> goldAnnotations = CasUtil.select(goldCas, goldType);

		Collection<AnnotationFS> tps = new ArrayList<>();
		Collection<AnnotationFS> fps = new ArrayList<>();
		Collection<AnnotationFS> fns = new ArrayList<>();

		for (AnnotationFS goldAnnotation : goldAnnotations) {
			boolean found = false;
			for (AnnotationFS annotation : annotations) {
				if (equals(goldAnnotation, annotation)) {
					tps.add(annotation);
					found = true;
					break;
				}
			}
			if (!found) {
				fns.add(goldAnnotation);
			}
		}

		for (AnnotationFS annotation : annotations) {
			boolean found = false;
			for (AnnotationFS goldAnnotation : goldAnnotations) {
				if (equals(goldAnnotation, annotation)) {
					found = true;
					break;
				}
			}
			if (!found) {
				fps.add(annotation);
			}
		}

		createEvaluationAnnotations(tps, TruePositive._TypeName, name, cas);
		createEvaluationAnnotations(fps, FalsePositive._TypeName, name, cas);
		createEvaluationAnnotations(fns, FalseNegative._TypeName, name, cas);
	}


	private static boolean equals(AnnotationFS goldAnnotation, AnnotationFS annotation) {

		boolean sameType = goldAnnotation.getType().getName()
				.equals(annotation.getType().getName());
		boolean sameBegin = goldAnnotation.getBegin() == annotation.getBegin();
		boolean sameEnd = goldAnnotation.getEnd() == annotation.getEnd();
		return sameType && sameBegin && sameEnd;
	}


	private static void createEvaluationAnnotations(Collection<AnnotationFS> annotations,
			String typeName, String group, CAS cas) {

		Type outcomeType = cas.getTypeSystem().getType(typeName);
		Feature originalFeature = outcomeType
				.getFeatureByBaseName(EvalAnnotation._FeatName_original);
		Feature groupFeature = outcomeType
				.getFeatureByBaseName(EvalAnnotation._FeatName_group);

		for (AnnotationFS each : annotations) {
			AnnotationFS outcomeAnnotation = cas.createAnnotation(outcomeType, each.getBegin(),
					each.getEnd());
			outcomeAnnotation.setFeatureValue(originalFeature, each);
			outcomeAnnotation.setStringValue(groupFeature, group);
			RutaBasicUtils.addAnnotation(outcomeAnnotation);
			cas.addFsToIndexes(outcomeAnnotation);
		}

	}


	public static String createHtmlTable(
			final Map<String, Map<String, EvaluationResult>> evaluationData) {

		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<table>");
		// appendHeaderRow(sb);

		// aggregate overall and label micro
		Map<String, EvaluationResult> labelMicroAverage = new TreeMap<>();
		EvaluationResult overallResult = new EvaluationResult("All");
		for (Entry<String, Map<String, EvaluationResult>> docEntry : evaluationData.entrySet()) {
			Map<String, EvaluationResult> typeMap = docEntry.getValue();
			for (Entry<String, EvaluationResult> typeEntry : typeMap.entrySet()) {
				String typeName = typeEntry.getKey();
				EvaluationResult typeResult = typeEntry.getValue();
				EvaluationResult typeMicro = labelMicroAverage.get(typeName);
				if (typeMicro == null) {
					typeMicro = new EvaluationResult(typeName);
					labelMicroAverage.put(typeName, typeMicro);
				}
				typeMicro.add(typeResult);
				overallResult.add(typeResult);
			}
		}
		overallResult.appendHtmlRow("Overall", sb);
		appendHeaderRow(sb);
		for (Entry<String, EvaluationResult> entry : labelMicroAverage.entrySet()) {
			entry.getValue().appendHtmlRow("Overall", sb);
		}
		appendHeaderRow(sb);
		// new details for each document
		for (Entry<String, Map<String, EvaluationResult>> docEntry : evaluationData.entrySet()) {

			String docName = docEntry.getKey();
			if (evaluationData.size() == 1) {
				docName = null;
			}
			Map<String, EvaluationResult> typeMap = docEntry.getValue();

			EvaluationResult docResult = new EvaluationResult("All");
			for (Entry<String, EvaluationResult> typeEntry : typeMap.entrySet()) {
				docResult.add(typeEntry.getValue());
			}
			docResult.appendHtmlRow(docName, sb);
			for (Entry<String, EvaluationResult> typeEntry : typeMap.entrySet()) {
				typeEntry.getValue().appendHtmlRow("", sb);
			}

		}

		sb.append("</table>");
		sb.append("</html>");
		return sb.toString();
	}


	private static void appendHeaderRow(StringBuilder sb) {

		sb.append("<tr>");
		sb.append("<th>Document</th>");
		sb.append("<th>Type</th>");
		sb.append("<th>F1</th>");
		sb.append("<th>Precision</th>");
		sb.append("<th>Recall</th>");
		sb.append("<th>TP</th>");
		sb.append("<th>FP</th>");
		sb.append("<th>FN</th>");
		sb.append("</tr>");
		sb.append("\n");
	}

}
