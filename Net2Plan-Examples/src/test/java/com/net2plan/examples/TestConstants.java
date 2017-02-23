package com.net2plan.examples;

/**
 * @author Jorge San Emeterio
 * @date 22-Feb-17
 */
public final class TestConstants
{
    private TestConstants() {}

    public final static String CPLEX_NOT_FOUND_ERROR = "Could not find CPLEX solver, ignoring test...";
    public final static String IPOPT_NOT_FOUND_ERROR = "Could not find IPOPT solver, ignoring test...";
    public final static String GLPK_NOT_FOUND_ERROR = "Could not find GLPK solver, ignoring test...";

    public static final String TEST_REPORT_FILE_DIRECTORY = "src/test/resources/temp/reports";
    public static final String TEST_ALGORITHM_FILE_DIRECTORY = "src/test/resources/temp/algorithms";
}
