/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sysds.test.functions.builtin;

// import java.util.HashMap;

import org.junit.Test;
import org.apache.sysds.common.Types.ExecMode;
import org.apache.sysds.lops.LopProperties.ExecType;
import org.apache.sysds.common.Types;
import org.apache.sysds.common.Types.FileFormat;
// import org.apache.sysds.runtime.matrix.data.MatrixValue.CellIndex;
import org.apache.sysds.runtime.io.FrameWriter;
import org.apache.sysds.runtime.io.FrameWriterFactory;
import org.apache.sysds.runtime.matrix.data.FrameBlock;
import org.apache.sysds.test.AutomatedTestBase;
import org.apache.sysds.test.TestConfiguration;
import org.apache.sysds.test.TestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import java.io.IOException;

public class BuiltinCorrectTyposTest extends AutomatedTestBase 
{
	private final static String TEST_NAME = "correct_typos";
	private final static String TEST_DIR = "functions/builtin/";
	private static final String TEST_CLASS_DIR = TEST_DIR + BuiltinCorrectTyposTest.class.getSimpleName() + "/";
	
	private final static Types.ValueType[] schema = {Types.ValueType.STRING};

	private final static int numberDataPoints = 100;
	private final static int maxFrequencyErrors = 10;
	private final static boolean corruptData = true;
	// for every error number below between (1 and maxFrequencyErrors) identical errors are made
	// errors can still overlap though
	private final static int numberCharSwaps = 5;
	private final static int numberCharChanges = 5;
	private final static int numberCharAdds = 5;
	private final static int numberCharDeletions = 5;
	private final static int numberWrongCapitalizations = 10;

	private static Random generator;
	
	@Override
	public void setUp() {
		addTestConfiguration(TEST_NAME,new TestConfiguration(TEST_CLASS_DIR, TEST_NAME,new String[]{"B"})); 
	}

  @Test
  public void testCorrectTyposCP() throws IOException {
    runCorrectTyposTest(true, 0.1, 3, ExecType.CP);
  }

  // TODO: this test fails unless the new frames are printed before accessing them
  // @Test
  // public void testCorrectTyposSP() throws IOException {
    // runCorrectTyposTest(true, ExecType.SPARK);
  // }

	
	private void runCorrectTyposTest(boolean decapitalize, double frequency_threshold, 
      int distance_threshold, ExecType instType) throws IOException
	{
		ExecMode platformOld = setExecMode(instType);

    System.out.println("Begin CorrectTyposTest");
    try
    {
      loadTestConfiguration(getTestConfiguration(TEST_NAME));

      String HOME = SCRIPT_DIR + TEST_DIR;
      fullDMLScriptName = HOME + TEST_NAME + ".dml";

      fullRScriptName = HOME + TEST_NAME + ".R";
      programArgs = new String[]{
        "-nvargs", "X=" + input("X"), "Y=" + output("Y"),
        "frequency_threshold=" + frequency_threshold, 
        "distance_threshold=" + distance_threshold,
        "decapitalize=" + decapitalize};
      rCmd = "Rscript" + " " + fullRScriptName + " " + inputDir() + " " + expectedDir();

      System.out.println("Create dataset");
      FrameBlock frame = new FrameBlock(schema);
      FrameWriter writer = FrameWriterFactory.createFrameWriter(FileFormat.CSV);
      int rows = initFrameData(frame);
      int cols = 1;
      // System.out.println("Write dataset");
      // System.out.println(frame.getNumColumns());
			writer.writeFrameToHDFS(frame.slice(0, rows - 1, 0, 0, new FrameBlock()), input("X"), rows, cols);

      System.out.println("Run test");
      runTest(true, false, null, -1);
      System.out.println("DONE");

      // TestUtils.compareDMLFrameWithJavaFrame(schema, , output("Y"));
      //compare matrices
      // HashMap<CellIndex, Double> dmlfile = readDMLMatrixFromOutputDir("B");
      // HashMap<CellIndex, Double> rfile  = readRMatrixFromExpectedDir("B");
      // TestUtils.compareMatrices(dmlfile, rfile, eps, "Stat-DML", "Stat-R");
    }
    finally {
      rtplatform = platformOld;
    }
	}

	private static int initFrameData(FrameBlock frame) {
		int seed = 5;
		generator = new Random(seed);
		List<Integer> bins = new ArrayList<Integer>();
		String[] correctStrings = getCorrectData(numberDataPoints, bins);
		String[] corruptedStrings;
		if (corruptData) {
			corruptedStrings = getCorruptedData(correctStrings, bins, maxFrequencyErrors, numberCharSwaps, numberCharChanges, numberCharAdds, numberCharDeletions, numberWrongCapitalizations);
		} else {
			corruptedStrings = correctStrings;
		}
		frame.appendColumn(corruptedStrings);
		return corruptedStrings.length;
  }


	private static String[] getCorrectData(int numberDataPoints, List<Integer> bins) {
		//String[] allCountries = new String[] {"Albania", "Andorra", "Armenia", "Austria", "Azerbaijan", "Belarus", "Belgium", "Bosnia and Herzegovina", "Bulgaria", "Croatia", "Cyprus", "Czechia", "Denmark", "Estonia", "Finland", "France", "Georgia", "Germany", "Greece", "Hungary", "Iceland", "Ireland", "Italy", "Kazakhstan", "Kosovo", "Latvia", "Liechtenstein", "Lithuania", "Luxembourg", "Malta", "Moldova", "Monaco", "Montenegro", "Netherlands", "North Macedonia (formerly Macedonia)", "Norway", "Poland", "Portugal", "Romania", "Russia", "San Marino", "Serbia", "Slovakia", "Slovenia", "Spain", "Sweden", "Switzerland", "Turkey", "Ukraine", "United Kingdom (UK)", "Vatican City (Holy See)"};

		String[] allCountries = new String[] {"Austria", "Belarus", "Denmark", "Germany", "Italy", "Liechtenstein"};

		//String[] allCountries = new String[] {"A", "1"};

		List<String> chosenCountries = new ArrayList<String>();
		int remainingDataPoints = numberDataPoints;
		bins.add(0);
		for (int i = 0; i < allCountries.length-1; i++) {
			int rand = generator.nextInt((int)(remainingDataPoints/Math.sqrt(allCountries.length)));
			for (int j = 0; j < rand; j++) {
				chosenCountries.add(allCountries[i]);
			}
			remainingDataPoints -= rand;
			bins.add(numberDataPoints - remainingDataPoints);
		}
		for (int i = 0; i < remainingDataPoints; i++) {
			chosenCountries.add(allCountries[allCountries.length - 1]);
		}
		bins.add(numberDataPoints);
		String[] string_array = new String[chosenCountries.size()];
		chosenCountries.toArray(string_array);
		return string_array;
	}

	private static String[] getCorruptedData(String[] data, List<Integer> bins, int maxFrequencyErrors, int numberSwaps, int numberChanges, int numberAdds, int numberDeletions, int numberWrongCapitalizations) {
		for (int i = 0; i < numberSwaps; i++) {
			makeSwapErrors(data, bins, maxFrequencyErrors);
		}
		for (int i = 0; i < numberChanges; i++) {
			makeChangeErrors(data, bins, maxFrequencyErrors);
		}
		for (int i = 0; i < numberAdds; i++) {
			makeAddErrors(data, bins, maxFrequencyErrors);
		}
		for (int i = 0; i < numberDeletions; i++) {
			makeDeletionErrors(data, bins, maxFrequencyErrors);
		}
		for (int i = 0; i < numberWrongCapitalizations; i++) {
			makeCapitalizationErrors(data, bins, maxFrequencyErrors);
		}
		return data;
	}



	private static void makeSwapErrors(String[] data, List<Integer> bins, int maxFrequencyErrors){
		int randIndex = generator.nextInt(data.length);
		int leftBinIndex = Integer.max(0, (-Collections.binarySearch(bins, randIndex) - 2));
		int rightBinIndex = Integer.max(1, (-Collections.binarySearch(bins, randIndex) - 1));
		int leftIndex = bins.get(leftBinIndex);
		int rightIndex = bins.get(rightBinIndex) - 1;

		int charPos = generator.nextInt(data[randIndex].length() - 1) + 1;
		for (int j = 0; j < generator.nextInt(maxFrequencyErrors) + 1; j++) {
			int randErrorIndex = generator.nextInt(rightIndex + 1 - leftIndex) + leftIndex;
			String str = data[randErrorIndex];
			if (str.length() > 1 && charPos < str.length()) {
				data[randErrorIndex] = str.substring(0, charPos - 1) + str.charAt(charPos) + str.charAt(charPos - 1) + str.substring(charPos + 1);
			}
		}
	}

	private static void makeChangeErrors(String[] data, List<Integer> bins, int maxFrequencyErrors){
		int randIndex = generator.nextInt(data.length);
		int leftBinIndex = Integer.max(0, (-Collections.binarySearch(bins, randIndex) - 2));
		int rightBinIndex = Integer.max(1, (-Collections.binarySearch(bins, randIndex) - 1));
		int leftIndex = bins.get(leftBinIndex);
		int rightIndex = bins.get(rightBinIndex) - 1;

		int charPos = generator.nextInt(data[randIndex].length());
		String newChar = Character.toString((char)(generator.nextInt('z' + 1 - 'a') + 'a'));
		for (int j = 0; j < generator.nextInt(maxFrequencyErrors) + 1; j++) {
			int randErrorIndex = generator.nextInt(rightIndex + 1 - leftIndex) + leftIndex;
			String str = data[randErrorIndex];
			if (str.length() > 0 && charPos < str.length()) {
				data[randErrorIndex] =  str.substring(0, charPos) + newChar + str.substring(charPos + 1);
			}
		}
	}

	private static void makeAddErrors(String[] data, List<Integer> bins, int maxFrequencyErrors){
		int randIndex = generator.nextInt(data.length);
		int leftBinIndex = Integer.max(0, (-Collections.binarySearch(bins, randIndex) - 2));
		int rightBinIndex = Integer.max(1, (-Collections.binarySearch(bins, randIndex) - 1));
		int leftIndex = bins.get(leftBinIndex);
		int rightIndex = bins.get(rightBinIndex) - 1;

		int charPos = generator.nextInt(data[randIndex].length() + 1);
		String newChar = Character.toString((char)(generator.nextInt('z' + 1 - 'a') + 'a'));
		for (int j = 0; j < generator.nextInt(maxFrequencyErrors) + 1; j++) {
			int randErrorIndex = generator.nextInt(rightIndex + 1 - leftIndex) + leftIndex;
			String str = data[randErrorIndex];
			if (str.length() > 0 && charPos < str.length() + 1) {
				data[randErrorIndex] =  str.substring(0, charPos) + newChar + str.substring(charPos);
			}
		}
	}

	private static void makeDeletionErrors(String[] data, List<Integer> bins, int maxFrequencyErrors){
		int randIndex = generator.nextInt(data.length);
		int leftBinIndex = Integer.max(0, (-Collections.binarySearch(bins, randIndex) - 2));
		int rightBinIndex = Integer.max(1, (-Collections.binarySearch(bins, randIndex) - 1));
		int leftIndex = bins.get(leftBinIndex);
		int rightIndex = bins.get(rightBinIndex) - 1;

		int charPos = generator.nextInt(data[randIndex].length());
		for (int j = 0; j < generator.nextInt(maxFrequencyErrors) + 1; j++) {
			int randErrorIndex = generator.nextInt(rightIndex + 1 - leftIndex) + leftIndex;
			String str = data[randErrorIndex];
			if (str.length() > 1 && charPos < str.length()) {
				data[randErrorIndex] =  str.substring(0, charPos) + str.substring(charPos + 1);
			}
		}
	}

	private static void makeCapitalizationErrors(String[] data, List<Integer> bins, int maxFrequencyErrors){
		int randIndex = generator.nextInt(data.length);
		int leftBinIndex = Integer.max(0, (-Collections.binarySearch(bins, randIndex) - 2));
		int rightBinIndex = Integer.max(1, (-Collections.binarySearch(bins, randIndex) - 1));
		int leftIndex = bins.get(leftBinIndex);
		int rightIndex = bins.get(rightBinIndex) - 1;

		for (int j = 0; j < generator.nextInt(maxFrequencyErrors) + 1; j++) {
			int randErrorIndex = generator.nextInt(rightIndex + 1 - leftIndex) + leftIndex;
			String str = data[randErrorIndex];
			if (str.length() > 0) {
				if (Character.isUpperCase(str.charAt(0))) {
					data[randErrorIndex] = str.substring(0, 1).toLowerCase() + str.substring(1);
				}
				else {
					data[randErrorIndex] = str.substring(0, 1).toUpperCase() + str.substring(1);
				}
			}
		}
	}
}
