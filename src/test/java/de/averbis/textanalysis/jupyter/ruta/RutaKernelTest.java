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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.ruta.engine.RutaEngine;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.CasLoadMode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;
import io.github.spencerpark.jupyter.kernel.display.mime.MIMEType;

/**
 *
 * @author entwicklerteam
 */
public class RutaKernelTest {

	private static RutaKernel KERNEL = IRuta.getInstance();


	@Before
	public void resetKernel() {

		KERNEL.clear();
		// TODO target/ is maven, but this is gradle territory
		// KERNEL.setOutputDir(new File("target/test-output"));
		KERNEL.setScriptPaths(new String[] { "src/test/resources/script" });
		KERNEL.setDescriptorPaths(new String[] { "src/test/resources/descriptor" });
		KERNEL.setResourcePaths(new String[] { "src/test/resources/resources" });
	}


	@Test
	public void testComplete() throws Exception {

		assertFirstReplacementOptions("%saveT", 6, "%saveTypeSystem");
		assertFirstReplacementOptions("%%doc", 5, "%%documentText");
		assertFirstReplacementOptions("{->MAR", 5, "MARK");
		assertFirstReplacementOptions("-PART", 5, "PARTOF");
		assertFirstReplacementOptions("Sentence{CONT(Problem)}", 13, "CONTAINS");
		assertFirstReplacementOptions("TYPESYSTEM TestTypeSystem;Tes", 28, "Test");
		assertFirstReplacementOptions("Document; DECL MyType", 14, "DECLARE");
		assertFirstReplacementOptions("ANY C;", 5, "CW");
		assertFirstReplacementOptions("Document; DECL", 14, "DECLARE");
		assertFirstReplacementOptions("DECL", 4, "DECLARE");
		assertNoReplacementOptions("ANY+{", 4);
		assertNoReplacementOptions("   ", 2);
		assertNoReplacementOptions("%%saveC", 6);
	}


	@Test
	public void testInspect() throws Exception {

		Assert.assertNotNull(KERNEL.inspect("MARK", 4, true));
		Assert.assertNotNull(KERNEL.inspect("MARK", 2, true));
		Assert.assertNotNull(KERNEL.inspect("Document{CONTAINS(CW)}", 10, true));
		Assert.assertNotNull(KERNEL.inspect("Document{CONTAINS(CW)}", 9, true));
	}


	@Test
	public void testFormatException() throws Exception {

		JCas jcas = JCasFactory.createJCas();

		assertFormattedException(jcas.getCas(), "CW", Arrays.asList(
				"Error in Test, line 1, \"<EOF>\": expected SEMI, but found <unknown token>"));
		assertFormattedException(jcas.getCas(), "CW;;", Arrays.asList(
				"Error in Test, line 1, \";\": expected 'none', but found SEMI"));
		assertFormattedException(jcas.getCas(), "cw;", Arrays.asList(
				"Not able to resolve annotation/type expression: cw in script Test"));
	}


	private void assertFirstReplacementOptions(String code, int at, String expected)
			throws Exception {

		ReplacementOptions complete = KERNEL.complete(code, at);
		Assert.assertNotNull(code, complete);
		List<String> replacements = complete.getReplacements();
		Assert.assertFalse(code, replacements.isEmpty());
		String firstReplacement = replacements.get(0);
		Assert.assertEquals(expected, firstReplacement);
	}


	private void assertNoReplacementOptions(String code, int at)
			throws Exception {

		ReplacementOptions complete = KERNEL.complete(code, at);
		Assert.assertNull(code, complete);
	}


	private void assertFormattedException(CAS cas, String rulesWithError, List<String> expected) {

		try {
			AnalysisEngine ae = AnalysisEngineFactory.createEngine(RutaEngine.class,
					RutaEngine.PARAM_RULES, rulesWithError,
					RutaEngine.PARAM_RULES_SCRIPT_NAME, "Test");
			ae.process(cas);
		} catch (Exception e) {
			List<String> formatError = KERNEL.formatError(e);
			Assert.assertEquals(expected, formatError);
		}
	}


	@Test
	public void testExternalTypeSystem() throws Exception {

		KERNEL.eval("TYPESYSTEM TestTypeSystem;\n"
				+ "Test;\n");
	}


	@Test
	public void testEvalOutput() throws Exception {

		String expected = "<span style='background:#008000'>This</span> is a test.";

		DisplayData displayData = KERNEL.eval("%documentText \"This is a test.\"\n"
				+ "DECLARE EvalOutput;\n"
				+ "CW{->EvalOutput};\n"
				+ "COLOR(EvalOutput, \"green\");\n");
		String actual = (String) displayData.getData(MIMEType.TEXT_HTML);
		Assert.assertEquals(expected, actual);
	}


	@Test
	public void testNoInput() throws Exception {

		KERNEL.clear();
		DisplayData eval = KERNEL.eval("DECLARE EvalOutput;\n"
				+ "CW{->EvalOutput};");

		Assert.assertEquals("No inputDir and no documentText set.",
				eval.getData(MIMEType.TEXT_PLAIN));
	}


	@Test
	public void testProcessingMultipleFiles() throws Exception {

		SerialFormat format = SerialFormat.COMPRESSED_FILTERED_TSI;
		String inputDir = "./src/test/resources/test-input2";
		String outputDir = "./target/test-output/testProcessingMultipleFiles";
		File dir = new File(outputDir);
		FileUtils.deleteDirectory(dir);
		KERNEL.eval("%inputDir " + inputDir + "\n"
				+ "%outputDir " + outputDir + "\n"
				+ "%setCasSerialFormat " + format + "\n"
				+ "DECLARE EvalOutput;\n"
				+ "CW{->EvalOutput};");

		File[] files = dir.listFiles();
		Arrays.sort(files);
		Assert.assertEquals(2, files.length);
		Assert.assertEquals("sample.txt.bcas", files[0].getName());
		Assert.assertEquals("sample2.txt.bcas", files[1].getName());
	}


	@Test
	public void testEvalWithDeclare() throws Exception {

		KERNEL.eval("DECLARE TestType1, EvalWithDeclare;\n"
				+ "EvalWithDeclare;\n");
	}


	@Test
	public void testDocumentTextLineMagic() throws Exception {

		String text = "This is a test.";
		String cell = Stream.of("%documentText en \"" + text + "\"",
				"CW;").collect(Collectors.joining("\n"));
		DisplayData displayData = KERNEL.eval(cell);
		Assert.assertEquals(text, displayData.getData(MIMEType.TEXT_HTML));
	}


	@Test
	public void testImportScript() throws Exception {

		String text = "This is a test.";
		String cell = Stream.of("%documentText en \"" + text + "\"",
				"SCRIPT uima.ruta.ExternalScript;",
				"CALL(ExternalScript);").collect(Collectors.joining("\n"));
		DisplayData displayData = KERNEL.eval(cell);
		String output = (String) displayData.getData(MIMEType.TEXT_HTML);
		Assert.assertTrue(output.contains("'background:#ffc0cb'")); // pink
	}


	@Test
	public void testWordlist() throws Exception {

		String text = "David, Peter and Richard work on Ruta Kernel.";
		String cell = Stream.of("%documentText en \"" + text + "\"",
				"WORDLIST firstNames = 'firstnames.txt';",
				"DECLARE FirstName;",
				"MARKFAST(FirstName, firstNames);",
				"COLOR(FirstName, \"pink\");").collect(Collectors.joining("\n"));
		DisplayData displayData = KERNEL.eval(cell);
		String output = (String) displayData.getData(MIMEType.TEXT_HTML);
		Assert.assertTrue(output.contains("'background:#ffc0cb'")); // pink
	}


	@Test
	public void testIncrementalCells() throws Exception {

		KERNEL.eval("%documentText en \"This is a test.\"");
		KERNEL.eval("DECLARE Test1;");
		KERNEL.eval("\"test\"-> Test1;");
		KERNEL.eval("COLOR(Test1, \"green\");");
		KERNEL.eval("DECLARE SWBeforeTest;");
		KERNEL.eval("SW{->SWBeforeTest} Test1;");
		DisplayData displayData = KERNEL.eval("COLOR(SWBeforeTest, \"pink\");");

		String output = (String) displayData.getData(MIMEType.TEXT_HTML);
		Assert.assertTrue(output.contains("'background:#008000'")); // green
		Assert.assertTrue(output.contains("'background:#ffc0cb'")); // pink
	}


	@Test
	@Ignore("not yet supported")
	public void testIncrementalCellsWithVariables() throws Exception {

		KERNEL.eval("%documentText en \"This is a test.\"");
		KERNEL.eval("DECLARE Test2;");
		KERNEL.eval("STRING s = \"test\";");
		KERNEL.eval("W{REGEXP(s)-> Test2};");
		DisplayData displayData = KERNEL.eval("COLOR(Test2, \"pink\");");

		String output = (String) displayData.getData(MIMEType.TEXT_HTML);
		Assert.assertTrue(output.contains("'background:#ffc0cb'")); // pink
	}


	@Test
	public void testWriteScriptFile() throws Exception {

		String expectedFileContent = "//%documentText en \"This is a test.\"\n"
				+ "//%writescript ./test-output/WriteScriptFile.ruta\n"
				+ "DECLARE CW2;"
				+ "CW{-> CW2};";

		KERNEL.eval("%documentText en \"This is a test.\"\n"
				+ "%writescript ./test-output/WriteScriptFile.ruta\n"
				+ "DECLARE CW2;"
				+ "CW{-> CW2};");

		File file = new File("test-output/WriteScriptFile.ruta");
		Assert.assertTrue(file.exists());
		Assert.assertEquals(expectedFileContent,
				FileUtils.readFileToString(file, StandardCharsets.UTF_8));
	}


	@Test
	public void testConfigurationParameters() throws Exception {

		DisplayData displayData = KERNEL.eval("%documentText en \"This is a vartest.\"\n"
				+ "%configParams --varNames var1 --varValues vartest\n"
				+ "STRING var1;\n"
				+ "DECLARE VarType;\n"
				+ "SW{REGEXP(var1)->VarType};\n"
				+ "COLOR(VarType, \"pink\");");

		String output = (String) displayData.getData(MIMEType.TEXT_HTML);
		Assert.assertTrue(output.contains("'background:#ffc0cb'")); // pink
	}


	@Test
	public void testCompleteOnTypeDeclaredInPreviousCell() throws Exception {

		KERNEL.eval("%documentText en \"This is a test.\"");
		KERNEL.eval("DECLARE CompleteOnType;");
		assertFirstReplacementOptions("CompleteOnT", 11, "CompleteOnType");
	}


	@Test
	public void testProcessExternalCas() throws Exception {

		String inputDirPath = "target/temp-cas-input/";
		String outputDirPath = "target/temp-cas-output/";
		String inputPath = inputDirPath + "cas.xmi";
		String tsPath = inputDirPath + "ts.xml";
		String outputPath = outputDirPath + "cas.xmi";

		FileUtils.deleteDirectory(new File(inputDirPath));
		FileUtils.deleteDirectory(new File(outputDirPath));
		new File(inputDirPath).mkdirs();

		TypeSystemDescription tsd = new TypeSystemDescription_impl();
		String external = "External";
		String text = "External cas text.";
		tsd.addType(external, "", CAS.TYPE_NAME_ANNOTATION);
		CAS externalCas = CasFactory.createCas(tsd);
		externalCas.setDocumentText(text);
		try (FileOutputStream fos = new FileOutputStream(inputPath)) {
			CasIOUtils.save(externalCas, fos, SerialFormat.XMI);
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		}
		try (FileOutputStream fos = new FileOutputStream(tsPath)) {
			tsd.toXML(fos);
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		}
		KERNEL.eval("%inputDir");
		KERNEL.eval("%outputDir");
		KERNEL.eval("%documentText en \"Text set by magic.\"");
		KERNEL.eval("%loadCas " + inputPath);
		KERNEL.eval("%loadTypeSystem " + tsPath);
		KERNEL.eval("\"" + external + "\"{->" + external + "};");
		KERNEL.eval("%saveCas " + outputPath);

		externalCas.reset();
		CasIOUtils.load(new File(outputPath).toURI().toURL(), null, externalCas,
				CasLoadMode.LENIENT);

		Assert.assertEquals(text, externalCas.getDocumentText());
		Type type = externalCas.getTypeSystem().getType(external);
		Collection<AnnotationFS> select = CasUtil.select(externalCas, type);
		Assert.assertEquals(1, select.size());
		Assert.assertEquals(external, select.iterator().next().getCoveredText());
	}


	@Test
	public void testReadingCasDirectly() throws Exception {

		DisplayData eval = KERNEL.eval("%loadCas src/test/resources/test-input/xmi/example.xmi");
		String output = (String) eval.getData(MIMEType.TEXT_HTML);
		Assert.assertTrue(output.startsWith("ANAMNESE"));
	}


	@Test
	public void testCellMagicLineBreaksWithoutColoring() throws Exception {

		String text = "This is a text\nwith line breaks.";
		String cell = "%%documentText\n" + text;

		KERNEL.eval(cell);
		DisplayData eval = KERNEL.eval("Document;");
		String html = (String) eval.getData(MIMEType.TEXT_HTML);

		Assert.assertEquals("This is a text<br />with line breaks.", html);
	}


	@Test
	public void thatUnknownMagicGeneratesException() {

		assertThatExceptionOfType(MagicsProcessorException.class)
				.isThrownBy(() -> KERNEL.eval("%DUMMY\n"));
	}
}
