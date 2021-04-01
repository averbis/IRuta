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

/**
 * Exception thrown by the {@link MagicsProcessor}. This is an unchecked exception because the
 * {@link MagicParser#transformLineMagics(String, Function<LineMagicParseContext, String>)} method
 * does not allow for checked exception.
 *
 * @author entwicklerteam
 */
public class MagicsProcessorException extends RuntimeException {

	private static final long serialVersionUID = 3148027870729790754L;


	public MagicsProcessorException(String message, Throwable cause) {

		super(message, cause);
	}


	public MagicsProcessorException(String message) {

		super(message);
	}


	public MagicsProcessorException(Throwable cause) {

		super(cause);
	}
}
