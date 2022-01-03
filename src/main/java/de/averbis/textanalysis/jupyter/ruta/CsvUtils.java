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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;

/**
 *
 * @author entwicklerteam
 */
public class CsvUtils {

	private CsvUtils() {

		super();
	}


	public static void writeCsvToFile(final List<String[]> data, File file, List<String> csvConfig)
			throws IOException {

		file.getParentFile().mkdirs();
		try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(),
				StandardCharsets.UTF_8)) {
			writeCsv(data, writer, csvConfig);
		}
	}


	public static String convertRelationalDataToHtml(final List<String[]> data,
			List<String> csvHeaders) {

		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<table>");
		sb.append("\n");
		if (!data.isEmpty()) {
			CsvUtils.appendHeaderHtmlRow(sb, csvHeaders);
		}

		for (String[] row : data) {
			sb.append("<tr>");
			for (String cell : row) {
				sb.append("<td>");
				sb.append(cell);
				sb.append("</td>");
			}
			sb.append("</tr>");
			sb.append("\n");
		}
		sb.append("</table>");
		sb.append("</html>");
		return sb.toString();
	}


	public static String getFeatureValue(FeatureStructure fs, String featurePath,
			boolean withHighlighting) {

		if (fs == null) {
			return "";
		}

		if (StringUtils.isBlank(featurePath)) {
			if (fs instanceof AnnotationFS) {
				AnnotationFS a = (AnnotationFS) fs;
				if (withHighlighting) {
					return RutaColoringUtils.createHighlightingHtml(a);
				}
				return a.getCoveredText();
			}
			return fs.getType().getName();
		}

		String featureName = featurePath;
		int indexOf = featurePath.indexOf('.');
		if (indexOf > 0) {
			featureName = featurePath.substring(0, indexOf);
		}

		Type type = fs.getType();
		Feature feature = type.getFeatureByBaseName(featureName);
		if (feature == null) {
			throw new IllegalArgumentException(
					"Feature '" + featureName + "' in path '" + featurePath
							+ "' is not deinfed for type '" + type.getName() + "'.");
		}
		Type range = feature.getRange();

		if (range.isPrimitive()) {
			return fs.getFeatureValueAsString(feature);
		} else if (range.isArray()) {
			throw new IllegalArgumentException(
					"Feature '" + featureName + "' is defined as an array in type '"
							+ type.getName() + "', but arrays are not yet supported.");
		}
		String remainingFeaturePath = indexOf > 0 ? featurePath.substring(indexOf + 1) : null;
		return getFeatureValue(fs.getFeatureValue(feature), remainingFeaturePath, withHighlighting);
	}


	public static List<String> resolveTypeNames(List<String> typeNames, TypeSystem typeSystem) {

		if (typeNames == null) {
			return null;
		}
		List<String> result = new ArrayList<>();
		for (String typeName : typeNames) {
			Type type = getTypeByName(typeName, typeSystem);
			if (type == null) {
				throw new IllegalArgumentException("Configured type name not known: " + typeName);
			}
			result.add(type.getName());
		}
		return result;
	}


	public static Type getTypeByName(String typeName, TypeSystem typeSystem) {

		Type type = typeSystem.getType(typeName);
		if (type == null) {
			// fallback, first short name
			Iterator<Type> typeIterator = typeSystem.getTypeIterator();
			while (typeIterator.hasNext()) {
				Type each = typeIterator.next();
				if (each.getShortName().equals(typeName)) {
					type = each;
					break;
				}
			}
		}
		return type;
	}


	public static void writeCsv(final List<String[]> data, Writer writer, List<String> csvHeaders)
			throws IOException {

		try (CSVPrinter csvPrinter = new CSVPrinter(writer,
				CSVFormat.DEFAULT.withRecordSeparator("\n"));) {
			appendHeaderRow(csvPrinter, csvHeaders);
			for (String[] row : data) {
				csvPrinter.printRecord(Arrays.asList(row));
			}

			csvPrinter.flush();
		}
	}


	private static void appendHeaderHtmlRow(StringBuilder sb, List<String> csvHeaders) {

		if (csvHeaders == null || csvHeaders.isEmpty()) {
			return;
		}

		sb.append("<tr>");
		for (String each : csvHeaders) {
			sb.append("<th>");
			sb.append(each);
			sb.append("</th>");
		}
		sb.append("</tr>");
		sb.append("\n");
	}


	private static void appendHeaderRow(CSVPrinter csvPrinter, List<String> csvHeaders)
			throws IOException {

		if (csvHeaders == null || csvHeaders.isEmpty()) {
			return;
		}
		csvPrinter.printRecord(csvHeaders);
	}
}
