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
package org.apache.qpid.disttest.results;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import org.apache.qpid.disttest.controller.ResultsForAllTests;
import org.apache.qpid.disttest.results.ResultsCsvWriter;
import org.apache.qpid.disttest.results.aggregation.ITestResult;
import org.apache.qpid.disttest.results.formatting.CSVFormatter;
import org.apache.qpid.test.utils.QpidTestCase;
import org.apache.qpid.test.utils.TestFileUtils;
import org.apache.qpid.util.FileUtils;

public class ResultsCsvWriterTest extends QpidTestCase
{
    private CSVFormatter _csvFormater = mock(CSVFormatter.class);

    private File _outputDir = TestFileUtils.createTestDirectory();

    private ResultsCsvWriter _resultsFileWriter = new ResultsCsvWriter(_outputDir);

    @Override
    public void setUp()
    {
        _resultsFileWriter.setCsvFormater(_csvFormater);
    }

    public void testWriteResultsToFile()
    {
        List<ITestResult> testResult1 = mock(List.class);
        ResultsForAllTests results1 = mock(ResultsForAllTests.class);
        when(results1.getTestResults()).thenReturn(testResult1);


        List<ITestResult> testResult2 = mock(List.class);
        ResultsForAllTests results2 = mock(ResultsForAllTests.class);
        when(results2.getTestResults()).thenReturn(testResult2);

        String expectedCsvContents1 = "expected-csv-contents1";
        String expectedCsvContents2 = "expected-csv-contents2";
        String expectedSummaryFileContents = "expected-summary-file";
        when(_csvFormater.format(testResult1)).thenReturn(expectedCsvContents1);
        when(_csvFormater.format(testResult2)).thenReturn(expectedCsvContents2);

        _resultsFileWriter.begin();
        _resultsFileWriter.writeResults(results1, "config1.json");

        File resultsFile1 = new File(_outputDir, "config1.csv");
        assertEquals(expectedCsvContents1, FileUtils.readFileAsString(resultsFile1));

        _resultsFileWriter.writeResults(results2, "config2.json");

        File resultsFile2 = new File(_outputDir, "config2.csv");
        assertEquals(expectedCsvContents2, FileUtils.readFileAsString(resultsFile2));

        when(_csvFormater.format(any(List.class))).thenReturn(expectedSummaryFileContents);

        _resultsFileWriter.end();

        File summaryFile = new File(_outputDir, ResultsCsvWriter.TEST_SUMMARY_FILE_NAME);
        assertEquals(expectedSummaryFileContents, FileUtils.readFileAsString(summaryFile));
    }


}
