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

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.fit.internal.ReflectionUtil;
import org.apache.uima.resource.metadata.ConfigurationParameter;

/**
 *
 * @author entwicklerteam
 */
public class UimaFitUtils {

	private static final Map<String, String> JAVA_UIMA_TYPE_MAP = new HashMap<>();
	static {
		JAVA_UIMA_TYPE_MAP.put(Boolean.class.getName(), ConfigurationParameter.TYPE_BOOLEAN);
		JAVA_UIMA_TYPE_MAP.put(Float.class.getName(), ConfigurationParameter.TYPE_FLOAT);
		JAVA_UIMA_TYPE_MAP.put(Double.class.getName(), ConfigurationParameter.TYPE_FLOAT);
		JAVA_UIMA_TYPE_MAP.put(Integer.class.getName(), ConfigurationParameter.TYPE_INTEGER);
		JAVA_UIMA_TYPE_MAP.put(String.class.getName(), ConfigurationParameter.TYPE_STRING);
		JAVA_UIMA_TYPE_MAP.put("boolean", ConfigurationParameter.TYPE_BOOLEAN);
		JAVA_UIMA_TYPE_MAP.put("float", ConfigurationParameter.TYPE_FLOAT);
		JAVA_UIMA_TYPE_MAP.put("double", ConfigurationParameter.TYPE_FLOAT);
		JAVA_UIMA_TYPE_MAP.put("int", ConfigurationParameter.TYPE_INTEGER);

	}


	private UimaFitUtils() {

		super();
	}


	public static Object getParameterValue(Entry<String, List<String>> value,
			Class<?> annotatorClass) throws Exception {

		Field field = ReflectionUtil.getField(annotatorClass, value.getKey());
		String[] stringValue = value.getValue().toArray(new String[0]);
		String valueType = getConfigurationParameterType(field);
		boolean isMultiValued = isMultiValued(field);

		if (!isMultiValued) {
			if (ConfigurationParameter.TYPE_BOOLEAN.equals(valueType)) {
				return Boolean.parseBoolean(stringValue[0]);
			} else if (ConfigurationParameter.TYPE_FLOAT.equals(valueType)) {
				return Float.parseFloat(stringValue[0]);
			} else if (ConfigurationParameter.TYPE_INTEGER.equals(valueType)) {
				return Integer.parseInt(stringValue[0]);
			} else if (ConfigurationParameter.TYPE_STRING.equals(valueType)) {
				return stringValue[0];
			}
			throw new UIMA_IllegalArgumentException(
					UIMA_IllegalArgumentException.METADATA_ATTRIBUTE_TYPE_MISMATCH,
					new Object[] {
							valueType, "type" });
		}
		if (ConfigurationParameter.TYPE_BOOLEAN.equals(valueType)) {
			Boolean[] returnValues = new Boolean[stringValue.length];
			for (int i = 0; i < stringValue.length; i++) {
				returnValues[i] = Boolean.parseBoolean(stringValue[i]);
			}
			return returnValues;
		} else if (ConfigurationParameter.TYPE_FLOAT.equals(valueType)) {
			Float[] returnValues = new Float[stringValue.length];
			for (int i = 0; i < stringValue.length; i++) {
				returnValues[i] = Float.parseFloat(stringValue[i]);
			}
			return returnValues;
		} else if (ConfigurationParameter.TYPE_INTEGER.equals(valueType)) {
			Integer[] returnValues = new Integer[stringValue.length];
			for (int i = 0; i < stringValue.length; i++) {
				returnValues[i] = Integer.parseInt(stringValue[i]);
			}
			return returnValues;
		} else if (ConfigurationParameter.TYPE_STRING.equals(valueType)) {
			return stringValue;
		}
		throw new UIMA_IllegalArgumentException(
				UIMA_IllegalArgumentException.METADATA_ATTRIBUTE_TYPE_MISMATCH,
				new Object[] { valueType, "type" });

	}


	private static boolean isMultiValued(Field field) {

		Class<?> parameterClass = field.getType();
		if (parameterClass.isArray()) {
			return true;
		} else if (Collection.class.isAssignableFrom(parameterClass)) {
			return true;
		}
		return false;
	}


	private static String getConfigurationParameterType(Field field) {

		Class<?> parameterClass = field.getType();
		String parameterClassName;
		if (parameterClass.isArray()) {
			parameterClassName = parameterClass.getComponentType().getName();
		} else if (Collection.class.isAssignableFrom(parameterClass)) {
			ParameterizedType collectionType = (ParameterizedType) field.getGenericType();
			parameterClassName = ((Class<?>) (collectionType.getActualTypeArguments()[0]))
					.getName();
		} else {
			parameterClassName = parameterClass.getName();
		}
		String parameterType = JAVA_UIMA_TYPE_MAP.get(parameterClassName);
		if (parameterType == null) {
			return ConfigurationParameter.TYPE_STRING;
		}
		return parameterType;
	}
}
