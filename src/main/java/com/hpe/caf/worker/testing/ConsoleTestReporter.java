package com.hpe.caf.worker.testing;

import java.io.PrintStream;

/**
 * Created by ploch on 10/02/2016.
 */
public class ConsoleTestReporter implements TestResultsReporter {
    @Override
    public void reportResults(TestResult result) {
        System.out.println("================================================================================");
        System.out.println(" EXECUTION REPORT ");
        System.out.println("================================================================================");
        System.out.println();
        if (result.isSuccess())
        {
            System.out.println(" Completed successfully all test cases." );
        }
        else {
            System.out.println(" Did not complete successfully.");
        }
        System.out.println();
        System.out.println(" Test cases: ");
        System.out.println("================================================================================");
        System.out.println();
        for (TestCaseResult testCaseResult : result.getResults()) {
            TestCaseInfo info = testCaseResult.getTestCaseInfo();
            if (info == null) {
                System.out.println(" No details about test case. Please update test case file.");
            }
            else {
                printTestCaseInfo(System.out, info);
            }

            System.out.println("*** TEST CASE SUCCEEDED? " + testCaseResult.isSucceeded());
            System.out.println();
            System.out.println("============================================");
            System.out.println();
        }
    }

    private void printTestCaseInfo(PrintStream out, TestCaseInfo info) {
        out.println("Test case id: " + info.getTestCaseId());
        out.println("Description: " + info.getDescription());
        out.println("Associated tickets: " + info.getAssociatedTickets());
        out.println("Comments: " + info.getComments());
    }
}
