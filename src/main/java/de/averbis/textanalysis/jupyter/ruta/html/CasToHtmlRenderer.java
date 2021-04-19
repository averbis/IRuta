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

import static de.averbis.textanalysis.jupyter.ruta.html.CasToHtmlRenderer.HtmlConstants.ATTR_CLASS;
import static de.averbis.textanalysis.jupyter.ruta.html.CasToHtmlRenderer.HtmlConstants.ATTR_DATA_ID;
import static de.averbis.textanalysis.jupyter.ruta.html.CasToHtmlRenderer.HtmlConstants.CLASS_ANNOTATIONS;
import static de.averbis.textanalysis.jupyter.ruta.html.CasToHtmlRenderer.HtmlConstants.CLASS_TEXT;
import static de.averbis.textanalysis.jupyter.ruta.html.CasToHtmlRenderer.HtmlConstants.ELEM_SPAN;
import static java.util.Collections.sort;
import static java.util.Comparator.comparing;
import static org.apache.uima.fit.util.JCasUtil.select;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSException;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

/**
 * TODO copied for testing
 *
 * Description: This class writes the annotations of a CAS default view into an XHTML template and
 * adds some JavaScript code making the presentation a little interactive.
 * <p>
 * Subclasses may extend this class to provide specific output for some types of the type system or
 * other improvements.
 * <p>
 *
 * @author entwicklerteam
 */
public class CasToHtmlRenderer {

	private static final String ELLIPSES = "...";

	private static final Logger LOG = LoggerFactory.getLogger(CasToHtmlRenderer.class);


	/**
	 * A simple enumerator of CSS background colors.
	 */
	public static class ColorRing {

		private String[] colors = {
				"color:black;background:lightblue;",
				"color:black;background:lightgreen;",
				"color:black;background:orange;",
				"color:black;background:yellow;",
				"color:black;background:pink;",
				"color:black;background:salmon;",
				"color:black;background:cyan;",
				"color:black;background:violet;",
				"color:black;background:tan;",
				"color:white;background:brown;",
				"color:white;background:blue;",
				"color:white;background:green;",
				"color:white;background:red;",
				"color:white;background:mediumpurple;",
				"color:white;background:black;",
				"color:black;background:coral;",
				"color:white;background:darkblue;",
				"color:white;background:darkmagenta;",
				"color:black;background:khaki;",
				"color:black;background:lime;",
				"color:white;background:olive;" };

		private int pointer = 0;


		/**
		 * Returns the next color. When each available color has been returned it will start over.
		 * <p>
		 *
		 * @return A CSS style including a background color together with a readable foreground
		 *         color.
		 */
		public String next() {

			String color = colors[pointer++];
			if (pointer >= colors.length) {
				pointer = 0;
			}
			return color;
		}

	}

	/**
	 * Some constants for the XHTML template.
	 */
	public interface HtmlConstants {

		String CLASS_CONTROLS = "controls";
		String CLASS_TEXT = "text";
		String CLASS_ANNOTATIONS = "annotations";
		String ELEM_SPAN = "b";
		String ELEM_PRE = "pre";
		String ELEM_LABEL = "label";
		String ELEM_INPUT = "input";
		String ATTR_DATA_ID = "data-id";
		String ATTR_CLASS = "class";
		String ATTR_TYPE = "type";
		String ATTR_VALUE = "value";
		String ATTR_TITLE = "title";
		String TYPE_CHECKBOX = "checkbox";
	}


	/**
	 * The XHTML template.
	 */
	private static final String TEMPLATE = "annotations.xhtml";

	/**
	 * Length of the teaser string for each annotation.
	 */
	private static final int TEASER_LENGTH = 10;

	/**
	 * List of types that are permitted to appear in the output document.
	 */
	private Set<String> allowedTypes;

	/**
	 * Lookup index for accessing DOM elements by id.
	 */
	private Map<String, Element> elementByDataIdIndex = null;

	/**
	 * Maps types to unique artificial keys.
	 */
	private Map<Type, String> typeIds = new LinkedHashMap<>();

	/**
	 * The XML document that is constructed from an XHTML template and filled with content from a
	 * CAS.
	 */
	private Document document;

	/**
	 * Unique ID for the rendering run used to "jail" the CSS classes we produce as part of the
	 * rendering process. This makes sure that multiple rendered CASes shown on the same HTML page
	 * do not interfere with each other in terms of coloring.
	 */
	private String requestedRenderId;
	private String renderUniqueCssClass;


	/**
	 * @param allowedTypes
	 *            Types that are permitted to occur in the output. {@code null} means that all types
	 *            are permitted.
	 */
	public void setAllowedTypes(Set<String> allowedTypes) {

		this.allowedTypes = allowedTypes;
	}


	/**
	 * Override the randomly generated render ID with a known one - useful for testing.
	 */
	public void setRenderId(String renderId) {

		requestedRenderId = renderId;
	}


	/**
	 * Check if a certain type should appear in the output document.
	 * <p>
	 *
	 * @param type
	 * @return {@code true} if the type is allowed and {@code false} otherwise.
	 */
	private boolean isAllowedType(Type type) {

		return allowedTypes == null || allowedTypes.contains(type.getName());
	}


	/**
	 * @param xml
	 *            must not be null
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws NullPointerException
	 *             if {@code xml} is {@code null}
	 */
	private Document parse(InputStream xml)
			throws SAXException, IOException, ParserConfigurationException {

		if (xml == null) {
			throw new IllegalArgumentException("No XML template found.");
		}

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		// Difference between significant and insignificant whitespace is declared in DTD.
		// dbFactory.setIgnoringElementContentWhitespace(true);
		dbFactory.setValidating(false);
		dbFactory.setNamespaceAware(true);
		// Loading an external DTD (e.g. the XHTML DTD) takes very long time. The standard loader
		// does not have a cache.
		dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
				false);
		return dbFactory.newDocumentBuilder().parse(xml);
	}


	/**
	 * Creates an element using the namespace from the given element. If the element does not have a
	 * namespace, no namespace is used.
	 * <p>
	 *
	 * @param element
	 * @param tagName
	 * @return Element
	 */
	private Element createElementWithSameNamespace(Element element, String tagName) {

		String namespace = element.getNamespaceURI();
		if (namespace != null) {
			return document.createElementNS(namespace, tagName);
		}
		return document.createElement(tagName);
	}


	/**
	 * Get an element by the value of its id attribute. The values of the id attribute are supposed
	 * to be unique.
	 * <p>
	 * Remark: Document::getElementById() does not work unless an attribute is declared to be of
	 * type ID (by DTD or by a setter). Using a DTD from the web (like HTML DTDs), however, takes
	 * long download times before the parsing can start.
	 * <p>
	 *
	 * @param id
	 * @return Element
	 */
	private Element getElementByDataId(String id) {

		if (elementByDataIdIndex == null) {
			elementByDataIdIndex = new LinkedHashMap<>();
			NodeList all = document.getElementsByTagName("*");
			for (int i = 0; i < all.getLength(); i++) {
				Element element = (Element) all.item(i);
				String value = element.getAttribute(ATTR_DATA_ID);
				if (value != null) {
					elementByDataIdIndex.put(value, element);
				}
			}
		}
		return elementByDataIdIndex.get(id);
	}


	private Element getFirstElementByClass(String cssClass) {

		NodeList all = document.getElementsByTagName("*");
		for (int i = 0; i < all.getLength(); i++) {
			Element element = (Element) all.item(i);
			String value = element.getAttribute(ATTR_CLASS);
			if (cssClass.equals(value)) {
				return element;
			}
		}
		return null;
	}


	/**
	 * Get an unique artificial key for a type. During the document's lifetime the key will stay the
	 * same.
	 * <p>
	 *
	 * @param type
	 * @return unique key
	 */
	private String getTypeId(Type type) {

		String id = typeIds.get(type);
		if (id == null) {
			id = "t" + typeIds.size();
			typeIds.put(type, id);
		}
		return id;
	}


	/**
	 * Set the id attribute {@code HtmlConstants.ATTR_ID} of an element and update the index
	 * accordingly.
	 * <p>
	 *
	 * @param element
	 * @param id
	 */
	private void setIdAttribute(Element element, String id) {

		LOG.debug("Add element {} with id {}, element, id");

		// construct the index in case it does not exist
		getElementByDataId(id);

		element.setAttribute(HtmlConstants.ATTR_DATA_ID, id);
		elementByDataIdIndex.put(id, element);
	}


	/**
	 * Serialize the document. Subclasses may alter the properties used for the transformation.
	 * <p>
	 *
	 * @param constantColors
	 *            A switch that indicates if a type will always have the same color as long as the
	 *            type system does not change
	 * @param jcas
	 *            The CAS containing the annotations
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 * @throws DOMException
	 * @throws LSException
	 */
	public String render(boolean constantColors, JCas jcas)
			throws SAXException, IOException, ParserConfigurationException {

		renderUniqueCssClass = "_"
				+ (requestedRenderId != null ? requestedRenderId : UUID.randomUUID().toString());
		elementByDataIdIndex = null;
		typeIds = new LinkedHashMap<>();
		document = parse(getXmlTemplate());

		Element annotationViewerElement = getFirstElementByClass("annotation-viewer");
		annotationViewerElement.setAttribute("class",
				annotationViewerElement.getAttribute("class") + " " + renderUniqueCssClass);

		addCharacters(jcas.getDocumentText());

		List<Type> usedTypes = addAnnotationRangesAndDetails(jcas);
		addControls(usedTypes);
		addColors(constantColors ? getAllTypes(jcas) : usedTypes, usedTypes);

		DOMImplementationLS domImplementation = (DOMImplementationLS) document.getImplementation();
		LSSerializer lsSerializer = domImplementation.createLSSerializer();
		lsSerializer.setNewLine("\n");
		lsSerializer.getDomConfig().setParameter("xml-declaration", Boolean.FALSE);
		return lsSerializer.writeToString(document);
	}


	private List<Type> getAllTypes(JCas jcas) {

		LinkedList<Type> typeList = new LinkedList<>();
		Iterator<Type> typesIter = jcas.getTypeSystem().getTypeIterator();
		while (typesIter.hasNext()) {
			typeList.add(typesIter.next());
		}
		return typeList;
	}


	private void addCharacters(String documentText) {

		if (documentText == null) {
			return;
		}

		Element text = getFirstElementByClass(CLASS_TEXT);
		if (text == null) {
			return;
		}

		for (int i = 0; i < documentText.length(); i++) {
			Element span = createElementWithSameNamespace(text, ELEM_SPAN);
			span.setAttribute(ATTR_DATA_ID, "c" + i);
			span.setIdAttribute(ATTR_DATA_ID, true);
			span.setTextContent(String.valueOf(documentText.charAt(i)));
			text.appendChild(span);
		}
	}


	private List<Type> addAnnotationRangesAndDetails(JCas jcas) {

		int number = 0;
		Set<Type> typeSet = new LinkedHashSet<>();

		List<Annotation> sortedAnnotationList = new ArrayList<>(
				select(jcas, Annotation.class));
		sort(sortedAnnotationList, new AnnotationComparator());

		for (Annotation annotation : sortedAnnotationList) {
			if (isAllowedType(annotation.getType())) {
				typeSet.add(annotation.getType());
				addAnnotationRange(annotation, number);
				addAnnotationDetails(annotation, number);
				++number;
			}
		}

		List<Type> typeList = new ArrayList<>(typeSet);
		typeList.sort(comparing(Type::getShortName).thenComparing(Type::getName));
		return typeList;
	}


	private void addAnnotationRange(Annotation annotation, int number) {

		if (getFirstElementByClass(HtmlConstants.CLASS_TEXT) == null) {
			return;
		}

		for (int i = annotation.getBegin(); i < annotation.getEnd(); i++) {
			Element character = getElementByDataId("c" + i);
			if (character != null) {
				character.setAttribute(ATTR_CLASS,
						character.getAttribute(ATTR_CLASS) + " "
								+ getTypeId(annotation.getType()) + " " + "a"
								+ number);
			}
		}
	}


	private void addAnnotationDetails(Annotation annotation, int number) {

		Element annotations = getFirstElementByClass(HtmlConstants.CLASS_ANNOTATIONS);
		if (annotations == null) {
			return;
		}

		StringBuilder pretty = new StringBuilder();
		annotation.prettyPrint(0, 2, pretty, true, getTeaser(annotation));
		Element pre = createElementWithSameNamespace(annotations, HtmlConstants.ELEM_PRE);
		pre.setAttribute(ATTR_CLASS, getTypeId(annotation.getType()));
		setIdAttribute(pre, "a" + number);
		pre.setTextContent(pretty.toString());
		annotations.appendChild(pre);
	}


	private String getTeaser(Annotation annotation) {

		if (annotation.getCoveredText().length() > CasToHtmlRenderer.TEASER_LENGTH
				+ CasToHtmlRenderer.ELLIPSES.length()) {
			return annotation.getCoveredText().substring(0,
					CasToHtmlRenderer.TEASER_LENGTH)
					+ CasToHtmlRenderer.ELLIPSES;
		}
		return annotation.getCoveredText();
	}


	/**
	 * Add colors for the types to the style sheet of the document. A {@code ColorRing} is used to
	 * generate the colors. The colors are continuously assigned to the types.
	 * <p>
	 *
	 * @param types
	 *            List of types (may be the complete list of available type or the list of types
	 *            used in the annotations).
	 * @param usedTypes
	 *            List of types used in the annotations.
	 */
	private void addColors(List<Type> types, List<Type> usedTypes) {

		String renderIdCss = "." + renderUniqueCssClass + " ";

		StringBuilder style = new StringBuilder();
		ColorRing colors = new ColorRing();
		Map<String, String> typeColors = new LinkedHashMap<>();
		for (Type type : types) {
			String color = colors.next();
			// only write CSS rule for allowed and used types in order to save space
			if (isAllowedType(type) && usedTypes.contains(type)) {
				String cssType = getTypeId(type);
				typeColors.put(cssType, color);

				String normal = "." + cssType + " ." + cssType;
				style.append(renderIdCss).append(normal).append("{")
						.append(color)
						.append("}\n");
				style.append(renderIdCss).append(".").append(CLASS_TEXT)
						.append(normal)
						.append("{cursor:pointer;}\n");
				style.append(renderIdCss).append(".").append(CLASS_ANNOTATIONS)
						.append(normal)
						.append(".inline-block{display:inline-block;}\n");
			}
		}

		for (int i = 1; i < typeColors.size(); i++) {
			for (Entry<String, String> entry : typeColors.entrySet()) {
				style.append(renderIdCss).append(".").append(entry.getKey()).append("-").append(i)
						.append(" .").append(entry.getKey()).append("{").append(entry.getValue())
						.append("}\n");
			}
		}

		addColorStyle(style.toString());
		getElementByDataId("colors").removeAttribute(ATTR_DATA_ID);
	}


	private void addControls(List<Type> types) {

		Element controls = getFirstElementByClass(HtmlConstants.CLASS_CONTROLS);
		if (controls == null) {
			return;
		}

		StringBuilder classes = new StringBuilder();
		for (Type type : types) {
			Element label = createElementWithSameNamespace(controls, HtmlConstants.ELEM_LABEL);
			label.setAttribute(HtmlConstants.ATTR_CLASS, getTypeId(type));
			controls.appendChild(document.createTextNode(" "));
			controls.appendChild(label);
			Element check = createElementWithSameNamespace(label, HtmlConstants.ELEM_INPUT);
			check.setAttribute(HtmlConstants.ATTR_TYPE, HtmlConstants.TYPE_CHECKBOX);
			check.setAttribute(HtmlConstants.ATTR_VALUE, getTypeId(type));
			label.appendChild(check);
			label.appendChild(document.createTextNode(type.getShortName()));
			label.setAttribute(HtmlConstants.ATTR_TITLE, type.getName());
			if (classes.length() > 0) {
				classes.append(" ");
			}
			classes.append(getTypeId(type));
		}

		controls.setAttribute(ATTR_CLASS,
				controls.getAttribute(ATTR_CLASS) + " " + classes);
	}


	private void addColorStyle(String stylesheet) {

		Element style = getElementByDataId("colors");
		if (style != null) {
			style.setTextContent(style.getTextContent() + stylesheet);
		}
	}


	/**
	 * Return the input stream for the XML document resource inside the jar file.
	 * <p>
	 *
	 * @return InputStream
	 */
	private InputStream getXmlTemplate() {

		return this.getClass().getClassLoader()
				.getResourceAsStream(CasToHtmlRenderer.TEMPLATE);
	}


	/**
	 * Retrieves the encoding used for deserializing the XHTML template from the XML parser. The
	 * encoding is always known because the XML document is constructed from a file. The XML parser
	 * raises an exception in case of a wrong encoding and no document will be created.
	 *
	 * @return The encoding that was used to deserialize the XHTML template.
	 */
	public String getEncoding() {

		if (document.getXmlEncoding() != null) {
			// XML declaration, e.g. <?xml version="1.0" encoding="ISO-8859-15"?>
			// This might be missing in the input file.
			return document.getXmlEncoding();
		}
		// Default encoding of Java, e.g. -Dfile.encoding=UTF-8
		// Must be known because the XML document is read from a file.
		// The XML parser raises an exception if the file's actual encoding doesn't match this
		// encoding.
		return document.getInputEncoding();
	}
}
