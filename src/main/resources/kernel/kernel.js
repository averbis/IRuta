define([
  "jquery",
  "codemirror/lib/codemirror",
  "codemirror/addon/selection/active-line",
  "codemirror/addon/mode/simple",
  "codemirror/addon/fold/foldcode",
  "codemirror/addon/fold/foldgutter",
  "codemirror/addon/fold/indent-fold",
  "notebook/js/codecell"
], function ($) {
  "use strict";

	function load_css(text) {
		var css = document.createElement("style");
		css.type = "text/css";
		css.innerHTML = text;
		document.body.appendChild(css);
	}
	
	var onload = function () {
		/* 
		* Code mirror simple mode definition for UIMA Ruta
		*/
		CodeMirror.defineSimpleMode("ruta", {
			start: [
				{regex: /%%.*/, token: "ruta-cell-magic", sol: true, next: "document"},
				{regex: /"(?:[^\\]|\\.)*?(?:"|$)/, token: "ruta-string"},
				{regex: /'(?:[^\\]|\\.)*?(?:'|$)/, token: "ruta-string"},
				{regex: /\b(?:MARKSCORE|MARKFAST|MARKTABLE|MARKLAST|MARKFIRST|ADDRETAINTYPE|REMOVERETAINTYPE|ADDFILTERTYPE|REMOVEFILTERTYPE|COLOR|FILLOBJECT|RETAINTYPE|SETFEATURE|ASSIGN|PUTINLIST|ATTRIBUTE|FILTERTYPE|CREATE|UNMARK|TRANSFER|MARKONCE|TRIE|GATHER|EXEC|MERGE|GETLIST|REMOVEDUPLICATE|GETFEATURE|MATCHEDTEXT|CLEAR|UNMARKALL|SHIFT|CONFIGURE|DYNAMICANCHORING|GREEDYANCHORING|GET|DEL|CALL|LOG|TRIM|REPLACE|ADD|REMOVE|MARK|FILL|SPLIT)\b/, token: "ruta-action"},
				{regex: /\b(?:CONTAINS|IF|INLIST|PARTOF|TOTALCOUNT|CURRENTCOUNT|CONTEXTCOUNT|LAST|VOTE|COUNT|NEAR|REGEXP|POSITION|SCORE|ISLISTEMPTY|MOFN|AND|OR|FEATURE|PARSE|IS|BEFORE|AFTER|STARTSWITH|ENDSWITH|PARTOFNEQ|SIZE|NOT)\b/, token: "ruta-condition"},
				{regex: /\b(?:ALL|ANY|AMP|BREAK|W|NUM|PM|Document|MARKUP|SW|CW|CAP|PERIOD|NBSP|SENTENCEEND|COLON|COMMA|SEMICOLON|WS|SPACE|SPECIAL|EXCLAMATION|QUESTION)\b/, token: "ruta-basic"},
				{regex: /\b(?:WORDLIST|BOOLEANLIST|INTLIST|DOUBLELIST|FLOATLIST|STRINGLIST|TYPELIST|DECLARE|BOOLEAN|PACKAGE|TYPESYSTEM|INT|DOUBLE|FLOAT|STRING|SCRIPT|WORDTABLE|ENGINE|BLOCK|UIMAFIT|IMPORT|ANNOTATIONLIST|ANNOTATION|ACTION|CONDITION|VAR|TYPE|FROM|AS|FOREACH)\b/, token: "ruta-declaration"},
				{regex: /\b(?:true|false|null)\b/, token: "ruta-atom"},
				{regex: /0x[a-f\d]+|[-+]?(?:\.\d+|\d+\.?\d*)(?:e[-+]?\d+)?/i,
				token: "ruta-number"},
				{regex: /%.*/, token: "ruta-line-magic", sol: true},
				{regex: /\/\/.*/, token: "ruta-comment"},
				
				// A next property will cause the mode to move to a different state
				{regex: /\/\*/, token: "ruta-comment", next: "comment"},
				//{regex: /[-+\/*=<>!]+/, token: "ruta-operator"},
				// indent and dedent properties guide autoindentation
				{regex: /[\{\(]/, indent: true},
				{regex: /[\}\)]/, dedent: true},
				{regex: /[a-z$][\w$]*/, token: "ruta-variable"},
			],
			// The multi-line comment state.
			comment: [
				{regex: /.*?\*\//, token: "ruta-comment", next: "start"},
				{regex: /.*/, token: "ruta-comment"}
			],
			// no syntax coloring in util cell magic
			document: [
				{regex: /.*/, token: "ruta-variable"}
			],
			// The meta property contains global information about the mode. It
			// can contain properties like lineComment, which are supported by
			// all modes, and also directives like dontIndentStates, which are
			// specific to simple modes.
			meta: {
				dontIndentStates: ["ruta-comment"],
				lineComment: "//"
			}
		});
		
		CodeMirror.defineMIME("text/ruta", "ruta");
		// bug vatlab / sos - notebook #55
		CodeMirror.autoLoadMode = function () { };

		load_css(`
			.cm-s-ipython span.cm-ruta-cell-magic { font-weight: bold; color: #aa22ff; }
			.cm-s-ipython span.cm-ruta-line-magic { font-weight: bold; color: #aa22ff; }
			.cm-s-ipython span.cm-ruta-action { font-weight: bold; color: #000080; }
			.cm-s-ipython span.cm-ruta-condition { font-weight: bold; color: #008000; }
			.cm-s-ipython span.cm-ruta-declaration { font-weight: bold; color: #800000; }
			.cm-s-ipython span.cm-ruta-basic { font-weight: bold; color: #808080; }
			.cm-s-ipython span.cm-ruta-string { color: #2A00FF; }
			.cm-s-ipython span.cm-ruta-atom { color: #219; }
			.cm-s-ipython span.cm-ruta-number { color: #164; }
			.cm-s-ipython span.cm-ruta-variable { color: black; }
			.cm-s-ipython span.cm-ruta-comment { color: #3F7F5F; }
			.cm-s-ipython span.cm-meta { color: #FF1717; }
			.cm-s-ipython span.cm-error { color: #f00; }
			
			.cm-s-ipython .CodeMirror-activeline-background { background: #e8f2ff; }
			.cm-s-ipython .CodeMirror-matchingbracket { outline:1px solid grey; color:black !important; }
		`);
		
		load_css(`
			.annotation-viewer { display: flex; flex: 1; flex-direction: column; }
			.annotation-viewer .error { color:red; }
			.annotation-viewer .busy { cursor:wait; }
			.annotation-viewer .controls { display: flex; flex: 0; flex-direction: row; flex-wrap: wrap; }
			.annotation-viewer .controls label { white-space:nowrap; margin-left:0.2em; padding:2px 4px 2px 2px; cursor:pointer; border-radius:4px; }
			.annotation-viewer .controls button { line-height:2.5ex; }
			.annotation-viewer .cas > * { overflow:auto; border:1px solid blue; box-sizing:border-box; }
			.annotation-viewer .cas { display: flex; flex: 1; min-height: 10em; max-height: 30em; }
			.annotation-viewer .text-wrapper { display: flex; flex: 0.7; margin:0 8px 0 0; }
			.annotation-viewer .text { white-space:pre-wrap; word-wrap:break-word; background-color:white; }
			.annotation-viewer .text b { font-weight:normal; }
			.annotation-viewer .annotations { flex: 0.3; background-color:white; }
			.annotation-viewer .annotations pre { display:none; min-width:100%; margin-top:0; }
			.annotation-viewer .clearfix:after { clear: both; content: ""; display: table; }
			.annotation-viewer .hidden, .deflated { display:none; }
			.annotation-viewer .underline { text-decoration:underline; }
			.annotation-viewer .emph { background:lightgray!important; color:darkblue!important; }
		`);
		
		// The initialization of CodeMirror and of the kernel.js happen asynchronously, so it may happen
		// that the kernel.js is not yet done configuring the Ruta CodeMirror style when CodeMirror is
		// told to apply the syntax highlighting. So what we do here is schedule a slow ball which
		// checks all the code cells coming from Ruta and re-applies/re-sets the proper code style.
		setTimeout(() => {
			Jupyter.notebook.get_cells().map(function(cell) {
				try {
					if (cell.cell_type == 'code' && 
						(cell.kernel.name == 'ruta' || // Regular Jupyter notebooks 
						cell.user_highlight['base_mode'] == 'ruta') // SOS notebooks
					) { 
						cell.code_mirror.setOption('mode', 'ruta');
					}
				}
				catch (e) {
					console.error("Unable to update highlighting", e);
				}
			});
	  }, 500);
	}

	return {
    onload: onload
  };

})
