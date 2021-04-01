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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;

/**
 *
 * @author entwicklerteam
 */
public class RutaDocumentationUtils {

	private RutaDocumentationUtils() {

		// nothing here
	}


	public static Map<String, String> loadDocumentationSafely(String path) {

		try {
			return loadDocumentation(path);
		} catch (ParserException | IOException e) {
			// TODO what to do?
			e.printStackTrace();
		}
		return Collections.emptyMap();
	}


	public static Map<String, String> loadDocumentation(String path)
			throws IOException, ParserException {

		URL resource = RutaDocumentationUtils.class.getClassLoader().getResource(path);

		String document = "";
		try (InputStream is = resource.openStream()) {
			document = IOUtils.toString(is, StandardCharsets.UTF_8);
		}

		Parser parser = new Parser(document);
		NodeList list = parser.parse(null);
		HtmlDocumentationVisitor visitor = new HtmlDocumentationVisitor(document);
		list.visitAllNodesWith(visitor);
		return visitor.getMap();
	}


	public static class HtmlDocumentationVisitor extends NodeVisitor {

		private Map<String, String> map;

		private int divDepth = 0;

		private int elementStart = 0;

		private String document;


		public HtmlDocumentationVisitor(String document) {

			super();
			this.document = document;
			map = new TreeMap<>();
		}


		@Override
		public void visitTag(Tag tag) {

			String name = tag.getTagName().toLowerCase();
			if ("div".equals(name)) {
				divDepth++;
				if (divDepth == 1) {
					elementStart = tag.getStartPosition();
				}
			}

		}


		@Override
		public void visitEndTag(Tag tag) {

			String name = tag.getTagName().toLowerCase();
			if ("div".equals(name)) {
				if (divDepth == 1) {
					String section = document.substring(elementStart, tag.getEndPosition());
					processSection(section);
				}
				divDepth--;
			}

		}


		private void processSection(String text) {

			String section = text;
			Pattern pattern = Pattern.compile("title=\"\\d+\\.\\d+\\.\\d+\\.&nbsp;(\\p{Upper}+)\"");
			Matcher matcher = pattern.matcher(section);
			boolean found = matcher.find();
			if (found) {
				String group = matcher.group(1);
				section = section.trim();
				section = section.replaceAll("</?a.*>", "");
				section = section.replaceAll("\\d+\\.\\d+\\.\\d+.\\d+\\.&nbsp;", "");
				section = section.replaceAll("\\d+\\.\\d+\\.\\d+.&nbsp;", "");
				map.put(group, section);
			}
		}


		public Map<String, String> getMap() {

			return map;
		}
	}
}
