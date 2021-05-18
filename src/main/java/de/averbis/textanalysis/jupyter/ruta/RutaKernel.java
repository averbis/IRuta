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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.ruta.RutaProcessRuntimeException;
import org.apache.uima.ruta.descriptor.RutaDescriptorInformation;
import org.apache.uima.ruta.extensions.RutaParseException;
import org.apache.uima.ruta.extensions.RutaParseRuntimeException;
import org.apache.uima.ruta.resource.RutaResourceLoader;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.springframework.core.io.Resource;
import org.xml.sax.SAXException;

import de.averbis.textanalysis.jupyter.ruta.html.CasToHtmlRenderer;
import de.averbis.textanalysis.jupyter.ruta.magic.CASMagics;
import de.averbis.textanalysis.jupyter.ruta.magic.DirectoryMagics;
import de.averbis.textanalysis.jupyter.ruta.magic.DisplayModeMagics;
import de.averbis.textanalysis.jupyter.ruta.magic.EvaluationMagics;
import de.averbis.textanalysis.jupyter.ruta.magic.RelationalDataMagics;
import de.averbis.textanalysis.jupyter.ruta.magic.RutaEngineMagics;
import io.github.spencerpark.jupyter.kernel.BaseKernel;
import io.github.spencerpark.jupyter.kernel.LanguageInfo;
import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;
import io.github.spencerpark.jupyter.kernel.display.mime.MIMEType;
import io.github.spencerpark.jupyter.kernel.magic.registry.CellMagicFunction;
import io.github.spencerpark.jupyter.kernel.magic.registry.LineMagicFunction;
import io.github.spencerpark.jupyter.kernel.magic.registry.Magics;

/**
 *
 * @author entwicklerteam
 */
public class RutaKernel extends BaseKernel {

	private static final String DEFAULT_DOCUMENT_NAME = "document";
	private final LanguageInfo languageInfo;
	private ClassLoader classLoader;
	private ResourceManager resourceManager;

	private String[] scriptPaths = RutaUtils.DEFAULT_SCRIPT_PATHS;
	private String[] descriptorPaths = RutaUtils.DEFAULT_DESCRIPTOR_PATHS;
	private String[] resourcePaths = RutaUtils.DEFAULT_RESOURCES_PATHS;
	private File inputDir;
	private File outputDir;
	private File loadCasFile;
	private File saveCasFile;
	private String documentText;
	private String documentLanguage;
	private String documentName;
	private File writeScriptFile;
	private File saveTypeSystemFile;
	private File saveCsvFile;
	private Map<String, Object> configurationParameters;
	private SerialFormat serialFormat = SerialFormat.XMI;
	private DisplayMode displayMode = DisplayMode.RUTA_COLORING;
	private List<String> csvConfig;
	private List<String> actualCsvHeaders;
	private List<String> evaluationTypeNames;
	private List<String> dynamicHtmlAllowedTypeNames;

	private JCas jcas;
	private TypeSystemDescription typeSystemDescription;
	private List<String[]> relationalData = new ArrayList<>();
	private List<String[]> relationalDatawithHihglighting = new ArrayList<>();

	// stores evaluation results across documents: document -> type -> outcomes
	private Map<String, Map<String, EvaluationResult>> evaluationData;

	private TypeSystemDescription rutaTypeSystem;
	private List<String> rutaTypeNames;
	private final Map<String, String> documentationMap;

	private Magics magics;
	private MagicsProcessor magicsProcessor;
	private List<String> magicNames;


	public RutaKernel() {

		super();

		languageInfo = new LanguageInfo.Builder("UIMA Ruta")
				.version("3.1.0")
				.mimetype("text/ruta")
				.fileExtension(".ruta")
				.codemirror("ruta")
				.pygments("ruta")
				.build();

		magics = new Magics();
		magics.registerMagics(new CASMagics());
		magics.registerMagics(new DirectoryMagics());
		magics.registerMagics(new RutaEngineMagics());
		magics.registerMagics(new DisplayModeMagics());
		magics.registerMagics(new RelationalDataMagics());
		magics.registerMagics(new EvaluationMagics());

		initializeMagicNames();

		magicsProcessor = new MagicsProcessor(magics);

		// TODO maven magics? move to some lazy getter?
		URL[] classPathUrls = new URL[0];
		classLoader = new URLClassLoader(classPathUrls,
				this.getClass().getClassLoader());
		try {
			resourceManager = UIMAFramework.newDefaultResourceManager();
			resourceManager.setDataPath(descriptorPaths[0]);
			// resourceManager.setExtensionClassLoader(classLoader, false);
			rutaTypeSystem = UIMAFramework.getXMLParser()
					.parseTypeSystemDescription(new XMLInputSource(classLoader
							.getResource("org/apache/uima/ruta/engine/BasicTypeSystem.xml")));
			rutaTypeSystem.resolveImports(resourceManager);
			rutaTypeNames = getTypeNames(rutaTypeSystem);
		} catch (InvalidXMLException | IOException e) {
			// TODO what do do?
			e.printStackTrace();
		}

		documentationMap = new HashMap<>();
		documentationMap.putAll(
				RutaDocumentationUtils.loadDocumentationSafely("documentation/Actions.html"));
		documentationMap.putAll(
				RutaDocumentationUtils.loadDocumentationSafely("documentation/Conditions.html"));

	}


	private void initializeMagicNames() {

		magicNames = new ArrayList<>();

		try {

			@SuppressWarnings("unchecked")
			Map<String, LineMagicFunction<?>> lineMagics = (Map<String, LineMagicFunction<?>>) FieldUtils
					.readField(magics, "lineMagics", true);
			@SuppressWarnings("unchecked")
			Map<String, CellMagicFunction<?>> cellMagics = (Map<String, CellMagicFunction<?>>) FieldUtils
					.readField(magics, "cellMagics", true);
			lineMagics.keySet().forEach(m -> magicNames.add(MagicsProcessor.LINE_MAGIC_PREFIX + m));
			cellMagics.keySet().forEach(m -> magicNames.add(MagicsProcessor.CELL_MAGIC_PREFIX + m));

		} catch (IllegalAccessException e) {
			System.err.println("Failed to initialize names of registered magics!");
		}

	}


	@Override
	public DisplayData eval(String cell) throws Exception {

		String script = magicsProcessor.process(cell);

		if (script == null) {
			// do not try to process CAS if the magics say there are no rules, e.g., it was a
			// document cell magic
			return null;
		}

		saveScript(script);

		AnalysisEngineDescription analysisEngineDescription = RutaUtils
				.createRutaAnalysisEngineDescription(script, classLoader, scriptPaths,
						descriptorPaths, resourcePaths, configurationParameters);

		boolean modified = updateTypeSystemDescription(analysisEngineDescription);
		analysisEngineDescription.getAnalysisEngineMetaData().setTypeSystem(typeSystemDescription);
		updateResourceManager(modified);
		updateJCas(modified);

		saveTypeSystem();
		// reset and do not append across cell runs
		relationalData = new ArrayList<>();
		relationalDatawithHihglighting = new ArrayList<>();
		evaluationData = new LinkedHashMap<>();

		if (inputDir != null) {
			if (!inputDir.exists()) {
				return new DisplayData(
						"No folder named " + inputDir.getAbsolutePath());
			}

			Collection<File> files = FileUtils.listFiles(inputDir,
					new String[] { "txt", "xmi", "bcas" }, false);
			if (files.isEmpty()) {
				return new DisplayData("No files in folder " + inputDir.getAbsolutePath());
			}

			int counter = 0;
			long start = System.currentTimeMillis();
			for (File file : files) {

				System.out.print("Processed " + counter++ + "/" + files.size() + " files.\r");
				RutaUtils.fillCas(jcas.getCas(), file, StandardCharsets.UTF_8, evaluationTypeNames);
				process(file.getName(), analysisEngineDescription);
			}
			long end = System.currentTimeMillis();
			System.out.println("Processed " + counter++ + "/" + files.size()
					+ " files. (took " + (end - start) / 1000 + "s)");
		} else if (jcas != null && jcas.getDocumentText() != null) {
			process(documentName, analysisEngineDescription);
		} else {
			return new DisplayData("No inputDir and no documentText set.");
		}

		printRelationalDataSummary();

		// reset, only evaluate in one cell
		evaluationTypeNames = null;

		saveCsv();

		DisplayData displayData = createDisplayData();
		return displayData;
	}


	private void process(String docName, AnalysisEngineDescription analysisEngineDescription)
			throws ResourceInitializationException, AnalysisEngineProcessException, IOException {

		RutaUtils.applyRuta(jcas, analysisEngineDescription, resourceManager);
		addRelationalData(docName);
		addEvaluationData(docName);
		saveCas(docName);
	}


	private void printRelationalDataSummary() {

		if (csvConfig != null && !csvConfig.isEmpty()) {
			int size = 0;
			if (saveCsvFile != null) {
				size = relationalData.size();
				System.out.println(size + " rows stored.");
			}
			if (displayMode.equals(DisplayMode.CSV)) {
				size = relationalDatawithHihglighting.size();
				System.out.println(size + " rows created.");
			}
		}
	}


	private DisplayData createDisplayData() throws Exception {

		switch (displayMode) {
			case RUTA_COLORING:
				return createRutaColoringDisplayData();
			case DYNAMIC_HTML:
				return createDynamicHtmlDisplayData();
			case CSV:
				return createCSVDisplayData();
			case EVALUATION:
				return createEvaluationDisplayData();
			case NONE:
			default:
				return null;
		}
	}


	private DisplayData createDynamicHtmlDisplayData()
			throws SAXException, IOException, ParserConfigurationException {

		DisplayData displayData = new DisplayData();
		CasToHtmlRenderer casToHtmlRenderer = new CasToHtmlRenderer();
		casToHtmlRenderer.setAllowedTypes(
				CsvUtils.resolveTypeNames(dynamicHtmlAllowedTypeNames, jcas.getTypeSystem()));
		String casAsHtml = casToHtmlRenderer.render(true, jcas);
		displayData.putHTML(casAsHtml);
		return displayData;
	}


	private DisplayData createCSVDisplayData() {

		String html = CsvUtils.convertRelationalDataToHtml(relationalDatawithHihglighting,
				actualCsvHeaders);
		DisplayData displayData = new DisplayData();
		MIMEType mimeType = MIMEType.TEXT_HTML;
		displayData.putData(mimeType, html);
		return displayData;
	}


	private DisplayData createEvaluationDisplayData() {

		DisplayData displayData = new DisplayData();
		displayData.putHTML(EvaluationUtils.createHtmlTable(evaluationData));
		return displayData;
	}


	private DisplayData createRutaColoringDisplayData() {

		DisplayData displayData = new DisplayData();
		displayData.putHTML(RutaColoringUtils.createHighlightingHtml(jcas));
		return displayData;
	}


	private void addRelationalData(String currentDocumentName) {

		if (csvConfig != null && !csvConfig.isEmpty()) {
			if (saveCsvFile != null) {
				relationalData.addAll(createRow(currentDocumentName, false));
			}
			if (displayMode.equals(DisplayMode.CSV)) {
				relationalDatawithHihglighting.addAll(createRow(currentDocumentName, true));
			}
		}
	}


	private List<String[]> createRow(String currentDocumentName, boolean withHighlighting) {

		List<String[]> result = new ArrayList<>();
		String typeName = csvConfig.get(0);
		boolean addCoveredText = true;
		if (typeName.startsWith("-")) {
			typeName = typeName.substring(1);
			addCoveredText = false;
		}
		actualCsvHeaders = new ArrayList<>();

		List<String> featurePaths = Collections.emptyList();
		if (csvConfig.size() > 1) {
			featurePaths = csvConfig.subList(1, csvConfig.size());
		}
		int columns = featurePaths.size();
		if (currentDocumentName != null) {
			columns++;
			actualCsvHeaders.add("Document");
		}
		if (addCoveredText) {
			columns++;
			actualCsvHeaders.add(typeName);
		}
		actualCsvHeaders.addAll(featurePaths);

		TypeSystem typeSystem = jcas.getTypeSystem();
		Type type = CsvUtils.getTypeByName(typeName, typeSystem);
		if (type == null) {
			throw new IllegalArgumentException("Configured type name not known: " + typeName);
		}
		Collection<AnnotationFS> annotations = CasUtil.select(jcas.getCas(), type);
		for (AnnotationFS annotationFS : annotations) {
			int index = 0;
			String[] row = new String[columns];
			if (currentDocumentName != null) {
				row[index++] = currentDocumentName;
			}
			if (addCoveredText) {
				if (withHighlighting) {
					row[index++] = RutaColoringUtils.createHighlightingHtml(annotationFS);
				} else {
					row[index++] = annotationFS.getCoveredText();
				}
			}
			for (String path : featurePaths) {
				row[index++] = CsvUtils.getFeatureValue(annotationFS, path, withHighlighting);
			}
			result.add(row);
		}
		return result;
	}


	private void addEvaluationData(String currentDocumentName) {

		if (evaluationTypeNames != null && !evaluationTypeNames.isEmpty()) {
			EvaluationUtils.evaluate(evaluationTypeNames, jcas);
		}
		if (displayMode.equals(DisplayMode.EVALUATION)) {
			Map<String, EvaluationResult> docResult = EvaluationUtils.createEvaluationResult(jcas);
			evaluationData.put(currentDocumentName, docResult);
		}
	}


	private void saveCas(String documentFileName) throws IOException {

		if (saveCasFile != null) {
			saveCas(saveCasFile);
			saveCasFile = null;
		}

		if (outputDir == null) {
			return;
		}
		String name = documentFileName;
		if (name == null) {
			name = DEFAULT_DOCUMENT_NAME;
		}
		if (!name.endsWith("." + serialFormat.getDefaultFileExtension())) {
			name = name + "." + serialFormat.getDefaultFileExtension();
		}
		File outputFile = new File(outputDir, name);
		saveCas(outputFile);
	}


	private void saveCas(File outputFile) throws IOException, FileNotFoundException {

		outputFile.getParentFile().mkdirs();
		try (FileOutputStream fos = new FileOutputStream(outputFile)) {
			CasIOUtils.save(jcas.getCas(), fos, serialFormat);
		}
	}


	private void saveTypeSystem() throws SAXException, IOException, FileNotFoundException {

		if (saveTypeSystemFile != null) {
			saveTypeSystemFile.getParentFile().mkdirs();
			try (FileOutputStream fos = new FileOutputStream(saveTypeSystemFile)) {
				typeSystemDescription.toXML(fos);
			}
			saveTypeSystemFile = null;
		}
	}


	private void saveScript(String script) throws IOException {

		if (writeScriptFile != null) {
			File parentFile = writeScriptFile.getParentFile();
			if (parentFile != null) {
				parentFile.mkdirs();
			}
			FileUtils.writeStringToFile(writeScriptFile, script, StandardCharsets.UTF_8);
			writeScriptFile = null;
		}
	}


	private void saveCsv() throws IOException {

		if (saveCsvFile != null) {
			CsvUtils.writeCsvToFile(relationalData, saveCsvFile, actualCsvHeaders);
		}
	}


	@Override
	public ReplacementOptions complete(String code, int at) throws Exception {

		int start = getCompletionContextStart(code, at);

		if (start >= 0 && start != at) {
			return getReplacementOptions(code, start, at);
		}

		return null;
	}


	private ReplacementOptions getReplacementOptions(String code, int start, int at)
			throws Exception {

		String context = code.substring(start, at).toLowerCase();
		List<String> candidates = new ArrayList<>();

		// TODO optimize this method

		RutaDescriptorInformation descriptorInformation = RutaUtils
				.createRutaDescriptorInformationSafely(code, classLoader);
		List<String> keywords = RutaKeywords.getKeywords();

		for (String each : descriptorInformation.getTypeShortNames()) {
			if (StringUtils.startsWith(each.toLowerCase(), context)) {
				candidates.add(each);
			}
		}

		for (String each : rutaTypeNames) {
			if (StringUtils.startsWith(each.toLowerCase(), context)) {
				candidates.add(each);
			}
		}

		if (!descriptorInformation.getImportedTypeSystems().isEmpty()) {

			RutaResourceLoader descriptorRutaResourceLoader = new RutaResourceLoader(
					descriptorPaths, classLoader);
			for (String each : descriptorInformation.getImportedTypeSystems()) {
				Resource resource = descriptorRutaResourceLoader.getResourceWithDotNotation(each,
						".xml");
				URL url = null;
				if (resource != null) {
					url = resource.getURL();
				}
				TypeSystemDescription tsdesc = UIMAFramework.getXMLParser()
						.parseTypeSystemDescription(new XMLInputSource(url));
				tsdesc.resolveImports(resourceManager);
				List<String> typeNames = getTypeNames(tsdesc);
				for (String eachTypeName : typeNames) {
					if (StringUtils.startsWith(eachTypeName.toLowerCase(), context)) {
						candidates.add(eachTypeName);
					}
				}
			}
		}

		if (typeSystemDescription != null) {
			List<String> typeNames = getTypeNames(typeSystemDescription);
			for (String eachTypeName : typeNames) {
				if (StringUtils.startsWith(eachTypeName.toLowerCase(), context)) {
					candidates.add(eachTypeName);
				}
			}
		}

		for (String each : keywords) {
			if (StringUtils.startsWith(each.toLowerCase(), context)) {
				candidates.add(each);
			}
		}

		for (String each : magicNames) {
			if (StringUtils.startsWith(each.toLowerCase(), context)) {
				candidates.add(each);
			}
		}

		if (candidates.isEmpty()) {
			return null;
		}
		candidates = new ArrayList<>(new HashSet<>(candidates));
		candidates.sort(new CompletionCandidateComparator(context));

		return new ReplacementOptions(candidates, start, at);
	}


	private List<String> getTypeNames(TypeSystemDescription tsd) {

		List<String> result = new ArrayList<>();
		for (TypeDescription typeDescription : tsd.getTypes()) {
			String name = typeDescription.getName();
			int lastIndexOf = name.lastIndexOf(".");
			if (lastIndexOf > 0) {
				name = name.substring(lastIndexOf + 1);
			}
			result.add(name);
		}
		return result;
	}


	private int getCompletionContextStart(String code, int at) {

		if (at <= 0 || at > code.length()) {
			return -1;
		}
		if (at > 0 && code.length() > 0 && Character.isWhitespace(code.charAt(at - 1))) {
			// TODO completion on empty statement?
			return -1;
		}

		int pointer = at;
		while (pointer > 0) {
			pointer--;
			char c = code.charAt(pointer);
			if (Character.isWhitespace(c)) {
				// new token
				return pointer + 1;
			}

			if (!Character.isDigit(c) && !Character.isLetter(c) && c != '_' && c != '%') {
				return pointer + 1;
			}

		}
		return 0;
	}


	@Override
	public DisplayData inspect(String code, int at, boolean extraDetail) throws Exception {

		String element = getElementAt(code, at);
		String doc = documentationMap.get(element);
		if (doc == null) {
			return null;
		}
		DisplayData result = new DisplayData();
		result.putHTML(doc);
		return result;
	}


	private String getElementAt(String code, int at) {

		int start = at - 1;
		int end = at;

		while (start >= 0) {
			char c = code.charAt(start);
			if (!Character.isLetter(c)) {
				break;
			}
			start--;
		}
		while (end < code.length()) {
			char c = code.charAt(end);
			if (!Character.isLetter(c)) {
				break;
			}
			end++;
		}

		return code.substring(start + 1, end);
	}


	@Override
	public List<String> formatError(Exception e) {

		// return super.formatError(e);
		Exception root = getRootException(e);
		String message = root.getMessage();

		// options to provide special logic
		if (root instanceof RutaParseException) {
			return Arrays.asList(message);
		}
		if (root instanceof RutaParseRuntimeException) {
			return Arrays.asList(message);
		}
		if (root instanceof RutaProcessRuntimeException) {
			return Arrays.asList(message);
		}
		if (root instanceof ResourceInitializationException) {
			return Arrays.asList(message);
		}

		if (root instanceof RuntimeException && !StringUtils.isBlank(message)) {
			return Arrays.asList(message);
		}

		// fallback to stacktrace
		return super.formatError(root);
	}


	private Exception getRootException(Exception e) {

		Exception root = e;
		while (root.getCause() != null && root.getCause() instanceof Exception) {
			root = (Exception) root.getCause();
		}
		return root;
	}


	private boolean updateTypeSystemDescription(AnalysisEngineDescription analysisEngineDescription)
			throws ResourceInitializationException {

		TypeSystemDescription cellTypeSystemDescription = analysisEngineDescription
				.getAnalysisEngineMetaData().getTypeSystem();
		if (typeSystemDescription == null) {

			typeSystemDescription = cellTypeSystemDescription;
			return true;
		}

		TypeSystemDescription merged = CasCreationUtils.mergeTypeSystems(
				Arrays.asList(typeSystemDescription, cellTypeSystemDescription),
				resourceManager);

		// System.out.println("Old type system: " + typeSystemDescription.getTypes().length);
		// System.out.println("New type system: " + cellTypeSystemDescription.getTypes().length);
		// System.out.println("Merged type system: " + merged.getTypes().length);

		if (merged.equals(typeSystemDescription)) {
			return false;
		}
		typeSystemDescription = merged;
		return true;
	}


	public void addTypeSystem(File file) throws Exception {

		TypeSystemDescription fileTSD = UIMAFramework.getXMLParser()
				.parseTypeSystemDescription(new XMLInputSource(file));
		if (typeSystemDescription == null) {

			typeSystemDescription = fileTSD;
			return;
		}

		TypeSystemDescription merged = CasCreationUtils.mergeTypeSystems(
				Arrays.asList(typeSystemDescription, fileTSD),
				resourceManager);

		if (!merged.equals(typeSystemDescription) && jcas != null) {
			typeSystemDescription = merged;
			RutaUtils.upgradeCas(jcas.getCas(), jcas.getCas(), typeSystemDescription);
		}
	}


	private void updateJCas(boolean modifiedTypeSystem) throws Exception {

		boolean casCreated = false;
		if (jcas == null) {
			jcas = JCasFactory.createJCas(typeSystemDescription);
			casCreated = true;
		}

		if (!casCreated && modifiedTypeSystem) {
			RutaUtils.upgradeCas(jcas.getCas(), jcas.getCas(), typeSystemDescription);
		}

		// CAS was specifically set via %readCas line magic
		if (loadCasFile != null) {
			jcas.reset();

			try {
				RutaUtils.fillCas(jcas.getCas(), loadCasFile, StandardCharsets.UTF_8,
						evaluationTypeNames);
				documentName = loadCasFile.getName();
			} finally {
				// reset the pointer even if there is an exception
				loadCasFile = null;
			}

			// This resets other input modalities
			documentText = null;
			inputDir = null;
		}

		// documentText was changed via line/cell magic
		else if (documentText != null
				&& !StringUtils.equals(documentText, jcas.getDocumentText())) {
			jcas.reset();
			setJCasTextAndLanguage();
			// documentText line magic resets batch mode
			inputDir = null;
		}

	}


	private void updateResourceManager(boolean modifiedTypeSystem) throws Exception {

		if (modifiedTypeSystem) {
			ResourceManager oldResourceManager = resourceManager;
			String dataPath = oldResourceManager.getDataPath();
			resourceManager = UIMAFramework.newDefaultResourceManager();
			resourceManager.setDataPath(dataPath);
		}

	}


	private void setJCasTextAndLanguage() {

		jcas.setDocumentText(documentText);
		if (documentLanguage != null) {
			jcas.setDocumentLanguage(documentLanguage);
		}
	}


	public void clear() {

		scriptPaths = RutaUtils.DEFAULT_SCRIPT_PATHS;
		descriptorPaths = RutaUtils.DEFAULT_DESCRIPTOR_PATHS;
		resourcePaths = RutaUtils.DEFAULT_RESOURCES_PATHS;
		inputDir = null;
		outputDir = null;
		documentText = null;
		documentLanguage = null;
		documentName = null;
		loadCasFile = null;
		saveCasFile = null;
		writeScriptFile = null;
		saveTypeSystemFile = null;
		serialFormat = SerialFormat.XMI;
		displayMode = DisplayMode.RUTA_COLORING;
		configurationParameters = null;
		csvConfig = null;
		evaluationTypeNames = null;
		dynamicHtmlAllowedTypeNames = null;

		jcas = null;
		typeSystemDescription = null;
		relationalData = new ArrayList<>();
		relationalDatawithHihglighting = new ArrayList<>();
		evaluationData = null;
	}


	@Override
	public LanguageInfo getLanguageInfo() {

		return languageInfo;
	}


	public void setDocumentText(String text) {

		documentText = text;
	}


	public String getDocumentText() {

		return documentText;
	}


	public void setDocumentLanguage(String language) {

		documentLanguage = language;
	}


	public String getDocumentLanguage() {

		return documentLanguage;
	}


	public void setInputDir(File dir) {

		inputDir = dir;
	}


	public File getInputDir() {

		return inputDir;
	}


	public void setOutputDir(File dir) {

		outputDir = dir;
	}


	public File getOutputDir() {

		return outputDir;
	}


	public void setScriptPaths(String[] paths) {

		scriptPaths = paths;
	}


	public String[] getScriptPaths() {

		return scriptPaths;
	}


	public void setDescriptorPaths(String[] paths) {

		descriptorPaths = paths;
	}


	public String[] getDescriptorPaths() {

		return descriptorPaths;
	}


	public void setResourcePaths(String[] paths) {

		resourcePaths = paths;
	}


	public String[] getResourcePaths() {

		return resourcePaths;
	}


	public void setWriteScriptFile(File file) {

		writeScriptFile = file;
	}


	public File getWriteScriptFile() {

		return writeScriptFile;
	}


	public void setSaveTypeSystemFile(File path) {

		saveTypeSystemFile = path;
	}


	public File getSaveTypeSystemFile() {

		return saveTypeSystemFile;
	}


	public void setConfigurationParameters(Map<String, Object> params) {

		configurationParameters = params;

	}


	public Map<String, Object> getConfigurationParameters() {

		return configurationParameters;
	}


	public void resetCas() {

		if (jcas != null) {
			jcas.reset();
		}

		documentText = null;
		documentLanguage = null;
	}


	public void setDocumentName(String name) {

		documentName = name;
	}


	public String getDocumentName() {

		return documentName;
	}


	public void setCasSerialFormat(SerialFormat format) {

		serialFormat = format;
	}


	public SerialFormat getCasSerialFormat() {

		return serialFormat;
	}


	public void setDisplayMode(DisplayMode mode) {

		displayMode = mode;
	}


	public DisplayMode getDisplayMode() {

		return displayMode;
	}


	public void setSaveCSVFile(File file) {

		saveCsvFile = file;
	}


	public File getSaveCSVFile() {

		return saveCsvFile;
	}


	public void setCSVConfig(List<String> config) {

		csvConfig = config;
	}


	public List<String> getCSVConfig() {

		return csvConfig;
	}


	public void setLoadCasFile(File file) {

		loadCasFile = file;
	}


	public void setSaveCasFile(File file) {

		saveCasFile = file;
	}


	public TypeSystemDescription getTypeSystemDescription() {

		return typeSystemDescription;
	}


	public void setEvaluationTypeNames(List<String> evalTypes) {

		evaluationTypeNames = evalTypes;
	}


	public List<String> getEvaluationTypeNames() {

		return evaluationTypeNames;
	}


	public void setDynamicHtmlAllowedTypeNames(List<String> typeNames) {

		dynamicHtmlAllowedTypeNames = typeNames;
	}


	public List<String> getDynamicHtmlAllowedTypeNames() {

		return dynamicHtmlAllowedTypeNames;
	}

}
