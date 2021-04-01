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
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author entwicklerteam
 */
public class RutaKeywords {

	public static List<String> conditions = Arrays.asList("CONTAINS", "IF", "INLIST", "PARTOF",
			"TOTALCOUNT", "CURRENTCOUNT", "CONTEXTCOUNT", "LAST", "VOTE", "COUNT", "NEAR", "REGEXP",
			"POSITION", "SCORE", "ISLISTEMPTY", "MOFN", "AND", "OR", "FEATURE", "PARSE", "IS",
			"BEFORE", "AFTER", "STARTSWITH", "ENDSWITH", "PARTOFNEQ", "SIZE", "NOT");

	public static List<String> declarations = Arrays.asList("WORDLIST", "DECLARE", "BOOLEAN",
			"PACKAGE", "TYPE", "TYPESYSTEM", "INT", "DOUBLE", "FLOAT", "STRING", "SCRIPT",
			"WORDTABLE", "ENGINE", "BLOCK", "BOOLEANLIST", "INTLIST", "DOUBLELIST", "FLOATLIST",
			"STRINGLIST", "TYPELIST", "UIMAFIT", "IMPORT", "FROM", "AS", "null", "ANNOTATION",
			"ANNOTATIONLIST", "ACTION", "CONDITION", "VAR", "FOREACH");

	public static List<String> actions = Arrays.asList("DEL", "CALL", "MARK", "MARKSCORE", "COLOR",
			"LOG", "REPLACE", "FILLOBJECT", "RETAINTYPE", "SETFEATURE", "ASSIGN", "PUTINLIST",
			"ATTRIBUTE", "MARKFAST", "FILTERTYPE", "CREATE", "FILL", "MARKTABLE", "UNMARK",
			"TRANSFER", "MARKONCE", "TRIE", "GATHER", "EXEC", "MARKLAST", "ADD", "REMOVE", "MERGE",
			"GET", "GETLIST", "REMOVEDUPLICATE", "GETFEATURE", "MATCHEDTEXT", "CLEAR", "UNMARKALL",
			"SHIFT", "CONFIGURE", "DYNAMICANCHORING", "TRIM", "ADDRETAINTYPE", "REMOVERETAINTYPE",
			"ADDFILTERTYPE", "REMOVEFILTERTYPE", "MARKFIRST", "GREEDYANCHORING", "SPLIT");

	public static List<String> basics = Arrays.asList("ALL", "ANY", "AMP", "BREAK", "W", "NUM",
			"PM", "Document", "MARKUP", "SW", "CW", "CAP", "PERIOD", "NBSP", "SENTENCEEND", "COLON",
			"COMMA", "SEMICOLON", "WS", "_", "SPACE", "SPECIAL", "EXCLAMATION", "QUESTION", "#");

	private static List<String> allKeywords;

	static {
		allKeywords = new ArrayList<>();
		allKeywords.addAll(conditions);
		allKeywords.addAll(declarations);
		allKeywords.addAll(actions);
		// resolved by type system
		// allKeywords.addAll(basics);
	}


	public static List<String> getKeywords() {

		return allKeywords;
	}

}
