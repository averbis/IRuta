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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.ruta.RutaScriptFactory;
import org.apache.uima.ruta.action.ActionFactory;
import org.apache.uima.ruta.condition.ConditionFactory;
import org.apache.uima.ruta.descriptor.RutaBuildOptions;
import org.apache.uima.ruta.descriptor.RutaDescriptorFactory;
import org.apache.uima.ruta.descriptor.RutaDescriptorInformation;
import org.apache.uima.ruta.engine.RutaEngine;
import org.apache.uima.ruta.expression.ExpressionFactory;
import org.apache.uima.ruta.extensions.RutaExternalFactory;
import org.apache.uima.ruta.parser.RutaLexer;
import org.apache.uima.ruta.parser.RutaParser;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.InvalidXMLException;

/**
 *
 * @author entwicklerteam
 */
public class RutaUtils {

	public static final String[] DEFAULT_SCRIPT_PATHS = new String[] { "." };
	public static final String[] DEFAULT_DESCRIPTOR_PATHS = new String[] { "." };
	public static final String[] DEFAULT_RESOURCES_PATHS = new String[] { "." };
	public static final String DEFAULT_INPUT_DIR = "input";
	public static final String DEFAULT_OUTPUT_DIR = "output";


	private RutaUtils() {

		// nothing here
	}


	public static void applyRuta(JCas jcas, AnalysisEngineDescription description,
			ResourceManager resourceManager)
			throws ResourceInitializationException, AnalysisEngineProcessException {

		AnalysisEngine analysisEngine = UIMAFramework.produceAnalysisEngine(description,
				resourceManager, null);
		analysisEngine.process(jcas);
	}


	public static CAS applyRuta(File inputFile, AnalysisEngineDescription description,
			String datapath)
			throws ResourceInitializationException, IOException, AnalysisEngineProcessException {

		// new resource manager for extended CAS with new types
		ResourceManager resourceManager = UIMAFramework.newDefaultResourceManager();
		resourceManager.setDataPath(datapath);

		AnalysisEngine analysisEngine = UIMAFramework.produceAnalysisEngine(description,
				resourceManager, null);
		CAS cas = analysisEngine.newCAS();

		fillCas(cas, inputFile, StandardCharsets.UTF_8);

		analysisEngine.process(cas);

		return cas;
	}


	public static CAS applyRuta(String documentText, String documentLanguage,
			AnalysisEngineDescription description,
			String datapath)
			throws ResourceInitializationException, IOException, AnalysisEngineProcessException {

		// new resource manager for extended CAS with new types
		ResourceManager resourceManager = UIMAFramework.newDefaultResourceManager();
		resourceManager.setDataPath(datapath);

		AnalysisEngine analysisEngine = UIMAFramework.produceAnalysisEngine(description,
				resourceManager, null);
		CAS cas = analysisEngine.newCAS();
		cas.setDocumentText(documentText);
		cas.setDocumentLanguage(documentLanguage);

		analysisEngine.process(cas);

		return cas;
	}


	public static void fillCas(CAS cas, File file, Charset encoding) throws IOException {

		cas.reset();
		String extension = FilenameUtils.getExtension(file.getName());
		if ("txt".equals(extension)) {
			String document = FileUtils.readFileToString(file, encoding);
			cas.setDocumentText(document);
		} else {
			CasIOUtils.load(file.toURI().toURL(), null, cas, true);
		}

	}


	public static AnalysisEngineDescription createRutaAnalysisEngineDescription(String script,
			ClassLoader classLoader, String[] scriptPaths, String[] descriptorPaths,
			String[] resourcePaths, Map<String, Object> configurationParameters)
			throws IOException, RecognitionException, InvalidXMLException,
			ResourceInitializationException, URISyntaxException {

		Pair<AnalysisEngineDescription, TypeSystemDescription> descriptions = createDescriptions(
				script, classLoader, scriptPaths, descriptorPaths, resourcePaths);
		AnalysisEngineDescription analysisEngineDescription = descriptions.getKey();
		ConfigurationParameterSettings configurationParameterSettings = analysisEngineDescription
				.getAnalysisEngineMetaData().getConfigurationParameterSettings();
		configurationParameterSettings
				.setParameterValue(RutaEngine.PARAM_RULES_SCRIPT_NAME, "");
		if (configurationParameters != null) {
			for (Entry<String, Object> entry : configurationParameters.entrySet()) {
				configurationParameterSettings.setParameterValue(entry.getKey(), entry.getValue());
			}
		}
		return analysisEngineDescription;
	}


	public static Pair<AnalysisEngineDescription, TypeSystemDescription> createDescriptions(
			String script, ClassLoader classLoader, String[] scriptPaths, String[] descriptorPaths,
			String[] resourcePaths)
			throws IOException, RecognitionException, InvalidXMLException,
			ResourceInitializationException, URISyntaxException {

		RutaDescriptorFactory rdf = new RutaDescriptorFactory();
		RutaBuildOptions options = new RutaBuildOptions();
		options.setClassLoader(classLoader);
		options.setResolveImports(true);
		RutaDescriptorInformation descriptorInformation = rdf.parseDescriptorInformation(script, "",
				options);
		Pair<AnalysisEngineDescription, TypeSystemDescription> descriptions = rdf
				.createDescriptions(null, null, descriptorInformation, options, scriptPaths,
						descriptorPaths, resourcePaths);
		return descriptions;
	}


	public static RutaDescriptorInformation createRutaDescriptorInformationSafely(String script,
			ClassLoader classLoader) {

		RutaBuildOptions options = new RutaBuildOptions();
		options.setClassLoader(classLoader);
		options.setResolveImports(true);

		CharStream st = new ANTLRStringStream(script);
		RutaLexer lexer = new RutaLexer(st);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		RutaParser parser = new RutaParser(tokens);
		RutaDescriptorInformation descInfo = new RutaDescriptorInformation();
		parser.setDescriptorInformation(descInfo);

		ExpressionFactory expressionFactory = new ExpressionFactory();
		ActionFactory actionFactory = new ActionFactory();
		ConditionFactory conditionFactory = new ConditionFactory();
		RutaScriptFactory scriptFactory = new RutaScriptFactory(expressionFactory);

		parser.setContext(null);
		parser.setScriptFactory(scriptFactory);
		parser.setExpressionFactory(expressionFactory);
		parser.setExternalFactory(new RutaExternalFactory());
		parser.setActionFactory(actionFactory);
		parser.setConditionFactory(conditionFactory);
		parser.setResourcePaths(new String[0]);
		// ResourceManager rm = getResourceManager(options);
		// parser.setResourceManager(rm);
		descInfo.setScriptName("");
		try {
			parser.file_input("");
		} catch (Exception e) {
			// e.printStackTrace();
			// do not care about parse exceptions
		}
		descInfo.setRules(script);
		return descInfo;
	}


	/**
	 * Load the contents from the source CAS, upgrade it to the target type system and write the
	 * results to the target CAS. An in-place upgrade can be achieved by using the same CAS as
	 * source and target.
	 */
	public static void upgradeCas(CAS aSourceCas, CAS aTargetCas,
			TypeSystemDescription aTargetTypeSystem)
			throws UIMAException, IOException {

		// Save source CAS type system (do this early since we might do an in-place upgrade)
		TypeSystem souceTypeSystem = aSourceCas.getTypeSystem();

		// Save source CAS contents
		ByteArrayOutputStream serializedCasContents = new ByteArrayOutputStream();
		Serialization.serializeWithCompression(aSourceCas, serializedCasContents, souceTypeSystem);

		// Re-initialize the target CAS with new type system
		CAS tempCas = JCasFactory.createJCas(aTargetTypeSystem).getCas();
		CASCompleteSerializer serializer = Serialization.serializeCASComplete((CASImpl) tempCas);
		Serialization.deserializeCASComplete(serializer, (CASImpl) aTargetCas);

		// Leniently load the source CAS contents into the target CAS
		Serialization.deserializeCAS(aTargetCas,
				new ByteArrayInputStream(serializedCasContents.toByteArray()), souceTypeSystem,
				null);

		// Make sure JCas is properly initialized too
		aTargetCas.getJCas();
	}

}
