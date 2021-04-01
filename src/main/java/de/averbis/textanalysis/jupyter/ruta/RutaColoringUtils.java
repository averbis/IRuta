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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.ruta.type.RutaBasic;
import org.apache.uima.ruta.type.RutaColoring;
import org.apache.uima.tools.stylemap.StyleMapEntry;

/**
 *
 * @author entwicklerteam
 */
public class RutaColoringUtils {

	private static Map<String, String> colorNameMap;

	static {
		colorNameMap = new HashMap<>();
		colorNameMap.put("#000000", "black");
		colorNameMap.put("#c0c0c0", "silver");
		colorNameMap.put("#808080", "gray");
		colorNameMap.put("#ffffff", "white");
		colorNameMap.put("#800000", "maroon");
		colorNameMap.put("#ff0000", "red");
		colorNameMap.put("#800080", "purple");
		colorNameMap.put("#ff00ff", "fuchsia");
		colorNameMap.put("#008000", "green");
		colorNameMap.put("#00ff00", "lime");
		colorNameMap.put("#808000", "olive");
		colorNameMap.put("#ffff00", "yellow");
		colorNameMap.put("#000080", "navy");
		colorNameMap.put("#0000ff", "blue");
		colorNameMap.put("#00ffff", "aqua");
		colorNameMap.put("#000000", "black");
		colorNameMap.put("#add8e6", "lightblue");
		colorNameMap.put("#90ee90", "lightgreen");
		colorNameMap.put("#ffa500", "orange");
		colorNameMap.put("#ffc0cb", "pink");
		colorNameMap.put("#fa8072", "salmon");
		colorNameMap.put("#00ffff", "cyan");
		colorNameMap.put("#ee82ee", "violet");
		colorNameMap.put("#d2b48c", "tan");
		colorNameMap.put("#a52a2a", "brown");
		colorNameMap.put("#ffffff", "white");
		colorNameMap.put("#9370db", "mediumpurple");
		// in other order for lookup
		colorNameMap.put("black", "#000000");
		colorNameMap.put("silver", "#c0c0c0");
		colorNameMap.put("gray", "#808080");
		colorNameMap.put("white", "#ffffff");
		colorNameMap.put("maroon", "#800000");
		colorNameMap.put("red", "#ff0000");
		colorNameMap.put("purple", "#800080");
		colorNameMap.put("fuchsia", "#ff00ff");
		colorNameMap.put("green", "#008000");
		colorNameMap.put("lime", "#00ff00");
		colorNameMap.put("olive", "#808000");
		colorNameMap.put("yellow", "#ffff00");
		colorNameMap.put("navy", "#000080");
		colorNameMap.put("blue", "#0000ff");
		colorNameMap.put("aqua", "#00ffff");
		colorNameMap.put("black", "#000000");
		colorNameMap.put("lightblue", "#add8e6");
		colorNameMap.put("lightgreen", "#90ee90");
		colorNameMap.put("orange", "#ffa500");
		colorNameMap.put("pink", "#ffc0cb");
		colorNameMap.put("salmon", "#fa8072");
		colorNameMap.put("cyan", "#00ffff");
		colorNameMap.put("violet", "#ee82ee");
		colorNameMap.put("tan", "#d2b48c");
		colorNameMap.put("brown", "#a52a2a");
		colorNameMap.put("white", "#ffffff");
		colorNameMap.put("mediumpurple", "#9370db");
	}


	private RutaColoringUtils() {

		// nothing here
	}


	public static String createHighlightingHtml(JCas jcas) {

		return createHighlightingHtml((AnnotationFS) jcas.getDocumentAnnotationFs());
	}


	public static String createHighlightingHtml(AnnotationFS annotation) {

		if (annotation == null) {
			return null;
		}
		JCas jcas = annotation.getJCas();
		if (JCasUtil.select(jcas, RutaColoring.class).isEmpty()) {
			// modifications are only supported right now, if there is coloring
			return postprocess(annotation.getCoveredText());
		}

		LinkedHashMap<Type, StyleMapEntry> styleMap = createStyleList(jcas);

		String startTag = "<span style='background:&bgcolor'>";
		String endTag = "</span>";
		List<Type> priorizedColoredTypes = getPriorizedColoredTypes(styleMap);

		StringBuilder html = new StringBuilder();
		Collection<RutaBasic> selectCovered = Collections.emptyList();
		if (annotation.getCoveredText().equals(jcas.getDocumentText())) {
			selectCovered = JCasUtil.select(jcas, RutaBasic.class);
		} else {
			selectCovered = JCasUtil.selectCovered(RutaBasic.class, annotation);
		}
		for (RutaBasic each : selectCovered) {

			String replace = each.getReplacement() == null ? each.getCoveredText()
					: each.getReplacement();
			Type type = getColorType(each, priorizedColoredTypes);
			if (type != null && !"".equals(replace)) {
				StyleMapEntry entry = styleMap.get(type);
				String backgroundColor = "#"
						+ Integer.toHexString(entry.getBackground().getRGB()).substring(2);
				String newStartTag = startTag.replaceAll("&bgcolor", backgroundColor);
				html.append(newStartTag);
				html.append(replace);
				html.append(endTag);
			} else {
				html.append(replace);
			}
		}

		return postprocess(html.toString());
	}


	private static String postprocess(String html) {

		String result = html;
		result = result.replace("\r\n", "<br />").replace("\n", "<br />").replace("\r", "<br />");
		result = result.trim();
		return result;
	}


	public static LinkedHashMap<Type, StyleMapEntry> createStyleList(JCas jcas) {

		LinkedHashMap<Type, StyleMapEntry> result = new LinkedHashMap<>();
		for (RutaColoring each : JCasUtil.select(jcas, RutaColoring.class)) {

			String targetType = each.getTargetType();
			if (targetType == null) {
				continue;
			}
			Type type = jcas.getTypeSystem().getType(targetType);
			if (type == null) {
				continue;
			}
			StyleMapEntry entry = new StyleMapEntry();
			entry.setAnnotationTypeName(targetType);
			String fgColor = each.getFgColor();
			entry.setForeground(parseColorBackground(fgColor));
			String bgColor = each.getBgColor();
			entry.setBackground(parseColorBackground(bgColor));
			entry.setChecked(each.getSelected());

			result.put(type, entry);
		}

		return result;
	}


	private static Color parseColorBackground(String color) {

		if (color == null) {
			return Color.BLACK;
		}
		if (color.startsWith("#")) {
			return Color.decode(color);
		}
		String string = colorNameMap.get(color);
		if (string != null) {
			return Color.decode(string);
		}
		return Color.LIGHT_GRAY;
	}


	private static List<Type> getPriorizedColoredTypes(Map<Type, StyleMapEntry> styleMap) {

		List<Type> result = new ArrayList<>(styleMap.keySet());
		return result;
	}


	private static Type getColorType(RutaBasic basic, List<Type> coloredTypes) {

		if (coloredTypes.isEmpty()) {
			return null;
		}
		List<Type> colorTypes = new ArrayList<>();
		for (Type each : coloredTypes) {
			if (basic.isPartOf(each)) {
				colorTypes.add(each);
			}
		}
		Type best = null;
		if (!colorTypes.isEmpty()) {
			best = colorTypes.get(0);
		}
		return best;
	}
}
