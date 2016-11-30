/*
 * (c) Copyright 2015-2016 Hewlett Packard Enterprise Development LP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hpe.caf.worker.testing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by ploch on 10/02/2016.
 */
public class ConsoleTestReporter implements TestResultsReporter {

    private Map<String, TestCaseReport> reportMap = new HashMap<>();

    private class TestCaseReport {

        private final String testCaseId;

        private final Set<TestCaseInfo> successful = new HashSet<>();
        private final Set<TestCaseInfo> failures = new HashSet<>();

        public TestCaseReport(String testCaseId) {
            this.testCaseId = testCaseId;
        }

        /**
         * Getter for property 'testCaseId'.
         *
         * @return Value for property 'testCaseId'.
         */
        public String getTestCaseId() {
            return testCaseId;
        }

        /**
         * Getter for property 'successful'.
         *
         * @return Value for property 'successful'.
         */
        public Set<TestCaseInfo> getSuccessful() {
            return successful;
        }

        /**
         * Getter for property 'failures'.
         *
         * @return Value for property 'failures'.
         */
        public Set<TestCaseInfo> getFailures() {
            return failures;
        }
    }


    @Override
    public void reportResults(TestResult result) {
        println("================================================================================");
        println(" EXECUTION REPORT ");
        println("================================================================================");
        println();
        if (result.isSuccess())
        {
            println(" Completed successfully all test cases." );
        }
        else {
            println(" Did not complete successfully.");
        }
        println();
        println(" Test Instances: ");
        println("================================================================================");
        println();
        int successes = 0;
        int failures = 0;
        for (TestCaseResult testCaseResult : result.getResults()) {
            TestCaseInfo info = testCaseResult.getTestCaseInfo();
            if (info == null) {
                println(" No details about test case. Please update test case file.");
            }
            else {
                TestCaseReport testCaseReport = reportMap.get(info.getTestCaseId());
                if (testCaseReport == null) {
                    testCaseReport = new TestCaseReport(info.getTestCaseId());
                    reportMap.put(info.getTestCaseId(), testCaseReport);
                }
                if (testCaseResult.isSucceeded()) {
                    testCaseReport.getSuccessful().add(info);
                }
                else {
                    testCaseReport.getFailures().add(info);
                }
                printTestCaseInfo(info);
            }

            if (testCaseResult.isSucceeded()) {
                successes++;
            }
            else {
                failures++;
            }

            println("*** TEST INSTANCE SUCCEEDED? " + testCaseResult.isSucceeded());
            println();
            println("============================================");
            println();
        }

        println("Number of tests passed:" + successes);
        println("Number of tests failed:" + failures);

        println();

        println("================================================================================");
        println(" REPORT PER TEST CASE ");
        println("================================================================================");
        println();

        int successfulTestCases = 0;
        int failedTestCases = 0;

        for (Map.Entry<String, TestCaseReport> entry : reportMap.entrySet()) {

            println("Test case id: " + entry.getKey());
            println("Number of all test instances: " + (entry.getValue().getSuccessful().size() + entry.getValue().getFailures().size()));
            println("Successful: " + entry.getValue().getSuccessful().size());
            println("Failed: " + entry.getValue().getFailures().size());
            if (entry.getValue().getFailures().size() > 0) {
                failedTestCases++;
            }
            else {
                successfulTestCases++;
            }
            println();
            println("*****************************");
            println();
            println("Successful test instances:");
            println();
            entry.getValue().getSuccessful().forEach(this::printTestInstanceInfo);
            println("*****************************");
            println();
            println("Failed test instances:");
            println();
            entry.getValue().getFailures().forEach(this::printTestInstanceInfo);
            println();
            println("=======================================");
            println();
        }

        println();

        println("================================================================================");
        println();
        println("Number of all test cases: " + reportMap.size());
        println("Number of successful test cases (all test instances passes): " + successfulTestCases);
        println("Number of failed test cases: " + failedTestCases);
        println();
        println("================================================================================");


    }

    private void printTestInstanceInfo(TestCaseInfo info) {
        println("Description: " + info.getDescription());
        println("Associated tickets: " + info.getAssociatedTickets());
        println("Comments: " + info.getComments());
        println();
    }

    private void printTestCaseInfo(TestCaseInfo info) {
        println("Test case id: " + info.getTestCaseId());
        println("Description: " + info.getDescription());
        println("Associated tickets: " + info.getAssociatedTickets());
        println("Comments: " + info.getComments());
    }

    private void println() {
        println("");
    }

    private void println(String str) {
        System.out.println(str);
    }
}
