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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

public class EvaluationResult {

	public enum Outcome {
		TP, TN, FP, FN;
	}


	private boolean zeroForEmpty = true;

	private DecimalFormat decimalFormat = new DecimalFormat("0.000",
			DecimalFormatSymbols.getInstance(Locale.US));

	private int tp;
	private int tn;
	private int fp;
	private int fn;

	private final String name;


	public EvaluationResult(String name) {

		super();
		this.name = name;
	}


	public void add(int value, Outcome outcome) {

		switch (outcome) {
			case TP:
				tp += value;
				break;
			case TN:
				tn += value;
				break;
			case FP:
				fp += value;
				break;
			case FN:
				fn += value;
				break;
			default:
				throw new IllegalArgumentException(
						"Outcome '" + outcome + "' is not supported!");
		}
	}


	public void increment(Outcome outcome) {

		this.add(1, outcome);
	}


	public void add(EvaluationResult entry) {

		tp += entry.tp;
		tn += entry.tn;
		fp += entry.fp;
		fn += entry.fn;
	}


	public double getPrecision() {

		if (tp + fp == 0) {
			return zeroForEmpty ? 0 : 1;
		}
		return ((double) tp) / ((double) (tp + fp));
	}


	public double getRecall() {

		if (tp + fn == 0) {
			return zeroForEmpty ? 0 : 1;
		}
		return ((double) tp) / ((double) (tp + fn));
	}


	public double getF1Score() {

		double prec = getPrecision();
		double rec = getRecall();
		if (prec + rec == 0) {
			return 0;
		}

		return 2 * (prec * rec) / (prec + rec);
	}


	public void addAll(Collection<EvaluationResult> values) {

		for (EvaluationResult evaluationEntry : values) {
			add(evaluationEntry);
		}
	}


	public int getTP() {

		return tp;
	}


	public int getTN() {

		return tn;
	}


	public int getFP() {

		return fp;
	}


	public int getFN() {

		return fn;
	}


	public DecimalFormat getDecimalFormat() {

		return decimalFormat;
	}


	@Override
	public String toString() {

		return String.format("F1: %s, Precision: %s, Recall: %s",
				decimalFormat.format(Double.valueOf(getF1Score())),
				decimalFormat.format(Double.valueOf(getPrecision())),
				decimalFormat.format(Double.valueOf(getRecall())));

	}


	public String toCSV(String seperator) {

		List<String> values = new ArrayList<>();

		values.add(decimalFormat.format(getF1Score()));
		values.add(decimalFormat.format(getPrecision()));
		values.add(decimalFormat.format(getRecall()));
		values.add(String.valueOf(getTP()));
		values.add(String.valueOf(getFP()));
		values.add(String.valueOf(getFN()));

		return StringUtils.join(values, seperator);

	}


	public void appendHtmlRow(String docName, StringBuilder sb) {

		sb.append("<tr>");
		if (docName != null) {
			sb.append("<td>");
			sb.append(docName);
			sb.append("</td>");
		}
		sb.append("<td>");
		sb.append(name);
		sb.append("</td>");
		sb.append("<td>");
		sb.append(decimalFormat.format(getF1Score()));
		sb.append("</td>");
		sb.append("<td>");
		sb.append(decimalFormat.format(getPrecision()));
		sb.append("</td>");
		sb.append("<td>");
		sb.append(decimalFormat.format(getRecall()));
		sb.append("</td>");
		sb.append("<td>");
		sb.append(getTP());
		sb.append("</td>");
		sb.append("<td>");
		sb.append(getFP());
		sb.append("</td>");
		sb.append("<td>");
		sb.append(getFN());
		sb.append("</td>");
		sb.append("</tr>");
		sb.append("\n");
	}


	public String getName() {

		return name;
	}
}
